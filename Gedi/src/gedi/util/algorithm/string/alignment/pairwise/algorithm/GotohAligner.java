package gedi.util.algorithm.string.alignment.pairwise.algorithm;

import gedi.util.algorithm.string.alignment.pairwise.AbstractAligner;
import gedi.util.algorithm.string.alignment.pairwise.Alignment;
import gedi.util.algorithm.string.alignment.pairwise.AlignmentMode;
import gedi.util.algorithm.string.alignment.pairwise.gapCostFunctions.AffineGapCostFunction;
import gedi.util.algorithm.string.alignment.pairwise.scoring.Scoring;



public class GotohAligner<A> extends AbstractAligner<A,A> {

	private FloatGotoh aligner = new FloatGotoh();
	
	private float gapOpen;
	private float gapExtend;
	
	
	public GotohAligner(Scoring<A> scoring, float gapOpen, float gapExtend, AlignmentMode mode) {
		super(scoring,new AffineGapCostFunction(gapOpen, gapExtend),mode);
		
		if (gapOpen>0 || gapExtend>0)
			throw new RuntimeException("Positive gap scores are not allowed!");
		
		this.gapOpen = gapOpen;
		this.gapExtend = gapExtend;
	}
	
	@Override
	public float align() {
		return aligner.align(scoring, scoring.getLength1(), scoring.getLength2(), gapOpen, gapExtend, mode);
	}
	
	@Override
	public float align(Alignment alignment) {
		return aligner.align(scoring, scoring.getLength1(), scoring.getLength2(), gapOpen, gapExtend, mode,alignment);
	}
	
	
}
