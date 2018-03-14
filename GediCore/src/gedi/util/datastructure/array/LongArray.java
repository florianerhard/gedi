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
package gedi.util.datastructure.array;

import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;

import java.io.IOException;

public abstract class LongArray implements NumericArray {

	@Override
	public String format(int index) {
		return getLong(index)+"";
	}
	
	@Override
	public String formatDecimals(int index, int decimals) {
		return getLong(index)+"";
	}

	
	
	@Override
	public boolean isIntegral() {
		return true;
	}
	
	@Override
	public void setInfimum(int index) {
		setLong(index, Long.MIN_VALUE);
	}
	
	@Override
	public void setSupremum(int index) {
		setLong(index, Long.MAX_VALUE);
	}
	
	
	
	@Override
	public NumericArrayType getType() {
		return NumericArrayType.Long;
	}

	@Override
	public void parseElement(int index, String s) {
		setLong(index, Long.parseLong(s));
	}
	
	@Override
	public Number get(int index) {
		return getLong(index);
	}
	
	@Override
	public void set(int index, Number n) {
		setLong(index, n.longValue());
	}
	
	
	@Override
	public byte getByte(int index) {
		return (byte) getLong(index);
	}

	@Override
	public short getShort(int index) {
		return (short) getLong(index);
	}

	@Override
	public int getInt(int index) {
		return (int) getLong(index);
	}


	@Override
	public float getFloat(int index) {
		return (float) getLong(index);
	}

	public double getDouble(int index) {
		return (double) getLong(index);
	}
	
	@Override
	public int compare(int index1, int index2) {
		return Long.compare(getLong(index1), getLong(index2));
	}
	
	@Override
	public int compareInCum(int index1, int index2) {
		long b1 = index1==0?getLong(index1):(getLong(index1)-getLong(index1-1));
		long b2 = index2==0?getLong(index2):(getLong(index2)-getLong(index2-1));
		return Long.compare(b1,b2);
	}


	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putInt(length());
		for (int i=0; i<length(); i++)
			out.putLong(getLong(i));
	}

	@Override
	public void setByte(int index, byte value) {
		setLong(index, value);
	}

	@Override
	public void setShort(int index, short value) {
		setLong(index, value);		
	}

	@Override
	public void setInt(int index, int value) {
		setLong(index, value);		
	}

	@Override
	public void setFloat(int index, float value) {
		setLong(index, (long) value);		
	}
	
	@Override
	public void setDouble(int index, double value) {
		setLong(index,(long) value);
	}
	@Override
	public void add(NumericArray a) {
		for (int i=0; i<length(); i++)
			add(i,a.getLong(i));
	}
	
	@Override
	public void subtract(NumericArray a) {
		for (int i=0; i<length(); i++)
			add(i,-a.getLong(i));
	}
	
	@Override
	public void add(int index, byte value) {
		add(index, (long)value);
	}

	@Override
	public void add(int index, short value) {
		add(index, (long)value);		
	}
	
	@Override
	public void add(int index, long value) {
		setLong(index, getLong(index)+value);
	}

	@Override
	public void add(int index, int value) {
		add(index, (long)value);		
	}

	@Override
	public void add(int index, float value) {
		add(index, (long) value);		
	}
	
	@Override
	public void add(int index, double value) {
		add(index,(long) value);
	}


	@Override
	public void mult(NumericArray a) {
		for (int i=0; i<length(); i++)
			mult(i,a.getLong(i));
	}
	
	
	@Override
	public void mult(int index, byte value) {
		mult(index, (long)value);
	}

	@Override
	public void mult(int index, short value) {
		mult(index, (long)value);		
	}
	
	@Override
	public void mult(int index, long value) {
		setLong(index, getLong(index)*value);
	}

	@Override
	public void mult(int index, int value) {
		mult(index, (long)value);		
	}

	@Override
	public void mult(int index, float value) {
		mult(index, (long) value);		
	}
	
	@Override
	public void mult(int index, double value) {
		mult(index,(long) value);
	}


	@Override
	public int compare(int index1, NumericArray a2, int index2) {
		return Long.compare(getLong(index1), a2.getLong(index2));
	}

	@Override
	public NumericArray cumSum(int from, int to) {
		for (int i=from+1; i<to; i++)
			setLong(i, getLong(i-1)+getLong(i));
		return this;
	}

	@Override
	public NumericArray deCumSum(int from, int to) {
		for (int i=to-1; i>from; i--)
			setLong(i,getLong(i)-getLong(i-1));
		return this;
	}

	@Override
	public NumericArray switchElements(int i, int j) {
		long tmp = getLong(i);
		setLong(i, getLong(j));
		setLong(j, tmp);
		return this;
	}

	@Override
	public NumericArray copy(NumericArray fromArray, int fromIndex, int to) {
		setLong(to, fromArray.getLong(fromIndex));
		return this;
	}

	@Override
	public NumericArray copyRange(int start, NumericArray dest, int destOffset,
			int len) {
		int to = start+len;
		for (int i=start; i<to; i++)
			dest.setLong(destOffset+i, getLong(i));
		return this;
	}

	@Override
	public void serializeElement(int index, BinaryWriter writer) throws IOException {
		writer.putLong(getLong(index));
	}
	
	@Override
	public void deserializeElement(int index, BinaryReader reader) throws IOException {
		setLong(index, reader.getLong());
	}

	public int binarySearch(long n) {
		return binarySearch(n, 0, length());
	}
	public int binarySearch(long n, int fromIndex, int toIndex) {
		int low = fromIndex;
		int high = toIndex - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			int comp = Long.compare(getLong(mid), n);

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
	public String toString() {
        int iMax = length() - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(getLong(i));
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
            if (a.getLong(i) != getLong(i))
                return false;

        return true;
	}
	
	@Override
	public int hashCode() {
        int result = 1;
        for (int i=0; i<length(); i++) {
        	int elementHash = Long.hashCode(getLong(i));
            result = 31 * result + elementHash;
        }
        return result;
	}	

}
