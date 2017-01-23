package gedi.util.datastructure.array;

import gedi.util.datastructure.array.decorators.NumericArraySlice;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.datastructure.collections.doublecollections.DoubleIterator;
import gedi.util.datastructure.collections.intcollections.IntIterator;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryReaderWriter;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.PageFileReaderWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

import java.io.File;
import java.io.IOException;
import java.util.AbstractList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

public interface NumericArray extends BinarySerializable {


	public static enum NumericArrayType {
		Byte {
			@Override
			public Class<? extends Number> getType() {
				return java.lang.Byte.TYPE;
			}
			@Override
			NumericArray createMemory(int length) {
				return new MemoryByteArray(length);
			}
			@Override
			NumericArray createDisk() {
				return new DiskByteArray();
			}
			@Override
			public int getBytes() {
				return java.lang.Byte.BYTES;
			}
		},
		Short {
			@Override
			public Class<? extends Number> getType() {
				return java.lang.Short.TYPE;
			}
			@Override
			NumericArray createMemory(int length) {
				return new MemoryShortArray(length);
			}
			@Override
			NumericArray createDisk() {
				return new DiskShortArray();
			}
			@Override
			public int getBytes() {
				return java.lang.Short.BYTES;
			}
		},
		Integer {
			@Override
			public Class<? extends Number> getType() {
				return java.lang.Integer.TYPE;
			}
			@Override
			NumericArray createMemory(int length) {
				return new MemoryIntegerArray(length);
			}
			@Override
			NumericArray createDisk() {
				return new DiskIntegerArray();
			}
			@Override
			public int getBytes() {
				return java.lang.Integer.BYTES;
			}
		},
		Long {
			@Override
			public Class<? extends Number> getType() {
				return java.lang.Long.TYPE;
			}
			@Override
			NumericArray createMemory(int length) {
				return new MemoryLongArray(length);
			}
			@Override
			NumericArray createDisk() {
				return new DiskLongArray();
			}
			@Override
			public int getBytes() {
				return java.lang.Long.BYTES;
			}
		},
		Float {
			@Override
			public Class<? extends Number> getType() {
				return java.lang.Float.TYPE;
			}
			@Override
			NumericArray createMemory(int length) {
				return new MemoryFloatArray(length);
			}
			@Override
			NumericArray createDisk() {
				return new DiskFloatArray();
			}
			@Override
			public int getBytes() {
				return java.lang.Float.BYTES;
			}
		},
		Double {
			@Override
			public Class<? extends Number> getType() {
				return java.lang.Double.TYPE;
			}
			@Override
			NumericArray createMemory(int length) {
				return new MemoryDoubleArray(length);
			}
			@Override
			NumericArray createDisk() {
				return new DiskDoubleArray();
			}
			@Override
			public int getBytes() {
				return java.lang.Double.BYTES;
			}
		};

		public abstract Class<? extends Number> getType();
		abstract NumericArray createMemory(int length);
		abstract NumericArray createDisk();
		public abstract int getBytes();

		public static NumericArrayType fromType(Class<? extends Number> type) {
			for (NumericArrayType t : values())
				if (t.getType()==type) return t;
			return null;
		}
	}

	NumericArray clear();

	void setInfimum(int index);
	void setSupremum(int index);

	void setByte(int index, byte value);
	void setShort(int index, short value);
	void setInt(int index, int value);
	void setLong(int index, long value);
	void setFloat(int index, float value);
	void setDouble(int index, double value);

	/**
	 * Must be of same length
	 * @param a
	 */
	void add(NumericArray a);

	void add(int index, byte value);
	void add(int index, short value);
	void add(int index, int value);
	void add(int index, long value);
	void add(int index, float value);
	void add(int index, double value);

	
	void mult(NumericArray a);

	void mult(int index, byte value);
	void mult(int index, short value);
	void mult(int index, int value);
	void mult(int index, long value);
	void mult(int index, float value);
	void mult(int index, double value);

	boolean isReadOnly();

	NumericArrayType getType();

	int length();

	byte getByte(int index);
	short getShort(int index);
	int getInt(int index);
	long getLong(int index);
	float getFloat(int index);
	double getDouble(int index);


	String format(int index);
	String formatDecimals(int index, int decimals);

	default double evaluate(NumericArrayFunction fun) {
		return fun.applyAsDouble(this);
	}

	default void applyInPlace(DoubleUnaryOperator op) {
		for (int i=0; i<length(); i++)
			setDouble(i, op.applyAsDouble(getDouble(i)));
	}

	/**
	 * Reads the appropriate number from reader (byte,short,int,long,float or double) and stores it in element index
	 * @param index
	 * @param reader
	 * @throws IOException
	 */
	void deserializeElement(int index, BinaryReader reader) throws IOException;
	/**
	 * Writes the number stored at index to the given writer, writing the appropriate type.
	 * @param index
	 * @param writer
	 * @throws IOException
	 */
	void serializeElement(int index, BinaryWriter writer) throws IOException;


	default int compareTo(int index, Number n) {
		return Double.compare(getDouble(index), n.doubleValue());
	}

	default boolean equals(int index, Number n) {
		return compareTo(index, n)==0;
	}


	int compare(int index1, int index2);
	/**
	 * Assumes that is is a cumulative sum and compares the non-cumulate entries index1 and index2
	 * @param index1
	 * @param index2
	 * @return
	 */
	int compareInCum(int index1, int index2);
	int compare(int index1, NumericArray a2, int index2);

	/**
	 * Inplace, returns this for chaining
	 * @param increasing
	 * @return
	 */
	default NumericArray sort() {
		return sort(0,length());
	}
	/**
	 * Inplace, returns this for chaining
	 * @return
	 */
	default NumericArray cumSum() {
		return cumSum(0,length());
	}
	/**
	 * Inplace, returns this for chaining
	 * @return
	 */
	default NumericArray deCumSum() {
		return deCumSum(0,length());
	}

	Number get(int index);
	void set(int index, Number n);

	default NumericArray reverse() {
		return reverse(0,length());
	}

	default NumericArray createMemoryCopy() {
		NumericArray re = NumericArray.createMemory(length(), getType());
		copyRange(0, re, 0, length());
		return re;
	}
	default NumericArray copy(int from, int to) {
		return copy(this,from,to);
	}
	NumericArray copy(NumericArray fromArray, int fromIndex, int to);
	NumericArray copyRange(int start, NumericArray dest, int destOffset, int len);
	NumericArray switchElements(int i, int j);

	/**
	 * Inplace, returns this for chaining
	 * @param increasing
	 * @return
	 */
	default NumericArray sort(int from, int to) {
		ArrayTimSort.sort(this, from, to, null, 0, 0);
		return this;
	}
	/**
	 * Inplace, returns this for chaining
	 * @return
	 */
	NumericArray cumSum(int from, int to);
	/**
	 * Inplace, returns this for chaining
	 * @return
	 */
	NumericArray deCumSum(int from, int to);
	/**
	 * Inplace, returns this for chaining
	 * @return
	 */
	default NumericArray reverse(int from, int to) {
		to--;
		while (from< to) {
			switchElements(from++,to--);
		}
		return this;
	}

	default NumericArraySlice slice(int start, int end) {
		return new NumericArraySlice(this,start,end);
	}

	default int binarySearch(NumericArray a, int index) {
		return binarySearch(a, index, 0, length());
	}
	default int binarySearch(NumericArray a, int index, int fromIndex, int toIndex) {
		int low = fromIndex;
		int high = toIndex - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
		int comp = compare(mid, a, index);

		if (comp < 0)
			low = mid + 1;
		else if (comp > 0)
			high = mid - 1;
		else
			return mid; // key found
		}
		return -(low + 1);  // key not found.
	}


	default NumericArray convert(NumericArrayType type) {
		if (type==getType()) return this;
		NumericArray re = createMemory(length(), type);
		copyRange(0, re, 0, length());
		return re;
	}

	default double[] toDoubleArray() {
		return toDoubleArray(0,length());
	}

	default double[] toDoubleArray(int start, int end) {
		double[] re = new double[end-start];
		for (int i=0; i<re.length; i++)
			re[i] = getDouble(start+i);
		return re;
	}

	default float[] toFloatArray() {
		return toFloatArray(0,length());
	}

	default float[] toFloatArray(int start, int end) {
		float[] re = new float[end-start];
		for (int i=0; i<re.length; i++)
			re[i] = getFloat(start+i);
		return re;
	}


	default byte[] toByteArray() {
		return toByteArray(0,length());
	}

	default byte[] toByteArray(int start, int end) {
		byte[] re = new byte[end-start];
		for (int i=0; i<re.length; i++)
			re[i] = getByte(start+i);
		return re;
	}

	default short[] toShortArray() {
		return toShortArray(0,length());
	}

	default short[] toShortArray(int start, int end) {
		short[] re = new short[end-start];
		for (int i=0; i<re.length; i++)
			re[i] = getShort(start+i);
		return re;
	}

	default int[] toIntArray() {
		return toIntArray(0,length());
	}

	default int[] toIntArray(int start, int end) {
		int[] re = new int[end-start];
		for (int i=0; i<re.length; i++)
			re[i] = getInt(start+i);
		return re;
	}

	default long[] toLongArray() {
		return toLongArray(0,length());
	}

	default long[] toLongArray(int start, int end) {
		long[] re = new long[end-start];
		for (int i=0; i<re.length; i++)
			re[i] = getLong(start+i);
		return re;
	}

	default Number[] toNumberArray() {
		Number[] re = new Number[length()];
		for (int i=0; i<re.length; i++)
			re[i] = get(i);
		return re;
	}

	default NumericArray copy() {
		return NumericArray.copyMemory(this);
	}

	public static void write(BinaryWriter writer, NumericArray array) throws IOException {
		writer.putByte(array.getType().ordinal());
		array.serialize(writer);
	}

	public static NumericArray readMemory(BinaryReader reader) throws IOException {
		NumericArray array = NumericArrayType.values()[reader.getByte()].createMemory(-1);
		array.deserialize(reader);
		return array;
	}

	public static NumericArray readDisk(BinaryReader reader) throws IOException {
		NumericArray array = NumericArrayType.values()[reader.getByte()].createDisk();
		array.deserialize(reader);
		return array;
	}

	public static NumericArray readDisk(BinaryReaderWriter reader, boolean write) throws IOException {
		NumericArray array = NumericArrayType.values()[reader.getByte()].createDisk();
		array.deserialize(reader);
		return array;
	}

	public static NumericArray wrap(byte[] a) {
		return new MemoryByteArray(a);
	}
	public static NumericArray wrap(short[] a) {
		return new MemoryShortArray(a);
	}
	public static NumericArray wrap(int[] a) {
		return new MemoryIntegerArray(a);
	}
	public static NumericArray wrap(long[] a) {
		return new MemoryLongArray(a);
	}
	public static NumericArray wrap(float[] a) {
		return new MemoryFloatArray(a);
	}
	public static NumericArray wrap(double[] a) {
		return new MemoryDoubleArray(a);
	}

	public static NumericArraySlice wrap(byte[] a, int start, int end) {
		return new MemoryByteArray(a).slice(start,end);
	}
	public static NumericArraySlice wrap(short[] a, int start, int end) {
		return new MemoryShortArray(a).slice(start,end);
	}
	public static NumericArraySlice wrap(int[] a, int start, int end) {
		return new MemoryIntegerArray(a).slice(start,end);
	}
	public static NumericArraySlice wrap(long[] a, int start, int end) {
		return new MemoryLongArray(a).slice(start,end);
	}
	public static NumericArraySlice wrap(float[] a, int start, int end) {
		return new MemoryFloatArray(a).slice(start,end);
	}
	public static NumericArraySlice wrap(double[] a, int start, int end) {
		return new MemoryDoubleArray(a).slice(start,end);
	}

	public static NumericArray copyMemory(NumericArray a) {
		NumericArray re = a.getType().createMemory(a.length());
		a.copyRange(0, re, 0, re.length());
		return re;
	}

	public static NumericArray createMemory(int length, NumericArrayType type) {
		return type.createMemory(length);
	}

	public static NumericArray createDisk(int length, NumericArrayType type) throws IOException {
		File f = File.createTempFile("GEDI", "na");
		f.deleteOnExit();

		return createDisk(new PageFileReaderWriter(f.getPath()), length, type);
	}

	public static NumericArray createDisk(BinaryReaderWriter data, int length, NumericArrayType type) throws IOException {
		data.putInt(length);
		data.position(data.position()-Integer.BYTES);
		NumericArray re = type.createDisk();
		re.deserialize(data);
		return re;
	}
	boolean isIntegral();
	default boolean isNA(int index) { 
		return Double.isNaN(getDouble(index));
	}


	default String toTableString() {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<length(); i++) {
			if (i>0) sb.append("\n");
			sb.append(i).append("\t").append(format(i));
		}
		return sb.toString();
	}

	default String toArrayString() {
		int iMax = length() - 1;
		if (iMax == -1)
			return "[]";

		StringBuilder b = new StringBuilder();
		b.append('[');
		for (int i = 0; ; i++) {
			b.append(format(i));
			if (i == iMax)
				return b.append(']').toString();
			b.append(", ");
		}
	}
	
	default IntIterator intIterator() {
		return new IntIterator() {
			int index = 0;
			@Override
			public int nextInt() {
				return getInt(index++);
			}
			
			@Override
			public boolean hasNext() {
				return index<length();
			}
		};
	}
	
	default DoubleIterator doubleIterator() {
		return new DoubleIterator() {
			int index = 0;
			@Override
			public double nextDouble() {
				return getDouble(index++);
			}
			
			@Override
			public boolean hasNext() {
				return index<length();
			}
		};
	}
	
	default ExtendedIterator<Number> iterator() {
		return new ExtendedIterator<Number>() {
			int index = 0;
			@Override
			public Number next() {
				return get(index++);
			}
			
			@Override
			public boolean hasNext() {
				return index<length();
			}
		};
	}

	void parseElement(int index, String s);
	
	
	default NumericArray parse(String[] a) {
		for (int i=0; i<a.length; i++) {
			parseElement(i,a[i]);
		}
		return this;
	}

	default List<? extends Number> asList() {
		return new AbstractList<Number>() {

			@Override
			public Number get(int index) {
				return NumericArray.this.get(index);
			}

			@Override
			public int size() {
				return NumericArray.this.length();
			}

			@Override
			public Number set(int index, Number element) {
				Number re = get(index);
				NumericArray.this.set(index, element);
				return re;
			}
		};
	}

}
