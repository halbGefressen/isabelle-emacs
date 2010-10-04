(*  Title:      HOL/Sledgehammer.thy
    Author:     Lawrence C. Paulson, Cambridge University Computer Laboratory
    Author:     Jia Meng, Cambridge University Computer Laboratory and NICTA
    Author:     Fabian Immler, TU Muenchen
    Author:     Jasmin Blanchette, TU Muenchen
*)

header {* Sledgehammer: Isabelle--ATP Linkup *}

theory Sledgehammer
imports Plain
uses
  ("Tools/ATP/atp_problem.ML")
  ("Tools/ATP/atp_proof.ML")
  ("Tools/ATP/atp_systems.ML")
  ("Tools/Sledgehammer/sledgehammer_util.ML")
  ("Tools/Sledgehammer/sledgehammer_filter.ML")
  ("Tools/Sledgehammer/sledgehammer_translate.ML")
  ("Tools/Sledgehammer/sledgehammer_reconstruct.ML")
  ("Tools/Sledgehammer/sledgehammer.ML")
  ("Tools/Sledgehammer/sledgehammer_minimize.ML")
  ("Tools/Sledgehammer/sledgehammer_isar.ML")
begin

use "Tools/ATP/atp_problem.ML"
use "Tools/ATP/atp_proof.ML"
use "Tools/ATP/atp_systems.ML"
setup ATP_Systems.setup

use "Tools/Sledgehammer/sledgehammer_util.ML"
use "Tools/Sledgehammer/sledgehammer_filter.ML"
use "Tools/Sledgehammer/sledgehammer_translate.ML"
use "Tools/Sledgehammer/sledgehammer_reconstruct.ML"
use "Tools/Sledgehammer/sledgehammer.ML"
setup Sledgehammer.setup
use "Tools/Sledgehammer/sledgehammer_minimize.ML"
use "Tools/Sledgehammer/sledgehammer_isar.ML"
setup Sledgehammer_Isar.setup

end
