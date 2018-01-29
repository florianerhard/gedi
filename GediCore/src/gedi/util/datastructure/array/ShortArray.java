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

public abstract class ShortArray implements NumericArray {


	@Override
	public boolean isIntegral() {
		return true;
	}
	@Override
	public String format(int index) {
		return getShort(index)+"";
	}
	
	@Override
	public String formatDecimals(int index, int decimals) {
		return getShort(index)+"";
	}


	@Override
	public void setInfimum(int index) {
		setShort(index, Short.MIN_VALUE);
	}
	
	@Override
	public void setSupremum(int index) {
		setShort(index,Short.MAX_VALUE);
	}
	
	
	@Override
	public NumericArrayType getType() {
		return NumericArrayType.Short;
	}
	
	@Override
	public void parseElement(int index, String s) {
		setShort(index, Short.parseShort(s));
	}
	
	
	
	@Override
	public Number get(int index) {
		return getShort(index);
	}
	@Override
	public void set(int index, Number n) {
		setShort(index, n.shortValue());
	}
	
	
	@Override
	public byte getByte(int index) {
		return (byte) getShort(index);
	}

	@Override
	public int getInt(int index) {
		return (int) getShort(index);
	}

	@Override
	public long getLong(int index) {
		return (long) getShort(index);
	}

	@Override
	public float getFloat(int index) {
		return (float) getShort(index);
	}
	
	@Override
	public double getDouble(int index) {
		return getShort(index);
	}

	@Override
	public int compare(int index1, int index2) {
		return Short.compare(getShort(index1), getShort(index2));
	}

	@Override
	public int compareInCum(int index1, int index2) {
		int b1 = index1==0?getShort(index1):(getShort(index1)-getShort(index1-1));
		int b2 = index2==0?getShort(index2):(getShort(index2)-getShort(index2-1));
		return Integer.compare(b1,b2);
	}

	
	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putInt(length());
		for (int i=0; i<length(); i++)
			out.putShort(getShort(i));
	}

	@Override
	public void setByte(int index, byte value) {
		setShort(index, value);
	}

	@Override
	public void setInt(int index, int value) {
		setShort(index, (short) value);		
	}

	@Override
	public void setLong(int index, long value) {
		setShort(index, (short) value);		
	}

	@Override
	public void setFloat(int index, float value) {
		setShort(index, (short) value);		
	}

	@Override
	public void setDouble(int index, double value) {
		setShort(index, (short) value);		
	}
	
	@Override
	public void add(NumericArray a) {
		for (int i=0; i<length(); i++)
			add(i,a.getShort(i));
	}
	
	@Override
	public void subtract(NumericArray a) {
		for (int i=0; i<length(); i++)
			add(i,-a.getShort(i));
	}
	
	@Override
	public void add(int index, byte value) {
		add(index, (short) value);
	}

	@Override
	public void add(int index, short value) {
		setShort(index, (short) (getShort(index)+value));
	}
	
	@Override
	public void add(int index, int value) {
		add(index, (short) value);		
	}

	@Override
	public void add(int index, long value) {
		add(index, (short) value);		
	}

	@Override
	public void add(int index, float value) {
		add(index, (short) value);		
	}

	@Override
	public void add(int index, double value) {
		add(index, (short) value);		
	}

	
	@Override
	public void mult(NumericArray a) {
		for (int i=0; i<length(); i++)
			mult(i,a.getShort(i));
	}
	
	@Override
	public void mult(int index, byte value) {
		mult(index, (short) value);
	}

	@Override
	public void mult(int index, short value) {
		setShort(index, (short) (getShort(index)*value));
	}
	
	@Override
	public void mult(int index, int value) {
		mult(index, (short) value);		
	}

	@Override
	public void mult(int index, long value) {
		mult(index, (short) value);		
	}

	@Override
	public void mult(int index, float value) {
		mult(index, (short) value);		
	}

	@Override
	public void mult(int index, double value) {
		mult(index, (short) value);		
	}

	@Override
	public int compare(int index1, NumericArray a2, int index2) {
		return Short.compare(getShort(index1), a2.getShort(index2));
	}

	@Override
	public NumericArray cumSum(int from, int to) {
		for (int i=from+1; i<to; i++)
			setShort(i, (short) (getShort(i-1)+getShort(i)));
		return this;
	}

	@Override
	public NumericArray deCumSum(int from, int to) {
		for (int i=to-1; i>from; i--)
			setShort(i,(short) (getShort(i)-getShort(i-1)));
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
			dest.setShort(destOffset+i, getShort(i));
		return this;
	}
	
	@Override
	public void serializeElement(int index, BinaryWriter writer) throws IOException {
		writer.putShort(getShort(index));
	}
	
	@Override
	public void deserializeElement(int index, BinaryReader reader) throws IOException {
		setShort(index, reader.getShort());
	}

	public int binarySearch(short n) {
		return binarySearch(n, 0, length());
	}
	public int binarySearch(short n, int fromIndex, int toIndex) {
		int low = fromIndex;
		int high = toIndex - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			int comp = Short.compare(getShort(mid), n);

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
            b.append(getShort(i));
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
            if (a.getShort(i) != getShort(i))
                return false;

        return true;
	}
	
	@Override
	public int hashCode() {
        int result = 1;
        for (int i=0; i<length(); i++) {
        	int elementHash = Short.hashCode(getShort(i));
            result = 31 * result + elementHash;
        }
        return result;
	}
	

}
