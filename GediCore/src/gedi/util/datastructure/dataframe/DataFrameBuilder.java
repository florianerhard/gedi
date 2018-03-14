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

import gedi.util.StringUtils;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.math.stat.factor.Factor;

import java.util.ArrayList;
import java.util.Collection;

@Deprecated
public class DataFrameBuilder {
	
	public enum StartWith {
		Int,Double,String
	}

	private Collection<?>[] columns;
	private String[] names;
	
	public DataFrameBuilder(int columns) {
		this(columns,StartWith.Int);
	}
	public DataFrameBuilder(int columns, StartWith start) {
		this.names = new String[columns];
		for (int i=0; i<names.length; i++)
			names[i] = "V"+(i+1);
		this.columns = new Collection[columns];
		for (int i=0; i<this.columns.length; i++)
			this.columns[i] = init(start);
	}
	public DataFrameBuilder(String[] names) {
		this(names,StartWith.Int);
	}
	public DataFrameBuilder(String[] names, StartWith start) {
		this.names = names;
		this.columns = new Collection[names.length];
		for (int i=0; i<this.columns.length; i++)
			this.columns[i] = init(start);
	}
	
	private Collection<?> init(StartWith start) {
		switch (start) {
		case Int: return new IntArrayList();
		case Double: return new DoubleArrayList();
		case String: return new ArrayList<String>();
		default: throw new RuntimeException("Unknown!");
		}
	}
	
	
	public DataFrameBuilder addRow(String[] unparsed) {
		if (unparsed.length!=columns.length) throw new RuntimeException("Unequal number of columns!");
		for (int i=0; i<unparsed.length; i++) 
			addEntry(unparsed[i], i);
		return this;
	}
	
	public DataFrameBuilder addRow(String[] unparsed, int[] cols) {
		if (cols.length!=columns.length) throw new RuntimeException("Unequal number of columns!");
		for (int i=0; i<cols.length; i++) 
			addEntry(unparsed[i], cols[i]);
		return this;
	}
	
	@SuppressWarnings("unchecked")
	private void addEntry(String s, int col) {
		if (columns[col] instanceof IntArrayList) {
			if (StringUtils.isInt(s)) 
				((IntArrayList)columns[col]).add(Integer.parseInt(s));
			else if (StringUtils.isNumeric(s)) {
				DoubleArrayList l = toDouble((IntArrayList)columns[col]);
				columns[col] = l;
				l.add(Double.parseDouble(s));
			}
			else {
				ArrayList<String> l = toString((IntArrayList)columns[col]);
				columns[col] = l;
				l.add(s);
			}
		} else if (columns[col] instanceof DoubleArrayList) {
			if (StringUtils.isNumeric(s)) 
				((DoubleArrayList)columns[col]).add(Double.parseDouble(s));
			else {
				ArrayList<String> l = toString((DoubleArrayList)columns[col]);
				columns[col] = l;
				l.add(s);
			}
		} else 
			((ArrayList<String>)columns[col]).add(s);
	}
	
	
	@SuppressWarnings("unchecked")
	public DataFrame build() {
		DataFrame re = new DataFrame();
		for (int i=0; i<columns.length; i++) {
			if (columns[i] instanceof IntArrayList) 
				re.add(new IntegerDataColumn(names[i], ((IntArrayList)columns[i]).toIntArray()));
			else if (columns[i] instanceof DoubleArrayList) 
				re.add(new DoubleDataColumn(names[i], ((DoubleArrayList)columns[i]).toDoubleArray()));
			else 
				re.add(new FactorDataColumn(names[i], ((ArrayList<Factor>)columns[i]).toArray(new Factor[0])));
		}
		return re;
	}
	
	private DoubleArrayList toDouble(IntArrayList l) {
		DoubleArrayList re = new DoubleArrayList(l.size());
		for (int i=0; i<l.size(); i++)
			re.add((double)l.getInt(i));
		return re;
	}
	
	private ArrayList<String> toString(IntArrayList l) {
		ArrayList<String> re = new ArrayList<String>(l.size());
		for (int i=0; i<l.size(); i++)
			re.add(""+l.getInt(i));
		return re;
	}
	
	private ArrayList<String> toString(DoubleArrayList l) {
		ArrayList<String> re = new ArrayList<String>(l.size());
		for (int i=0; i<l.size(); i++)
			re.add(""+l.getDouble(i));
		return re;
	}
	
	
}
