package gedi.util.datastructure.array;

import gedi.util.io.randomaccess.BinaryReader;

import java.io.IOException;
import java.util.Arrays;

public class MemoryShortArray extends ShortArray {
	
	private short[] a;
	
	public MemoryShortArray() {
	}
	
	public MemoryShortArray(short[] wrap) {
		this.a = wrap;
	}
	public MemoryShortArray(int length) {
		if (length>=0) a = new short[length];
	}

	@Override
	public void setShort(int index, short value) {
		a[index] = value;
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Override
	public int length() {
		return a.length;
	}

	@Override
	public short getShort(int index) {
		return a[index];
	}
	@Override
	public NumericArray clear() {
		Arrays.fill(a, (short)0);
		return this;
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		int len = in.getInt();
		if (a==null || a.length!=len)
			a = new short[len];
		for (int i=0; i<a.length; i++)
			a[i] = in.getShort();
	}
	
	
	@Override
	public int compare(int index1, NumericArray a2, int index2) {
		return Short.compare(a[index1], a2.getShort(index2));
	}

	@Override
	public int compare(int index1, int index2) {
		return Short.compare(a[index1], a[index2]);
	}
	
	@Override
	public int compareInCum(int index1, int index2) {
		int b1 = index1==0?a[index1]:(a[index1]-a[index1-1]);
		int b2 = index2==0?a[index2]:(a[index2]-a[index2-1]);
		return Integer.compare(b1,b2);
	}
	
	
	@Override
	public NumericArray copy(int from, int to) {
		a[to] = a[from];
		return this;
	}
	
	@Override
	public NumericArray sort(int from, int to) {
		Arrays.sort(a, from, to);
		return this;
	}
	
	@Override
	public NumericArray reverse(int from, int to) {
		to--;
        while (from< to) {
        	short tmp = a[from];
    		a[from++] = a[to];
    		a[to++] = tmp;
        }
        return this;
	}
	

	@Override
	public NumericArray cumSum(int from, int to) {
		for (int i=from+1; i<to; i++)
			a[i] += a[i-1];
		return this;
	}

	@Override
	public NumericArray deCumSum(int from, int to) {
		for (int i=to-1; i>from; i--)
			a[i] -= a[i-1];
		return this;
	}

	@Override
	public NumericArray switchElements(int i, int j) {
		short tmp = a[i];
		a[i] = a[j];
		a[j] = tmp;
		return this;
	}

	@Override
	public NumericArray copy(NumericArray fromArray, int fromIndex, int to) {
		a[to]=fromArray.getShort(fromIndex);
		return this;
	}

	@Override
	public NumericArray copyRange(int start, NumericArray dest, int destOffset,
			int len) {
		if (dest instanceof MemoryShortArray)
			System.arraycopy(a, start, ((MemoryShortArray)dest).a, destOffset, len);
		else
			return super.copyRange(start, dest, destOffset, len);
		return this;
	}


}
