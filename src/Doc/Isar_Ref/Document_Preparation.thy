theory Document_Preparation
imports Base Main
begin

chapter \<open>Document preparation \label{ch:document-prep}\<close>

text \<open>Isabelle/Isar provides a simple document preparation system
  based on {PDF-\LaTeX}, with support for hyperlinks and bookmarks
  within that format.  This allows to produce papers, books, theses
  etc.\ from Isabelle theory sources.

  {\LaTeX} output is generated while processing a \emph{session} in
  batch mode, as explained in the \emph{The Isabelle System Manual}
  @{cite "isabelle-system"}.  The main Isabelle tools to get started with
  document preparation are @{tool_ref mkroot} and @{tool_ref build}.

  The classic Isabelle/HOL tutorial @{cite "isabelle-hol-book"} also
  explains some aspects of theory presentation.\<close>


section \<open>Markup commands \label{sec:markup}\<close>

text \<open>
  \begin{matharray}{rcl}
    @{command_def "chapter"} & : & @{text "any \<rightarrow> any"} \\
    @{command_def "section"} & : & @{text "any \<rightarrow> any"} \\
    @{command_def "subsection"} & : & @{text "any \<rightarrow> any"} \\
    @{command_def "subsubsection"} & : & @{text "any \<rightarrow> any"} \\
    @{command_def "paragraph"} & : & @{text "any \<rightarrow> any"} \\
    @{command_def "subparagraph"} & : & @{text "any \<rightarrow> any"} \\
    @{command_def "text"} & : & @{text "any \<rightarrow> any"} \\
    @{command_def "txt"} & : & @{text "any \<rightarrow> any"} \\
    @{command_def "text_raw"} & : & @{text "any \<rightarrow> any"} \\
  \end{matharray}

  Markup commands provide a structured way to insert text into the
  document generated from a theory.  Each markup command takes a
  single @{syntax text} argument, which is passed as argument to a
  corresponding {\LaTeX} macro.  The default macros provided by
  @{file "~~/lib/texinputs/isabelle.sty"} can be redefined according
  to the needs of the underlying document and {\LaTeX} styles.

  Note that formal comments (\secref{sec:comments}) are similar to
  markup commands, but have a different status within Isabelle/Isar
  syntax.

  @{rail \<open>
    (@@{command chapter} | @@{command section} | @@{command subsection} |
      @@{command subsubsection} | @@{command paragraph} | @@{command subparagraph} |
      @@{command text} | @@{command txt} | @@{command text_raw}) @{syntax text}
  \<close>}

  \<^descr> @{command chapter}, @{command section}, @{command subsection} etc.\ mark
  section headings within the theory source. This works in any context, even
  before the initial @{command theory} command. The corresponding {\LaTeX}
  macros are @{verbatim \<open>\isamarkupchapter\<close>}, @{verbatim
  \<open>\isamarkupsection\<close>}, @{verbatim \<open>\isamarkupsubsection\<close>} etc.\

  \<^descr> @{command text} and @{command txt} specify paragraphs of plain text.
  This corresponds to a {\LaTeX} environment @{verbatim
  \<open>\begin{isamarkuptext}\<close>} @{text "\<dots>"} @{verbatim \<open>\end{isamarkuptext}\<close>}
  etc.

  \<^descr> @{command text_raw} is similar to @{command text}, but without
  any surrounding markup environment. This allows to inject arbitrary
  {\LaTeX} source into the generated document.


  All text passed to any of the above markup commands may refer to formal
  entities via \emph{document antiquotations}, see also \secref{sec:antiq}.
  These are interpreted in the present theory or proof context.

  \<^medskip>
  The proof markup commands closely resemble those for theory
  specifications, but have a different formal status and produce
  different {\LaTeX} macros.
\<close>


section \<open>Document antiquotations \label{sec:antiq}\<close>

text \<open>
  \begin{matharray}{rcl}
    @{command_def "print_antiquotations"}@{text "\<^sup>*"} & : & @{text "context \<rightarrow> "} \\
    @{antiquotation_def "theory"} & : & @{text antiquotation} \\
    @{antiquotation_def "thm"} & : & @{text antiquotation} \\
    @{antiquotation_def "lemma"} & : & @{text antiquotation} \\
    @{antiquotation_def "prop"} & : & @{text antiquotation} \\
    @{antiquotation_def "term"} & : & @{text antiquotation} \\
    @{antiquotation_def term_type} & : & @{text antiquotation} \\
    @{antiquotation_def typeof} & : & @{text antiquotation} \\
    @{antiquotation_def const} & : & @{text antiquotation} \\
    @{antiquotation_def abbrev} & : & @{text antiquotation} \\
    @{antiquotation_def typ} & : & @{text antiquotation} \\
    @{antiquotation_def type} & : & @{text antiquotation} \\
    @{antiquotation_def class} & : & @{text antiquotation} \\
    @{antiquotation_def "text"} & : & @{text antiquotation} \\
    @{antiquotation_def goals} & : & @{text antiquotation} \\
    @{antiquotation_def subgoals} & : & @{text antiquotation} \\
    @{antiquotation_def prf} & : & @{text antiquotation} \\
    @{antiquotation_def full_prf} & : & @{text antiquotation} \\
    @{antiquotation_def ML} & : & @{text antiquotation} \\
    @{antiquotation_def ML_op} & : & @{text antiquotation} \\
    @{antiquotation_def ML_type} & : & @{text antiquotation} \\
    @{antiquotation_def ML_structure} & : & @{text antiquotation} \\
    @{antiquotation_def ML_functor} & : & @{text antiquotation} \\
    @{antiquotation_def verbatim} & : & @{text antiquotation} \\
    @{antiquotation_def "file"} & : & @{text antiquotation} \\
    @{antiquotation_def "url"} & : & @{text antiquotation} \\
    @{antiquotation_def "cite"} & : & @{text antiquotation} \\
  \end{matharray}

  The overall content of an Isabelle/Isar theory may alternate between
  formal and informal text.  The main body consists of formal
  specification and proof commands, interspersed with markup commands
  (\secref{sec:markup}) or document comments (\secref{sec:comments}).
  The argument of markup commands quotes informal text to be printed
  in the resulting document, but may again refer to formal entities
  via \emph{document antiquotations}.

  For example, embedding @{verbatim \<open>@{term [show_types] "f x = a + x"}\<close>}
  within a text block makes
  \isa{{\isacharparenleft}f{\isasymColon}{\isacharprime}a\ {\isasymRightarrow}\ {\isacharprime}a{\isacharparenright}\ {\isacharparenleft}x{\isasymColon}{\isacharprime}a{\isacharparenright}\ {\isacharequal}\ {\isacharparenleft}a{\isasymColon}{\isacharprime}a{\isacharparenright}\ {\isacharplus}\ x} appear in the final {\LaTeX} document.

  Antiquotations usually spare the author tedious typing of logical
  entities in full detail.  Even more importantly, some degree of
  consistency-checking between the main body of formal text and its
  informal explanation is achieved, since terms and types appearing in
  antiquotations are checked within the current theory or proof
  context.

  \<^medskip>
  Antiquotations are in general written @{verbatim "@{"}@{text "name
  [options] arguments"}@{verbatim "}"}. The short form @{verbatim
  "\<^control>"}@{text "\<open>argument\<close>"} (without surrounding @{verbatim
  "@{"}@{text "\<dots>"}@{verbatim "}"}) works where the name is a single control
  symbol and the argument a single cartouche.

  @{rail \<open>
    @@{command print_antiquotations} ('!'?)
    ;
    @{syntax_def antiquotation}:
      '@{' antiquotation_body '}' |
      @{syntax_ref control_symbol} @{syntax_ref cartouche}
  \<close>}

  %% FIXME less monolithic presentation, move to individual sections!?
  @{rail \<open>
    @{syntax_def antiquotation_body}:
      @@{antiquotation theory} options @{syntax name} |
      @@{antiquotation thm} options styles @{syntax thmrefs} |
      @@{antiquotation lemma} options @{syntax prop} @'by' @{syntax method} @{syntax method}? |
      @@{antiquotation prop} options styles @{syntax prop} |
      @@{antiquotation term} options styles @{syntax term} |
      @@{antiquotation (HOL) value} options styles @{syntax term} |
      @@{antiquotation term_type} options styles @{syntax term} |
      @@{antiquotation typeof} options styles @{syntax term} |
      @@{antiquotation const} options @{syntax term} |
      @@{antiquotation abbrev} options @{syntax term} |
      @@{antiquotation typ} options @{syntax type} |
      @@{antiquotation type} options @{syntax name} |
      @@{antiquotation class} options @{syntax name} |
      @@{antiquotation text} options @{syntax text}
    ;
    @{syntax antiquotation}:
      @@{antiquotation goals} options |
      @@{antiquotation subgoals} options |
      @@{antiquotation prf} options @{syntax thmrefs} |
      @@{antiquotation full_prf} options @{syntax thmrefs} |
      @@{antiquotation ML} options @{syntax text} |
      @@{antiquotation ML_op} options @{syntax text} |
      @@{antiquotation ML_type} options @{syntax text} |
      @@{antiquotation ML_structure} options @{syntax text} |
      @@{antiquotation ML_functor} options @{syntax text} |
      @@{antiquotation verbatim} options @{syntax text} |
      @@{antiquotation "file"} options @{syntax name} |
      @@{antiquotation file_unchecked} options @{syntax name} |
      @@{antiquotation url} options @{syntax name} |
      @@{antiquotation cite} options @{syntax cartouche}? (@{syntax name} + @'and')
    ;
    options: '[' (option * ',') ']'
    ;
    option: @{syntax name} | @{syntax name} '=' @{syntax name}
    ;
    styles: '(' (style + ',') ')'
    ;
    style: (@{syntax name} +)
  \<close>}

  Note that the syntax of antiquotations may \emph{not} include source
  comments @{verbatim "(*"}~@{text "\<dots>"}~@{verbatim "*)"} nor verbatim
  text @{verbatim "{*"}~@{text "\<dots>"}~@{verbatim "*}"}.

  \<^descr> @{command "print_antiquotations"} prints all document antiquotations
  that are defined in the current context; the ``@{text "!"}'' option
  indicates extra verbosity.

  \<^descr> @{text "@{theory A}"} prints the name @{text "A"}, which is
  guaranteed to refer to a valid ancestor theory in the current
  context.

  \<^descr> @{text "@{thm a\<^sub>1 \<dots> a\<^sub>n}"} prints theorems @{text "a\<^sub>1 \<dots> a\<^sub>n"}.
  Full fact expressions are allowed here, including attributes
  (\secref{sec:syn-att}).

  \<^descr> @{text "@{prop \<phi>}"} prints a well-typed proposition @{text
  "\<phi>"}.

  \<^descr> @{text "@{lemma \<phi> by m}"} proves a well-typed proposition
  @{text "\<phi>"} by method @{text m} and prints the original @{text "\<phi>"}.

  \<^descr> @{text "@{term t}"} prints a well-typed term @{text "t"}.
  
  \<^descr> @{text "@{value t}"} evaluates a term @{text "t"} and prints
  its result, see also @{command_ref (HOL) value}.

  \<^descr> @{text "@{term_type t}"} prints a well-typed term @{text "t"}
  annotated with its type.

  \<^descr> @{text "@{typeof t}"} prints the type of a well-typed term
  @{text "t"}.

  \<^descr> @{text "@{const c}"} prints a logical or syntactic constant
  @{text "c"}.
  
  \<^descr> @{text "@{abbrev c x\<^sub>1 \<dots> x\<^sub>n}"} prints a constant abbreviation
  @{text "c x\<^sub>1 \<dots> x\<^sub>n \<equiv> rhs"} as defined in the current context.

  \<^descr> @{text "@{typ \<tau>}"} prints a well-formed type @{text "\<tau>"}.

  \<^descr> @{text "@{type \<kappa>}"} prints a (logical or syntactic) type
    constructor @{text "\<kappa>"}.

  \<^descr> @{text "@{class c}"} prints a class @{text c}.

  \<^descr> @{text "@{text s}"} prints uninterpreted source text @{text
  s}.  This is particularly useful to print portions of text according
  to the Isabelle document style, without demanding well-formedness,
  e.g.\ small pieces of terms that should not be parsed or
  type-checked yet.

  \<^descr> @{text "@{goals}"} prints the current \emph{dynamic} goal
  state.  This is mainly for support of tactic-emulation scripts
  within Isar.  Presentation of goal states does not conform to the
  idea of human-readable proof documents!

  When explaining proofs in detail it is usually better to spell out
  the reasoning via proper Isar proof commands, instead of peeking at
  the internal machine configuration.
  
  \<^descr> @{text "@{subgoals}"} is similar to @{text "@{goals}"}, but
  does not print the main goal.
  
  \<^descr> @{text "@{prf a\<^sub>1 \<dots> a\<^sub>n}"} prints the (compact) proof terms
  corresponding to the theorems @{text "a\<^sub>1 \<dots> a\<^sub>n"}. Note that this
  requires proof terms to be switched on for the current logic
  session.
  
  \<^descr> @{text "@{full_prf a\<^sub>1 \<dots> a\<^sub>n}"} is like @{text "@{prf a\<^sub>1 \<dots>
  a\<^sub>n}"}, but prints the full proof terms, i.e.\ also displays
  information omitted in the compact proof term, which is denoted by
  ``@{text _}'' placeholders there.
  
  \<^descr> @{text "@{ML s}"}, @{text "@{ML_op s}"}, @{text "@{ML_type
  s}"}, @{text "@{ML_structure s}"}, and @{text "@{ML_functor s}"}
  check text @{text s} as ML value, infix operator, type, structure,
  and functor respectively.  The source is printed verbatim.

  \<^descr> @{text "@{verbatim s}"} prints uninterpreted source text literally
  as ASCII characters, using some type-writer font style.

  \<^descr> @{text "@{file path}"} checks that @{text "path"} refers to a
  file (or directory) and prints it verbatim.

  \<^descr> @{text "@{file_unchecked path}"} is like @{text "@{file
  path}"}, but does not check the existence of the @{text "path"}
  within the file-system.

  \<^descr> @{text "@{url name}"} produces markup for the given URL, which
  results in an active hyperlink within the text.

  \<^descr> @{text "@{cite name}"} produces a citation @{verbatim
  \<open>\cite{name}\<close>} in {\LaTeX}, where the name refers to some Bib{\TeX}
  database entry.

  The variant @{text "@{cite \<open>opt\<close> name}"} produces @{verbatim
  \<open>\cite[opt]{name}\<close>} with some free-form optional argument. Multiple names
  are output with commas, e.g. @{text "@{cite foo \<AND> bar}"} becomes
  @{verbatim \<open>\cite{foo,bar}\<close>}.

  The {\LaTeX} macro name is determined by the antiquotation option
  @{antiquotation_option_def cite_macro}, or the configuration option
  @{attribute cite_macro} in the context. For example, @{text "@{cite
  [cite_macro = nocite] foobar}"} produces @{verbatim \<open>\nocite{foobar}\<close>}.
\<close>


subsection \<open>Styled antiquotations\<close>

text \<open>The antiquotations @{text thm}, @{text prop} and @{text
  term} admit an extra \emph{style} specification to modify the
  printed result.  A style is specified by a name with a possibly
  empty number of arguments;  multiple styles can be sequenced with
  commas.  The following standard styles are available:

  \<^descr> @{text lhs} extracts the first argument of any application
  form with at least two arguments --- typically meta-level or
  object-level equality, or any other binary relation.
  
  \<^descr> @{text rhs} is like @{text lhs}, but extracts the second
  argument.
  
  \<^descr> @{text "concl"} extracts the conclusion @{text C} from a rule
  in Horn-clause normal form @{text "A\<^sub>1 \<Longrightarrow> \<dots> A\<^sub>n \<Longrightarrow> C"}.
  
  \<^descr> @{text "prem"} @{text n} extract premise number
  @{text "n"} from from a rule in Horn-clause
  normal form @{text "A\<^sub>1 \<Longrightarrow> \<dots> A\<^sub>n \<Longrightarrow> C"}
\<close>


subsection \<open>General options\<close>

text \<open>The following options are available to tune the printed output
  of antiquotations.  Note that many of these coincide with system and
  configuration options of the same names.

  \<^descr> @{antiquotation_option_def show_types}~@{text "= bool"} and
  @{antiquotation_option_def show_sorts}~@{text "= bool"} control
  printing of explicit type and sort constraints.

  \<^descr> @{antiquotation_option_def show_structs}~@{text "= bool"}
  controls printing of implicit structures.

  \<^descr> @{antiquotation_option_def show_abbrevs}~@{text "= bool"}
  controls folding of abbreviations.

  \<^descr> @{antiquotation_option_def names_long}~@{text "= bool"} forces
  names of types and constants etc.\ to be printed in their fully
  qualified internal form.

  \<^descr> @{antiquotation_option_def names_short}~@{text "= bool"}
  forces names of types and constants etc.\ to be printed unqualified.
  Note that internalizing the output again in the current context may
  well yield a different result.

  \<^descr> @{antiquotation_option_def names_unique}~@{text "= bool"}
  determines whether the printed version of qualified names should be
  made sufficiently long to avoid overlap with names declared further
  back.  Set to @{text false} for more concise output.

  \<^descr> @{antiquotation_option_def eta_contract}~@{text "= bool"}
  prints terms in @{text \<eta>}-contracted form.

  \<^descr> @{antiquotation_option_def display}~@{text "= bool"} indicates
  if the text is to be output as multi-line ``display material'',
  rather than a small piece of text without line breaks (which is the
  default).

  In this mode the embedded entities are printed in the same style as
  the main theory text.

  \<^descr> @{antiquotation_option_def break}~@{text "= bool"} controls
  line breaks in non-display material.

  \<^descr> @{antiquotation_option_def quotes}~@{text "= bool"} indicates
  if the output should be enclosed in double quotes.

  \<^descr> @{antiquotation_option_def mode}~@{text "= name"} adds @{text
  name} to the print mode to be used for presentation.  Note that the
  standard setup for {\LaTeX} output is already present by default,
  including the modes @{text latex} and @{text xsymbols}.

  \<^descr> @{antiquotation_option_def margin}~@{text "= nat"} and
  @{antiquotation_option_def indent}~@{text "= nat"} change the margin
  or indentation for pretty printing of display material.

  \<^descr> @{antiquotation_option_def goals_limit}~@{text "= nat"}
  determines the maximum number of subgoals to be printed (for goal-based
  antiquotation).

  \<^descr> @{antiquotation_option_def source}~@{text "= bool"} prints the
  original source text of the antiquotation arguments, rather than its
  internal representation.  Note that formal checking of
  @{antiquotation "thm"}, @{antiquotation "term"}, etc. is still
  enabled; use the @{antiquotation "text"} antiquotation for unchecked
  output.

  Regular @{text "term"} and @{text "typ"} antiquotations with @{text
  "source = false"} involve a full round-trip from the original source
  to an internalized logical entity back to a source form, according
  to the syntax of the current context.  Thus the printed output is
  not under direct control of the author, it may even fluctuate a bit
  as the underlying theory is changed later on.

  In contrast, @{antiquotation_option source}~@{text "= true"}
  admits direct printing of the given source text, with the desirable
  well-formedness check in the background, but without modification of
  the printed text.


  For Boolean flags, ``@{text "name = true"}'' may be abbreviated as
  ``@{text name}''.  All of the above flags are disabled by default,
  unless changed specifically for a logic session in the corresponding
  @{verbatim "ROOT"} file.
\<close>


section \<open>Markup via command tags \label{sec:tags}\<close>

text \<open>Each Isabelle/Isar command may be decorated by additional
  presentation tags, to indicate some modification in the way it is
  printed in the document.

  @{rail \<open>
    @{syntax_def tags}: ( tag * )
    ;
    tag: '%' (@{syntax ident} | @{syntax string})
  \<close>}

  Some tags are pre-declared for certain classes of commands, serving
  as default markup if no tags are given in the text:

  \<^medskip>
  \begin{tabular}{ll}
    @{text "theory"} & theory begin/end \\
    @{text "proof"} & all proof commands \\
    @{text "ML"} & all commands involving ML code \\
  \end{tabular}
  \<^medskip>

  The Isabelle document preparation system
  @{cite "isabelle-system"} allows tagged command regions to be presented
  specifically, e.g.\ to fold proof texts, or drop parts of the text
  completely.

  For example ``@{command "by"}~@{text "%invisible auto"}'' causes
  that piece of proof to be treated as @{text invisible} instead of
  @{text "proof"} (the default), which may be shown or hidden
  depending on the document setup.  In contrast, ``@{command
  "by"}~@{text "%visible auto"}'' forces this text to be shown
  invariably.

  Explicit tag specifications within a proof apply to all subsequent
  commands of the same level of nesting.  For example, ``@{command
  "proof"}~@{text "%visible \<dots>"}~@{command "qed"}'' forces the whole
  sub-proof to be typeset as @{text visible} (unless some of its parts
  are tagged differently).

  \<^medskip>
  Command tags merely produce certain markup environments for
  type-setting.  The meaning of these is determined by {\LaTeX}
  macros, as defined in @{file "~~/lib/texinputs/isabelle.sty"} or
  by the document author.  The Isabelle document preparation tools
  also provide some high-level options to specify the meaning of
  arbitrary tags to ``keep'', ``drop'', or ``fold'' the corresponding
  parts of the text.  Logic sessions may also specify ``document
  versions'', where given tags are interpreted in some particular way.
  Again see @{cite "isabelle-system"} for further details.
\<close>


section \<open>Railroad diagrams\<close>

text \<open>
  \begin{matharray}{rcl}
    @{antiquotation_def "rail"} & : & @{text antiquotation} \\
  \end{matharray}

  @{rail \<open>
    'rail' @{syntax text}
  \<close>}

  The @{antiquotation rail} antiquotation allows to include syntax
  diagrams into Isabelle documents.  {\LaTeX} requires the style file
  @{file "~~/lib/texinputs/railsetup.sty"}, which can be used via
  @{verbatim \<open>\usepackage{railsetup}\<close>} in @{verbatim "root.tex"}, for
  example.

  The rail specification language is quoted here as Isabelle @{syntax
  string} or text @{syntax "cartouche"}; it has its own grammar given
  below.

  \begingroup
  \def\isasymnewline{\isatext{\tt\isacharbackslash<newline>}}
  @{rail \<open>
  rule? + ';'
  ;
  rule: ((identifier | @{syntax antiquotation}) ':')? body
  ;
  body: concatenation + '|'
  ;
  concatenation: ((atom '?'?) +) (('*' | '+') atom?)?
  ;
  atom: '(' body? ')' | identifier |
    '@'? (string | @{syntax antiquotation}) |
    '\<newline>'
  \<close>}
  \endgroup

  The lexical syntax of @{text "identifier"} coincides with that of
  @{syntax ident} in regular Isabelle syntax, but @{text string} uses
  single quotes instead of double quotes of the standard @{syntax
  string} category.

  Each @{text rule} defines a formal language (with optional name),
  using a notation that is similar to EBNF or regular expressions with
  recursion.  The meaning and visual appearance of these rail language
  elements is illustrated by the following representative examples.

  \<^item> Empty @{verbatim "()"}

  @{rail \<open>()\<close>}

  \<^item> Nonterminal @{verbatim "A"}

  @{rail \<open>A\<close>}

  \<^item> Nonterminal via Isabelle antiquotation
  @{verbatim "@{syntax method}"}

  @{rail \<open>@{syntax method}\<close>}

  \<^item> Terminal @{verbatim "'xyz'"}

  @{rail \<open>'xyz'\<close>}

  \<^item> Terminal in keyword style @{verbatim "@'xyz'"}

  @{rail \<open>@'xyz'\<close>}

  \<^item> Terminal via Isabelle antiquotation
  @{verbatim "@@{method rule}"}

  @{rail \<open>@@{method rule}\<close>}

  \<^item> Concatenation @{verbatim "A B C"}

  @{rail \<open>A B C\<close>}

  \<^item> Newline inside concatenation
  @{verbatim "A B C \<newline> D E F"}

  @{rail \<open>A B C \<newline> D E F\<close>}

  \<^item> Variants @{verbatim "A | B | C"}

  @{rail \<open>A | B | C\<close>}

  \<^item> Option @{verbatim "A ?"}

  @{rail \<open>A ?\<close>}

  \<^item> Repetition @{verbatim "A *"}

  @{rail \<open>A *\<close>}

  \<^item> Repetition with separator @{verbatim "A * sep"}

  @{rail \<open>A * sep\<close>}

  \<^item> Strict repetition @{verbatim "A +"}

  @{rail \<open>A +\<close>}

  \<^item> Strict repetition with separator @{verbatim "A + sep"}

  @{rail \<open>A + sep\<close>}
\<close>


section \<open>Draft presentation\<close>

text \<open>
  \begin{matharray}{rcl}
    @{command_def "display_drafts"}@{text "\<^sup>*"} & : & @{text "any \<rightarrow>"} \\
  \end{matharray}

  @{rail \<open>
    @@{command display_drafts} (@{syntax name} +)
  \<close>}

  \<^descr> @{command "display_drafts"}~@{text paths} performs simple output of a
  given list of raw source files. Only those symbols that do not require
  additional {\LaTeX} packages are displayed properly, everything else is left
  verbatim.
\<close>

end
