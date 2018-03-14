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
package gedi.util.sequence;


import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import cern.colt.bitvector.BitVector;


public class DnaSequence extends BitVector implements CharSequence {

	
	private static final long serialVersionUID = -2998934882733151866L;

	public DnaSequence(CharSequence sequence) {
		super(sequence.length()*2);
		for (int i=0; i<sequence.length(); i++) {
			char c = sequence.charAt(i);
			putQuick(i*2, (SequenceUtils.inv_nucleotides[c]&1)==1);
			putQuick(i*2+1, ((SequenceUtils.inv_nucleotides[c]>>1)&1)==1);
		}
	}
	
	public DnaSequence(char... sequence) {
		super(sequence.length*2);
		for (int i=0; i<sequence.length; i++) {
			char c = sequence[i];
			putQuick(i*2, (SequenceUtils.inv_nucleotides[c]&1)==1);
			putQuick(i*2+1, ((SequenceUtils.inv_nucleotides[c]>>1)&1)==1);
		}
	}
	
	
	@Override
	public String toString() {
		return getSequence();
	}
	
	public int getSequenceLength() {
		return size()/2;
	}
	
	
	public String getSequence() {
		char[] re = new char[getSequenceLength()];
		for (int i=0; i<re.length; i++)
			re[i] = charAt(i);
		return new String(re);
	}
	

	@Override
	public char charAt(int index) {
		int r=0;
		if (getQuick(index*2))
			r|=1;
		if (getQuick(index*2+1))
			r|=2;
		return SequenceUtils.nucleotides[r];
	}

	@Override
	public int length() {
		return getSequenceLength();
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return new DnaSequence(getSequence().substring(start, end));
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj==this) return true;
		if (obj==null) return false;
		if (!(obj instanceof CharSequence)) return false;
		CharSequence s = (CharSequence) obj;
		if (s.length()!=length()) return false;
		for (int i=0; i<s.length(); i++) 
			if (s.charAt(i)!=charAt(i)) return false;
		return true;
	}
	
}
