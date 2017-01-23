package gedi.util.algorithm.string.alignment.pairwise.scoring;

public interface LongScoring<A> extends Scoring<A> {

	public long getLong(int p1, int p2);

	public int getMaxLength();

	public long correct(float score);
	public float correct(long parameter);

	public void ensurePrecision(int precision);

}
