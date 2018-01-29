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

package gedi.util.algorithm.string.alignment.pairwise.scoring;

import java.util.HashMap;

public abstract class AbstractScoring<A,P> implements Scoring<A> {

	protected HashMap<String,P> cache = new HashMap<String, P>();
	protected P s1;
	protected P s2;
	protected int maxLength;
	
	public abstract P encode(A s);
	public abstract A decode(P s);
	public abstract int length(P s);
	
	@Override
	public void clearCache() {
		cache.clear();
		maxLength = 0;
	}
	
	@Override
	public boolean containsSubject(String id) {
		return cache.containsKey(id);
	}
	
	public void setEncodedSubjects(P s1, P s2) {
		this.s1 = s1;
		this.s2 = s2;
	}
	
	public int getMaxLength() {
		return maxLength;
	}
	
	@Override
	public int getLength1() {
		return length(s1);
	}
	
	@Override
	public int getLength2() {
		return length(s2);
	}
	
	@Override
	public void cacheSubject(String id, A s) {
		P p = encode(s);
		cache.put(id, p);
		maxLength = Math.max(maxLength,length(p));
	}
	
	@Override
	public A getCachedSubject(String id) {
		return decode(cache.get(id));
	}

	@Override
	public void loadCachedSubjects(String id1, String id2) {
		P s1 = cache.get(id1);
		if (s1==null)
			throw new IllegalArgumentException(id1+" unknown!");
		P s2 = cache.get(id2);
		if (s2==null)
			throw new IllegalArgumentException(id2+" unknown!");
		setEncodedSubjects(s1,s2);
	}

	@Override
	public void setSubjects(A s1, A s2) {
		setEncodedSubjects(encode(s1), encode(s2));
	}
	
	
	public static int inferPrecision(float f) {
		String number = f+"";
		return number.indexOf('.')>=0 && !number.endsWith(".") ? number.length()-number.indexOf('.')-2: 0;
	}
	

}
