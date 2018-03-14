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
package gedi.util.algorithm.string.alignment.pairwise;

public class Alignment {

	int[] alignment1;
	int[] alignment2;
	int alignmentCount = 0;
	
	public Alignment() {
		this(1<<8);
	}
	
	public Alignment(int maxLength) {
		alignment1 = new int[maxLength];
		alignment2 = new int[maxLength];
	}
	
	private void ensureSize(int l) {
		if (alignment1.length<l) {
			int n = 1;
			for (;n<l; n<<=1);
			int[] alignment1 = new int[n];
			int[] alignment2 = new int[n];
			System.arraycopy(this.alignment1, 0, alignment1, 0, alignmentCount);
			System.arraycopy(this.alignment2, 0, alignment2, 0, alignmentCount);
			this.alignment1 = alignment1;
			this.alignment2 = alignment2;
		}
	}
	
	public void clear() {
		alignmentCount = 0;
	}
	
	public void add(int i, int j) {
		ensureSize(alignmentCount+1);
		alignment1[alignmentCount]=i;
		alignment2[alignmentCount]=j;
		alignmentCount++;
	}
	
	public int length() {
		return alignmentCount;
	}
	
	/**
	 * Gets an aligned position (leftmost position index=0)
	 * @param index
	 * @param point
	 */
	public void getPoint(int index, int[] point) {
		point[0] = alignment1[alignmentCount-1-index];
		point[1] = alignment2[alignmentCount-1-index];
	}
	
	/**
	 * Gets an aligned position in the first sequence (leftmost position index=0)
	 * @param index
	 * @param point
	 */
	public int getFirst(int index) {
		return alignment1[alignmentCount-1-index];
	}
	
	/**
	 * Gets an aligned position in the second sequence (leftmost position index=0)
	 * @param index
	 * @param point
	 */
	public int getSecond(int index) {
		return alignment2[alignmentCount-1-index];
	}
	
	public Alignment clone() {
		Alignment re = new Alignment();
		re.alignment1 = alignment1.clone();
		re.alignment2 = alignment2.clone();
		re.alignmentCount = alignmentCount;
		return re;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<length(); i++) {
			if (i>0) sb.append(",");
			sb.append(getFirst(i));
			sb.append("-");
			sb.append(getSecond(i));
		}
		return sb.toString();
	}
	
}
