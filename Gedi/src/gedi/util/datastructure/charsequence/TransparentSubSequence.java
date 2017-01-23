package gedi.util.datastructure.charsequence;

import gedi.util.StringUtils;

public class TransparentSubSequence implements CharSequence {
	
	private CharSequence parent;
	private int start;
	private int len;
	
	public TransparentSubSequence(CharSequence parent, int start, int end) {
		if (parent instanceof TransparentSubSequence) {
			TransparentSubSequence t = (TransparentSubSequence) parent;
			this.parent = t.parent;
			this.start = t.start+start;
		} else {
			this.parent = parent;
			this.start = start;
		}
		this.len = end-start;
	}

	@Override
	public int length() {
		return len;
	}
	
	@Override
	public int hashCode() {
		return StringUtils.hashCode(this);
	}
	
	@Override
	public boolean equals(Object obj) {
		return StringUtils.equals(this, obj);
	}
	
	@Override
	public char charAt(int index) {
		return parent.charAt(index+start);
	}
	
	@Override
	public String toString() {
		return StringUtils.toString(this);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return new TransparentSubSequence(this, start, end);
	}

}
