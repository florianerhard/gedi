package gedi.util.algorithm.string.alignment.pairwise.scoring;


public class SubstitutionMatrix extends AbstractScoring<CharSequence, int[]> implements LongScoring<CharSequence> {

	private float[][] matrix;
	
	private char[] indexToChar;
	private int[] charToIndex;
	private long[][] values;
	private long mult=0;
	
	private int precision;
	
	public void ensurePrecision(int precision) {
		if (precision>this.precision)
			build(indexToChar, matrix, precision);
	}
	
	public void build(char[] chars, float[][] matrix, int precision) {
		indexToChar = chars.clone();
		this.matrix = matrix;
		this.precision = precision;
		
		charToIndex = new int[256];
		for (int i=0; i<indexToChar.length; i++) 
			charToIndex[indexToChar[i]] = i;

		mult = (long)Math.pow(10, precision);
		values = new long[indexToChar.length][indexToChar.length];
		for (int i=0; i<indexToChar.length; i++)
			for (int j=0; j<indexToChar.length; j++)
				values[i][j] = (long)Math.round(matrix[i][j]*mult);
	}
	
	public int[] encode(CharSequence sequence) {
		int[] re = new int[sequence.length()];
		for (int i=0; i<sequence.length(); i++)
			re[i] = charToIndex[sequence.charAt(i)];
		return re;
	}
	
	public CharSequence decode(int[] sequence) {
		char[] re = new char[sequence.length];
		for (int i=0; i<sequence.length; i++)
			re[i] = indexToChar[sequence[i]];
		return new String(re);
	}
	
	
	public long getLong(int i, int j) {
		return values[s1[i]][s2[j]];
	}
	
	public float getFloat(int i, int j) {
		return matrix[s1[i]][s2[j]];
	}
	
	
	public float correct(long score) {
		return score/(float)mult;
	}
	
	public long correct(float param) {
		return (long) (param*mult);
	}
	
	@Override
	public int length(int[] s) {
		return s.length;
	}

	public float getCorrectionFactor() {
		return mult;
	}
	
}
