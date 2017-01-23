package gedi.util.algorithm.mss;

import gedi.util.datastructure.array.NumericArray;


public class MaximalScoringSubsequence {

	public ScoreSubsequence getMss(NumericArray a) {
		int n = a.length();
		int max = 0;
		int l = 1;
		int r = 0;
		int rmax = 0;
		int rstart = 0;
		for (int i=0; i<n; i++) {
			double x = a.getDouble(i);
			if (rmax+x>0)
				rmax+=x;
			else {
				rmax = 0;
				rstart = i;
			}
			if (rmax>max) {
				max = rmax;
				l = rstart;
				r = i;
			}
		}
		return new ScoreSubsequence(a, l, r+1);
	}
	
}
