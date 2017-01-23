package gedi.util.algorithm.string.alignment.pairwise.scoring;

public interface Scoring<A> {

	/**
	 * Gets the score for position p1 in the first sequence and position p2 in the second.
	 * @param p1
	 * @param p2
	 * @return
	 */
	public float getFloat(int p1, int p2);
	
	public boolean containsSubject(String id);
	public A getCachedSubject(String id);
	public void cacheSubject(String id, A s);
	public void loadCachedSubjects(String id1, String id2);
	public void setSubjects(A s1, A s2);
	
	public int getLength1();
	public int getLength2();

	public void clearCache();

}
