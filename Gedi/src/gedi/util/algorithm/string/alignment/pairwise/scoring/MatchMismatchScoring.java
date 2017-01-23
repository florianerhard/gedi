package gedi.util.algorithm.string.alignment.pairwise.scoring;


public class MatchMismatchScoring extends AbstractScoring<CharSequence, char[]> implements LongScoring<CharSequence> {

	private float match;
	private float mismatch;
	
	private long lma;
	private long lmm;
	private long mult=0;
	
	
	public MatchMismatchScoring(float match, float mismatch) {
		build(match,mismatch,
				Math.max(SubstitutionMatrix.inferPrecision(match),SubstitutionMatrix.inferPrecision(mismatch))
		);
	}
	
	public void ensurePrecision(int precision) {
		build(match, mismatch, precision);
	}
	
	public void build(float match, float mismatch, int precision) {
		this.match = match;
		this.mismatch = mismatch;
		mult = (long)Math.pow(10, precision);
		this.lma = (long)Math.round(match*mult);
		this.lmm = (long)Math.round(mismatch*mult);
	}
	
	public long getLong(int i, int j) {
		return s1[i]==s2[j]?lma:lmm;
	}
	
	public float getFloat(int i, int j) {
		return s1[i]==s2[j]?match:mismatch;
	}
	
	public CharSequence decode(char[] s) {
		return new String(s);
	}
	
	public char[] encode(CharSequence s) {
		char[] re = new char[s.length()];
		int n = s.length();
		for (int i=0; i<n; i++)
			re[i] =s.charAt(i);
		return re;
	}

	public float correct(long score) {
		return score/(float)mult;
	}
	
	public long correct(float param) {
		return (long) (param*mult);
	}
	
	@Override
	public int length(char[] s) {
		return s.length;
	}

	public float getCorrectionFactor() {
		return mult;
	}
	
	
}
