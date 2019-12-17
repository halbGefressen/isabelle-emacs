/*  Title:      Pure/Tools/phabricator.scala
    Author:     Makarius

Support for Phabricator server, notably for Ubuntu 18.04 LTS.

See also:
  - https://www.phacility.com/phabricator
  - https://secure.phabricator.com/book/phabricator
*/

package isabelle


import scala.util.matching.Regex


object Phabricator
{
  /** defaults **/

  /* required packages */

  val packages: List[String] =
    Build_Docker.packages ::: Linux.packages :::
    List(
      // https://secure.phabricator.com/source/phabricator/browse/master/scripts/install/install_ubuntu.sh 15e6e2adea61
      "git", "mysql-server", "apache2", "libapache2-mod-php", "php", "php-mysql",
      "php-gd", "php-curl", "php-apcu", "php-cli", "php-json", "php-mbstring",
      // more packages
      "php-xml", "php-zip", "python-pygments", "ssh", "subversion",
      // mercurial build packages
      "make", "gcc", "python", "python-dev", "python-docutils", "python-pygments", "python-openssl")


  /* global system resources */

  val www_user = "www-data"

  val daemon_user = "phabricator"

  val sshd_config = Path.explode("/etc/ssh/sshd_config")


  /* installation parameters */

  val default_name = "vcs"

  def phabricator_name(name: String = "", ext: String = ""): String =
    "phabricator" + (if (name.isEmpty) "" else "-" + name) + (if (ext.isEmpty) "" else "." + ext)

  def isabelle_phabricator_name(name: String = "", ext: String = ""): String =
    "isabelle-" + phabricator_name(name = name, ext = ext)

  def default_root(name: String): Path =
    Path.explode("/var/www") + Path.basic(phabricator_name(name = name))

  def default_repo(name: String): Path = default_root(name) + Path.basic("repo")

  val default_mailers: Path = Path.explode("mailers.json")

  val default_system_port = 22
  val alternative_system_port = 222
  val default_server_port = 2222

  val standard_mercurial_source = "https://www.mercurial-scm.org/release/mercurial-5.2.1.tar.gz"



  /** global configuration **/

  val global_config = Path.explode("/etc/" + isabelle_phabricator_name(ext = "conf"))

  def global_config_script(
    init: String = "",
    body: String = "",
    exit: String = ""): String =
  {
"""#!/bin/bash
""" + (if (init.nonEmpty) "\n" + init else "") + """
{
  while { unset REPLY; read -r; test "$?" = 0 -o -n "$REPLY"; }
  do
    NAME="$(echo "$REPLY" | cut -d: -f1)"
    ROOT="$(echo "$REPLY" | cut -d: -f2)"
    {
""" + Library.prefix_lines("      ", body) + """
    } < /dev/null
  done
} < """ + File.bash_path(global_config) + "\n" +
    (if (exit.nonEmpty) "\n" + exit + "\n" else "")
  }

  sealed case class Config(name: String, root: Path)
  {
    def home: Path = root + Path.explode(phabricator_name())

    def execute(command: String): Process_Result =
      Isabelle_System.bash("bin/" + command, cwd = home.file, redirect = true).check
  }

  def read_config(): List[Config] =
  {
    if (global_config.is_file) {
      for (entry <- Library.trim_split_lines(File.read(global_config)) if entry.nonEmpty)
      yield {
        space_explode(':', entry) match {
          case List(name, root) => Config(name, Path.explode(root))
          case _ => error("Malformed config file " + global_config + "\nentry " + quote(entry))
        }
      }
    }
    else Nil
  }

  def write_config(configs: List[Config])
  {
    File.write(global_config,
      configs.map(config => config.name + ":" + config.root.implode).mkString("", "\n", "\n"))
  }

  def get_config(name: String): Config =
    read_config().find(config => config.name == name) getOrElse
      error("Bad Isabelle/Phabricator installation " + quote(name))



  /** administrative tools **/

  /* Isabelle tool wrapper */

  val isabelle_tool1 =
    Isabelle_Tool("phabricator", "invoke command-line tool within Phabricator home directory", args =>
    {
      var list = false
      var name = default_name

      val getopts =
        Getopts("""
Usage: isabelle phabricator [OPTIONS] COMMAND [ARGS...]

  Options are:
    -l           list available Phabricator installations
    -n NAME      Phabricator installation name (default: """ + quote(default_name) + """)

  Invoke a command-line tool within the home directory of the named
  Phabricator installation.
""",
          "l" -> (_ => list = true),
          "n:" -> (arg => name = arg))

      val more_args = getopts(args)
      if (more_args.isEmpty && !list) getopts.usage()

      val progress = new Console_Progress

      if (list) {
        for (config <- read_config()) {
          progress.echo("phabricator " + quote(config.name) + " root " + config.root)
        }
      }
      else {
        val config = get_config(name)
        val result = progress.bash(Bash.strings(more_args), cwd = config.home.file, echo = true)
        if (!result.ok) error("Return code: " + result.rc.toString)
      }
    })



  /** setup **/

  def user_setup(name: String, description: String, ssh_setup: Boolean = false)
  {
    if (!Linux.user_exists(name)) {
      Linux.user_add(name, description = description, system = true, ssh_setup = ssh_setup)
    }
    else if (Linux.user_description(name) != description) {
      error("User " + quote(name) + " already exists --" +
        " for Phabricator it should have the description:\n  " + quote(description))
    }
  }

  def command_setup(name: String,
    init: String = "",
    body: String = "",
    exit: String = ""): Path =
  {
    val command = Path.explode("/usr/local/bin") + Path.basic(name)
    File.write(command, global_config_script(init = init, body = body, exit = exit))
    Isabelle_System.chmod("755", command)
    Isabelle_System.chown("root:root", command)
    command
  }

  def mercurial_setup(mercurial_source: String, progress: Progress = No_Progress)
  {
    progress.echo("\nMercurial installation from source " + quote(mercurial_source) + " ...")
    Isabelle_System.with_tmp_dir("mercurial")(tmp_dir =>
    {
      val archive =
        if (Url.is_wellformed(mercurial_source)) {
          val archive = tmp_dir + Path.basic("mercurial.tar.gz")
          Bytes.write(archive, Url.read_bytes(Url(mercurial_source)))
          archive
        }
        else Path.explode(mercurial_source)

      Isabelle_System.gnutar("-xzf " + File.bash_path(archive), dir = tmp_dir).check

      File.read_dir(tmp_dir).filter(name => (tmp_dir + Path.basic(name)).is_dir) match {
        case List(dir) =>
          val build_dir = tmp_dir + Path.basic(dir)
          progress.bash("make all && make install", cwd = build_dir.file, echo = true).check
        case dirs =>
          error("Bad archive " + archive +
            (if (dirs.isEmpty) "" else "\nmultiple directory entries " + commas_quote(dirs)))
      }
    })
  }

  def phabricator_setup(
    name: String = default_name,
    root: String = "",
    repo: String = "",
    package_update: Boolean = false,
    mercurial_source: String = "",
    progress: Progress = No_Progress)
  {
    /* system environment */

    Linux.check_system_root()

    progress.echo("System packages ...")

    if (package_update) {
      Linux.package_update(progress = progress)
      Linux.check_reboot_required()
    }

    Linux.package_install(packages, progress = progress)
    Linux.check_reboot_required()


    if (mercurial_source.nonEmpty) {
      for { name <- List("mercurial", "mercurial-common") if Linux.package_installed(name) } {
        error("Cannot install Mercurial from source:" +
          "package package " + quote(name) + " already installed")
      }
      mercurial_setup(mercurial_source, progress = progress)
    }


    /* users */

    if (name.contains((c: Char) => !(Symbol.is_ascii_letter(c) || Symbol.is_ascii_digit(c))) ||
        Set("", "ssh", "phd", "dump", daemon_user).contains(name)) {
      error("Bad installation name: " + quote(name))
    }

    user_setup(daemon_user, "Phabricator Daemon User", ssh_setup = true)
    user_setup(name, "Phabricator SSH User")


    /* basic installation */

    progress.echo("\nPhabricator installation ...")

    val root_path = if (root.nonEmpty) Path.explode(root) else default_root(name)
    val repo_path = if (repo.nonEmpty) Path.explode(repo) else default_repo(name)

    val configs = read_config()

    for (config <- configs if config.name == name) {
      error("Duplicate Phabricator installation " + quote(name) + " in " + config.root)
    }

    if (!Isabelle_System.bash("mkdir -p " + File.bash_path(root_path)).ok) {
      error("Failed to create root directory " + root_path)
    }

    Isabelle_System.chown(Bash.string(www_user) + ":" + Bash.string(www_user), root_path)
    Isabelle_System.chmod("755", root_path)

    progress.bash(cwd = root_path.file, echo = true,
      script = """
        set -e
        echo "Cloning distribution repositories:"

        git clone --branch stable https://github.com/phacility/libphutil.git
        git -C libphutil reset --hard 1750586fdc50a6cd98adba4aa2f5a7649bd91dbe

        git clone --branch stable https://github.com/phacility/arcanist.git
        git -C arcanist reset --hard bac2028421a4be6e34e08764bbbda49e68b3a604

        git clone --branch stable https://github.com/phacility/phabricator.git
        git -C phabricator reset --hard c4b4a53cad7722f031b725f8b41511e9d341d033
      """).check

    val config = Config(name, root_path)
    write_config(configs ::: List(config))

    config.execute("config set pygments.enabled true")


    /* local repository directory */

    progress.echo("\nRepository hosting setup ...")

    if (!Isabelle_System.bash("mkdir -p " + File.bash_path(repo_path)).ok) {
      error("Failed to create local repository directory " + repo_path)
    }

    Isabelle_System.chown(
      "-R " + Bash.string(daemon_user) + ":" + Bash.string(daemon_user), repo_path)
    Isabelle_System.chmod("755", repo_path)

    config.execute("config set repository.default-local-path " + File.bash_path(repo_path))


    val sudoers_file =
      Path.explode("/etc/sudoers.d") + Path.basic(isabelle_phabricator_name(name = name))
    File.write(sudoers_file,
      www_user + " ALL=(" + daemon_user + ") SETENV: NOPASSWD: /usr/bin/git, /usr/local/bin/hg, /usr/bin/hg, /usr/bin/ssh, /usr/bin/id\n" +
      name + " ALL=(" + daemon_user + ") SETENV: NOPASSWD: /usr/bin/git, /usr/bin/git-upload-pack, /usr/bin/git-receive-pack, /usr/local/bin/hg, /usr/bin/hg, /usr/bin/svnserve, /usr/bin/ssh, /usr/bin/id\n")

    Isabelle_System.chmod("440", sudoers_file)

    config.execute("config set diffusion.ssh-user " + Bash.string(config.name))


    /* MySQL setup */

    progress.echo("\nMySQL setup ...")

    File.write(Path.explode("/etc/mysql/mysql.conf.d/" + phabricator_name(ext = "cnf")),
"""[mysqld]
max_allowed_packet = 32M
innodb_buffer_pool_size = 1600M
local_infile = 0
""")

    Linux.service_restart("mysql")


    def mysql_conf(R: Regex, which: String): String =
    {
      val conf = Path.explode("/etc/mysql/debian.cnf")
      split_lines(File.read(conf)).collectFirst({ case R(a) => a }) match {
        case Some(res) => res
        case None => error("Cannot determine " + which + " from " + conf)
      }
    }

    val mysql_root_user = mysql_conf("""^user\s*=\s*(\S*)\s*$""".r, "superuser name")
    val mysql_root_password = mysql_conf("""^password\s*=\s*(\S*)\s*$""".r, "superuser password")

    val mysql_name = phabricator_name(name = name).replace("-", "_")
    val mysql_user_string = SQL.string(mysql_name) + "@'localhost'"
    val mysql_password = Linux.generate_password()

    Isabelle_System.bash("mysql --user=" + Bash.string(mysql_root_user) +
      " --password=" + Bash.string(mysql_root_password) + " --execute=" +
      Bash.string(
        """DROP USER IF EXISTS """ + mysql_user_string + "; " +
        """CREATE USER """ + mysql_user_string +
        """ IDENTIFIED BY """ + SQL.string(mysql_password) + """ PASSWORD EXPIRE NEVER; """ +
        """GRANT ALL ON `""" + (mysql_name + "_%").replace("_", "\\_") +
        """`.* TO """ + mysql_user_string + ";")).check

    config.execute("config set mysql.user " + Bash.string(mysql_name))
    config.execute("config set mysql.pass " + Bash.string(mysql_password))

    config.execute("config set phabricator.cache-namespace " + Bash.string(mysql_name))
    config.execute("config set storage.default-namespace " + Bash.string(mysql_name))
    config.execute("config set storage.mysql-engine.max-size 8388608")

    progress.bash("bin/storage upgrade --force", cwd = config.home.file, echo = true).check


    /* database dump */

    val dump_name = isabelle_phabricator_name(name = "dump")
    command_setup(dump_name, body =
"""mkdir -p "$ROOT/database" && chown root:root "$ROOT/database" && chmod 700 "$ROOT/database"
[ -e "$ROOT/database/dump.sql.gz" ] && mv -f "$ROOT/database/dump.sql.gz" "$ROOT/database/dump-old.sql.gz"
echo "Creating $ROOT/database/dump.sql.gz"
"$ROOT/phabricator/bin/storage" dump --compress --output "$ROOT/database/dump.sql.gz" 2>&1 | fgrep -v '[Warning] Using a password on the command line interface can be insecure' """)


    /* Phabricator upgrade */

    command_setup(isabelle_phabricator_name(name = "upgrade"),
      init =
"""BRANCH="${1:-stable}"
if [ "$BRANCH" != "master" -a "$BRANCH" != "stable" ]
then
  echo "Bad branch: \"$BRANCH\""
  exit 1
fi

systemctl stop isabelle-phabricator-phd
systemctl stop apache2
""",
      body =
"""echo -e "\nUpgrading phabricator \"$NAME\" root \"$ROOT\" ..."
for REPO in libphutil arcanist phabricator
do
  cd "$ROOT/$REPO"
  echo -e "\nUpdating \"$REPO\" ..."
  git checkout "$BRANCH"
  git pull
done
echo -e "\nUpgrading storage ..."
"$ROOT/phabricator/bin/storage" upgrade --force
""",
      exit =
"""systemctl start apache2
systemctl start isabelle-phabricator-phd""")


    /* PHP setup */

    val php_version =
      Isabelle_System.bash("""php --run 'echo PHP_MAJOR_VERSION . "." . PHP_MINOR_VERSION;'""")
        .check.out

    val php_conf =
      Path.explode("/etc/php") + Path.basic(php_version) +  // educated guess
        Path.explode("apache2/conf.d") +
        Path.basic(isabelle_phabricator_name(ext = "ini"))

    File.write(php_conf,
      "post_max_size = 32M\n" +
      "opcache.validate_timestamps = 0\n" +
      "memory_limit = 512M\n" +
      "max_execution_time = 120\n")


    /* Apache setup */

    progress.echo("Apache setup ...")

    val apache_root = Path.explode("/etc/apache2")
    val apache_sites = apache_root + Path.explode("sites-available")

    if (!apache_sites.is_dir) error("Bad Apache sites directory " + apache_sites)

    val server_name = phabricator_name(name = name, ext = "lvh.me")  // alias for "localhost" for testing
    val server_url = "http://" + server_name

    File.write(apache_sites + Path.basic(isabelle_phabricator_name(name = name, ext = "conf")),
"""<VirtualHost *:80>
    ServerName """ + server_name + """
    ServerAdmin webmaster@localhost
    DocumentRoot """ + config.home.implode + """/webroot

    ErrorLog ${APACHE_LOG_DIR}/error.log
    RewriteEngine on
    RewriteRule ^(.*)$  /index.php?__path__=$1  [B,L,QSA]
</VirtualHost>

# vim: syntax=apache ts=4 sw=4 sts=4 sr noet
""")

    Isabelle_System.bash( """
      set -e
      a2enmod rewrite
      a2ensite """ + Bash.string(isabelle_phabricator_name(name = name))).check

    config.execute("config set phabricator.base-uri " + Bash.string(server_url))

    Linux.service_restart("apache2")

    progress.echo("\nWeb configuration via " + server_url)


    /* PHP daemon */

    progress.echo("\nPHP daemon setup ...")

    val phd_log_path = Path.explode("/var/tmp/phd")
    Isabelle_System.mkdirs(phd_log_path)
    Isabelle_System.chown(
      "-R " + Bash.string(daemon_user) + ":" + Bash.string(daemon_user), phd_log_path)
    Isabelle_System.chmod("755", phd_log_path)

    config.execute("config set phd.user " + Bash.string(daemon_user))
    config.execute("config set phd.log-directory /var/tmp/phd/" +
      isabelle_phabricator_name(name = name) + "/log")

    val phd_name = isabelle_phabricator_name(name = "phd")
    Linux.service_shutdown(phd_name)
    val phd_command = command_setup(phd_name, body = """"$ROOT/phabricator/bin/phd" "$@" """)
    try {
      Linux.service_install(phd_name,
"""[Unit]
Description=PHP daemon manager for Isabelle/Phabricator
After=syslog.target network.target apache2.service mysql.service

[Service]
Type=oneshot
User=""" + daemon_user + """
Group=""" + daemon_user + """
Environment=PATH=/sbin:/usr/sbin:/usr/local/sbin:/usr/local/bin:/usr/bin:/bin
ExecStart=""" + phd_command.implode + """ start --force
ExecStop=""" + phd_command.implode + """ stop
RemainAfterExit=yes

[Install]
WantedBy=multi-user.target
""")
    }
    catch {
      case ERROR(msg) =>
        progress.bash("bin/phd status", cwd = config.home.file, echo = true).check
        error(msg)
    }
  }


  /* Isabelle tool wrapper */

  val isabelle_tool2 =
    Isabelle_Tool("phabricator_setup", "setup Phabricator server on Ubuntu Linux", args =>
    {
      var mercurial_source = ""
      var repo = ""
      var package_update = false
      var name = default_name
      var root = ""

      val getopts =
        Getopts("""
Usage: isabelle phabricator_setup [OPTIONS]

  Options are:
    -M SOURCE    install Mercurial from source: local PATH, or URL, or ":" for
                 """ + standard_mercurial_source + """
    -R DIR       repository directory (default: """ + default_repo("NAME") + """)
    -U           full update of system packages before installation
    -n NAME      Phabricator installation name (default: """ + quote(default_name) + """)
    -r DIR       installation root directory (default: """ + default_root("NAME") + """)

  Install Phabricator as LAMP application (Linux, Apache, MySQL, PHP).

  The installation name (default: """ + quote(default_name) + """) is mapped to a regular
  Unix user; this is relevant for public SSH access.
""",
          "M:" -> (arg => mercurial_source = (if (arg == ":") standard_mercurial_source else arg)),
          "R:" -> (arg => repo = arg),
          "U" -> (_ => package_update = true),
          "n:" -> (arg => name = arg),
          "r:" -> (arg => root = arg))

      val more_args = getopts(args)
      if (more_args.nonEmpty) getopts.usage()

      val progress = new Console_Progress

      val release = Linux.Release()
      if (!release.is_ubuntu_18_04) error("Bad Linux version: Ubuntu 18.04 LTS required")

      phabricator_setup(name = name, root = root, repo = repo,
        package_update = package_update, mercurial_source = mercurial_source, progress = progress)
    })



  /** setup mail **/

  val mailers_template: String =
"""[
  {
    "key": "example.org",
    "type": "smtp",
    "options": {
      "host": "mail.example.org",
      "port": 465,
      "user": "phabricator@example.org",
      "password": "********",
      "protocol": "ssl",
      "message-id": true
    }
  }
]"""

  def phabricator_setup_mail(
    name: String = default_name,
    config_file: Option[Path] = None,
    test_user: String = "",
    progress: Progress = No_Progress)
  {
    Linux.check_system_root()

    val config = get_config(name)
    val default_config_file = config.root + default_mailers

    val mail_config = config_file getOrElse default_config_file

    def setup_mail
    {
      progress.echo("Using mail configuration from " + mail_config)
      config.execute("config set cluster.mailers --stdin < " + File.bash_path(mail_config))

      if (test_user.nonEmpty) {
        progress.echo("Sending test mail to " + quote(test_user))
        progress.bash(cwd = config.home.file, echo = true,
          script = """echo "Test from Phabricator ($(date))" | bin/mail send-test --subject "Test" --to """ +
            Bash.string(test_user)).check
      }
    }

    if (config_file.isEmpty) {
      if (!default_config_file.is_file) {
        File.write(default_config_file, mailers_template)
        Isabelle_System.chmod("600", default_config_file)
      }
      if (File.read(default_config_file) == mailers_template) {
        progress.echo("Please invoke the tool again, after providing details in\n  " +
          default_config_file.implode + "\n")
      }
      else setup_mail
    }
    else setup_mail
  }


  /* Isabelle tool wrapper */

  val isabelle_tool3 =
    Isabelle_Tool("phabricator_setup_mail",
      "setup mail for one Phabricator installation", args =>
    {
      var test_user = ""
      var name = default_name
      var config_file: Option[Path] = None

      val getopts =
        Getopts("""
Usage: isabelle phabricator_setup_mail [OPTIONS]

  Options are:
    -T USER      send test mail to Phabricator user
    -f FILE      config file (default: """ + default_mailers + """ within Phabricator root)
    -n NAME      Phabricator installation name (default: """ + quote(default_name) + """)

  Provide mail configuration for existing Phabricator installation.
""",
          "T:" -> (arg => test_user = arg),
          "f:" -> (arg => config_file = Some(Path.explode(arg))),
          "n:" -> (arg => name = arg))

      val more_args = getopts(args)
      if (more_args.nonEmpty) getopts.usage()

      val progress = new Console_Progress

      phabricator_setup_mail(name = name, config_file = config_file,
        test_user = test_user, progress = progress)
    })



  /** setup ssh **/

  /* sshd config */

  private val Port = """^\s*Port\s+(\d+)\s*$""".r
  private val No_Port = """^#\s*Port\b.*$""".r
  private val Any_Port = """^#?\s*Port\b.*$""".r

  def conf_ssh_port(port: Int): String =
    if (port == 22) "#Port 22" else "Port " + port

  def read_ssh_port(conf: Path): Int =
  {
    val lines = split_lines(File.read(conf))
    val ports =
      lines.flatMap({
        case Port(Value.Int(p)) => Some(p)
        case No_Port() => Some(22)
        case _ => None
      })
    ports match {
      case List(port) => port
      case Nil => error("Missing Port specification in " + conf)
      case _ => error("Multiple Port specifications in " + conf)
    }
  }

  def write_ssh_port(conf: Path, port: Int): Boolean =
  {
    val old_port = read_ssh_port(conf)
    if (old_port == port) false
    else {
      val lines = split_lines(File.read(conf))
      val lines1 = lines.map({ case Any_Port() => conf_ssh_port(port) case line => line })
      File.write(conf, cat_lines(lines1))
      true
    }
  }


  /* phabricator_setup_ssh */

  def phabricator_setup_ssh(
    server_port: Int = default_server_port,
    system_port: Int = default_system_port,
    progress: Progress = No_Progress)
  {
    Linux.check_system_root()

    val configs = read_config()

    if (server_port == system_port) {
      error("Port for Phabricator sshd coincides with system port: " + system_port)
    }

    val sshd_conf_system = Path.explode("/etc/ssh/sshd_config")
    val sshd_conf_server = sshd_conf_system.ext(isabelle_phabricator_name())

    val ssh_name = isabelle_phabricator_name(name = "ssh")
    Linux.service_shutdown(ssh_name)

    val old_system_port = read_ssh_port(sshd_conf_system)
    if (old_system_port != system_port) {
      progress.echo("Reconfigurig system ssh service")
      Linux.service_shutdown("ssh")
      write_ssh_port(sshd_conf_system, system_port)
      Linux.service_start("ssh")
    }

    progress.echo("Configuring " + ssh_name + " service")

    val ssh_command = command_setup(ssh_name, body =
"""if [ "$1" = "$NAME" ]
then
  exec "$ROOT/phabricator/bin/ssh-auth" "$@"
fi""", exit = "exit 1")

    File.write(sshd_conf_server,
"""# OpenBSD Secure Shell server for Isabelle/Phabricator
AuthorizedKeysCommand """ + ssh_command.implode + """
AuthorizedKeysCommandUser """ + daemon_user + """
AuthorizedKeysFile none
AllowUsers """ + configs.map(_.name).mkString(" ") + """
Port """ + server_port + """
Protocol 2
PermitRootLogin no
AllowAgentForwarding no
AllowTcpForwarding no
PrintMotd no
PrintLastLog no
PasswordAuthentication no
ChallengeResponseAuthentication no
PidFile /var/run/""" + ssh_name + """.pid
""")

    Linux.service_install(ssh_name,
"""[Unit]
Description=OpenBSD Secure Shell server for Isabelle/Phabricator
After=network.target auditd.service
ConditionPathExists=!/etc/ssh/sshd_not_to_be_run

[Service]
EnvironmentFile=-/etc/default/ssh
ExecStartPre=/usr/sbin/sshd -f """ + sshd_conf_server.implode + """ -t
ExecStart=/usr/sbin/sshd -f """ + sshd_conf_server.implode + """ -D $SSHD_OPTS
ExecReload=/usr/sbin/sshd -f """ + sshd_conf_server.implode + """ -t
ExecReload=/bin/kill -HUP $MAINPID
KillMode=process
Restart=on-failure
RestartPreventExitStatus=255
Type=notify
RuntimeDirectory=sshd-phabricator
RuntimeDirectoryMode=0755

[Install]
WantedBy=multi-user.target
Alias=""" + ssh_name + """.service
""")

    for (config <- configs) {
      progress.echo("phabricator " + quote(config.name) + " port " +  server_port)
      config.execute("config set diffusion.ssh-port " + Bash.string(server_port.toString))
      if (server_port == 22) config.execute("config delete diffusion.ssh-port")
    }
  }


  /* Isabelle tool wrapper */

  val isabelle_tool4 =
    Isabelle_Tool("phabricator_setup_ssh",
      "setup ssh service for all Phabricator installations", args =>
    {
      var server_port = default_server_port
      var system_port = default_system_port

      val getopts =
        Getopts("""
Usage: isabelle phabricator_setup_ssh [OPTIONS]

  Options are:
    -p PORT      sshd port for Phabricator servers (default: """ + default_server_port + """)
    -q PORT      sshd port for the operating system (default: """ + default_system_port + """)

  Configure ssh service for all Phabricator installations: a separate sshd
  is run in addition to the one of the operating system, and ports need to
  be distinct.

  A particular Phabricator installation is addressed by using its
  name as the ssh user; the actual Phabricator user is determined via
  stored ssh keys.
""",
          "p:" -> (arg => server_port = Value.Int.parse(arg)),
          "q:" -> (arg => system_port = Value.Int.parse(arg)))

      val more_args = getopts(args)
      if (more_args.nonEmpty) getopts.usage()

      val progress = new Console_Progress

      phabricator_setup_ssh(
        server_port = server_port, system_port = system_port, progress = progress)
    })



  /** conduit API **/

  object Conduit
  {
    def apply(ssh_host: String, ssh_user: String, ssh_port: Int = 22): Conduit =
      new Conduit(ssh_host, ssh_user, ssh_port)
  }

  final class Conduit private(ssh_host: String, ssh_user: String, ssh_port: Int)
  {
    /* connection */

    require(ssh_host.nonEmpty && ssh_port >= 0)

    private def ssh_user_prefix: String = if (ssh_user.isEmpty) "" else ssh_user + "@"
    private def ssh_port_suffix: String = if (ssh_port == 22) "" else ":" + ssh_port

    override def toString: String = ssh_user_prefix + ssh_host + ssh_port_suffix


    /* execute methods */

    def execute(
      method: String,
      args: List[String] = Nil,
      input: JSON.T = JSON.Object.empty): JSON.T =
    {
      Isabelle_System.with_tmp_file("input", "json")(input_file =>
      {
        File.write(input_file, JSON.Format(JSON.Object("params" -> JSON.Format(input))))
        val result =
          Isabelle_System.bash(
            "ssh -p " + ssh_port + " " + Bash.string(ssh_user_prefix + ssh_host) +
            " conduit " + Bash.strings(method :: args) + " < " + File.bash_path(input_file)).check
        JSON.parse(result.out, strict = false)
      })
    }

    def execute_result(
      method: String,
      args: List[String] = Nil,
      input: JSON.T = JSON.Object.empty): API.Result =
    {
      API.make_result(execute(method, args = args, input = input))
    }


    /* concrete methods */

    def ping(): String = execute_result("conduit.ping").get_string

    lazy val user_phid: String = execute_result("user.whoami").get_value(JSON.string(_, "phid"))
    lazy val user_name: String = execute_result("user.whoami").get_value(JSON.string(_, "userName"))
  }

  object API
  {
    /* result with optional error */

    object Error_Code extends Enumeration
    {
      val bad_diff = Value("ERR-BAD-DIFF")
      val bad_document = Value("ERR-BAD-DOCUMENT")
      val bad_file = Value("ERR-BAD-FILE")
      val bad_phid = Value("ERR-BAD-PHID")
      val bad_revision = Value("ERR-BAD-REVISION")
      val bad_task = Value("ERR-BAD-TASK")
      val bad_token = Value("ERR-BAD-TOKEN")
      val bad_version = Value("ERR-BAD-VERSION")
      val conduit_call = Value("ERR-CONDUIT-CALL")
      val conduit_core = Value("ERR-CONDUIT-CORE")
      val invalid_auth = Value("ERR-INVALID-AUTH")
      val invalid_certificate = Value("ERR-INVALID-CERTIFICATE")
      val invalid_engine_ = Value("ERR-INVALID_ENGINE")  // FIXME !?
      val invalid_engine = Value("ERR-INVALID-ENGINE")
      val invalid_parameter = Value("ERR-INVALID-PARAMETER")
      val invalid_session = Value("ERR-INVALID-SESSION")
      val invalid_token = Value("ERR-INVALID-TOKEN")
      val invalid_usage = Value("ERR-INVALID-USAGE")
      val invalid_user = Value("ERR-INVALID-USER")
      val need_diff = Value("ERR-NEED-DIFF")
      val need_file = Value("ERR-NEED-FILE")
      val no_certificate = Value("ERR-NO-CERTIFICATE")
      val no_content = Value("ERR-NO-CONTENT")
      val no_effect = Value("ERR-NO-EFFECT")
      val no_paste = Value("ERR-NO-PASTE")
      val not_found = Value("ERR-NOT-FOUND")
      val not_pusher = Value("ERR-NOT-PUSHER")
      val oauth_access = Value("ERR-OAUTH-ACCESS")
      val permissions = Value("ERR-PERMISSIONS")
      val rate_limit = Value("ERR-RATE-LIMIT")
      val unknown_client = Value("ERR-UNKNOWN-CLIENT")
      val unknown_repository = Value("ERR-UNKNOWN-REPOSITORY")
      val unknown_type = Value("ERR-UNKNOWN-TYPE")
      val unknown_vcs_type = Value("ERR-UNKNOWN-VCS-TYPE")
      val unsupported_vcs = Value("ERR-UNSUPPORTED-VCS")

      val unknown_error = Value("ERR-UNKNOWN-ERROR")
    }

    sealed case class Result(
      result: JSON.T,
      error_code: Option[Error_Code.Value],
      error_info: String)
    {
      def ok: Boolean = error_code.isEmpty && error_info.isEmpty

      def get: JSON.T =
        if (error_info.nonEmpty) error(error_info)
        else if (error_code.nonEmpty) error(error_code.get.toString)
        else result

      def get_value[A](unapply: JSON.T => Option[A]): A =
        unapply(get) getOrElse error("Bad JSON result: " + JSON.Format(result))

      def get_string: String = get_value(JSON.Value.String.unapply)
    }

    def make_error_code(str: String): Error_Code.Value =
      try { Error_Code.withName(str) }
      catch { case _: java.util.NoSuchElementException => Error_Code.unknown_error }

    def make_result(json: JSON.T): Result =
      Result(
        JSON.value(json, "result").getOrElse(JSON.Object.empty),
        JSON.string(json, "error_code").map(make_error_code),
        JSON.string(json, "error_info").getOrElse(""))


    /* repository operations */

    object VCS extends Enumeration
    {
      val hg, git, svn = Value
    }

    def edit(typ: String, value: JSON.T): JSON.Object.T =
      JSON.Object("type" -> typ, "value" -> value)

    def edits(typ: String, value: Option[JSON.T]): List[JSON.Object.T] =
      List(edit(typ, value))

    def opt_edits(typ: String, value: Option[JSON.T]): List[JSON.Object.T] =
      value.map(edit(typ, _)).toList
  }
}
