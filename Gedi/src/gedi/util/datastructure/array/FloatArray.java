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

import gedi.app.Config;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;

import java.io.IOException;

public abstract class FloatArray implements NumericArray {

	
	@Override
	public String format(int index) {
		return String.format(Config.getInstance().getRealFormat(),getFloat(index));
	}
	@Override
	public String formatDecimals(int index, int decimals) {
		return String.format("%."+decimals+"f",getFloat(index));
	}
	
	@Override
	public boolean isIntegral() {
		return false;
	}
	
	@Override
	public boolean isNA(int index) {
		return Float.isNaN(getFloat(index));
	}
	
	@Override
	public void setInfimum(int index) {
		setFloat(index, Float.NEGATIVE_INFINITY);
	}
	
	@Override
	public Number get(int index) {
		return getFloat(index);
	}
	
	@Override
	public void parseElement(int index, String s) {
		setFloat(index, Float.parseFloat(s));
	}
	
	
	@Override
	public void set(int index, Number n) {
		setFloat(index, n.floatValue());
	}
	
	
	@Override
	public void setSupremum(int index) {
		setFloat(index, Float.POSITIVE_INFINITY);
	}
	
	@Override
	public NumericArrayType getType() {
		return NumericArrayType.Float;
	}

	@Override
	public byte getByte(int index) {
		return (byte) getFloat(index);
	}

	@Override
	public short getShort(int index) {
		return (short) getFloat(index);
	}

	@Override
	public int getInt(int index) {
		return (int) getFloat(index);
	}

	@Override
	public long getLong(int index) {
		return (long) getFloat(index);
	}
	
	@Override
	public double getDouble(int index) {
		return getFloat(index);
	}

	@Override
	public void serializeElement(int index, BinaryWriter writer) throws IOException {
		writer.putFloat(getFloat(index));
	}
	
	@Override
	public void deserializeElement(int index, BinaryReader reader) throws IOException {
		setFloat(index, reader.getFloat());
	}
	
	public int binarySearch(float n) {
		return binarySearch(n, 0, length());
	}
	public int binarySearch(float n, int fromIndex, int toIndex) {
		int low = fromIndex;
		int high = toIndex - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			int comp = Float.compare(getFloat(mid), n);

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
		return Float.compare(getFloat(index1), getFloat(index2));
	}
	
	@Override
	public int compareInCum(int index1, int index2) {
		float b1 = index1==0?getFloat(index1):(getFloat(index1)-getFloat(index1-1));
		float b2 = index2==0?getFloat(index2):(getFloat(index2)-getFloat(index2-1));
		return Float.compare(b1,b2);
	}


	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putInt(length());
		for (int i=0; i<length(); i++)
			out.putFloat(getFloat(i));
	}

	@Override
	public void setByte(int index, byte value) {
		setFloat(index, value);
	}

	@Override
	public void setShort(int index, short value) {
		setFloat(index, value);		
	}

	@Override
	public void setInt(int index, int value) {
		setFloat(index, value);		
	}

	@Override
	public void setLong(int index, long value) {
		setFloat(index, value);		
	}

	@Override
	public void setDouble(int index, double value) {
		setFloat(index, (float) value);		
	}

	
	@Override
	public void add(NumericArray a) {
		for (int i=0; i<length(); i++)
			add(i,a.getFloat(i));
	}
	
	@Override
	public void add(int index, byte value) {
		add(index, (float) value);
	}

	@Override
	public void add(int index, short value) {
		add(index, (float) value);		
	}

	@Override
	public void add(int index, int value) {
		add(index, (float) value);		
	}

	@Override
	public void add(int index, long value) {
		add(index, (float) value);		
	}

	@Override
	public void add(int index, double value) {
		add(index, (float) value);		
	}

	@Override
	public void add(int index, float value) {
		setFloat(index, getFloat(index)+value);
	}
	
	@Override
	public void mult(NumericArray a) {
		for (int i=0; i<length(); i++)
			mult(i,a.getFloat(i));
	}
	
	@Override
	public void mult(int index, byte value) {
		mult(index, (float) value);
	}

	@Override
	public void mult(int index, short value) {
		mult(index, (float) value);		
	}

	@Override
	public void mult(int index, int value) {
		mult(index, (float) value);		
	}

	@Override
	public void mult(int index, long value) {
		mult(index, (float) value);		
	}

	@Override
	public void mult(int index, double value) {
		mult(index, (float) value);		
	}

	@Override
	public void mult(int index, float value) {
		setFloat(index, getFloat(index)*value);
	}

	@Override
	public int compare(int index1, NumericArray a2, int index2) {
		return Float.compare(getFloat(index1), a2.getFloat(index2));
	}

	@Override
	public NumericArray cumSum(int from, int to) {
		for (int i=from+1; i<to; i++)
			setFloat(i, getFloat(i-1)+getFloat(i));
		return this;
	}

	@Override
	public NumericArray deCumSum(int from, int to) {
		for (int i=to-1; i>from; i--)
			setFloat(i,getFloat(i)-getFloat(i-1));
		return this;
	}

	@Override
	public NumericArray switchElements(int i, int j) {
		float tmp = getFloat(i);
		setFloat(i, getFloat(j));
		setFloat(j, tmp);
		return this;
	}

	@Override
	public NumericArray copy(NumericArray fromArray, int fromIndex, int to) {
		setFloat(to, fromArray.getFloat(fromIndex));
		return this;
	}

	@Override
	public NumericArray copyRange(int start, NumericArray dest, int destOffset,
			int len) {
		int to = start+len;
		for (int i=start; i<to; i++)
			dest.setFloat(destOffset+i, getFloat(i));
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
            b.append(getFloat(i));
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
            if (a.getFloat(i) != getFloat(i))
                return false;

        return true;
	}
	
	@Override
	public int hashCode() {
        int result = 1;
        for (int i=0; i<length(); i++) {
        	int elementHash = Float.hashCode(getFloat(i));
            result = 31 * result + elementHash;
        }
        return result;
	}
	

}
