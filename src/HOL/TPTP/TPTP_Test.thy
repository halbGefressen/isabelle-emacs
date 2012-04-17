(*  Title:      HOL/TPTP/TPTP_Test.thy
5A    Author:     Nik Sultana, Cambridge University Computer Laboratory

Some tests for the TPTP interface. Some of the tests rely on the Isabelle
environment variable TPTP_PROBLEMS_PATH, which should point to the
TPTP-vX.Y.Z/Problems directory.
*)

theory TPTP_Test
imports TPTP_Parser
begin

ML {*
  val warning_out = Attrib.setup_config_string @{binding "warning_out"} (K "")
  fun S x y z = x z (y z)
*}

section "Parser tests"

ML {*
  fun payload_of (TPTP_Syntax.Annotated_Formula (_, _, _, _, fmla, _)) = fmla
  val payloads_of = map payload_of
*}


section "Source problems"
ML {*
  (*problem source*)
  val tptp_probs_dir =
    Path.explode "$TPTP_PROBLEMS_PATH"
    |> Path.expand;

  (*list of files to under test*)
  val files = TPTP_Syntax.get_file_list tptp_probs_dir;

(*  (*test problem-name parsing and mangling*)
  val problem_names =
    map (Path.base #>
         Path.implode #>
         TPTP_Problem_Name.parse_problem_name #>
         TPTP_Problem_Name.mangle_problem_name)
        files*)
*}


section "Supporting test functions"
ML {*
  fun report ctxt str =
    let
      val warning_out = Config.get ctxt warning_out
    in
      if warning_out = "" then warning str
      else
        let
          val out_stream = TextIO.openAppend warning_out
        in (TextIO.output (out_stream, str ^ "\n");
            TextIO.flushOut out_stream;
            TextIO.closeOut out_stream)
        end
    end

  fun test_fn ctxt f msg default_val file_name =
    let
      val _ = TPTP_Syntax.debug tracing (msg ^ " " ^ Path.print file_name)
    in
     (f file_name; ())
     (*otherwise report exceptions as warnings*)
     handle exn =>
       if Exn.is_interrupt exn then
         reraise exn
       else
         (report ctxt (msg ^ " test: file " ^ Path.print file_name ^
          " raised exception: " ^ ML_Compiler.exn_message exn);
          default_val)
    end

  fun timed_test ctxt f =
    let
      fun f' x = (f x; ())
      val time =
        Timing.timing (List.app f') files
        |> fst
      val duration =
        #elapsed time
        |> Time.toSeconds
        |> Real.fromLargeInt
      val average =
        (StringCvt.FIX (SOME 3),
         (duration / Real.fromInt (length files)))
        |-> Real.fmt
    in
      report ctxt ("Test timing: " ^ Timing.message time ^ "\n(about " ^ average ^
       "s per problem)")
    end
*}


ML {*
  fun situate file_name = Path.append tptp_probs_dir (Path.explode file_name);
  fun parser_test ctxt = (*FIXME argument order*)
    test_fn ctxt
     (fn file_name =>
        Path.implode file_name
        |> (fn file =>
             ((*report ctxt file; this is if you want the filename in the log*)
              TPTP_Parser.parse_file file)))
     "parser"
     ()
*}

declare [[warning_out = ""]]

end