/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */

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
