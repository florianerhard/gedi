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

package gedi.util.io.text.tsv.formats;

import gedi.core.data.table.GediNamingPolicy;
import gedi.core.data.table.Table;
import gedi.core.data.table.TableType;
import gedi.core.data.table.Tables;
import gedi.util.ReflectionUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.charsequence.MaskedCharSequence;
import gedi.util.datastructure.collections.bitcollections.BitList;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.datastructure.dataframe.DataColumn;
import gedi.util.datastructure.dataframe.DataFrame;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.text.HeaderLine;
import gedi.util.io.text.LineIterator;
import gedi.util.math.stat.factor.Factor;
import gedi.util.mutable.MutableTuple;
import gedi.util.orm.Orm;
import gedi.util.parsing.Parser;
import gedi.util.userInteraction.progress.Progress;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;

import net.sf.cglib.beans.BeanGenerator;

@SuppressWarnings("unchecked")
public class CsvReader {


	private String name;
	private String[][] read;
	private LineIterator it;
	private char separator;
	private HeaderLine header;
	private Parser[] parsers;
	private Progress progress;
	private boolean maskQuotes;

	
	public CsvReader(String name, String[][] read, LineIterator it, char separator, boolean maskQuotes,
			HeaderLine header, Parser[] parsers) {
		this.name = name;
		this.read = read;
		this.it = it;
		this.separator = separator;
		this.maskQuotes = maskQuotes;
		this.header = header;
		this.parsers = parsers;
	}
	
	public CsvReader setProgress(Progress progress) {
		this.progress = progress;
		return this;
	}
	
	public Parser getParser(int col) {
		return parsers[col];
	}

	public ExtendedIterator<Object[]> iterateArray() {
		return iterateUnparsed().map(l->{
			Object[] re = new Object[parsers.length];
			for (int i=0; i<l.length; i++)
				re[i] =parsers[i].apply(l[i]);
			return re;
		});
	}
	
	public ExtendedIterator<String[]> iterateUnparsed() {
		return EI.wrap(read).chain(it.map(l->maskQuotes?MaskedCharSequence.maskQuotes(l, '\0').splitAndUnmask(separator):StringUtils.split(l, separator)))
				.iff(progress!=null, it->it.progress(progress, -1, a->"Reading csv"));
	}
	
	
	public ExtendedIterator<MutableTuple> iterateTuple() {
		Class[] types = EI.wrap(parsers).map(p->p.getParsedType()).toArray(new Class[0]);
		return iterateUnparsed().map(l->{
			MutableTuple tup = new MutableTuple(types);
			for (int i=0; i<l.length; i++)
				tup.set(i, parsers[i].apply(l[i]));
			return tup;
		});
	}
	
	public <T> ExtendedIterator<T> iterateObjects() {
		return iterateObjects(createClass());
	}
	
	public HeaderLine getHeader() {
		return header;
	}
	
	private Class createClass() {
		int cols = parsers.length;
		BeanGenerator gen = new BeanGenerator();
		gen.setNamingPolicy(new GediNamingPolicy(StringUtils.toJavaIdentifier(name)));
		for (int i=0; i<cols; i++) {
			String lab = StringUtils.toJavaIdentifier(header.get(i));
			gen.addProperty(lab, ReflectionUtils.toPrimitveClass(parsers[i].getParsedType()));
		}
		return (Class) gen.createClass();
	}

	public <T> ExtendedIterator<T> iterateObjects(Class<T> cls) {
		return iterateUnparsed().map(l->Orm.fromFunctor(cls, (fi)->fi>=parsers.length?null:parsers[fi].apply(l[fi])));
	}

	public <T> Table<T> readTable() {
		Class<?> cls = createClass();
		String creator = getClass().getSimpleName();
		int version = Tables.getInstance().getMostRecentVersion(TableType.Temporary,name)+1;
		Table tab = Tables.getInstance().create(TableType.Temporary,Tables.getInstance().buildMeta(name, creator, version, "", cls));
		
		tab.beginAddBatch();
		iterateObjects(cls).forEachRemaining(o->tab.add(o));
		tab.endAddBatch();
		
		return tab;
	}
	
	public DataFrame readDataFrame() {
		Collection[] lists = readLists();
		DataFrame df = new DataFrame();
		
		for (int i = 0; i < lists.length; i++) {
			if (getParser(i).getParsedType()==String.class) 
				lists[i] = Factor.fromStrings(lists[i]);
			df.add(DataColumn.fromCollection(getHeader().get(i),lists[i]));
		}
		return df;
	}
	
	public DataFrame readDataFrame(String[] names) {
		IntArrayList read = new IntArrayList();
		for (int i=0; i<names.length; i++)
			if (names[i].length()>0)
				read.add(i);
			
		Collection[] lists = readLists(read.toIntArray());
		DataFrame df = new DataFrame();
		
		for (int i = 0; i < lists.length; i++) {
			if (getParser(i).getParsedType()==String.class) 
				lists[i] = Factor.fromStrings(lists[i]);
			df.add(DataColumn.fromCollection(names[i],lists[i]));
		}
		return df;
	}
	
	public Collection[] readLists(int[] read) {
		Collection[] re = new Collection[read.length];
		for (int c=0; c<read.length; c++) {
			int i = read[c];
			if (parsers[i].getParsedType()==Boolean.class)
				re[i] = new BitList();
			else if (parsers[i].getParsedType()==Integer.class)
				re[i] = new IntArrayList();
			else if (parsers[i].getParsedType()==Double.class)
				re[i] = new DoubleArrayList();
			else
				re[i] = new ArrayList();
		}
		iterateArray().forEachRemaining(a-> {
			for (int c=0; c<read.length; c++)
				re[read[c]].add(a[read[c]]);
		});
		
		return re;
	}
	
	public Collection[] readLists() {
		Collection[] re = new Collection[parsers.length];
		for (int i=0; i<re.length; i++) {
			if (parsers[i].getParsedType()==Boolean.class)
				re[i] = new BitList();
			else if (parsers[i].getParsedType()==Integer.class)
				re[i] = new IntArrayList();
			else if (parsers[i].getParsedType()==Double.class)
				re[i] = new DoubleArrayList();
			else
				re[i] = new ArrayList();
		}
		iterateArray().forEachRemaining(a-> {
			for (int i=0; i<a.length; i++)
				re[i].add(a[i]);
		});
		
		return re;
	}
	
	
	public String toJson() {
		Object[] a = iterateMap(true).toArray();
		DynamicObject d = DynamicObject.from(a);
		return d.toJson();
	}
	
	public ExtendedIterator<LinkedHashMap<String, Object>> iterateMap(boolean camelcase) {
		return iterateTuple().map(tup->{
			LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
			for (int i=0; i<tup.size(); i++) {
				String n = header.get(i);
				if (camelcase)
					n = StringUtils.toCamelcase(n);
				map.put(n, tup.get(i));
			}
			return map;
		});
	}

	
}
