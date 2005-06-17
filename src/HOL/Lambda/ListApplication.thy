(*  Title:      HOL/Lambda/ListApplication.thy
    ID:         $Id$
    Author:     Tobias Nipkow
    Copyright   1998 TU Muenchen
*)

header {* Application of a term to a list of terms *}

theory ListApplication imports Lambda begin

syntax
  "_list_application" :: "dB => dB list => dB"    (infixl "\<degree>\<degree>" 150)
translations
  "t \<degree>\<degree> ts" == "foldl (op \<degree>) t ts"

lemma apps_eq_tail_conv [iff]: "(r \<degree>\<degree> ts = s \<degree>\<degree> ts) = (r = s)"
  apply (induct_tac ts rule: rev_induct)
   apply auto
  done

lemma Var_eq_apps_conv [iff]:
    "\<And>s. (Var m = s \<degree>\<degree> ss) = (Var m = s \<and> ss = [])"
  apply (induct ss)
   apply auto
  done

lemma Var_apps_eq_Var_apps_conv [iff]:
    "\<And>ss. (Var m \<degree>\<degree> rs = Var n \<degree>\<degree> ss) = (m = n \<and> rs = ss)"
  apply (induct rs rule: rev_induct)
   apply simp
   apply blast
  apply (induct_tac ss rule: rev_induct)
   apply auto
  done

lemma App_eq_foldl_conv:
  "(r \<degree> s = t \<degree>\<degree> ts) =
    (if ts = [] then r \<degree> s = t
    else (\<exists>ss. ts = ss @ [s] \<and> r = t \<degree>\<degree> ss))"
  apply (rule_tac xs = ts in rev_exhaust)
   apply auto
  done

lemma Abs_eq_apps_conv [iff]:
    "(Abs r = s \<degree>\<degree> ss) = (Abs r = s \<and> ss = [])"
  apply (induct_tac ss rule: rev_induct)
   apply auto
  done

lemma apps_eq_Abs_conv [iff]: "(s \<degree>\<degree> ss = Abs r) = (s = Abs r \<and> ss = [])"
  apply (induct_tac ss rule: rev_induct)
   apply auto
  done

lemma Abs_apps_eq_Abs_apps_conv [iff]:
    "\<And>ss. (Abs r \<degree>\<degree> rs = Abs s \<degree>\<degree> ss) = (r = s \<and> rs = ss)"
  apply (induct rs rule: rev_induct)
   apply simp
   apply blast
  apply (induct_tac ss rule: rev_induct)
   apply auto
  done

lemma Abs_App_neq_Var_apps [iff]:
    "\<forall>s t. Abs s \<degree> t ~= Var n \<degree>\<degree> ss"
  apply (induct_tac ss rule: rev_induct)
   apply auto
  done

lemma Var_apps_neq_Abs_apps [iff]:
    "\<And>ts. Var n \<degree>\<degree> ts ~= Abs r \<degree>\<degree> ss"
  apply (induct ss rule: rev_induct)
   apply simp
  apply (induct_tac ts rule: rev_induct)
   apply auto
  done

lemma ex_head_tail:
  "\<exists>ts h. t = h \<degree>\<degree> ts \<and> ((\<exists>n. h = Var n) \<or> (\<exists>u. h = Abs u))"
  apply (induct_tac t)
    apply (rule_tac x = "[]" in exI)
    apply simp
   apply clarify
   apply (rename_tac ts1 ts2 h1 h2)
   apply (rule_tac x = "ts1 @ [h2 \<degree>\<degree> ts2]" in exI)
   apply simp
  apply simp
  done

lemma size_apps [simp]:
  "size (r \<degree>\<degree> rs) = size r + foldl (op +) 0 (map size rs) + length rs"
  apply (induct_tac rs rule: rev_induct)
   apply auto
  done

lemma lem0: "[| (0::nat) < k; m <= n |] ==> m < n + k"
  apply simp
  done

lemma lift_map [simp]:
    "\<And>t. lift (t \<degree>\<degree> ts) i = lift t i \<degree>\<degree> map (\<lambda>t. lift t i) ts"
  by (induct ts) simp_all

lemma subst_map [simp]:
    "\<And>t. subst (t \<degree>\<degree> ts) u i = subst t u i \<degree>\<degree> map (\<lambda>t. subst t u i) ts"
  by (induct ts) simp_all

lemma app_last: "(t \<degree>\<degree> ts) \<degree> u = t \<degree>\<degree> (ts @ [u])"
  by simp


text {* \medskip A customized induction schema for @{text "\<degree>\<degree>"}. *}

lemma lem [rule_format (no_asm)]:
  "[| !!n ts. \<forall>t \<in> set ts. P t ==> P (Var n \<degree>\<degree> ts);
    !!u ts. [| P u; \<forall>t \<in> set ts. P t |] ==> P (Abs u \<degree>\<degree> ts)
  |] ==> \<forall>t. size t = n --> P t"
proof -
  case rule_context
  show ?thesis
   apply (induct_tac n rule: nat_less_induct)
   apply (rule allI)
   apply (cut_tac t = t in ex_head_tail)
   apply clarify
   apply (erule disjE)
    apply clarify
    apply (rule prems)
    apply clarify
    apply (erule allE, erule impE)
      prefer 2
      apply (erule allE, erule mp, rule refl)
     apply simp
     apply (rule lem0)
      apply force
     apply (rule elem_le_sum)
     apply force
    apply clarify
    apply (rule prems)
     apply (erule allE, erule impE)
      prefer 2
      apply (erule allE, erule mp, rule refl)
     apply simp
    apply clarify
    apply (erule allE, erule impE)
     prefer 2
     apply (erule allE, erule mp, rule refl)
    apply simp
    apply (rule le_imp_less_Suc)
    apply (rule trans_le_add1)
    apply (rule trans_le_add2)
    apply (rule elem_le_sum)
    apply force
    done
qed

theorem Apps_dB_induct:
  "[| !!n ts. \<forall>t \<in> set ts. P t ==> P (Var n \<degree>\<degree> ts);
    !!u ts. [| P u; \<forall>t \<in> set ts. P t |] ==> P (Abs u \<degree>\<degree> ts)
  |] ==> P t"
proof -
  case rule_context
  show ?thesis
    apply (rule_tac t = t in lem)
      prefer 3
      apply (rule refl)
     apply (assumption | rule prems)+
    done
qed

end
