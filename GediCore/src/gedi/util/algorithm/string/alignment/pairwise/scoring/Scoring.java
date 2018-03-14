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

public interface Scoring<A> {

	/**
	 * Gets the score for position p1 in the first sequence and position p2 in the second.
	 * @param p1
	 * @param p2
	 * @return
	 */
	public float getFloat(int p1, int p2);
	
	public boolean containsSubject(String id);
	public A getCachedSubject(String id);
	public void cacheSubject(String id, A s);
	public void loadCachedSubjects(String id1, String id2);
	public void setSubjects(A s1, A s2);
	
	public int getLength1();
	public int getLength2();

	public void clearCache();

}
