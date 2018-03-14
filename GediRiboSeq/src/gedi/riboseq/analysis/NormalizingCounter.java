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

import java.util.HashMap;
import java.util.Set;


class NormalizingCounter<T> {

	private HashMap<T,CountObject> values = new HashMap<>();
	
	public void count(T key, double val) {
		CountObject co = values.computeIfAbsent(key, x->new CountObject());
		co.counted++;
		co.value+=val;
	}
	
	public void add(NormalizingCounter<T> c) {
		for (T key : c.values.keySet()) {
			CountObject oo = c.values.get(key);
			CountObject co = values.computeIfAbsent(key, x->new CountObject());
			co.counted+=oo.counted;
			co.value+=oo.value;
		}
	}
	
	public Set<T> keySet() {
		return values.keySet();
	}
	
	public double value(T key) {
		CountObject oo = values.get(key);
		if (oo==null) return 0;
		return oo.value/oo.counted;
	}
	
	public int count(T key) {
		CountObject oo = values.get(key);
		if (oo==null) return 0;
		return oo.counted;
	}
	
	private static class CountObject {
		int counted;
		double value;
	}
	
}
