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

package gedi.util.datastructure.charsequence;

import java.util.Arrays;

import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import cern.colt.bitvector.BitVector;

public class CharArrayCharSequence implements CharSequence {

	private char[] seq;
	
	public CharArrayCharSequence(char[] seq) {
		this.seq = seq;
	}

	@Override
	public int length() {
		return seq.length;
	}

	@Override
	public char charAt(int index) {
		return seq[index];
	}
	

	@Override
	public CharSequence subSequence(int start, int end) {
		return new CharArrayCharSequence(Arrays.copyOfRange(seq, start, end));
	}
	
	@Override
	public String toString() {
		return StringUtils.toString(this);
	}
	
	@Override
	public boolean equals(Object obj) {
		return StringUtils.equals(this, obj);
	}
	
	@Override
	public int hashCode() {
		return StringUtils.hashCode(this);
	}

	
}
