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

import gedi.util.datastructure.array.DoubleArray;
import gedi.util.datastructure.array.IntegerArray;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.functions.EI;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import gedi.util.math.stat.factor.Factor;
import gedi.util.orm.Orm;
import gedi.util.plotting.Aes;
import gedi.util.plotting.GGPlot;

import java.io.IOException;
import java.io.Writer;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * Columns may either contain booleans, ints, doubles or Factors
 * @author flo
 *
 */
public class DataFrame extends AbstractCollection<DataColumn> implements BinarySerializable {

	private LinkedHashMap<String,DataColumn> columns = new LinkedHashMap<>();
	private int rows = -1;
	
	
	@Override
	public Iterator<DataColumn> iterator() {
		return columns.values().iterator();
	}

	@Override
	public int size() {
		return columns.size();
	}
	
	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putInt(rows);
		out.putInt(columns.size());
		
		for (String n : columns.keySet()) {
			out.putAsciiChar(columns.get(n).getClass().getSimpleName().charAt(0));
			columns.get(n).serialize(out);
		}
	}
	
	@Override
	public void deserialize(BinaryReader in) throws IOException {
		columns = new LinkedHashMap<>();
		rows = in.getInt();
		int cols = in.getInt();

		for (int i=0; i<cols; i++) {
			DataColumn c = null;
			char t = in.getAsciiChar();
			switch (t) {
			case 'B': c = Orm.create(BooleanDataColumn.class); break;
			case 'D': c = Orm.create(DoubleDataColumn.class); break;
			case 'I': c = Orm.create(IntegerDataColumn.class); break;
			case 'F': c = Orm.create(FactorDataColumn.class); break;
			default: throw new RuntimeException(String.valueOf(t));
			}
			c.deserialize(in);
			add(c);
		}
	}
	
	
	public boolean add(DataColumn col) {
		if (rows>=0 && col.size()!=rows) throw new IllegalArgumentException("Row number does not match!");
		if (columns.containsKey(col.name())) throw new IllegalArgumentException("Column with name "+col.name()+" already exists!");
		columns.put(col.name(), col);
		rows = col.size();
		return true;
	}
	
	public DataColumn remove(String name) {
		return columns.remove(name);
	}
	public DataColumn remove(DataColumn col) {
		return columns.remove(col.name());
	}
	
	public IntegerDataColumn getIntegerColumn(String name) {
		return (IntegerDataColumn) columns.get(name);
	}
	
	public IntegerDataColumn getIntegerColumn(int index) {
		String[] head = columns.keySet().toArray(new String[0]);
		return (IntegerDataColumn) (DataColumn) getColumn(head[index]);
	}
	
	public DoubleDataColumn getDoubleColumn(String name) {
		return (DoubleDataColumn) columns.get(name);
	}
	
	public DoubleDataColumn getDoubleColumn(int index) {
		String[] head = columns.keySet().toArray(new String[0]);
		return (DoubleDataColumn) (DataColumn) getColumn(head[index]);
	}
	
	public FactorDataColumn getFactorColumn(String name) {
		return (FactorDataColumn) columns.get(name);
	}
	
	public FactorDataColumn getFactorColumn(int index) {
		String[] head = columns.keySet().toArray(new String[0]);
		return (FactorDataColumn) (DataColumn) getColumn(head[index]);
	}
	
	public boolean hasColumn(String name) {
		return columns.containsKey(name);
	}
	
	public <T> DataColumn<T> getColumn(String name) {
		return columns.get(name);
	}
	public Collection<String> getColumnNames() {
		return columns.keySet();
	}
	
	public <T> DataColumn<T> getColumn(int index) {
		String[] head = columns.keySet().toArray(new String[0]);
		return getColumn(head[index]);
	}
	
	public Object[] getRow(int r) {
		Object[] re = new Object[columns()];
		for (int i=0; i<re.length; i++)
			re[i] = getColumn(i).getValue(r);
		return re;
	}
	
	public int columns() {
		return columns.size();
	}
	
	public int rows() {
		return rows==-1?0:rows;
	}
	
	public Writer write(Writer wr, boolean header, String delim) throws IOException {
		String[] head = columns.keySet().toArray(new String[0]);
		if (header) {
			for (int i=0; i<head.length; i++) {
				if (i>0) wr.append(delim);
				wr.append(head[i]);
			}
			wr.append("\n");
		}
		
		for (int i=0; i<rows; i++) {
			for (int j=0; j<head.length; j++) {
				if (j>0) wr.append("\t");
				wr.append(columns.get(head[j]).toString(i));
			}
			wr.append("\n");
		}
		return wr;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		String[] head = columns.keySet().toArray(new String[0]);
		for (int i=0; i<head.length; i++) {
			if (i>0) sb.append("\t");
			sb.append(head[i]);
		}
		sb.append("\n");
		
		for (int i=0; i<rows; i++) {
			for (int j=0; j<head.length; j++) {
				if (j>0) sb.append("\t");
				sb.append(columns.get(head[j]).toString(i));
			}
			sb.append("\n");
		}
		
		return sb.toString();
	}

	public DataFrame createView(int... cols) {
		DataFrame re = new DataFrame();
		for (int c : cols)
			re.add(getColumn(c));
		return re;
	}

	
	public DataFrame createView(String... names) {
		DataFrame re = new DataFrame();
		for (String n: names)
			re.add(getColumn(n));
		return re;
	}

	
	/**
	 * Merges the other dataframe to this and returns this
	 * @param other
	 * @return
	 */
	public DataFrame merge(DataFrame other) {
		for (DataColumn c : other)
			add(c);
		return this;
	}

	/**
	 * Melts the dataframe with factor name "column" and value name "Value"
	 * @return
	 */
	public DataFrame melt() {
		return melt("Column","Value");
	}
	/**
	 * Melts all numeric {@link DataColumn}s: Keep all other columns, create two new ones: One is a factor column 
	 * with the given name, the other is either a {@link IntegerDataColumn} or a {@link DoubleDataColumn} with all the values
	 * Returns a new dataframe
	 * @return
	 */
	public DataFrame melt(String factorName, String valueName) {
		ArrayList<DataColumn> keep = new ArrayList<>();
		ArrayList<DataColumn> numeric = new ArrayList<>();
		boolean isDouble = false;
		
		for (int i=0; i<columns(); i++)
			if (getColumn(i).isDouble()) { 
				numeric.add(getDoubleColumn(i));
			}
			else if (getColumn(i).isInteger()) { 
				numeric.add(getIntegerColumn(i));
			}
			else
				keep.add(getColumn(i));
		
		if (numeric.size()==0) throw new RuntimeException("No numeric column available!");
		
		DataColumn[] col = new DataColumn[keep.size()+2];
		for (int i=0; i<keep.size(); i++) 
			col[i] = keep.get(i).newInstance(keep.get(i).name(), rows()*numeric.size());
		int FAC = col.length-2;
		int VAL = col.length-1;
		col[FAC] = new FactorDataColumn(factorName, new Factor[rows()*numeric.size()]);
		if (!isDouble)
			col[VAL] = new IntegerDataColumn(valueName, (IntegerArray) NumericArray.createMemory(rows()*numeric.size(), NumericArrayType.Integer));
		else
			col[VAL] = new DoubleDataColumn(valueName, (DoubleArray) NumericArray.createMemory(rows()*numeric.size(), NumericArrayType.Double));
		
		Factor[] fac = Factor.create(EI.wrap(numeric).map(d->d.name()).toArray(String.class)).getLevels();
		
		for (int r=0; r<rows(); r++) {
			for (int f=0; f<numeric.size(); f++) {
				for (int c=0; c<keep.size(); c++) {
					keep.get(c).copyValueTo(r,col[c],r*numeric.size()+f);
				}
				col[FAC].setFactorValue(r*numeric.size()+f, fac[f]);
				numeric.get(f).copyValueTo(r, col[VAL], r*numeric.size()+f);
			}
		}
		
		DataFrame df = new DataFrame();
		df.addAll(Arrays.asList(col));
		return df;
	}
	
	public GGPlot ggplot(Aes...aes) {
		return new GGPlot(this, aes);
	}
	
	
	
}
