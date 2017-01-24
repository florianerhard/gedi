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

package gedi.util.algorithm.mss;

import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.tree.redblacktree.Interval;


public class ScoreSubsequence implements Interval {
	
	private NumericArray parent;
	private int offset;
	private int length;
	private double sum = Double.NaN;
	public ScoreSubsequence(NumericArray parent, int start, int end) {
		super();
		this.parent = parent;
		this.offset = start;
		this.length = end-start;
	}
	
	public double getSum() {
		if (Double.isNaN(sum)) {
			sum = 0;
			for (int i=0; i<length; i++)
				sum=+get(i);
		}
		return sum;
	}
	
	public int getStartInParent() {
		return offset;
	}
	
	public int getEndInParent() {
		return offset+length;
	}
	
	public void setBoundaries(int start, int end) {
		this.offset = start;
		this.length = end-start;
	}
	
	public void setBoundariesRelative(int start, int end) {
		this.offset+=start;
		this.length+=end-start;
	}
	
	public double get(int i) {
		return parent.getDouble(i+offset);
	}

	public int length() {
		return length;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(offset).append("-").append(offset+length).append(" [");
		for (int i=offset; i<offset+length; i++) {
			if (i>offset)
				sb.append(",");
			sb.append(parent.get(i));
		}
		sb.append("]");
		return sb.toString();
	}

	@Override
	public int getStart() {
		return offset;
	}

	@Override
	public int getStop() {
		return offset+length-1;
	}

}
