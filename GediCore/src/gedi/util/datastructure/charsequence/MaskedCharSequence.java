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

import gedi.core.region.ArrayGenomicRegion;
import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;

import java.util.ArrayList;
import java.util.Arrays;

import cern.colt.bitvector.BitVector;

public class MaskedCharSequence implements CharSequence {

	private CharSequence seq;
	private BitVector masked;
	private char mask;
	
	private MaskedCharSequence(CharSequence seq, BitVector masked, char mask) {
		this.seq = seq;
		this.masked = masked;
		this.mask = mask;
	}

	@Override
	public int length() {
		return seq.length();
	}

	@Override
	public char charAt(int index) {
		return masked.getQuick(index)?mask:seq.charAt(index);
	}

	@Override
	public MaskedCharSequence subSequence(int start, int end) {
		MaskedCharSequence re = new MaskedCharSequence(seq.subSequence(start, end), new BitVector(end-start), mask);
		re.masked.replaceFromToWith(0, re.masked.size()-1, masked, start);
		return re;
	}
	
	public CharSequence getUnmasked() {
		return seq;
	}
	
	public ArrayGenomicRegion getUnmaskedRegion() {
		IntArrayList re = new IntArrayList();
		if (!masked.get(0)) re.add(0);
		for (int i=1; i<length(); i++) {
			if (i!=0 && masked.get(i)!=masked.get(i-1)) re.add(i);
		}
		if (!masked.get(length()-1)) re.add(length());
		return new ArrayGenomicRegion(re);
	}
	
	public char getMask() {
		return mask;
	}
	
	
	public boolean isMasked(int index) {
		return masked.getQuick(index);
	}
	
	public boolean isAllMasked(int start, int end) {
		for (int i=start; i<end; i++)
			if (!isMasked(i)) return false;
		return true;
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

	
	public String[] splitAndUnmask(char separator) {
		if (length()==0)
			return new String[0];
		
		String s = toString();
		
		ArrayList<String> re = new ArrayList<String>();
		int p = 0;
		for (int sepIndex=s.indexOf(separator); sepIndex>=0; sepIndex = s.substring(p).indexOf(separator)) {
			re.add(seq.subSequence(p,p+sepIndex).toString());
			p += sepIndex+1;
		}
		re.add(seq.subSequence(p,seq.length()).toString());
		return re.toArray(new String[re.size()]);
	}
	
	public static MaskedCharSequence maskEscaped(CharSequence seq, char mask, char...escape) {
		int l = seq.length();
		BitVector masked = new BitVector(l);
		
		for (int i=0; i<seq.length(); i++) {
			if (seq.charAt(i)=='\\' && ++i<seq.length() && ArrayUtils.find(escape, seq.charAt(i))>=0) {
				masked.putQuick(i-1, true);
				masked.putQuick(i, true);
			}
		}
		
		return new MaskedCharSequence(seq, masked, mask);
	}
	
	public static MaskedCharSequence maskChars(CharSequence seq, char mask, char...chars) {
		int l = seq.length();
		boolean[] mchars = new boolean[265];
		for (char c : chars)
			mchars[c] = true;
		
		BitVector masked = new BitVector(l);
		
		for (int i=0; i<seq.length(); i++) {
			if (mchars[seq.charAt(i)]) {
				masked.putQuick(i, true);
			}
		}
		
		return new MaskedCharSequence(seq, masked, mask);
	}
	
	public static MaskedCharSequence maskQuotes(CharSequence seq, char mask) {
		return maskLeftToRight(seq,mask, new char[] {'\'','"'}, new char[] {'\'','"'});
	}
	
	public static boolean canMaskQuotes(CharSequence seq) {
		return canMaskLeftToRight(seq,new char[] {'\'','"'}, new char[] {'\'','"'});
	}
	
	public static boolean canMaskLeftToRight(CharSequence seq, char[] open, char[] close) {
		int within = -1;
		int depth = 0;
		
		ArrayUtils.parallelSort(open, close);
		
		int l = seq.length();
		
		for (int i=0; i<l; i++) {
			if (within==-1) {
				int p = Arrays.binarySearch(open, seq.charAt(i));
				if (p>=0) {
					within = p;
					depth = 1;
				}
			}
			else {
				if (seq.charAt(i)==close[within]){
					if (--depth==0)
						within = -1;
				} else if (seq.charAt(i)==open[within]){
					depth++;
				}
			}
		}
		return within==-1;
	}
	
	public static MaskedCharSequence maskLeftToRight(CharSequence seq, char mask, char[] open, char[] close) {
		int within = -1;
		int depth = 0;
		
		ArrayUtils.parallelSort(open, close);
		
		int l = seq.length();
		BitVector masked = new BitVector(l);
		
		for (int i=0; i<l; i++) {
			if (within==-1) {
				int p = Arrays.binarySearch(open, seq.charAt(i));
				if (p>=0) {
					within = p;
					depth = 1;
					masked.putQuick(i, true);
				}
			}
			else {
				masked.putQuick(i, true);
				if (seq.charAt(i)==close[within]){
					if (--depth==0)
						within = -1;
				} else if (seq.charAt(i)==open[within]){
					depth++;
				}
			}
		}
		
		if (within!=-1) 
			throw new IllegalStateException("No closing "+String.valueOf(close[within])+" for "+String.valueOf(open[within])+"!");
		
		return new MaskedCharSequence(seq, masked, mask);
		
	}
	
	
	public static MaskedCharSequence maskRightToLeft(CharSequence seq, char mask, char[] open, char[] close) {
		int within = -1;
		int depth = 0;
		
		ArrayUtils.parallelSort(close, open);
		
		int l = seq.length();
		BitVector masked = new BitVector(l);
		
		for (int i=l-1; i>=0; i--) {
			if (within==-1) {
				int p = Arrays.binarySearch(close, seq.charAt(i));
				if (p>=0) {
					within = p;
					depth = 1;
					masked.putQuick(i, true);
				}
			}
			else {
				masked.putQuick(i, true);
				if (seq.charAt(i)==open[within]){
					if (--depth==0)
						within = -1;
				} else if (seq.charAt(i)==close[within]){
					depth++;
				}
			}
		}
		
		if (within!=-1) throw new IllegalStateException("No closing "+String.valueOf(close[within])+" for "+String.valueOf(open[within])+"!");
		
		return new MaskedCharSequence(seq, masked, mask);
		
	}

	public static boolean canMaskRightToLeft(CharSequence seq, char[] open, char[] close) {
		int within = -1;
		int depth = 0;
		
		ArrayUtils.parallelSort(close, open);
		
		int l = seq.length();
		
		for (int i=l-1; i>=0; i--) {
			if (within==-1) {
				int p = Arrays.binarySearch(close, seq.charAt(i));
				if (p>=0) {
					within = p;
					depth = 1;
				}
			}
			else {
				if (seq.charAt(i)==open[within]){
					if (--depth==0)
						within = -1;
				} else if (seq.charAt(i)==close[within]){
					depth++;
				}
			}
		}
		return within==-1;
	}
}
