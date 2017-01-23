package gedi.util.math.stat;

import gedi.util.ArrayUtils;


public class DoubleRanking {

	/**
	 * are always maintained to be in the same order!
	 */
	private double[] data;
	private int[] ranks;
	private int[] iranks;
	
	
	/**
	 * Call sort!
	 * @param data
	 */
	public DoubleRanking(double[] data) {
		this.data = data;
		ranks = ArrayUtils.seq(0, data.length-1, 1);
		iranks = ranks.clone();
	}
	
	public double[] getData() {
		return data;
	}
	
	public int[] getRanks() {
		return ranks;
	}
	
	public int getOriginalIndex(int currentIndex) {
		return ranks[currentIndex];
	}
	public int getCurrentRank(int originalIndex) {
		return iranks[originalIndex];
	}
	public double getValue(int currentIndex) {
		return data[currentIndex];
	}
	
	
	/**
	 * The array is actially changed!
	 * @param ascending
	 * @return
	 */
	public DoubleRanking sort(boolean ascending) {
		ArrayUtils.parallelSort(data, ranks);
		if (!ascending) {
			ArrayUtils.reverse(data);
			ArrayUtils.reverse(ranks);
		}
		for (int i=0; i<ranks.length; i++)
			iranks[ranks[i]] = i;
		return this;
	}
	
	public void restore() {
		ArrayUtils.parallelSort(ranks, data);
		System.arraycopy(ranks, 0, iranks, 0, iranks.length);
	}

	public int size() {
		return data.length;
	}
	
}
