package gedi.util.datastructure.collections.bitcollections;

import java.util.Arrays;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.stat.descriptive.UnivariateStatistic;

import cern.colt.bitvector.BitVector;
import gedi.util.ArrayUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.collections.CapacityMode;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.datastructure.collections.doublecollections.DoubleIterator;
import gedi.util.math.stat.RandomNumbers;

public class BitList extends AbstractBitCollection {



	public final static int INITIAL_SIZE = 100;

	protected BitVector bv;
	protected int count;

	protected CapacityMode mode = CapacityMode.NextMultiplicativeOf2;

	public BitList() {
		this(INITIAL_SIZE);
	}

	public BitList(int initialSize) {
		this.bv = new BitVector(initialSize);
		this.count = 0;
	}

	public BitList(boolean[] a) {
		this.bv = new BitVector(a.length);
		for (int i=0; i<a.length; i++)
			bv.putQuick(i, a[i]);
		this.count = a.length;
	}
	
	public BitList(BitVector bv) {
		this.bv = bv;
		this.count = bv.size();
	}

	public void setMode(CapacityMode mode) {
		this.mode = mode;
	}

	public CapacityMode getMode() {
		return mode;
	}

	@Override
	public BitIterator iterator() {
		return new BitIterator.BitVectorIterator(bv,0,count);
	}

	/**
	 * Clears the array in constant time.
	 */
	public void clear() {
		count = 0;
	}


	public boolean getBit(int index) {
		return index<count?bv.getQuick(index):false;
	}

	public boolean add(boolean i) {
		ensureCapacity(count+1);
		bv.putQuick(count++, i);
		return true;
	}

	public boolean add(Boolean i) {
		ensureCapacity(count+1);
		bv.putQuick(count++, i);
		return true;
	}

	public void set(int index, boolean val) {
		for (int i=count; i<Math.min(index,bv.size()); i++)
			bv.putQuick(i, false);
		ensureCapacity(index+1);
		count = Math.max(count,index+1);
		bv.putQuick(index, val);
	}

	public void flip(int index) {
		set(index, !getBit(index));
	}

	public void addAll(BitList a) {
		ensureCapacity(count+a.count);
		bv.replaceFromToWith(count, count+a.count, a.bv, 0);
		count+=a.count;
	}

	public void addAll(boolean[] a) {
		ensureCapacity(count+a.length);
		for (int i=0; i<a.length; i++)
			bv.putQuick(count++, a[i]);
	}

	/**
	 * Adds all elements from a, starting from s (inclusive) to end (exclusive)
	 * @param a
	 * @param s
	 * @param e
	 */
	public void addAll(boolean[] a, int s, int e) {
		ensureCapacity(count+e-s);
		for (int i=s; i<e; i++)
			bv.putQuick(count++, a[i]);
	}

	public boolean getLastBit() {
		return getBit(size()-1);
	}

	public boolean getLastBit(int i) {
		return getBit(size()-i-1);
	}

	public boolean removeLast() {
		count--;
		return bv.getQuick(count);
	}

	public boolean removeEntry(int index) {
		boolean re = bv.getQuick(index);
		bv.putQuick(index, bv.getQuick(--count));
		return re;
	}

	public int size() {
		return count;
	}

	public boolean isEmpty() {
		return size()==0;
	}


	public void shuffle(RandomNumbers stochastics) {
		boolean intTemp;
		for (int i=0; i<size(); i++) {
			int j = stochastics.getUnif(i, size());
			if (j!=i) {
				intTemp = bv.getQuick(i);
				bv.putQuick(i, bv.getQuick(j));
				bv.putQuick(j, intTemp);
			}
		}
	}

	private void ensureCapacity(int cap) {
		if (bv==null || bv.size()<=cap) {
			switch (mode) {
			case Exact:
				bv.setSize(cap);
				break;
			case NextMultiplicativeOf2:
				int nm = 1;
				for (;nm<cap; nm<<=1);
				bv.setSize(nm);
				break;
			default:
				throw new RuntimeException("Mode "+mode.toString()+" not supported!");
			}
		}
	}

	@Override
	public String toString() {
		return toString("[",",","]");
	}

	public String toString(String prefix, String infix, String suffix) {
		StringBuilder sb = new StringBuilder();
		sb.append(prefix);
		if (size()>0)
			sb.append(getBit(0));
		for (int i=1; i<size(); i++) {
			sb.append(infix);
			sb.append(getBit(i));
		}
		sb.append(suffix);
		return sb.toString();
	}

	public BitList clone() {
		BitList re = new BitList(bv.copy());
		re.count = count;
		return re;
	}

	public BitVector getRaw() {
		return bv;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof BitList)) return false;
		BitList o = (BitList) obj;
		if (count!=o.count) return false;
		
		for (int i=0; i<count; i++)
			if (bv.getQuick(i)!=o.bv.getQuick(i))
				return false;
		return true;
	}

	@Override
	public int hashCode() {
		int result = 1;
		for (int i=0; i<count; i++) {
			int h = getBit(i)?1<<(i%32):0;
			result = 31 * result + h;
		}

		return result;
	}


	
}
