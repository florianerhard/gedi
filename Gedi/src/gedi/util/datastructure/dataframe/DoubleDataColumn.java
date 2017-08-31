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
import gedi.util.datastructure.array.DoubleArray;
import gedi.util.datastructure.array.IntegerArray;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.math.stat.factor.Factor;
import gedi.util.parsing.BooleanParser;

import java.io.IOException;
import java.util.function.DoubleUnaryOperator;

import cern.colt.bitvector.BitVector;

public class DoubleDataColumn implements DataColumn<DoubleArray> {

	private String name;
	private DoubleArray data;
	
	/**
	 * Data is NOT copied!
	 * @param name
	 * @param data
	 */
	public DoubleDataColumn(String name, DoubleArray data) {
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
		data = (DoubleArray) NumericArray.createMemory(0, NumericArrayType.Double);
		data.deserialize(in);
	}
	
	@Override
	public String toString(int row) {
		return data.format(row);
	}
	
	@Override
	public void copyValueTo(int fromIndex, DataColumn to, int toIndex) {
		to.setDoubleValue(toIndex, getDoubleValue(fromIndex));
	}
	
	public double apply(NumericArrayFunction fun) {
		return data.evaluate(fun);
	}
	
	/**
	 * Data is NOT copied!
	 * @param name
	 * @param data
	 */
	public DoubleDataColumn(String name, double[] data) {
		this.name = name;
		this.data = (DoubleArray) NumericArray.wrap(data);
	}
	
	@Override
	public DoubleDataColumn newInstance(String name, int length) {
		return new DoubleDataColumn(name, (DoubleArray) NumericArray.createMemory(length, NumericArrayType.Double));
	}
	
	@Override
	public boolean isBoolean() {
		return false;
	}
	
	@Override
	public boolean isInteger() {
		return false;
	}
	
	@Override
	public boolean isDouble() {
		return true;
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
		return data.getDouble(row);
	}

	@Override
	public int hashCode() {
		return name.hashCode() ^ data.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DoubleDataColumn){
			DoubleDataColumn c = (DoubleDataColumn)obj;
			return name.equals(c.name) && data.equals(c.data);
		}
		return false;
	}
	

	@Override
	public String toString() {
		return name+":"+data.toString();
	}
	
	@Override
	public DoubleArray getRaw() {
		return data;
	}
	
	@Override
	public Object getValue(int row) {
		return data.getDouble(row);
	}
	
	@Override
	public void setValue(int row, Object val) {
		double value;
		if (val instanceof Number) 
			value = ((Number)val).doubleValue();
		else
			value = 0;
		data.setDouble(row, value);
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
		return Factor.fromNumericArray(data).get(data.getDouble(row)+"");
	}

	@Override
	public void setBooleanValue(int row, boolean val) {
		data.setInt(row, val?1:0);
	}

	@Override
	public void setFactorValue(int row, Factor val) {
		data.setInt(row, val.getIndex());
	}

	public void transform(DoubleUnaryOperator op) {
		data.applyInPlace(op);
	}
}
