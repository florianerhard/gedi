package gedi.util.datastructure.array;

import gedi.app.Config;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;

import java.io.IOException;

public abstract class DoubleArray implements NumericArray {
	
	
	@Override
	public String format(int index) {
		return String.format(Config.getInstance().getRealFormat(),getDouble(index));
	}
	
	@Override
	public String formatDecimals(int index, int decimals) {
		return String.format("%."+decimals+"f",getDouble(index));
	}
	
	@Override
	public boolean isIntegral() {
		return false;
	}
	
	@Override
	public void setInfimum(int index) {
		setDouble(index, Double.NEGATIVE_INFINITY);
	}
	
	@Override
	public void setSupremum(int index) {
		setDouble(index, Double.POSITIVE_INFINITY);
	}
	
	@Override
	public void parseElement(int index, String s) {
		setDouble(index, Double.parseDouble(s));
	}
	
	@Override
	public Number get(int index) {
		return getDouble(index);
	}
	
	@Override
	public void set(int index, Number n) {
		setDouble(index, n.doubleValue());
	}
	
	
	@Override
	public NumericArrayType getType() {
		return NumericArrayType.Double;
	}

	@Override
	public byte getByte(int index) {
		return (byte) getDouble(index);
	}

	@Override
	public short getShort(int index) {
		return (short) getDouble(index);
	}

	@Override
	public int getInt(int index) {
		return (int) getDouble(index);
	}

	@Override
	public long getLong(int index) {
		return (long) getDouble(index);
	}

	@Override
	public float getFloat(int index) {
		return (float) getDouble(index);
	}
	
	@Override
	public void serializeElement(int index, BinaryWriter writer) throws IOException {
		writer.putDouble(getDouble(index));
	}
	
	@Override
	public void deserializeElement(int index, BinaryReader reader) throws IOException {
		setDouble(index, reader.getDouble());
	}
	
	public int binarySearch(double n) {
		return binarySearch(n, 0, length());
	}
	public int binarySearch(double n, int fromIndex, int toIndex) {
		int low = fromIndex;
		int high = toIndex - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			int comp = Double.compare(getDouble(mid), n);

			if (comp < 0)
				low = mid + 1;
			else if (comp > 0)
				high = mid - 1;
			else
				return mid; // key found
		}
		return -(low + 1);  // key not found.
	}

	@Override
	public int compare(int index1, int index2) {
		return Double.compare(getDouble(index1), getDouble(index2));
	}
	
	@Override
	public int compareInCum(int index1, int index2) {
		double b1 = index1==0?getDouble(index1):(getDouble(index1)-getDouble(index1-1));
		double b2 = index2==0?getDouble(index2):(getDouble(index2)-getDouble(index2-1));
		return Double.compare(b1,b2);
	}


	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putInt(length());
		for (int i=0; i<length(); i++)
			out.putDouble(getDouble(i));
	}

	@Override
	public void setByte(int index, byte value) {
		setDouble(index, value);
	}

	@Override
	public void setShort(int index, short value) {
		setDouble(index, value);		
	}

	@Override
	public void setInt(int index, int value) {
		setDouble(index, value);		
	}

	@Override
	public void setLong(int index, long value) {
		setDouble(index, value);		
	}

	@Override
	public void setFloat(int index, float value) {
		setDouble(index, value);		
	}

	@Override
	public void add(NumericArray a) {
		for (int i=0; i<length(); i++)
			add(i,a.getDouble(i));
	}
	
	@Override
	public void add(int index, byte value) {
		add(index, (double)value);
	}

	@Override
	public void add(int index, short value) {
		add(index, (double)value);		
	}

	@Override
	public void add(int index, int value) {
		add(index, (double)value);		
	}

	@Override
	public void add(int index, long value) {
		add(index, (double)value);		
	}

	@Override
	public void add(int index, float value) {
		add(index, (double)value);		
	}
	
	@Override
	public void add(int index, double value) {
		setDouble(index, getDouble(index)+value);
	}



	@Override
	public void mult(NumericArray a) {
		for (int i=0; i<length(); i++)
			mult(i,a.getDouble(i));
	}
	
	@Override
	public void mult(int index, byte value) {
		mult(index, (double)value);
	}

	@Override
	public void mult(int index, short value) {
		mult(index, (double)value);		
	}

	@Override
	public void mult(int index, int value) {
		mult(index, (double)value);		
	}

	@Override
	public void mult(int index, long value) {
		mult(index, (double)value);		
	}

	@Override
	public void mult(int index, float value) {
		mult(index, (double)value);		
	}
	
	@Override
	public void mult(int index, double value) {
		setDouble(index, getDouble(index)*value);
	}


	@Override
	public int compare(int index1, NumericArray a2, int index2) {
		return Double.compare(getDouble(index1), a2.getDouble(index2));
	}

	@Override
	public NumericArray cumSum(int from, int to) {
		for (int i=from+1; i<to; i++)
			setDouble(i, getDouble(i-1)+getDouble(i));
		return this;
	}

	@Override
	public NumericArray deCumSum(int from, int to) {
		for (int i=to-1; i>from; i--)
			setDouble(i,getDouble(i)-getDouble(i-1));
		return this;
	}

	@Override
	public NumericArray switchElements(int i, int j) {
		double tmp = getDouble(i);
		setDouble(i, getDouble(j));
		setDouble(j, tmp);
		return this;
	}

	@Override
	public NumericArray copy(NumericArray fromArray, int fromIndex, int to) {
		setDouble(to, fromArray.getDouble(fromIndex));
		return this;
	}

	@Override
	public NumericArray copyRange(int start, NumericArray dest, int destOffset,
			int len) {
		int to = start+len;
		for (int i=start; i<to; i++)
			dest.setDouble(destOffset+i, getDouble(i));
		return this;
	}


	@Override
	public String toString() {
        int iMax = length() - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(getDouble(i));
            if (i == iMax)
                return b.append(']').toString();
            b.append(", ");
        }
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this==obj)
            return true;
        if (obj==null || !(obj instanceof NumericArray))
            return false;

        NumericArray a = (NumericArray) obj;
        
        int length = a.length();
        if (length() != length)
            return false;

        for (int i=0; i<length; i++)
            if (a.getDouble(i) != getDouble(i))
                return false;

        return true;
	}
	
	@Override
	public int hashCode() {
        int result = 1;
        for (int i=0; i<length(); i++) {
        	int elementHash = Double.hashCode(getDouble(i));
            result = 31 * result + elementHash;
        }
        return result;
	}

}
