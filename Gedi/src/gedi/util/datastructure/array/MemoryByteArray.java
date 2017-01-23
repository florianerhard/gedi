package gedi.util.datastructure.array;

import gedi.util.io.randomaccess.BinaryReader;

import java.io.IOException;
import java.util.Arrays;

public class MemoryByteArray extends ByteArray {
	
	private byte[] a;
	
	public MemoryByteArray() {
	}
	
	public MemoryByteArray(byte[] wrap) {
		this.a = wrap;
	}
	public MemoryByteArray(int length) {
		if (length>=0) a = new byte[length];
	}

	@Override
	public void setByte(int index, byte value) {
		a[index] = value;
	}

	@Override
	public NumericArray clear() {
		Arrays.fill(a, (byte)0);
		return this;
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
	public byte getByte(int index) {
		return a[index];
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		int len = in.getInt();
		if (a==null || a.length!=len)
			a = new byte[len];
		for (int i=0; i<a.length; i++)
			a[i] = in.get();
	}
	
	
	@Override
	public int compare(int index1, NumericArray a2, int index2) {
		return Byte.compare(a[index1], a2.getByte(index2));
	}

	@Override
	public int compare(int index1, int index2) {
		return Byte.compare(a[index1], a[index2]);
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
        	byte tmp = a[from];
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
		byte tmp = a[i];
		a[i] = a[j];
		a[j] = tmp;
		return this;
	}

	@Override
	public NumericArray copy(NumericArray fromArray, int fromIndex, int to) {
		a[to]=fromArray.getByte(fromIndex);
		return this;
	}

	@Override
	public NumericArray copyRange(int start, NumericArray dest, int destOffset,
			int len) {
		if (dest instanceof MemoryByteArray)
			System.arraycopy(a, start, ((MemoryByteArray)dest).a, destOffset, len);
		else
			return super.copyRange(start, dest, destOffset, len);
		return this;
	}

}
