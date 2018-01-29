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
import java.util.Locale;

public abstract class ByteArray implements NumericArray {

	@Override
	public void setInfimum(int index) {
		setByte(index, Byte.MIN_VALUE);
	}
	
	@Override
	public void setSupremum(int index) {
		setByte(index, Byte.MAX_VALUE);
	}
	
	@Override
	public NumericArrayType getType() {
		return NumericArrayType.Byte;
	}

	@Override
	public String format(int index) {
		return getByte(index)+"";
	}
	
	@Override
	public String formatDecimals(int index, int decimals) {
		return getByte(index)+"";
	}
	
	@Override
	public void parseElement(int index, String s) {
		setByte(index, Byte.parseByte(s));
	}
	
	
	@Override
	public Number get(int index) {
		return getByte(index);
	}
	
	@Override
	public void set(int index, Number n) {
		setByte(index, n.byteValue());
	}
	
	@Override
	public boolean isIntegral() {
		return true;
	}

	@Override
	public short getShort(int index) {
		return getByte(index);
	}

	@Override
	public int getInt(int index) {
		return getByte(index);
	}

	@Override
	public long getLong(int index) {
		return getByte(index);
	}

	@Override
	public float getFloat(int index) {
		return getByte(index);
	}
	
	@Override
	public double getDouble(int index) {
		return getByte(index);
	}

	@Override
	public int compare(int index1, int index2) {
		return Byte.compare(getByte(index1), getByte(index2));
	}

	@Override
	public int compareInCum(int index1, int index2) {
		int b1 = index1==0?getByte(index1):(getByte(index1)-getByte(index1-1));
		int b2 = index2==0?getByte(index2):(getByte(index2)-getByte(index2-1));
		return Integer.compare(b1,b2);
	}

	@Override
	public void serializeElement(int index, BinaryWriter writer) throws IOException {
		writer.put(getByte(index));
	}
	
	@Override
	public void deserializeElement(int index, BinaryReader reader) throws IOException {
		setByte(index, reader.get());
	}
	
	public int binarySearch(byte n) {
		return binarySearch(n, 0, length());
	}
	public int binarySearch(byte n, int fromIndex, int toIndex) {
		int low = fromIndex;
		int high = toIndex - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			int comp = Byte.compare(getByte(mid), n);

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
	public void serialize(BinaryWriter out) throws IOException {
		out.putInt(length());
		for (int i=0; i<length(); i++)
			out.put(getByte(i));
	}

	@Override
	public void setShort(int index, short value) {
		setByte(index, (byte) value);		
	}

	@Override
	public void setInt(int index, int value) {
		setByte(index, (byte) value);		
	}

	@Override
	public void setLong(int index, long value) {
		setByte(index, (byte) value);		
	}

	@Override
	public void setFloat(int index, float value) {
		setByte(index, (byte) value);		
	}

	@Override
	public void setDouble(int index, double value) {
		setByte(index, (byte) value);
	}

	@Override
	public void add(NumericArray a) {
		for (int i=0; i<length(); i++)
			add(i,a.getByte(i));
	}
	
	@Override
	public void subtract(NumericArray a) {
		for (int i=0; i<length(); i++)
			add(i,-a.getByte(i));
	}

	
	@Override
	public void add(int index, byte value) {
		setByte(index, (byte) (getByte(index)+value));		
	}

	@Override
	public void add(int index, short value) {
		add(index, (byte) value);		
	}

	@Override
	public void add(int index, int value) {
		add(index, (byte) value);		
	}

	@Override
	public void add(int index, long value) {
		add(index, (byte) value);		
	}

	@Override
	public void add(int index, float value) {
		add(index, (byte) value);		
	}

	@Override
	public void add(int index, double value) {
		add(index, (byte) value);
	}
	
	

	@Override
	public void mult(NumericArray a) {
		for (int i=0; i<length(); i++)
			mult(i,a.getByte(i));
	}
	
	@Override
	public void mult(int index, byte value) {
		setByte(index, (byte) (getByte(index)*value));		
	}

	@Override
	public void mult(int index, short value) {
		mult(index, (byte) value);		
	}

	@Override
	public void mult(int index, int value) {
		mult(index, (byte) value);		
	}

	@Override
	public void mult(int index, long value) {
		mult(index, (byte) value);		
	}

	@Override
	public void mult(int index, float value) {
		mult(index, (byte) value);		
	}

	@Override
	public void mult(int index, double value) {
		mult(index, (byte) value);
	}
	

	@Override
	public int compare(int index1, NumericArray a2, int index2) {
		return Byte.compare(getByte(index1), a2.getByte(index2));
	}

	@Override
	public NumericArray cumSum(int from, int to) {
		for (int i=from+1; i<to; i++)
			setByte(i, (byte) (getByte(i-1)+getByte(i)));
		return this;
	}

	@Override
	public NumericArray deCumSum(int from, int to) {
		for (int i=to-1; i>from; i--)
			setByte(i,(byte) (getByte(i)-getByte(i-1)));
		return this;
	}

	@Override
	public NumericArray switchElements(int i, int j) {
		byte tmp = getByte(i);
		setByte(i, getByte(j));
		setByte(j, tmp);
		return this;
	}

	@Override
	public NumericArray copy(NumericArray fromArray, int fromIndex, int to) {
		setByte(to, fromArray.getByte(fromIndex));
		return this;
	}

	@Override
	public NumericArray copyRange(int start, NumericArray dest, int destOffset,
			int len) {
		int to = start+len;
		for (int i=start; i<to; i++)
			dest.setByte(destOffset+i, getByte(i));
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
            b.append(getByte(i));
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
            if (a.getByte(i) != getByte(i))
                return false;

        return true;
	}
	
	@Override
	public int hashCode() {
        int result = 1;
        for (int i=0; i<length(); i++) {
        	int elementHash = Byte.hashCode(getByte(i));
            result = 31 * result + elementHash;
        }
        return result;
	}
	

}
