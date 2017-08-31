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

import java.util.Arrays;
import java.util.Iterator;

import cern.colt.bitvector.BitVector;
import gedi.util.SequenceUtils;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;

public class Alphabet {
	
	private BitVector contains = new BitVector(256);
	private char[] chars;
	
	public Alphabet(char... chars) {
		for (char c : chars)
			contains.putQuick(c, true);
		this.chars = chars;
	}

	
	public ExtendedIterator<char[]> iterateWords(int length) {
		return new WordIterator(length);
	}
	
	public ExtendedIterator<char[]> iterateWords(char[] buf) {
		return new WordIterator(buf);
	}
	
	public final boolean isValid(char c) {
		return contains.getQuick(c);
	}
	
	public char[] getChars() {
		return chars;
	}
	
	public boolean isValid(CharSequence s) {
		for (int i=0; i<s.length(); i++)
			if (!isValid(s.charAt(i)))
				return false;
		return true;
	}
	
	public int getInvalidIndex(CharSequence s) {
		for (int i=0; i<s.length(); i++)
			if (!isValid(s.charAt(i)))
				return i;
		return -1;
	}
	
	public String notValidPart(CharSequence s) {
		for (int i=0; i<s.length(); i++)
			if (!isValid(s.charAt(i)))
				return s.charAt(i)+" @ "+i;
		return null;
	}

	/**
	 * Can be accessed like index['A']. Invalid chars return -1, the others an index in ascii order.
	 * Reverse is toCharArray
	 * @return
	 */
	public int[] createIndex() {
		int[] re = new int[256];
		int index = 0;
		for (int i=0; i<re.length; i++)
			re[i] = contains.getQuick(i)?index++:-1;
		return re;
	}
	
	
	public char getNonAlphabetCharacter() {
		for (int i='!'; i<contains.size(); i++)
			if (!contains.getQuick(i))
				return (char) i;
		throw new RuntimeException("Not enough characters left!");
	}
	
	public char[] getNonAlphabetCharacters(int num) {
		char[] re = new char[num];
		int index=0;
		for (int i='!'; index<num && i<contains.size(); i++)
			if (!contains.getQuick(i))
				re[index++] = (char) i;
		if (index<num)
			throw new RuntimeException("Not enough characters left!");
		return re;
	}
	
	public char[] getNonAlphabetCharactersEx(int num) {
		char[] re = new char[num];
		int index=0;
		for (int i=256; index<num; i++)
			re[index++] = (char) i;
		return re;
	}
	
	public char[] toCharArray() {
		char[] re = new char[contains.cardinality()];
		int index = 0;
		for (int i=0; i<contains.size(); i++) 
			if (contains.getQuick(i))
				re[index++] = (char)i;
		return re;
	}
	
	public Character[] toCharacterArray() {
		Character[] re = new Character[contains.cardinality()];
		int index = 0;
		for (int i=0; i<contains.size(); i++) 
			if (contains.getQuick(i))
				re[index++] = (char)i;
		return re;
	}
	
	public int size() {
		return contains.cardinality();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i=0; i<contains.size(); i++) 
			if (contains.getQuick(i)){
				sb.append((char)i);
				sb.append(",");
			}
		if (sb.length()>1)
			sb.deleteCharAt(sb.length()-1);
		sb.append("]");
		return sb.toString();
	}
	
	private class WordIterator implements ExtendedIterator<char[]> {
		int length;
		char[] buf;
		char[] valid;
		char[] next;
		boolean init = false;
		
		public WordIterator(int length) {
			this.length = length;
			valid = toCharArray();
			next = new char[256];
			for (int i=0; i<valid.length-1; i++)
				next[valid[i]] = valid[i+1];
		}
		
		public WordIterator(char[] buf) {
			this.length = buf.length;
			valid = toCharArray();
			next = new char[256];
			for (int i=0; i<valid.length-1; i++)
				next[valid[i]] = valid[i+1];
			this.buf = buf;
		}

		@Override
		public boolean hasNext() {
			if (buf==null)
				return true;
			char last = valid[valid.length-1];
			for (int i=0; i<length; i++)
				if (buf[i]!=last)
					return true;
			return false;
		}

		@Override
		public char[] next() {
			increment();
			return buf;
		}
		
		private void increment() {
			if (!init) {
				init = true;
				if (buf==null)
					buf = new char[length];
				Arrays.fill(buf, valid[0]);
				return;
			}
			for (int i=length-1; i>=0; i--) {
				buf[i] = next[buf[i]];
				if (buf[i]==0) 
					buf[i] = valid[0];
				else 
					return;
			}
		}

		@Override
		public void remove() {}
		
	}
	
	
	private static Alphabet rna;
	public static Alphabet getRna() {
		if (rna==null)
			rna = new Alphabet("ACGU".toCharArray());
		return rna;
	}
	private static Alphabet dna;
	public static Alphabet getDna() {
		if (dna==null)
			dna = new Alphabet("ACGT".toCharArray());
		return dna;
	}
	
	private static Alphabet amino;
	public static Alphabet getProtein() {
		if (amino==null)
			amino = new Alphabet(EI.wrap(SequenceUtils.code.values()).unique(false).concat("").toCharArray());
		return amino;
	}
	
	public static Alphabet buildAlphabet(CharSequence sequence) {
		Alphabet re = new Alphabet();
		for (int i=0; i<sequence.length(); i++)
			re.contains.putQuick(sequence.charAt(i), true);
		return re;
	}


	public Alphabet extend(char... chars) {
		Alphabet re = new Alphabet();
		re.contains = (BitVector) contains.clone();
		for (char c : chars)
			re.contains.putQuick(c, true);
		return re;
	}


	
}
