package gedi.util.algorithm.string.alignment.pairwise;

import gedi.util.algorithm.string.alignment.pairwise.formatter.SimpleAlignmentFormatter;
import gedi.util.algorithm.string.alignment.pairwise.gapCostFunctions.GapCostFunction;
import gedi.util.algorithm.string.alignment.pairwise.scoring.Scoring;

import java.util.Locale;

public interface Aligner<A> {
	
	default String alignFormat(A s1, A s2) {
		CharSequence c1 = (CharSequence) s1;
		CharSequence c2 = (CharSequence) s2;
		
		Alignment ali = new Alignment();
		float score = align(s1, s2, ali);
		
		int l1 = c1.length();
		int l2 = c2.length();
		int la = ali.length();
		int ma = 0;
		int mmp = 0;
		int mmn = 0;
		for (int i=0; i<ali.length(); i++)
			if (c1.charAt(ali.getFirst(i))==c2.charAt(ali.getSecond(i)))
				ma++;
			else if (getScoring().getFloat(ali.getFirst(i), ali.getSecond(i))>0)
				mmp++;
			else 
				mmn++;
		
		return "Score: "+score+" Lengths: "+l1+","+l2+" Alignment length: "+la+"\n"
				+ "Matches: "+ma+" Mismatches(+): "+mmp+" Mismatches(-): "+mmn+"\n"
				+ "Identity(%): "+String.format(Locale.US, "%.2f", 100.0*ma/(double)Math.min(l1, l2))+"\n"
				+new SimpleAlignmentFormatter(false).format(ali, (Scoring<CharSequence>) getScoring(), getGapCostFunction(), getMode(), (CharSequence)s1, (CharSequence)s2);
	}
	
	
	public float align(A s1, A s2);
	public float align(A s1, A s2, Alignment alignment);
	public float alignCache(String id1, String id2);
	public float alignCache(String id1, String id2, Alignment alignment);

	public GapCostFunction getGapCostFunction();
	public Scoring<A> getScoring();
	public AlignmentMode getMode();
	
}

