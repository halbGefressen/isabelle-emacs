/*  Title:      Pure/General/mailman.scala
    Author:     Makarius

Support for Mailman list servers.
*/

package isabelle


import java.net.URL


object Mailman
{
  /* mailing list archives */

  def archive(url: URL, name: String = ""): Archive =
  {
    val html = Url.read(url)
    val title =
      """<title>The ([^</>]*) Archives</title>""".r.findFirstMatchIn(html).map(_.group(1))
    val hrefs_text =
      """href="([^"]+\.txt(?:\.gz)?)"""".r.findAllMatchIn(html).map(_.group(1)).toList

    val list_url =
      Url(Library.take_suffix[Char](_ == '/', Url.trim_index(url).toString.toList)._1.mkString + "/")
    val list_name =
      (proper_string(name) orElse title).getOrElse(error("Failed to determine mailing list name"))
    new Archive(list_url, list_name, hrefs_text)
  }

  class Archive private[Mailman](val list_url: URL, val list_name: String, hrefs_text: List[String])
  {
    override def toString: String = list_name

    def download_text(target_dir: Path, progress: Progress = new Progress): List[Path] =
    {
      val dir = target_dir + Path.basic(list_name)
      Isabelle_System.make_directory(dir)

      hrefs_text.flatMap(name =>
        {
          val path = dir + Path.basic(name)
          val url = new URL(list_url, name)
          val connection = url.openConnection
          try {
            val length = connection.getContentLengthLong
            val timestamp = connection.getLastModified
            val file = path.file
            if (file.isFile && file.length == length && file.lastModified == timestamp) None
            else {
              progress.echo("Getting " + url)
              val bytes =
                using(connection.getInputStream)(Bytes.read_stream(_, hint = length.toInt max 1024))
              Bytes.write(file, bytes)
              file.setLastModified(timestamp)
              Some(path)
            }
          }
          finally { connection.getInputStream.close() }
        })
    }
  }


  /* Isabelle mailing lists */

  def isabelle_users: Archive =
    archive(Url("https://lists.cam.ac.uk/pipermail/cl-isabelle-users"), name = "isabelle-users")

  def isabelle_dev: Archive =
    archive(Url("https://mailmanbroy.in.tum.de/pipermail/isabelle-dev"))
}
