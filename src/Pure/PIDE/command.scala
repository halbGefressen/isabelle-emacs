/*  Title:      Pure/PIDE/command.scala
    Author:     Fabian Immler, TU Munich
    Author:     Makarius

Prover commands with accumulated results from execution.
*/

package isabelle


import scala.collection.mutable
import scala.collection.immutable.SortedMap


object Command
{
  type Edit = (Option[Command], Option[Command])
  type Blob = Exn.Result[(Document.Node.Name, Option[(SHA1.Digest, Chunk.File)])]



  /** accumulated results from prover **/

  /* results */

  object Results
  {
    type Entry = (Long, XML.Tree)
    val empty = new Results(SortedMap.empty)
    def make(es: List[Results.Entry]): Results = (empty /: es)(_ + _)
    def merge(rs: List[Results]): Results = (empty /: rs)(_ ++ _)
  }

  final class Results private(private val rep: SortedMap[Long, XML.Tree])
  {
    def defined(serial: Long): Boolean = rep.isDefinedAt(serial)
    def get(serial: Long): Option[XML.Tree] = rep.get(serial)
    def iterator: Iterator[Results.Entry] = rep.iterator

    def + (entry: Results.Entry): Results =
      if (defined(entry._1)) this
      else new Results(rep + entry)

    def ++ (other: Results): Results =
      if (this eq other) this
      else if (rep.isEmpty) other
      else (this /: other.iterator)(_ + _)

    override def hashCode: Int = rep.hashCode
    override def equals(that: Any): Boolean =
      that match {
        case other: Results => rep == other.rep
        case _ => false
      }
    override def toString: String = iterator.mkString("Results(", ", ", ")")
  }


  /* source chunks */

  abstract class Chunk
  {
    def name: Chunk.Name
    def range: Text.Range
    def symbol_index: Symbol.Index

    private lazy val hash: Int = (name, range, symbol_index).hashCode
    override def hashCode: Int = hash
    override def equals(that: Any): Boolean =
      that match {
        case other: Chunk =>
          hash == other.hash &&
          name == other.name &&
          range == other.range &&
          symbol_index == other.symbol_index
        case _ => false
      }
    override def toString: String = "Command.Chunk(" + name + ")"

    def decode(symbol_offset: Symbol.Offset): Text.Offset = symbol_index.decode(symbol_offset)
    def decode(symbol_range: Symbol.Range): Text.Range = symbol_index.decode(symbol_range)
    def incorporate(symbol_range: Symbol.Range): Option[Text.Range] =
    {
      def in(r: Symbol.Range): Option[Text.Range] =
        range.try_restrict(decode(r)) match {
          case Some(r1) if !r1.is_singularity => Some(r1)
          case _ => None
        }
     in(symbol_range) orElse in(symbol_range - 1)
    }
  }

  object Chunk
  {
    sealed abstract class Name
    case object Self extends Name
    case class File_Name(file_name: String) extends Name

    class File(file_name: String, text: CharSequence) extends Chunk
    {
      val name = Chunk.File_Name(file_name)
      val range = Text.Range(0, text.length)
      val symbol_index = Symbol.Index(text)
    }
  }


  /* markup */

  object Markup_Index
  {
    val markup: Markup_Index = Markup_Index(false, Chunk.Self)
  }

  sealed case class Markup_Index(status: Boolean, chunk_name: Chunk.Name)

  object Markups
  {
    val empty: Markups = new Markups(Map.empty)

    def init(markup: Markup_Tree): Markups =
      new Markups(Map(Markup_Index.markup -> markup))
  }

  final class Markups private(private val rep: Map[Markup_Index, Markup_Tree])
  {
    def apply(index: Markup_Index): Markup_Tree =
      rep.getOrElse(index, Markup_Tree.empty)

    def add(index: Markup_Index, markup: Text.Markup): Markups =
      new Markups(rep + (index -> (this(index) + markup)))

    override def hashCode: Int = rep.hashCode
    override def equals(that: Any): Boolean =
      that match {
        case other: Markups => rep == other.rep
        case _ => false
      }
    override def toString: String = rep.iterator.mkString("Markups(", ", ", ")")
  }


  /* state */

  object State
  {
    def merge_results(states: List[State]): Command.Results =
      Results.merge(states.map(_.results))

    def merge_markup(states: List[State], index: Markup_Index,
        range: Text.Range, elements: Document.Elements): Markup_Tree =
      Markup_Tree.merge(states.map(_.markup(index)), range, elements)
  }

  sealed case class State(
    command: Command,
    status: List[Markup] = Nil,
    results: Results = Results.empty,
    markups: Markups = Markups.empty)
  {
    lazy val protocol_status: Protocol.Status =
    {
      val warnings =
        if (results.iterator.exists(p => Protocol.is_warning(p._2)))
          List(Markup(Markup.WARNING, Nil))
        else Nil
      val errors =
        if (results.iterator.exists(p => Protocol.is_error(p._2)))
          List(Markup(Markup.ERROR, Nil))
        else Nil
      Protocol.Status.make((warnings ::: errors ::: status).iterator)
    }

    def markup(index: Markup_Index): Markup_Tree = markups(index)


    def eq_content(other: State): Boolean =
      command.source == other.command.source &&
      status == other.status &&
      results == other.results &&
      markups == other.markups

    private def add_status(st: Markup): State =
      copy(status = st :: status)

    private def add_markup(status: Boolean, chunk_name: Command.Chunk.Name, m: Text.Markup): State =
    {
      val markups1 =
        if (status || Protocol.liberal_status_elements(m.info.name))
          markups.add(Markup_Index(true, chunk_name), m)
        else markups
      copy(markups = markups1.add(Markup_Index(false, chunk_name), m))
    }

    def + (valid_id: Document_ID.Generic => Boolean, message: XML.Elem): State =
      message match {
        case XML.Elem(Markup(Markup.STATUS, _), msgs) =>
          (this /: msgs)((state, msg) =>
            msg match {
              case elem @ XML.Elem(markup, Nil) =>
                state.
                  add_status(markup).
                  add_markup(true, Command.Chunk.Self, Text.Info(command.proper_range, elem))
              case _ =>
                System.err.println("Ignored status message: " + msg)
                state
            })

        case XML.Elem(Markup(Markup.REPORT, _), msgs) =>
          (this /: msgs)((state, msg) =>
            {
              def bad(): Unit = System.err.println("Ignored report message: " + msg)

              msg match {
                case XML.Elem(Markup(name,
                  atts @ Position.Reported(id, chunk_name, symbol_range)), args)
                if valid_id(id) =>
                  command.chunks.get(chunk_name) match {
                    case Some(chunk) =>
                      chunk.incorporate(symbol_range) match {
                        case Some(range) =>
                          val props = Position.purge(atts)
                          val info = Text.Info(range, XML.Elem(Markup(name, props), args))
                          state.add_markup(false, chunk_name, info)
                        case None => bad(); state
                      }
                    case None => bad(); state
                  }

                case XML.Elem(Markup(name, atts), args)
                if !atts.exists({ case (a, _) => Markup.POSITION_PROPERTIES(a) }) =>
                  val range = command.proper_range
                  val props = Position.purge(atts)
                  val info: Text.Markup = Text.Info(range, XML.Elem(Markup(name, props), args))
                  state.add_markup(false, Command.Chunk.Self, info)

                case _ => /* FIXME bad(); */ state
              }
            })
        case XML.Elem(Markup(name, props), body) =>
          props match {
            case Markup.Serial(i) =>
              val message1 = XML.Elem(Markup(Markup.message(name), props), body)
              val message2 = XML.Elem(Markup(name, props), body)

              var st = copy(results = results + (i -> message1))
              if (Protocol.is_inlined(message)) {
                for {
                  (chunk_name, chunk) <- command.chunks
                  range <- Protocol.message_positions(valid_id, chunk, message)
                } st = st.add_markup(false, chunk_name, Text.Info(range, message2))
              }
              st

            case _ =>
              System.err.println("Ignored message without serial number: " + message)
              this
          }
    }
  }



  /** static content **/

  /* make commands */

  def name(span: List[Token]): String =
    span.find(_.is_command) match { case Some(tok) => tok.source case _ => "" }

  private def source_span(span: List[Token]): (String, List[Token]) =
  {
    val source: String =
      span match {
        case List(tok) => tok.source
        case _ => span.map(_.source).mkString
      }

    val span1 = new mutable.ListBuffer[Token]
    var i = 0
    for (Token(kind, s) <- span) {
      val n = s.length
      val s1 = source.substring(i, i + n)
      span1 += Token(kind, s1)
      i += n
    }
    (source, span1.toList)
  }

  def apply(
    id: Document_ID.Command,
    node_name: Document.Node.Name,
    blobs: List[Blob],
    span: List[Token]): Command =
  {
    val (source, span1) = source_span(span)
    new Command(id, node_name, blobs, span1, source, Results.empty, Markup_Tree.empty)
  }

  val empty: Command = Command(Document_ID.none, Document.Node.Name.empty, Nil, Nil)

  def unparsed(
    id: Document_ID.Command,
    source: String,
    results: Results,
    markup: Markup_Tree): Command =
  {
    val (source1, span) = source_span(List(Token(Token.Kind.UNPARSED, source)))
    new Command(id, Document.Node.Name.empty, Nil, span, source1, results, markup)
  }

  def unparsed(source: String): Command =
    unparsed(Document_ID.none, source, Results.empty, Markup_Tree.empty)

  def rich_text(id: Document_ID.Command, results: Results, body: XML.Body): Command =
  {
    val text = XML.content(body)
    val markup = Markup_Tree.from_XML(body)
    unparsed(id, text, results, markup)
  }


  /* perspective */

  object Perspective
  {
    val empty: Perspective = Perspective(Nil)
  }

  sealed case class Perspective(commands: List[Command])  // visible commands in canonical order
  {
    def same(that: Perspective): Boolean =
    {
      val cmds1 = this.commands
      val cmds2 = that.commands
      require(!cmds1.exists(_.is_undefined))
      require(!cmds2.exists(_.is_undefined))
      cmds1.length == cmds2.length &&
        (cmds1.iterator zip cmds2.iterator).forall({ case (c1, c2) => c1.id == c2.id })
    }
  }
}


final class Command private(
    val id: Document_ID.Command,
    val node_name: Document.Node.Name,
    val blobs: List[Command.Blob],
    val span: List[Token],
    val source: String,
    val init_results: Command.Results,
    val init_markup: Markup_Tree)
{
  /* classification */

  def is_undefined: Boolean = id == Document_ID.none
  val is_unparsed: Boolean = span.exists(_.is_unparsed)
  val is_unfinished: Boolean = span.exists(_.is_unfinished)

  val is_ignored: Boolean = !span.exists(_.is_proper)
  val is_malformed: Boolean = !is_ignored && (!span.head.is_command || span.exists(_.is_error))
  def is_command: Boolean = !is_ignored && !is_malformed

  def name: String = Command.name(span)

  override def toString =
    id + "/" + (if (is_command) name else if (is_ignored) "IGNORED" else "MALFORMED")


  /* blobs */

  def blobs_changed(doc_blobs: Document.Blobs): Boolean =
    blobs.exists({ case Exn.Res((name, _)) => doc_blobs.changed(name) case _ => false })

  def blobs_names: List[Document.Node.Name] =
    for (Exn.Res((name, _)) <- blobs) yield name

  def blobs_digests: List[SHA1.Digest] =
    for (Exn.Res((_, Some((digest, _)))) <- blobs) yield digest


  /* source chunks */

  def length: Int = source.length
  val range: Text.Range = Text.Range(0, length)

  val proper_range: Text.Range =
    Text.Range(0, (length /: span.reverse.iterator.takeWhile(_.is_improper))(_ - _.source.length))

  def source(range: Text.Range): String = source.substring(range.start, range.stop)

  val chunk: Command.Chunk =
    new Command.Chunk {
      def name: Command.Chunk.Name = Command.Chunk.Self
      def range: Text.Range = Command.this.range
      lazy val symbol_index = Symbol.Index(source)
    }

  val chunks: Map[Command.Chunk.Name, Command.Chunk] =
    ((Command.Chunk.Self -> chunk) ::
      (for (Exn.Res((name, Some((_, file)))) <- blobs)
        yield (Command.Chunk.File_Name(name.node) -> file))).toMap


  /* accumulated results */

  val init_state: Command.State =
    Command.State(this, results = init_results, markups = Command.Markups.init(init_markup))

  val empty_state: Command.State = Command.State(this)
}
