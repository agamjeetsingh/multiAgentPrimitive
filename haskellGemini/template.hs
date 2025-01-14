module Exam where

import Data.Char
import Data.List (nub)

type Symbol = String
type Rule = (Symbol, [Symbol]) -- (Left-hand side, Right-hand side)
type PCFG = ([Symbol], [Symbol], Symbol, [(Rule, Double)]) -- (Non-terminals, Terminals, Start symbol, Rules with probabilities)

-----------------------------------------------------------------------
-- Example PCFG
pcfgExample :: PCFG
pcfgExample = (["S", "NP", "VP", "N", "V"], -- Non-terminals
               ["cat", "dog", "eats"],       -- Terminals
               "S",                          -- Start symbol
               [ (("S", ["NP", "VP"]), 1.0)
               , (("NP", ["N"]), 0.6)
               , (("NP", ["NP", "NP"]), 0.4) -- Allow recursive NPs like "cat dog"
               , (("VP", ["V", "NP"]), 1.0)
               , (("N", ["cat"]), 0.5)
               , (("N", ["dog"]), 0.5)
               , (("V", ["eats"]), 1.0)
               ])

-----------------------------------------------------------------------
-- Part I

isTerminal :: PCFG -> Symbol -> Bool
isTerminal (_, ss, _, _) s = s `elem` ss

getRules :: PCFG -> Symbol -> [(Rule, Double)]
getRules (_, _, _, rs) s = [x | x@((s', s's), d) <- rs, s' == s]

isValidSentence :: PCFG -> [Symbol] -> Bool
isValidSentence pcfg ss = and (map (isTerminal pcfg) ss)

-----------------------------------------------------------------------
-- Part II

getBinaryRules :: PCFG -> [((Rule, Double))]
getBinaryRules = undefined

cyk :: PCFG -> [Symbol] -> Double
cyk = undefined

-----------------------------------------------------------------------
-- Part III
addUnknownWords :: Double -> PCFG -> PCFG
addUnknownWords = undefined

preprocessSentence :: PCFG -> [Symbol] -> [Symbol]
preprocessSentence pcfg@(nts, ts, start, rules) sent = undefined

cykUNK :: PCFG -> [Symbol] -> Double
cykUNK = undefined
-----------------------------------------------------------------------
-- Part IV (Tie-breaker)

-- Data type for representing parse trees
data ParseTree = ParseTree Symbol [ParseTree] | Terminal Symbol deriving (Show, Eq)

-- Modified CYK that also returns the parse tree
cykParse :: PCFG -> [Symbol] -> (Double, Maybe ParseTree)
cykParse = undefined