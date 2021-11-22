/*  Title:      Pure/Thy/latex.scala
    Author:     Makarius

Support for LaTeX.
*/

package isabelle


import java.io.{File => JFile}

import scala.annotation.tailrec
import scala.collection.mutable
import scala.util.matching.Regex


object Latex
{
  /* output name for LaTeX macros */

  private val output_name_map: Map[Char, String] =
    Map('_' -> "UNDERSCORE",
      '\'' -> "PRIME",
      '0' -> "ZERO",
      '1' -> "ONE",
      '2' -> "TWO",
      '3' -> "THREE",
      '4' -> "FOUR",
      '5' -> "FIVE",
      '6' -> "SIX",
      '7' -> "SEVEN",
      '8' -> "EIGHT",
      '9' -> "NINE")

  def output_name(name: String): String =
    if (name.exists(output_name_map.keySet)) {
      val res = new StringBuilder
      for (c <- name) {
        output_name_map.get(c) match {
          case None => res += c
          case Some(s) => res ++= s
        }
      }
      res.toString
    }
    else name


  /* index entries */

  def index_escape(str: String): String =
  {
    val special1 = "!\"@|"
    val special2 = "\\{}#"
    if (str.exists(c => special1.contains(c) || special2.contains(c))) {
      val res = new StringBuilder
      for (c <- str) {
        if (special1.contains(c)) {
          res ++= "\\char"
          res ++= Value.Int(c)
        }
        else {
          if (special2.contains(c)) { res += '"'}
          res += c
        }
      }
      res.toString
    }
    else str
  }

  object Index_Item
  {
    sealed case class Value(text: Text, like: String)
    def unapply(tree: XML.Tree): Option[Value] =
      tree match {
        case XML.Wrapped_Elem(Markup.Latex_Index_Item(_), text, like) =>
          Some(Value(text, XML.content(like)))
        case _ => None
      }
  }

  object Index_Entry
  {
    sealed case class Value(items: List[Index_Item.Value], kind: String)
    def unapply(tree: XML.Tree): Option[Value] =
      tree match {
        case XML.Elem(Markup.Latex_Index_Entry(kind), body) =>
          val items = body.map(Index_Item.unapply)
          if (items.forall(_.isDefined)) Some(Value(items.map(_.get), kind)) else None
        case _ => None
      }
  }


  /* output text and positions */

  type Text = XML.Body

  def position(a: String, b: String): String = "%:%" + a + "=" + b + "%:%\n"

  def init_position(file_pos: String): List[String] =
    if (file_pos.isEmpty) Nil
    else List("\\endinput\n", position(Markup.FILE, file_pos))

  class Output(options: Options)
  {
    def latex_output(latex_text: Text): String = apply(latex_text)

    def latex_macro0(name: String): Text =
      XML.string("\\" + name)

    def latex_macro(name: String, body: Text): Text =
      XML.enclose("\\" + name + "{", "}", body)

    def latex_environment(name: String, body: Text): Text =
      XML.enclose("%\n\\begin{" + name + "}%\n", "%\n\\end{" + name + "}", body)

    def latex_heading(kind: String, pos: Position.T, body: Text): Text =
      XML.enclose("%\n\\" + options.string("document_heading_prefix") + kind + "{", "%\n}\n", body)

    def latex_body(kind: String, pos: Position.T, body: Text): Text =
      latex_environment("isamarkup" + kind, body)

    def latex_delim(name: String, body: Text): Text =
    {
      val s = output_name(name)
      XML.enclose("%\n\\isadelim" + s + "\n", "%\n\\endisadelim" + s + "\n", body)
    }

    def latex_tag(name: String, body: Text): Text =
    {
      val s = output_name(name)
      XML.enclose("%\n\\isatag" + s + "\n", "%\n\\endisatag" + s + "\n{\\isafold" + s + "}%\n", body)
    }

    def index_item(item: Index_Item.Value): String =
    {
      val like = if (item.like.isEmpty) "" else index_escape(item.like) + "@"
      val text = index_escape(latex_output(item.text))
      like + text
    }

    def index_entry(entry: Index_Entry.Value): Text =
    {
      val items = entry.items.map(index_item).mkString("!")
      val kind = if (entry.kind.isEmpty) "" else "|" + index_escape(entry.kind)
      latex_macro("index", XML.string(items + kind))
    }


    /* standard output of text with per-line positions */

    def apply(latex_text: Text, file_pos: String = ""): String =
    {
      var line = 1
      val result = new mutable.ListBuffer[String]
      val positions = new mutable.ListBuffer[String] ++= init_position(file_pos)

      def traverse(body: XML.Body): Unit =
      {
        body.foreach {
          case XML.Text(s) =>
            line += s.count(_ == '\n')
            result += s
          case XML.Elem(Markup.Document_Latex(props), body) =>
            for { l <- Position.Line.unapply(props) if positions.nonEmpty } {
              val s = position(Value.Int(line), Value.Int(l))
              if (positions.last != s) positions += s
            }
            traverse(body)
          case XML.Elem(Markup.Latex_Output(_), body) =>
            traverse(XML.string(latex_output(body)))
          case XML.Elem(Markup.Latex_Macro0(name), Nil) =>
            traverse(latex_macro0(name))
          case XML.Elem(Markup.Latex_Macro(name), body) =>
            traverse(latex_macro(name, body))
          case XML.Elem(Markup.Latex_Environment(name), body) =>
            traverse(latex_environment(name, body))
          case XML.Elem(markup @ Markup.Latex_Heading(kind), body) =>
            traverse(latex_heading(kind, markup.position_properties, body))
          case XML.Elem(markup @ Markup.Latex_Body(kind), body) =>
            traverse(latex_body(kind, markup.position_properties, body))
          case XML.Elem(Markup.Latex_Delim(name), body) =>
            traverse(latex_delim(name, body))
          case XML.Elem(Markup.Latex_Tag(name), body) =>
            traverse(latex_tag(name, body))
          case Index_Entry(entry) =>
            traverse(index_entry(entry))
          case t: XML.Tree =>
            error("Bad latex markup" +
              (if (file_pos.isEmpty) "" else Position.here(Position.File(file_pos))) + ":\n" +
              XML.string_of_tree(t))
        }
      }
      traverse(latex_text)

      result ++= positions
      result.mkString
    }
  }


  /* generated .tex file */

  private val File_Pattern = """^%:%file=(.+)%:%$""".r
  private val Line_Pattern = """^*%:%(\d+)=(\d+)%:%$""".r

  def read_tex_file(tex_file: Path): Tex_File =
  {
    val positions =
      Line.logical_lines(File.read(tex_file)).reverse.
        takeWhile(_ != "\\endinput").reverse

    val source_file =
      positions match {
        case File_Pattern(file) :: _ => Some(file)
        case _ => None
      }

    val source_lines =
      if (source_file.isEmpty) Nil
      else
        positions.flatMap(line =>
          line match {
            case Line_Pattern(Value.Int(line), Value.Int(source_line)) => Some(line -> source_line)
            case _ => None
          })

    new Tex_File(tex_file, source_file, source_lines)
  }

  final class Tex_File private[Latex](
    tex_file: Path, source_file: Option[String], source_lines: List[(Int, Int)])
  {
    override def toString: String = tex_file.toString

    def source_position(l: Int): Option[Position.T] =
      source_file match {
        case None => None
        case Some(file) =>
          val source_line =
            source_lines.iterator.takeWhile({ case (m, _) => m <= l }).
              foldLeft(0) { case (_, (_, n)) => n }
          if (source_line == 0) None else Some(Position.Line_File(source_line, file))
      }

    def position(line: Int): Position.T =
      source_position(line) getOrElse Position.Line_File(line, tex_file.implode)
  }


  /* latex log */

  def latex_errors(dir: Path, root_name: String): List[String] =
  {
    val root_log_path = dir + Path.explode(root_name).ext("log")
    if (root_log_path.is_file) {
      for { (msg, pos) <- filter_errors(dir, File.read(root_log_path)) }
        yield "Latex error" + Position.here(pos) + ":\n" + Library.indent_lines(2, msg)
    }
    else Nil
  }

  def filter_errors(dir: Path, root_log: String): List[(String, Position.T)] =
  {
    val seen_files = Synchronized(Map.empty[JFile, Tex_File])

    def check_tex_file(path: Path): Option[Tex_File] =
      seen_files.change_result(seen =>
        seen.get(path.file) match {
          case None =>
            if (path.is_file) {
              val tex_file = read_tex_file(path)
              (Some(tex_file), seen + (path.file -> tex_file))
            }
            else (None, seen)
          case some => (some, seen)
        })

    def tex_file_position(path: Path, line: Int): Position.T =
      check_tex_file(path) match {
        case Some(tex_file) => tex_file.position(line)
        case None => Position.Line_File(line, path.implode)
      }

    object File_Line_Error
    {
      val Pattern: Regex = """^(.*?\.\w\w\w):(\d+): (.*)$""".r
      def unapply(line: String): Option[(Path, Int, String)] =
        line match {
          case Pattern(file, Value.Int(line), message) =>
            val path = File.standard_path(file)
            if (Path.is_wellformed(path)) {
              val file = (dir + Path.explode(path)).canonical
              val msg = Library.perhaps_unprefix("LaTeX Error: ", message)
              if (file.is_file) Some((file, line, msg)) else None
            }
            else None
          case _ => None
        }
    }

    val Line_Error = """^l\.\d+ (.*)$""".r
    val More_Error =
      List(
        "<argument>",
        "<template>",
        "<recently read>",
        "<to be read again>",
        "<inserted text>",
        "<output>",
        "<everypar>",
        "<everymath>",
        "<everydisplay>",
        "<everyhbox>",
        "<everyvbox>",
        "<everyjob>",
        "<everycr>",
        "<mark>",
        "<everyeof>",
        "<write>").mkString("^(?:", "|", ") (.*)$").r

    val Bad_Font = """^LaTeX Font Warning: (Font .*\btxmia\b.* undefined).*$""".r
    val Bad_File = """^! LaTeX Error: (File `.*' not found\.)$""".r

    val error_ignore =
      Set(
        "See the LaTeX manual or LaTeX Companion for explanation.",
        "Type  H <return>  for immediate help.")

    def error_suffix1(lines: List[String]): Option[String] =
    {
      val lines1 =
        lines.take(20).takeWhile({ case File_Line_Error((_, _, _)) => false case _ => true })
      lines1.zipWithIndex.collectFirst({
        case (Line_Error(msg), i) =>
          cat_lines(lines1.take(i).filterNot(error_ignore) ::: List(msg)) })
    }
    def error_suffix2(lines: List[String]): Option[String] =
      lines match {
        case More_Error(msg) :: _ => Some(msg)
        case _ => None
      }

    @tailrec def filter(lines: List[String], result: List[(String, Position.T)])
      : List[(String, Position.T)] =
    {
      lines match {
        case File_Line_Error((file, line, msg1)) :: rest1 =>
          val pos = tex_file_position(file, line)
          val msg2 = error_suffix1(rest1) orElse error_suffix2(rest1) getOrElse ""
          filter(rest1, (Exn.cat_message(msg1, msg2), pos) :: result)
        case Bad_Font(msg) :: rest =>
          filter(rest, (msg, Position.none) :: result)
        case Bad_File(msg) :: rest =>
          filter(rest, (msg, Position.none) :: result)
        case _ :: rest => filter(rest, result)
        case Nil => result.reverse
      }
    }

    filter(Line.logical_lines(root_log), Nil)
  }
}
