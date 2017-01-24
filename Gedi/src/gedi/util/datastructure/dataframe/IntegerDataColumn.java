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

package gedi.util.datastructure.dataframe;

import gedi.util.FileUtils;
import gedi.util.datastructure.array.IntegerArray;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.math.stat.factor.Factor;
import gedi.util.parsing.BooleanParser;

import java.io.IOException;

import cern.colt.bitvector.BitVector;

public class IntegerDataColumn implements DataColumn<IntegerArray> {

	private String name;
	private IntegerArray data;
	
	/**
	 * Data is NOT copied!
	 * @param name
	 * @param data
	 */
	public IntegerDataColumn(String name, IntegerArray data) {
		this.name = name;
		this.data = data;
	}
	
	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putString(name);
		data.serialize(out);
	}
	
	@Override
	public void deserialize(BinaryReader in) throws IOException {
		name = in.getString();
		data = (IntegerArray) NumericArray.createMemory(0, NumericArrayType.Integer);
		data.deserialize(in);
	}
	
	@Override
	public String toString(int row) {
		return data.format(row);
	}

	@Override
	public void copyValueTo(int fromIndex, DataColumn to, int toIndex) {
		to.setIntValue(toIndex, getIntValue(fromIndex));
	}
	
	/**
	 * Data is NOT copied!
	 * @param name
	 * @param data
	 */
	public IntegerDataColumn(String name, int[] data) {
		this.name = name;
		this.data = (IntegerArray) NumericArray.wrap(data);
	}
	
	@Override
	public IntegerDataColumn newInstance(String name, int length) {
		return new IntegerDataColumn(name, (IntegerArray) NumericArray.createMemory(length, NumericArrayType.Integer));
	}
	
	@Override
	public boolean isBoolean() {
		return false;
	}
	
	@Override
	public boolean isInteger() {
		return true;
	}
	
	@Override
	public boolean isDouble() {
		return false;
	}
	
	@Override
	public boolean isFactor() {
		return false;
	}
	

	@Override
	public String name() {
		return name;
	}
	
	@Override
	public int size() {
		return data.length();
	}


	@Override
	public int getIntValue(int row) {
		return data.getInt(row);
	}

	@Override
	public double getDoubleValue(int row) {
		return getIntValue(row);
	}

	@Override
	public int hashCode() {
		return name.hashCode() ^ data.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IntegerDataColumn){
			IntegerDataColumn c = (IntegerDataColumn)obj;
			return name.equals(c.name) && data.equals(c.data);
		}
		return false;
	}
	

	@Override
	public String toString() {
		return name+":"+data.toString();
	}
	
	@Override
	public IntegerArray getRaw() {
		return data;
	}
	
	@Override
	public void setIntValue(int row, int val) {
		data.setInt(row, val);
	}

	@Override
	public void setDoubleValue(int row, double val) {
		data.setDouble(row, val);
	}

	@Override
	public boolean getBooleanValue(int row) {
		return data.getInt(row)!=0;
	}

	@Override
	public Factor getFactorValue(int row) {
		return Factor.fromNumericArray(data).get(data.getInt(row)+"");
	}

	@Override
	public void setBooleanValue(int row, boolean val) {
		data.setInt(row, val?1:0);
	}

	@Override
	public void setFactorValue(int row, Factor val) {
		data.setInt(row, val.getIndex());
	}
}
