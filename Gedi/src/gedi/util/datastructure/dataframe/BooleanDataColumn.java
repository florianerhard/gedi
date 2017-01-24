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
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.math.stat.factor.Factor;
import gedi.util.parsing.BooleanParser;

import java.io.IOException;

import cern.colt.bitvector.BitVector;

public class BooleanDataColumn implements DataColumn<BitVector> {

	private String name;
	private BitVector data;
	
	/**
	 * Data is NOT copied!
	 * @param name
	 * @param data
	 */
	public BooleanDataColumn(String name, BitVector data) {
		this.name = name;
		this.data = data;
	}
	
	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putString(name);
		FileUtils.writeBitVector(data, out);
	}
	
	@Override
	public void deserialize(BinaryReader in) throws IOException {
		name = in.getString();
		data = new BitVector(0);
		FileUtils.readBitVector(data, in);
	}
	
	/**
	 * Data is NOT copied!
	 * @param name
	 * @param data
	 */
	public BooleanDataColumn(String name, boolean[] data) {
		this.name = name;
		this.data = new BitVector(data.length);
		for (int i=0; i<data.length; i++)
			this.data.putQuick(i, data[i]);
	}
	
	@Override
	public BooleanDataColumn newInstance(String name, int length) {
		return new BooleanDataColumn(name, new BitVector(length));
	}
	
	@Override
	public boolean isBoolean() {
		return true;
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
		return false;
	}
	
	@Override
	public String toString(int row) {
		return getBooleanValue(row)+"";
	}

	@Override
	public String name() {
		return name;
	}
	
	@Override
	public int size() {
		return data.size();
	}

	@Override
	public void copyValueTo(int fromIndex, DataColumn to, int toIndex) {
		to.setBooleanValue(toIndex, getBooleanValue(fromIndex));
	}

	@Override
	public int getIntValue(int row) {
		return data.get(row)?1:0;
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
		if (obj instanceof BooleanDataColumn){
			BooleanDataColumn c = (BooleanDataColumn)obj;
			return name.equals(c.name) && data.equals(c.data);
		}
		return false;
	}
	
	
	public boolean[] toBooleanArray() {
		boolean[] re = new boolean[data.size()];
		for (int i=0; i<re.length; i++)
			re[i] = this.data.getQuick(i);
		return re;
	}
	
	
	@Override
	public String toString() {
		return name+":"+data.toString();
	}
	
	@Override
	public BitVector getRaw() {
		return data;
	}
	
	@Override
	public void setIntValue(int row, int val) {
		data.putQuick(row, val!=0);
	}

	@Override
	public void setDoubleValue(int row, double val) {
		data.putQuick(row, val!=0);
	}

	@Override
	public boolean getBooleanValue(int row) {
		return data.getQuick(row);
	}

	@Override
	public Factor getFactorValue(int row) {
		return data.getQuick(row)?Factor.TRUE:Factor.FALSE;
	}

	@Override
	public void setBooleanValue(int row, boolean val) {
		data.putQuick(row, val);
	}

	@Override
	public void setFactorValue(int row, Factor val) {
		data.putQuick(row, val.getIndex()!=0);
	}
}
