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
