package gedi.util.algorithm.string.alignment.pairwise.util;

import gedi.util.algorithm.string.alignment.pairwise.Aligner;
import gedi.util.algorithm.string.alignment.pairwise.Alignment;
import gedi.util.algorithm.string.alignment.pairwise.AlignmentMode;
import gedi.util.algorithm.string.alignment.pairwise.gapCostFunctions.GapCostFunction;
import gedi.util.algorithm.string.alignment.pairwise.scoring.Scoring;

public class CheckScore {
	
	private float eps = 1e-3f;
	
	public void setEps(float eps) {
		this.eps = eps;
	}
	
	public float getEps() {
		return eps;
	}

	public void checkScore(Aligner<?> aligner, int n, int m, Alignment alignment, float score) {
		float check = getScore(aligner, n, m, alignment);
		if (Math.abs(score-check)>eps)
			throw new RuntimeException("Checkscore difference is too big! (Checkscore="+check+", expected="+score+", d="+Math.abs(score-check)+")");
	}
	
	public float diffScore(Aligner<?> aligner, int n, int m, Alignment alignment, float score) {
		return score-getScore(aligner, n, m, alignment);
	}
	
	public float getScore(Aligner<?> aligner, int n, int m, Alignment alignment) {
		GapCostFunction gap = aligner.getGapCostFunction();
		Scoring<?> scoring = aligner.getScoring();
		AlignmentMode mode = aligner.getMode();
		
		float checkscore=0;

		int li=-1,lj=-1;
		int[] point = new int[2];

		for (int ai=0; ai<alignment.length(); ai++)
		{
			
			alignment.getPoint(ai,point);

			if (li!=-1)
			{
				int gap1=point[0]-li-1;
				int gap2=point[1]-lj-1;

				if (gap1>0)
					checkscore+=gap.getGapCost(gap1);
				if (gap2>0)
					checkscore+=gap.getGapCost(gap2);
			}

			checkscore+=scoring.getFloat(point[0],point[1]);
			li=point[0]; lj=point[1];
		}

		//freeshift add end costs
		int open_xgap = 0;
		int open_ygap = 0;
		int end_ygap = 0;
		int end_xgap = 0;
		int open_gaps=0;
		int end_gaps=0;

		if (alignment.length()>0)
		{
			alignment.getPoint(0,point);
			open_xgap=point[0];
			open_ygap= point[1];
			open_gaps = Math.min(open_xgap, open_ygap);

			alignment.getPoint(alignment.length()-1,point);
			end_xgap = n-point[0]-1;
			end_ygap = m-point[1]-1;
			end_gaps = Math.min(end_xgap, end_ygap);
			
			switch(mode)
			{
			case Freeshift:
				if (open_gaps>0)
					checkscore+=gap.getGapCost(open_gaps);
				if (end_gaps>0)
					checkscore+=gap.getGapCost(end_gaps);
				break;
			case Global:
				if(open_xgap>0)
					checkscore+=gap.getGapCost(open_xgap);
				if(open_ygap>0)
					checkscore+=gap.getGapCost(open_ygap);
				if (end_xgap>0)
					checkscore+=gap.getGapCost(end_xgap);
				if (end_ygap>0)
					checkscore+=gap.getGapCost(end_ygap);
				break;
			case PrefixSuffix:
				if(open_xgap>0)
					checkscore+=gap.getGapCost(open_xgap);
				if (end_ygap>0)
					checkscore+=gap.getGapCost(end_ygap);
				break;
			case Local:
				break;
			}
		}
		else
		{
			if (mode==AlignmentMode.Global)
			{
				checkscore+=gap.getGapCost(n);
				checkscore+=gap.getGapCost(m);
			}
		}

		return checkscore;

	}
}
