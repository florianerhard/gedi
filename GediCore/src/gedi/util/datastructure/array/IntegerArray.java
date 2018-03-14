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

public abstract class IntegerArray implements NumericArray {

	@Override
	public String format(int index) {
		return getInt(index)+"";
	}
	
	@Override
	public String formatDecimals(int index, int decimals) {
		return getInt(index)+"";
	}
	
	
	@Override
	public boolean isIntegral() {
		return true;
	}
	
	@Override
	public void setInfimum(int index) {
		setInt(index, Integer.MIN_VALUE);
	}
	
	@Override
	public void setSupremum(int index) {
		setInt(index, Integer.MAX_VALUE);
	}
	
	@Override
	public NumericArrayType getType() {
		return NumericArrayType.Integer;
	}
	
	@Override
	public void parseElement(int index, String s) {
		setInt(index, Integer.parseInt(s));
	}
	
	
	
	@Override
	public Number get(int index) {
		return getInt(index);
	}
	
	@Override
	public void set(int index, Number n) {
		setInt(index, n.intValue());
	}
	

	@Override
	public byte getByte(int index) {
		return (byte) getInt(index);
	}

	@Override
	public short getShort(int index) {
		return (short) getInt(index);
	}


	@Override
	public long getLong(int index) {
		return (long) getInt(index);
	}

	@Override
	public float getFloat(int index) {
		return (float) getInt(index);
	}
	
	public double getDouble(int index) {
		return getInt(index);
	}

	
	@Override
	public int compare(int index1, int index2) {
		return Integer.compare(getInt(index1), getInt(index2));
	}
	
	@Override
	public int compareInCum(int index1, int index2) {
		int b1 = index1==0?getInt(index1):(getInt(index1)-getInt(index1-1));
		int b2 = index2==0?getInt(index2):(getInt(index2)-getInt(index2-1));
		return Integer.compare(b1,b2);
	}

	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putInt(length());
		for (int i=0; i<length(); i++)
			out.putInt(getInt(i));
	}

	@Override
	public void setByte(int index, byte value) {
		setInt(index, value);
	}

	@Override
	public void setShort(int index, short value) {
		setInt(index, value);		
	}


	@Override
	public void setLong(int index, long value) {
		setInt(index, (int) value);		
	}

	@Override
	public void setFloat(int index, float value) {
		setInt(index, (int) value);		
	}

	@Override
	public void setDouble(int index, double value) {
		setInt(index,(int) value);
	}
	
	@Override
	public void add(NumericArray a) {
		for (int i=0; i<length(); i++)
			add(i,a.getInt(i));
	}
	
	@Override
	public void subtract(NumericArray a) {
		for (int i=0; i<length(); i++)
			add(i,-a.getInt(i));
	}
	
	@Override
	public void add(int index, byte value) {
		add(index, (int)value);
	}

	@Override
	public void add(int index, short value) {
		add(index, (int)value);		
	}
	
	@Override
	public void add(int index, int value) {
		setInt(index, getInt(index)+value);
	}

	@Override
	public void add(int index, long value) {
		add(index, (int) value);		
	}

	@Override
	public void add(int index, float value) {
		add(index, (int) value);		
	}

	@Override
	public void add(int index, double value) {
		add(index,(int) value);
	}

	
	@Override
	public void mult(NumericArray a) {
		for (int i=0; i<length(); i++)
			mult(i,a.getInt(i));
	}
	
	@Override
	public void mult(int index, byte value) {
		mult(index, (int)value);
	}

	@Override
	public void mult(int index, short value) {
		mult(index, (int)value);		
	}
	
	@Override
	public void mult(int index, int value) {
		setInt(index, getInt(index)*value);
	}

	@Override
	public void mult(int index, long value) {
		mult(index, (int) value);		
	}

	@Override
	public void mult(int index, float value) {
		mult(index, (int) value);		
	}

	@Override
	public void mult(int index, double value) {
		mult(index,(int) value);
	}

	
	@Override
	public int compare(int index1, NumericArray a2, int index2) {
		return Integer.compare(getInt(index1), a2.getInt(index2));
	}

	@Override
	public NumericArray cumSum(int from, int to) {
		for (int i=from+1; i<to; i++)
			setInt(i, getInt(i-1)+getInt(i));
		return this;
	}

	@Override
	public NumericArray deCumSum(int from, int to) {
		for (int i=to-1; i>from; i--)
			setInt(i,getInt(i)-getInt(i-1));
		return this;
	}

	@Override
	public NumericArray switchElements(int i, int j) {
		int tmp = getInt(i);
		setInt(i, getInt(j));
		setInt(j, tmp);
		return this;
	}

	@Override
	public NumericArray copy(NumericArray fromArray, int fromIndex, int to) {
		setInt(to, fromArray.getInt(fromIndex));
		return this;
	}

	@Override
	public NumericArray copyRange(int start, NumericArray dest, int destOffset,
			int len) {
		int to = start+len;
		for (int i=start; i<to; i++)
			dest.setInt(destOffset+i, getInt(i));
		return this;
	}

	@Override
	public void serializeElement(int index, BinaryWriter writer) throws IOException {
		writer.putInt(getInt(index));
	}
	
	@Override
	public void deserializeElement(int index, BinaryReader reader) throws IOException {
		setInt(index, reader.getInt());
	}

	public int binarySearch(int n) {
		return binarySearch(n, 0, length());
	}
	public int binarySearch(int n, int fromIndex, int toIndex) {
		int low = fromIndex;
		int high = toIndex - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			int comp = Integer.compare(getInt(mid), n);

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
            b.append(getInt(i));
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
            if (a.getInt(i) != getInt(i))
                return false;

        return true;
	}
	
	@Override
	public int hashCode() {
        int result = 1;
        for (int i=0; i<length(); i++) {
        	int elementHash = Integer.hashCode(getInt(i));
            result = 31 * result + elementHash;
        }
        return result;
	}	

}
