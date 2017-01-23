package gedi.util.math.stat;

import java.util.Comparator;

import gedi.util.ArrayUtils;


public class Ranking<T> {

	/**
	 * are always maintained to be in the same order!
	 */
	private T[] data;
	private int[] ranks;
	private int[] iranks;
	private Comparator<T> comp;
	
	
	/**
	 * Call sort!
	 * @param data
	 */
	public Ranking(T[] data, Comparator<T> comp) {
		this.data = data;
		this.comp = comp;
		ranks = ArrayUtils.seq(0, data.length-1, 1);
		iranks = ranks.clone();
	}
	
	public T[] getData() {
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
	public T getValue(int currentIndex) {
		return data[currentIndex];
	}
	
	
	public Ranking<T> sort(boolean ascending) {
		ArrayUtils.parallelSort(data, ranks, comp);
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
