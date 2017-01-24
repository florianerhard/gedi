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


public class WildcardMatchMismatchScoring extends AbstractScoring<CharSequence, char[]> implements LongScoring<CharSequence> {

	private float match;
	private float mismatch;
	
	private long lma;
	private long lmm;
	private long mult=0;
	
	private char wildcard;
	
	
	public WildcardMatchMismatchScoring(float match, float mismatch) {
		this(match,mismatch,'\0');
	}
	
	public WildcardMatchMismatchScoring(float match, float mismatch, char wildcard) {
		build(match,mismatch,
				Math.max(SubstitutionMatrix.inferPrecision(match),SubstitutionMatrix.inferPrecision(mismatch))
		);
		this.wildcard = wildcard;
	}
	
	public void ensurePrecision(int precision) {
		build(match, mismatch, precision);
	}
	
	public void build(float match, float mismatch, int precision) {
		this.match = match;
		this.mismatch = mismatch;
		mult = (long)Math.pow(10, precision);
		this.lma = (long)Math.round(match*mult);
		this.lmm = (long)Math.round(mismatch*mult);
	}
	
	public long getLong(int i, int j) {
		if (s1[i]==wildcard) return 0;
		if (s2[j]==wildcard) return 0;
		return s1[i]==s2[j]?lma:lmm;
	}
	
	public float getFloat(int i, int j) {
		if (s1[i]==wildcard) return 0;
		if (s2[j]==wildcard) return 0;
		return s1[i]==s2[j]?match:mismatch;
	}
	
	public CharSequence decode(char[] s) {
		return new String(s);
	}
	
	public char[] encode(CharSequence s) {
		char[] re = new char[s.length()];
		int n = s.length();
		for (int i=0; i<n; i++)
			re[i] =s.charAt(i);
		return re;
	}

	public float correct(long score) {
		return score/(float)mult;
	}
	
	public long correct(float param) {
		return (long) (param*mult);
	}
	
	@Override
	public int length(char[] s) {
		return s.length;
	}

	public float getCorrectionFactor() {
		return mult;
	}
	
	
}
