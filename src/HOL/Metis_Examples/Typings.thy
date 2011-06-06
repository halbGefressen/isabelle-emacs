(*  Title:      HOL/Metis_Examples/Typings.thy
    Author:     Jasmin Blanchette, TU Muenchen

Testing the new Metis's (and hence Sledgehammer's) type translations.
*)

theory Typings
imports Main
begin

text {* Setup for testing Metis exhaustively *}

lemma fork: "P \<Longrightarrow> P \<Longrightarrow> P" by assumption

ML {*
open ATP_Translate

val polymorphisms = [Polymorphic, Monomorphic, Mangled_Monomorphic]
val levels =
  [All_Types, Nonmonotonic_Types, Finite_Types, Const_Arg_Types, No_Types]
val heaviness = [Heavyweight, Lightweight]
val type_syss =
  (levels |> map Simple_Types) @
  (map_product pair levels heaviness
   |> map_product pair polymorphisms
   |> map_product (fn constr => fn (poly, (level, heaviness)) =>
                      constr (poly, level, heaviness))
                  [Preds, Tags])

fun metis_eXhaust_tac ctxt ths =
  let
    fun tac [] st = all_tac st
      | tac (type_sys :: type_syss) st =
        st (* |> tap (fn _ => tracing (PolyML.makestring type_sys)) *)
           |> ((if null type_syss then all_tac else rtac @{thm fork} 1)
               THEN Metis_Tactics.metisX_tac ctxt (SOME type_sys) ths 1
               THEN COND (has_fewer_prems 2) all_tac no_tac
               THEN tac type_syss)
  in tac end
*}

method_setup metis_eXhaust = {*
  Attrib.thms >>
    (fn ths => fn ctxt => SIMPLE_METHOD (metis_eXhaust_tac ctxt ths type_syss))
*} "exhaustively run the new Metis with all type encodings"


text {* Metis tests *}

lemma "x = y \<Longrightarrow> y = x"
by metis_eXhaust

lemma "[a] = [1 + 1] \<Longrightarrow> a = 1 + (1::int)"
by (metis_eXhaust last.simps)

lemma "map Suc [0] = [Suc 0]"
by (metis_eXhaust map.simps)

lemma "map Suc [1 + 1] = [Suc 2]"
by (metis_eXhaust map.simps nat_1_add_1)

lemma "map Suc [2] = [Suc (1 + 1)]"
by (metis_eXhaust map.simps nat_1_add_1)

definition "null xs = (xs = [])"

lemma "P (null xs) \<Longrightarrow> null xs \<Longrightarrow> xs = []"
by (metis_eXhaust null_def)

lemma "(0::nat) + 0 = 0"
by (metis_eXhaust arithmetic_simps(38))

end
