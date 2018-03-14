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
package gedi.riboseq.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import gedi.util.StringUtils;
import gedi.util.functions.EI;
import gedi.util.io.text.LineWriter;

public abstract class MetaGenePriceAnalysis<T> implements PriceAnalysis<NormalizingCounter<T>[]> {

	private String keyName;
	private Function<T,String> keyStringer = k->StringUtils.toString(k);
	private Comparator<T> keyComparator = null;
	protected String[] conditions;
	
	
	public MetaGenePriceAnalysis(String[] conditions,String keyName) {
		this.conditions = conditions;
		this.keyName = keyName;
	}
	
	public void setKeyStringer(Function<T, String> keyStringer) {
		this.keyStringer = keyStringer;
	}
	
	public void setKeyComparator(Comparator<T> keyComparator) {
		this.keyComparator = keyComparator;
	}

	@Override
	public void header(LineWriter out) throws IOException {
		out.writef("Condition\t%s\tValue\n",keyName);
	}

	@Override
	public NormalizingCounter<T>[] createContext() {
		NormalizingCounter<T>[] re = new NormalizingCounter[conditions.length];
		for (int c=0; c<re.length; c++)
			re[c] = new NormalizingCounter<T>();
		return re;
	}

	@Override
	public void reduce(List<NormalizingCounter<T>[]> ctx, LineWriter out) {
		Iterator<NormalizingCounter<T>[]> it = ctx.iterator();
		NormalizingCounter<T>[] m = it.next();
		while (it.hasNext()){
			NormalizingCounter<T>[] cc = it.next();
			for (int c=0; c<m.length;c++)
				m[c].add(cc[c]);
		}
		
		for (int c=0; c<m.length; c++) {
			ArrayList<T> keys = new ArrayList<T>(m[c].keySet());
			if (keyComparator!=null)
				Collections.sort(keys, keyComparator);
			else if(EI.wrap(keys).castFiltered(Comparable.class).count()==keys.size())
				Collections.sort(keys, (a,b)->((Comparable)a).compareTo((Comparable)b));
			
			for (T key : keys) {
				out.writef2("%s\t%s\t%.5f\n",conditions[c],keyStringer.apply(key),m[c].value(key));
			}
		}
	}
	
	
}
