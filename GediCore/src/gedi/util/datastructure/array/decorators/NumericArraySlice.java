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

public class NumericArraySlice implements NumericArray {

	private NumericArray parent;
	private int start;
	private int end;
	
	public NumericArraySlice(NumericArray parent, int start, int end) {
		this.parent = parent;
		this.start = start;
		this.end = end;
	}
	
	@Override
	public String toString() {
		return toArrayString();
	}
	
	public NumericArraySlice setSlice(int start, int end) {
		this.start = start;
		this.end = end;
		return this;
	}
	
	public int getEnd() {
		return end;
	}
	
	public int getStart() {
		return start;
	}
	
	public NumericArray getParent() {
		return parent;
	}
	
	public NumericArraySlice slice(int start, int end) {
		return new NumericArraySlice(parent,start+this.start, end+this.start);
	}

	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putInt(length());
		for (int i=start; i<end; i++)
			parent.serializeElement(i, out);
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		throw new RuntimeException();
	}

	@Override
	public NumericArray clear() {
		for (int i=start; i<end; i++)
			parent.setByte(i, (byte)0);
		return this;
	}

	@Override
	public void setInfimum(int index) {
		parent.setInfimum(index+start);
	}

	@Override
	public void setSupremum(int index) {
		parent.setSupremum(index+start);
	}

	@Override
	public void setByte(int index, byte value) {
		parent.setByte(index+start,value);
	}

	@Override
	public void setShort(int index, short value) {
		parent.setShort(index+start,value);
	}

	@Override
	public void setInt(int index, int value) {
		parent.setInt(index+start,value);
	}

	@Override
	public void setLong(int index, long value) {
		parent.setLong(index+start,value);
	}

	@Override
	public void setFloat(int index, float value) {
		parent.setFloat(index+start,value);
	}

	@Override
	public void setDouble(int index, double value) {
		parent.setDouble(index+start,value);
	}

	@Override
	public void add(NumericArray a) {
		if (isIntegral())
			for (int i=start; i<end; i++)
				parent.add(i, a.getLong(i-start));
		else
			for (int i=start; i<end; i++)
				parent.add(i, a.getDouble(i-start));
	}

	@Override
	public void add(int index, byte value) {
		parent.add(index+start,value);
	}

	@Override
	public void add(int index, short value) {
		parent.add(index+start,value);
	}

	@Override
	public void add(int index, int value) {
		parent.add(index+start,value);
	}

	@Override
	public void add(int index, long value) {
		parent.add(index+start,value);
	}

	@Override
	public void add(int index, float value) {
		parent.add(index+start,value);
	}

	@Override
	public void add(int index, double value) {
		parent.add(index+start,value);
	}

	@Override
	public void mult(NumericArray a) {
		if (isIntegral())
			for (int i=start; i<end; i++)
				parent.mult(i, a.getLong(i-start));
		else
			for (int i=start; i<end; i++)
				parent.mult(i, a.getDouble(i-start));
	}

	@Override
	public void mult(int index, byte value) {
		parent.mult(index+start,value);
	}

	@Override
	public void mult(int index, short value) {
		parent.mult(index+start,value);
	}

	@Override
	public void mult(int index, int value) {
		parent.mult(index+start,value);
	}

	@Override
	public void mult(int index, long value) {
		parent.mult(index+start,value);
	}

	@Override
	public void mult(int index, float value) {
		parent.mult(index+start,value);
	}

	@Override
	public void mult(int index, double value) {
		parent.mult(index+start,value);
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
		return end-start;
	}

	@Override
	public byte getByte(int index) {
		return parent.getByte(index+start);
	}

	@Override
	public short getShort(int index) {
		return parent.getShort(index+start);
	}

	@Override
	public int getInt(int index) {
		return parent.getInt(index+start);
	}

	@Override
	public long getLong(int index) {
		return parent.getLong(index+start);
	}

	@Override
	public float getFloat(int index) {
		return parent.getFloat(index+start);
	}

	@Override
	public double getDouble(int index) {
		return parent.getDouble(index+start);
	}

	@Override
	public String format(int index) {
		return parent.format(index+start);
	}

	@Override
	public String formatDecimals(int index, int decimals) {
		return parent.formatDecimals(index+start, decimals);
	}

	@Override
	public void deserializeElement(int index, BinaryReader reader)
			throws IOException {
		parent.deserializeElement(index+start,reader);
	}

	@Override
	public void serializeElement(int index, BinaryWriter writer)
			throws IOException {
		parent.serializeElement(index+start,writer);
	}

	@Override
	public int compare(int index1, int index2) {
		return parent.compare(index1+start,index2+start);
	}

	@Override
	public int compareInCum(int index1, int index2) {
		return parent.compareInCum(index1+start,index2+start);
	}

	@Override
	public int compare(int index1, NumericArray a2, int index2) {
		return parent.compare(index1+start,a2,index2);
	}

	@Override
	public Number get(int index) {
		return parent.get(index+start);
	}

	@Override
	public void set(int index, Number n) {
		parent.set(index+start, n);
	}

	@Override
	public NumericArray copy(NumericArray fromArray, int fromIndex, int to) {
		parent.copy(fromArray, fromIndex, to+start);
		return this;
	}

	@Override
	public NumericArray copyRange(int start, NumericArray dest, int destOffset,
			int len) {
		parent.copyRange(start+this.start, dest, destOffset, len);
		return this;
	}

	@Override
	public NumericArray switchElements(int i, int j) {
		parent.switchElements(i+start, j+start);
		return this;
	}

	@Override
	public NumericArray cumSum(int from, int to) {
		parent.cumSum(from+start, to+start);
		return this;
	}

	@Override
	public NumericArray deCumSum(int from, int to) {
		parent.deCumSum(from+start, to+start);
		return this;
	}

	@Override
	public boolean isIntegral() {
		return parent.isIntegral();
	}

	@Override
	public void parseElement(int index, String s) {
		if (isIntegral())
			setLong(index, Long.parseLong(s));
		else
			setDouble(index, Double.parseDouble(s));
	}

}
