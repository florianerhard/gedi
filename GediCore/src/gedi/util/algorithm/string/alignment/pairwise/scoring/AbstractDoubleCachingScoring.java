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

public abstract class AbstractDoubleCachingScoring<A,P> implements Scoring<A> {

	protected HashMap<String,P> cacheEnc = new HashMap<String, P>();
	protected HashMap<String,A> cacheDec = new HashMap<String, A>();
	protected P s1;
	protected P s2;

	protected abstract P encode(A s);
	protected abstract int length(P s);
	
	
	@Override
	public boolean containsSubject(String id) {
		return cacheDec.containsKey(id);
	}
	
	@Override
	public void clearCache() {
		cacheEnc.clear();
		cacheDec.clear();
	}
	protected void setEncodedSubjects(P s1, P s2) {
		this.s1 = s1;
		this.s2 = s2;
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
		cacheEnc.put(id, encode(s));
		cacheDec.put(id, s);
	}
	
	@Override
	public A getCachedSubject(String id) {
		return cacheDec.get(id);
	}

	@Override
	public void loadCachedSubjects(String id1, String id2) {
		P s1 = cacheEnc.get(id1);
		if (s1==null)
			throw new IllegalArgumentException(id1+" unknown!");
		P s2 = cacheEnc.get(id2);
		if (s2==null)
			throw new IllegalArgumentException(id2+" unknown!");
		setEncodedSubjects(s1,s2);
	}

	@Override
	public void setSubjects(A s1, A s2) {
		setEncodedSubjects(encode(s1), encode(s2));
	}
	

}
