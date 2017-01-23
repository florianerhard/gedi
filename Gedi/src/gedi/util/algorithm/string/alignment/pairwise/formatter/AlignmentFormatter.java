package gedi.util.algorithm.string.alignment.pairwise.formatter;

import gedi.util.algorithm.string.alignment.pairwise.Alignment;
import gedi.util.algorithm.string.alignment.pairwise.AlignmentMode;
import gedi.util.algorithm.string.alignment.pairwise.gapCostFunctions.GapCostFunction;
import gedi.util.algorithm.string.alignment.pairwise.scoring.Scoring;

public interface AlignmentFormatter<A, S extends Scoring<A>,R> {

	R format(Alignment alignment, S scoring, GapCostFunction gap, AlignmentMode mode, A s1, A s2);
}
