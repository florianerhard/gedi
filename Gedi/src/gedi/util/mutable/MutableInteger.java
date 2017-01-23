package gedi.util.mutable;

import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.FixedSizeBinarySerializable;

import java.io.IOException;

public class MutableInteger extends Number implements Comparable<MutableInteger>, FixedSizeBinarySerializable, Mutable {
	public int N;
	public MutableInteger() {
	}
	
	public MutableInteger(int N) {
		this.N = N;
	}
	
	public MutableInteger set(int N) {
		this.N = N;
		return this;
	}
	
	public int preIncrement() {
		return ++N;
	}
	public int postIncrement() {
		return N++;
	}
	public int preDecrement() {
		return --N;
	}
	public int postDecrement() {
		return N--;
	}
	
	@Override
	public String toString() {
		return N+"";
	}
	@Override
	public double doubleValue() {
		return N;
	}
	@Override
	public float floatValue() {
		return N;
	}
	@Override
	public int intValue() {
		return N;
	}
	@Override
	public long longValue() {
		return N;
	}
	@Override
	public int compareTo(MutableInteger o) {
		return Integer.compare(N,o.N);
	}
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MutableInteger))
			return false;
		MutableInteger n = (MutableInteger) obj;
		return N==n.N;
	}
	@Override
	public int hashCode() {
		return Integer.hashCode(N);
	}
	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putInt(N);
	}
	@Override
	public void deserialize(BinaryReader in) throws IOException {
		N = in.getInt();
	}
	@Override
	public int getFixedSize() {
		return Integer.BYTES;
	}
	
	@Override
	public int size() {
		return 1;
	}
	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(int index) {
		if (index==0) return (T)new Integer(N);
		throw new IndexOutOfBoundsException();
	}
	@SuppressWarnings("unchecked")
	@Override
	public <T> T set(int index, T o) {
		if (index==0) {
			T re = (T)new Integer(N);
			N = (Integer)o;
			return re;
		}
		throw new IndexOutOfBoundsException();
	}
}
