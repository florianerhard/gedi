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
import java.util.Arrays;

import cern.colt.bitvector.BitVector;

public class FactorDataColumn implements DataColumn<Factor[]> {

	private String name;
	private Factor[] data;
	
	/**
	 * Data is NOT copied!
	 * @param name
	 * @param data
	 */
	public FactorDataColumn(String name, Factor[] data) {
		this.name = name;
		this.data = data;
	}
	
	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putString(name);
		Factor.serialize(out, data);
	}
	
	@Override
	public void deserialize(BinaryReader in) throws IOException {
		name = in.getString();
		data = Factor.deserialize(in);
	}
	
	@Override
	public FactorDataColumn newInstance(String name, int length) {
		return new FactorDataColumn(name, new Factor[length]);
	}
	
	@Override
	public void copyValueTo(int fromIndex, DataColumn to, int toIndex) {
		to.setFactorValue(toIndex, getFactorValue(fromIndex));
	}

	
	@Override
	public String toString(int row) {
		return data[row].name();
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
		return false;
	}
	
	@Override
	public boolean isFactor() {
		return true;
	}
	

	@Override
	public String name() {
		return name;
	}
	
	@Override
	public int size() {
		return data.length;
	}


	@Override
	public int getIntValue(int row) {
		return data[row].getIndex();
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
		if (obj instanceof FactorDataColumn){
			FactorDataColumn c = (FactorDataColumn)obj;
			return name.equals(c.name) && data.equals(c.data);
		}
		return false;
	}
	

	@Override
	public String toString() {
		return name+":"+Arrays.toString(data);
	}
	
	@Override
	public Factor[] getRaw() {
		return data;
	}
	
	@Override
	public void setIntValue(int row, int val) {
		for (Factor f : data) 
			if (f!=null){
				data[row] = f.get(val);
				return;
			}
		throw new RuntimeException("No factor prototype available, columns is empty!");
	}

	@Override
	public void setDoubleValue(int row, double val) {
		setIntValue(row, (int)val);
	}

	@Override
	public boolean getBooleanValue(int row) {
		return data[row].getIndex()!=0;
	}

	@Override
	public Factor getFactorValue(int row) {
		return data[row];
	}

	@Override
	public void setBooleanValue(int row, boolean val) {
		setIntValue(row, val?1:0);
	}

	@Override
	public void setFactorValue(int row, Factor val) {
		data[row] = val;
	}
}
