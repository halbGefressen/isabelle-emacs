{- generated by Isabelle -}

{-  Title:      Haskell/Tools/Markup.hs
    Author:     Makarius
    LICENSE:    BSD 3-clause (Isabelle)

Quasi-abstract markup elements.
-}

module Isabelle.Markup (
  T, empty, is_empty, properties,

  nameN, name, xnameN, xname, kindN,

  lineN, end_lineN, offsetN, end_offsetN, fileN, idN, positionN, position,

  markupN, consistentN, unbreakableN, indentN, widthN,
  blockN, block, breakN, break, fbreakN, fbreak, itemN, item,

  wordsN, words, no_wordsN, no_words,

  tfreeN, tfree, tvarN, tvar, freeN, free, skolemN, skolem, boundN, bound, varN, var,
  numeralN, numeral, literalN, literal, delimiterN, delimiter, inner_stringN, inner_string,
  inner_cartoucheN, inner_cartouche, inner_commentN, inner_comment,
  token_rangeN, token_range,
  sortingN, sorting, typingN, typing, class_parameterN, class_parameter,

  antiquotedN, antiquoted, antiquoteN, antiquote,

  paragraphN, paragraph, text_foldN, text_fold,

  keyword1N, keyword1, keyword2N, keyword2, keyword3N, keyword3, quasi_keywordN, quasi_keyword,
  improperN, improper, operatorN, operator, stringN, string, alt_stringN, alt_string,
  verbatimN, verbatim, cartoucheN, cartouche, commentN, comment,

  writelnN, writeln, stateN, state, informationN, information, tracingN, tracing,
  warningN, warning, legacyN, legacy, errorN, error, reportN, report, no_reportN, no_report,

  intensifyN, intensify,
  Output, no_output)
where

import Prelude hiding (words, error, break)

import Isabelle.Library
import qualified Isabelle.Properties as Properties
import qualified Isabelle.Value as Value


{- basic markup -}

type T = (String, Properties.T)

empty :: T
empty = ("", [])

is_empty :: T -> Bool
is_empty ("", _) = True
is_empty _ = False

properties :: Properties.T -> T -> T
properties more_props (elem, props) =
  (elem, fold_rev Properties.put more_props props)

markup_elem name = (name, (name, []) :: T)


{- misc properties -}

nameN :: String
nameN = "name"

name :: String -> T -> T
name a = properties [(nameN, a)]

xnameN :: String
xnameN = "xname"

xname :: String -> T -> T
xname a = properties [(xnameN, a)]

kindN :: String
kindN = "kind"


{- position -}

lineN, end_lineN :: String
lineN = "line"
end_lineN = "end_line"

offsetN, end_offsetN :: String
offsetN = "offset"
end_offsetN = "end_offset"

fileN, idN :: String
fileN = "file"
idN = "id"

positionN :: String; position :: T
(positionN, position) = markup_elem "position"


{- pretty printing -}

markupN, consistentN, unbreakableN, indentN :: String
markupN = "markup";
consistentN = "consistent";
unbreakableN = "unbreakable";
indentN = "indent";

widthN :: String
widthN = "width"

blockN :: String
blockN = "block"
block :: Bool -> Int -> T
block c i =
  (blockN,
    (if c then [(consistentN, Value.print_bool c)] else []) ++
    (if i /= 0 then [(indentN, Value.print_int i)] else []))

breakN :: String
breakN = "break"
break :: Int -> Int -> T
break w i =
  (breakN,
    (if w /= 0 then [(widthN, Value.print_int w)] else []) ++
    (if i /= 0 then [(indentN, Value.print_int i)] else []))

fbreakN :: String; fbreak :: T
(fbreakN, fbreak) = markup_elem "fbreak"

itemN :: String; item :: T
(itemN, item) = markup_elem "item"


{- text properties -}

wordsN :: String; words :: T
(wordsN, words) = markup_elem "words"

no_wordsN :: String; no_words :: T
(no_wordsN, no_words) = markup_elem "no_words"


{- inner syntax -}

tfreeN :: String; tfree :: T
(tfreeN, tfree) = markup_elem "tfree"

tvarN :: String; tvar :: T
(tvarN, tvar) = markup_elem "tvar"

freeN :: String; free :: T
(freeN, free) = markup_elem "free"

skolemN :: String; skolem :: T
(skolemN, skolem) = markup_elem "skolem"

boundN :: String; bound :: T
(boundN, bound) = markup_elem "bound"

varN :: String; var :: T
(varN, var) = markup_elem "var"

numeralN :: String; numeral :: T
(numeralN, numeral) = markup_elem "numeral"

literalN :: String; literal :: T
(literalN, literal) = markup_elem "literal"

delimiterN :: String; delimiter :: T
(delimiterN, delimiter) = markup_elem "delimiter"

inner_stringN :: String; inner_string :: T
(inner_stringN, inner_string) = markup_elem "inner_string"

inner_cartoucheN :: String; inner_cartouche :: T
(inner_cartoucheN, inner_cartouche) = markup_elem "inner_cartouche"

inner_commentN :: String; inner_comment :: T
(inner_commentN, inner_comment) = markup_elem "inner_comment"


token_rangeN :: String; token_range :: T
(token_rangeN, token_range) = markup_elem "token_range"


sortingN :: String; sorting :: T
(sortingN, sorting) = markup_elem "sorting"

typingN :: String; typing :: T
(typingN, typing) = markup_elem "typing"

class_parameterN :: String; class_parameter :: T
(class_parameterN, class_parameter) = markup_elem "class_parameter"


{- antiquotations -}

antiquotedN :: String; antiquoted :: T
(antiquotedN, antiquoted) = markup_elem "antiquoted"

antiquoteN :: String; antiquote :: T
(antiquoteN, antiquote) = markup_elem "antiquote"


{- text structure -}

paragraphN :: String; paragraph :: T
(paragraphN, paragraph) = markup_elem "paragraph"

text_foldN :: String; text_fold :: T
(text_foldN, text_fold) = markup_elem "text_fold"


{- outer syntax -}

keyword1N :: String; keyword1 :: T
(keyword1N, keyword1) = markup_elem "keyword1"

keyword2N :: String; keyword2 :: T
(keyword2N, keyword2) = markup_elem "keyword2"

keyword3N :: String; keyword3 :: T
(keyword3N, keyword3) = markup_elem "keyword3"

quasi_keywordN :: String; quasi_keyword :: T
(quasi_keywordN, quasi_keyword) = markup_elem "quasi_keyword"

improperN :: String; improper :: T
(improperN, improper) = markup_elem "improper"

operatorN :: String; operator :: T
(operatorN, operator) = markup_elem "operator"

stringN :: String; string :: T
(stringN, string) = markup_elem "string"

alt_stringN :: String; alt_string :: T
(alt_stringN, alt_string) = markup_elem "alt_string"

verbatimN :: String; verbatim :: T
(verbatimN, verbatim) = markup_elem "verbatim"

cartoucheN :: String; cartouche :: T
(cartoucheN, cartouche) = markup_elem "cartouche"

commentN :: String; comment :: T
(commentN, comment) = markup_elem "comment"


{- messages -}

writelnN :: String; writeln :: T
(writelnN, writeln) = markup_elem "writeln"

stateN :: String; state :: T
(stateN, state) = markup_elem "state"

informationN :: String; information :: T
(informationN, information) = markup_elem "information"

tracingN :: String; tracing :: T
(tracingN, tracing) = markup_elem "tracing"

warningN :: String; warning :: T
(warningN, warning) = markup_elem "warning"

legacyN :: String; legacy :: T
(legacyN, legacy) = markup_elem "legacy"

errorN :: String; error :: T
(errorN, error) = markup_elem "error"

reportN :: String; report :: T
(reportN, report) = markup_elem "report"

no_reportN :: String; no_report :: T
(no_reportN, no_report) = markup_elem "no_report"

intensifyN :: String; intensify :: T
(intensifyN, intensify) = markup_elem "intensify"


{- output -}

type Output = (String, String)

no_output :: Output
no_output = ("", "")
