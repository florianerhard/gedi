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
package gedi.util.datastructure.collections;

import gedi.util.mutable.MutableInteger;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

public class SimpleHistogram<T> extends HashMap<T,MutableInteger> {

	private Comparator<T> comparator;
	
	public SimpleHistogram<T> setComparator(Comparator<T> comparator) {
		this.comparator = comparator;
		return this;
	}
	
	public int getTotal() {
		int re = 0;
		for (MutableInteger n : values())
			re+=n.N;
		return re;
	}
	
	public int put(T element) {
		MutableInteger mi = get(element);
		if (mi==null) put(element,mi = new MutableInteger());
		return ++mi.N;
	}
	
	@Override
	public MutableInteger get(Object key) {
		MutableInteger re = super.get(key);
		if (re==null) put((T) key,re = new MutableInteger());
		return re;
	}
	
	public int getCount(T key) {
		MutableInteger re = super.get(key);
		if (re==null) put(key,re = new MutableInteger());
		return re.N;
	}
	
	public int increment(T key) {
		return ++get(key).N;
	}
	
	public int increment(T key, int count) {
		MutableInteger mi = get(key);
		mi.N+=count;
		return mi.N;
	}
	
	public void writeHistogram(Writer w) throws IOException {
		if (comparator!=null) {
			Object[] a = keySet().toArray();
			Arrays.sort(a,(Comparator)comparator);
			for (int i=0; i<a.length; i++)
				w.write(a[i]+"\t"+get(a[i])+"\n");
		} else {
			for (T e : keySet())
				w.write(e+"\t"+get(e)+"\n");
		}
	}
	
	@Override
	public String toString() {
		StringWriter sw = new StringWriter();
		try {
			writeHistogram(sw);
		} catch (IOException e) {
		}
		return sw.toString();
	}
	
	
}
