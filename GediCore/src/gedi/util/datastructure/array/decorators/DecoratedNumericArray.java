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
package gedi.util.datastructure.array.decorators;

import gedi.util.datastructure.array.NumericArray;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;

import java.io.IOException;

public class DecoratedNumericArray implements NumericArray {
	
	protected NumericArray parent;
	
	public DecoratedNumericArray(NumericArray parent) {
		this.parent = parent;
	}

	public NumericArray getParent() {
		return parent;
	}
	
	
	@Override
	public String format(int index) {
		return parent.format(index);
	}
	
	@Override
	public String formatDecimals(int index, int decimals) {
		return parent.formatDecimals(index, decimals);
	}
	
	@Override
	public boolean isIntegral() {
		return parent.isIntegral();
	}
	
	@Override
	public void parseElement(int index, String s) {
		parent.parseElement(index,s);
	}
	
	@Override
	public void setInfimum(int index) {
		parent.setInfimum(index);
	}
	
	@Override
	public void setSupremum(int index) {
		parent.setSupremum(index);
	}

	@Override
	public void serialize(BinaryWriter out) throws IOException {
		parent.serialize(out);
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		parent.deserialize(in);
	}

	@Override
	public void setByte(int index, byte value) {
		parent.setByte(index, value);
	}

	@Override
	public void setShort(int index, short value) {
		parent.setShort(index, value);
	}

	@Override
	public void setInt(int index, int value) {
		parent.setInt(index, value);
	}

	@Override
	public void setLong(int index, long value) {
		parent.setLong(index, value);
	}

	@Override
	public void setFloat(int index, float value) {
		parent.setFloat(index, value);
	}

	@Override
	public void setDouble(int index, double value) {
		parent.setDouble(index, value);
	}

	@Override
	public boolean isReadOnly() {
		return parent.isReadOnly();
	}

	@Override
	public NumericArrayType getType() {
		return parent.getType();
	}

	@Override
	public int length() {
		return parent.length();
	}

	@Override
	public byte getByte(int index) {
		return parent.getByte(index);
	}

	@Override
	public short getShort(int index) {
		return parent.getShort(index);
	}

	@Override
	public int getInt(int index) {
		return parent.getInt(index);
	}

	@Override
	public long getLong(int index) {
		return parent.getLong(index);
	}

	@Override
	public float getFloat(int index) {
		return parent.getFloat(index);
	}

	@Override
	public double getDouble(int index) {
		return parent.getDouble(index);
	}

	@Override
	public int compare(int index1, int index2) {
		return parent.compare(index1, index2);
	}

	@Override
	public int compareInCum(int index1, int index2) {
		return parent.compareInCum(index1, index2);
	}
	
	@Override
	public int compare(int index1, NumericArray a2, int index2) {
		return parent.compare(index1, a2, index2);
	}

	@Override
	public NumericArray copy(NumericArray fromArray, int fromIndex, int to) {
		parent.copy(fromArray, fromIndex, to);
		return this;
	}
	
	@Override
	public NumericArray sort(int from, int to) {
		parent.sort(from, to);
		return this;
	}
	
	@Override
	public NumericArray reverse(int from, int to) {
		parent.reverse(from, to);
		return this;
	}

	@Override
	public NumericArray copyRange(int start, NumericArray dest, int destOffset,
			int len) {
		parent.copyRange(start, dest, destOffset, len);
		return parent;
	}

	@Override
	public NumericArray switchElements(int i, int j) {
		parent.switchElements(i, j);
		return this;
	}

	@Override
	public NumericArray cumSum(int from, int to) {
		parent.cumSum(from,to);
		return this;
	}

	@Override
	public NumericArray deCumSum(int from, int to) {
		parent.deCumSum(from, to);
		return this;
	}

	@Override
	public void deserializeElement(int index, BinaryReader reader)
			throws IOException {
		parent.deserializeElement(index, reader);
	}

	@Override
	public void serializeElement(int index, BinaryWriter writer)
			throws IOException {
		parent.serializeElement(index, writer);
	}

	@Override
	public void add(NumericArray a) {
		parent.add(a);
	}
	
	@Override
	public void subtract(NumericArray a) {
		parent.subtract(a);
	}
	@Override
	public void add(int index, byte value) {
		parent.add(index,value);
	}

	@Override
	public void add(int index, short value) {
		parent.add(index,value);
	}

	@Override
	public void add(int index, int value) {
		parent.add(index,value);
	}

	@Override
	public void add(int index, long value) {
		parent.add(index,value);
	}

	@Override
	public void add(int index, float value) {
		parent.add(index,value);
	}

	@Override
	public void add(int index, double value) {
		parent.add(index,value);
	}
	
	@Override
	public void mult(NumericArray a) {
		parent.mult(a);
	}
	@Override
	public void mult(int index, byte value) {
		parent.mult(index,value);
	}

	@Override
	public void mult(int index, short value) {
		parent.mult(index,value);
	}

	@Override
	public void mult(int index, int value) {
		parent.mult(index,value);
	}

	@Override
	public void mult(int index, long value) {
		parent.mult(index,value);
	}

	@Override
	public void mult(int index, float value) {
		parent.mult(index,value);
	}

	@Override
	public void mult(int index, double value) {
		parent.mult(index,value);
	}

	@Override
	public Number get(int index) {
		return parent.get(index);
	}

	@Override
	public void set(int index, Number n) {
		parent.set(index,n);
	}

	@Override
	public NumericArray clear() {
		parent.clear();
		return this;
	}

}
