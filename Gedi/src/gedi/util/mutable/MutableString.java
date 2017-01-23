package gedi.util.mutable;

import gedi.util.StringUtils;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

import java.io.IOException;

public class MutableString implements CharSequence, Comparable<MutableString>, BinarySerializable, Mutable {
	public String s;
	public MutableString() {
	}
	
	public MutableString(String s) {
		this.s = s;
	}
	
	public MutableString set(String s) {
		this.s = s;
		return this;
	}
	
	
	@Override
	public String toString() {
		return s;
	}
	@Override
	public int compareTo(MutableString o) {
		return StringUtils.compare(s,o.s);
	}
	@Override
	public boolean equals(Object obj) {
		return StringUtils.equals(this, obj);
	}
	@Override
	public int hashCode() {
		return StringUtils.hashCode(this);
	}
	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putString(s==null?"":s);
	}
	@Override
	public void deserialize(BinaryReader in) throws IOException {
		s = in.getString();
	}

	@Override
	public int length() {
		return s==null?0:s.length();
	}

	@Override
	public char charAt(int index) {
		return s.charAt(index);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return s.substring(start, end);
	}
	
	@Override
	public int size() {
		return 1;
	}
	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(int index) {
		if (index==0) return (T)s;
		throw new IndexOutOfBoundsException();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T set(int index, T o) {
		if (index==0) {
			T re = (T)s;
			s = (String)o;
			return re;
		}
		throw new IndexOutOfBoundsException();
	}
}
