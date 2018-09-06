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
package gedi.util;

import gedi.core.reference.Chromosome;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.datastructure.charsequence.MaskedCharSequence;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.math.stat.RandomNumbers;
import gedi.util.sequence.Alphabet;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptException;

import cern.colt.bitvector.BitVector;

public class StringUtils {

	public static String concat(String glue, Object[] array, int start, int end) {
		return concat(glue, array, start, end, o->o.toString());
	}
	
	/**
	 * Concatenates an array using {@link Object#toString()} for each item
	 * and glue as separator.
	 * 
	 * @param glue the string that is inserted between each two items
	 * @param array the array
	 * @return concatenation of the array
	 */
	public static String concat(String glue, Object[] array) {
		return concat(glue, array, o->o.toString());
	}
	public static String concat(char glue, Object[] array) {
		return concat(String.valueOf(glue), array, o->o.toString());
	}
	
	public static <T> String concat(String glue, Iterable<T> col) {
		return concat(glue, col, o->String.valueOf(o));
	}
	
	public static String concat(String glue, int[] array) {
		return concat(glue, ArrayUtils.asList(array).toArray(), o->o.toString());
	}
	
	public static String concat(String glue, char[] array) {
		return concat(glue, ArrayUtils.asList(array).toArray(), o->o.toString());
	}
	
	public static String concat(String glue, long[] array) {
		return concat(glue, ArrayUtils.asList(array).toArray(),o->o.toString());
	}
	
	public static String concat(String glue, double[] array) {
		return concat(glue, ArrayUtils.asList(array).toArray(),o->o.toString());
	}
	
	public static String concat(String glue, double[] array, String format) {
		if (array.length==0) return "";
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<array.length-1; i++) {
			sb.append(String.format(Locale.US,format,array[i]));
			sb.append(glue);
		}
		sb.append(String.format(Locale.US,format,array[array.length-1]));
		return sb.toString();
	}
	
	public static String concat(String glue, float[] array) {
		return concat(glue, ArrayUtils.asList(array).toArray(), o->o.toString());
	}
	
	/**
	 * Concatenates an array using {@link Object#toString()} for each item
	 * and glue as separator.
	 * 
	 * @param glue the string that is inserted between each two items
	 * @param array the array
	 * @return concatenation of the array
	 */
	public static <T> String concat(String glue, T[] array, Function<? super T,String> transformer) {
		return concat(glue, array, 0, array.length, transformer);
	}
	
	public static <T> String concat(String glue, T[] array, int start, int end, Function<? super T,String> transformer) {
		start = Math.max(0, start);
		end = Math.min(array.length,end);
		
		if (end-start<=0) return "";
		StringBuffer sb = new StringBuffer();
		for (int i=start; i<end-1; i++) {
			sb.append(transformer.apply(array[i]));
			sb.append(glue);
		}
		sb.append(transformer.apply(array[end-1]));
		return sb.toString();
	}
	
	public static <T> String concat(String glue, Iterable<T> col, Function<? super T,String> transformer) {
		StringBuffer sb = new StringBuffer();
		if (col!=null)
			for (T e : col) {
				if (sb.length()>0) sb.append(glue);
				sb.append(transformer.apply(e));
			}
		return sb.toString();
	}
	
	
	/**
     * <p>Checks whether the String a valid Java number.</p>
     *
     * <p>Valid numbers include hexadecimal marked with the <code>0x</code>
     * qualifier, scientific notation and numbers marked with a type
     * qualifier (e.g. 123L).</p>
     *
     * <p><code>Null</code> and empty String will return
     * <code>false</code>.</p>
     *
     * @param str  the <code>String</code> to check
     * @return <code>true</code> if the string is a correctly formatted number
     */
    public static boolean isNumeric(String str) {
        if (str==null || str.length()==0) {
            return false;
        }
        char[] chars = str.toCharArray();
        int sz = chars.length;
        boolean hasExp = false;
        boolean hasDecPoint = false;
        boolean allowSigns = false;
        boolean foundDigit = false;
        // deal with any possible sign up front
        int start = (chars[0] == '-') ? 1 : 0;
        if (sz > start + 1) {
            if (chars[start] == '0' && chars[start + 1] == 'x') {
                int i = start + 2;
                if (i == sz) {
                    return false; // str == "0x"
                }
                // checking hex (it can't be anything else)
                for (; i < chars.length; i++) {
                    if ((chars[i] < '0' || chars[i] > '9')
                        && (chars[i] < 'a' || chars[i] > 'f')
                        && (chars[i] < 'A' || chars[i] > 'F')) {
                        return false;
                    }
                }
                return true;
            }
        }
        sz--; // don't want to loop to the last char, check it afterwords
              // for type qualifiers
        int i = start;
        // loop to the next to last char or to the last char if we need another digit to
        // make a valid number (e.g. chars[0..5] = "1234E")
        while (i < sz || (i < sz + 1 && allowSigns && !foundDigit)) {
            if (chars[i] >= '0' && chars[i] <= '9') {
                foundDigit = true;
                allowSigns = false;

            } else if (chars[i] == '.') {
                if (hasDecPoint || hasExp) {
                    // two decimal points or dec in exponent   
                    return false;
                }
                hasDecPoint = true;
            } else if (chars[i] == 'e' || chars[i] == 'E') {
                // we've already taken care of hex.
                if (hasExp) {
                    // two E's
                    return false;
                }
                if (!foundDigit) {
                    return false;
                }
                hasExp = true;
                allowSigns = true;
            } else if (chars[i] == '+' || chars[i] == '-') {
                if (!allowSigns) {
                    return false;
                }
                allowSigns = false;
                foundDigit = false; // we need a digit after the E
            } else {
                return false;
            }
            i++;
        }
        if (i < chars.length) {
            if (chars[i] >= '0' && chars[i] <= '9') {
                // no type qualifier, OK
                return true;
            }
            if (chars[i] == 'e' || chars[i] == 'E') {
                // can't have an E at the last byte
                return false;
            }
            if (!allowSigns
                && (chars[i] == 'd'
                    || chars[i] == 'D'
                    || chars[i] == 'f'
                    || chars[i] == 'F')) {
                return foundDigit;
            }
            if (chars[i] == 'l'
                || chars[i] == 'L') {
                // not allowing L with an exponent
                return foundDigit && !hasExp;
            }
            // last character is illegal
            return false;
        }
        // allowSigns is true iff the val ends in 'E'
        // found digit it to make sure weird stuff like '.' and '1E-' doesn't pass
        return !allowSigns && foundDigit;
    }

    /**
	 * Gets, if the given string contains an integer / long
	 * @see Integer#parseInt(String)
	 * @param s the string
	 * 
	 * @return if it is an integer
	 */
	public static boolean isInt(String s) {
		if (s.length()==0) return false;
		int o = s.charAt(0)=='-'?1:0;
		if (s.length()-o==0)
			return false;
		for (int i=o; i<s.length(); i++)
			if (!Character.isDigit(s.charAt(i)))
				return false;
		return true;
//		if (val==null || val.length()==0)
//			return false;
//		for (int i=0; i<val.length(); i++) {
//			char c = val.charAt(i);
//			if ((c<'0' || c>'9')&& (c!='-' || i>0))
//				return false;
//		}
//		return true;
	}
	
	/**
	 * How many leading chars of s can be parsed as int?
	 * @param s
	 * @return
	 */
	public static int countPrefixInt(String s) {
		if (s.length()==0) return 0;
		int o = s.charAt(0)=='-'?1:0;
		if (s.length()-o==0)
			return 0;
		for (int i=o; i<s.length(); i++)
			if (!Character.isDigit(s.charAt(i)))
				return i==o?0:i;
		return s.length();
	}

	public static String repeat(String s, int times) {
		StringBuilder sb = new StringBuilder();
		
		for (int i=0; i<times; i++)
			sb.append(s);
		
		return sb.toString();
	}
	
	public static String repeat(char s, int times) {
		StringBuilder sb = new StringBuilder();
		
		for (int i=0; i<times; i++)
			sb.append(s);
		
		return sb.toString();
	}

	/**
	 * Returns a string of given length, containing s repeatedly.
	 * @param s
	 * @param length
	 * @return
	 */
	public static String repeatCircular(String s, int length, int offset) {
		if (length==0)
			return "";
		s = s.substring(offset)+s.substring(0,offset);
		
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<length/s.length(); i++)
			sb.append(s);
		sb.append(s.substring(0,length%s.length()));
		return sb.toString();
	}
	
	public static int[] getCharHistogram(String s) {
		int[] re = new int[256];
		for (int i=0; i<s.length(); i++)
			if (s.charAt(i)<256)
				re[s.charAt(i)]++;
		return re;
	}

	public static String[] split(CharSequence s, int chunk) {
		String[] re = new String[(int)Math.ceil(s.length()/(double)chunk)];
		for (int i=0; i<re.length; i++)
			re[i] = s.subSequence(i*chunk, Math.min((i+1)*chunk,s.length())).toString();
		return re;
	}
	
	public static String[] split(String s, char separator) {
		if (s.length()==0)
			return new String[0];
		
		ArrayList<String> re = new ArrayList<String>();
		int p = 0;
		for (int sepIndex=s.indexOf(separator); sepIndex>=0; sepIndex = s.substring(p).indexOf(separator)) {
			re.add(s.substring(p,p+sepIndex));
			p += sepIndex+1;
		}
		re.add(s.substring(p));
		return re.toArray(new String[re.size()]);
	}
	
	
	public static String splitField(String s, char separator, int field) {
		if (s.length()==0)
			return null;
		
		int p = 0;
		int index = 0;
		for (int sepIndex=s.indexOf(separator); sepIndex>=0; sepIndex = s.substring(p).indexOf(separator)) {
			if (index++==field) return s.substring(p,p+sepIndex);
			p += sepIndex+1;
		}
		if (index++==field) return s.substring(p);
		return null;
	}
	
	public static String splitField(String s, String separator, int field) {
		if (s.length()==0)
			return null;
		
		int p = 0;
		int index = 0;
		for (int sepIndex=s.indexOf(separator); sepIndex>=0; sepIndex = s.substring(p).indexOf(separator)) {
			if (index++==field) return s.substring(p,p+sepIndex);
			p += sepIndex+separator.length();
		}
		
		if (index++==field) return s.substring(p);
		return null;
	}
	
	/**
	 * Removes all occurrences of c in s.
	 * @param s
	 * @param c
	 * @return
	 */
	public static String remove(String s, char c) {
		if (s.length()==0)
			return "";
		
		StringBuilder re = new StringBuilder();
		int p = 0;
		for (int sepIndex=s.indexOf(c); sepIndex>=0; sepIndex = s.substring(p).indexOf(c)) {
			re.append(s.substring(p,p+sepIndex));
			p += sepIndex+1;
		}
		re.append(s.substring(p));
		return re.toString();
	}
	
	/**
	 * If s starts with separator, the first entry of the returned array will be "" (last likewise!)
	 * @param s
	 * @param separator
	 * @return
	 */
	public static String[] split(String s, String separator) {
		if (s.length()==0)
			return new String[0];
		
		ArrayList<String> re = new ArrayList<String>();
		int p = 0;
		for (int sepIndex=s.indexOf(separator); sepIndex>=0; sepIndex = s.substring(p).indexOf(separator)) {
			re.add(s.substring(p,p+sepIndex));
			p += sepIndex+separator.length();
		}
		re.add(s.substring(p));
		return re.toArray(new String[re.size()]);
	}
	
	
	
	/**
	 * Similar to {@link #split(String, char)}, but reads only the first n=re.length fields. The returned array is
	 * re. If there are less than n fields in s, the remaining entries are then set to null.
	 * @param s
	 * @param separator
	 * @param re
	 * @return
	 */
	public static String[] split(String s, char separator, String[] re) {
		if (re==null) return split(s,separator);
		int index =0;
		int p = 0;
		for (int sepIndex=s.indexOf(separator); index<re.length & sepIndex>=0; sepIndex = s.substring(p).indexOf(separator)) {
			re[index++]=s.substring(p,p+sepIndex);
			p += sepIndex+1;
		}
		if (index<re.length)
			re[index++]=s.substring(p);
		for (; index<re.length; index++)
			re[index] = null;
		return re;
	}
	
	/**
	 * If s starts with separator, the first entry of the returned array will be "" (last likewise!)
	 * @param s
	 * @param separator
	 * @return
	 */
	public static String[] split(String s, String separator, String[] re) {
		if (re==null) return split(s,separator);
		int index =0;
		int p = 0;
		for (int sepIndex=s.indexOf(separator); index<re.length & sepIndex>=0; sepIndex = s.substring(p).indexOf(separator)) {
			re[index++]=s.substring(p,p+sepIndex);
			p += sepIndex+separator.length();
		}
		if (index<re.length)
			re[index++]=s.substring(p);
		for (; index<re.length; index++)
			re[index] = null;
		return re;
	}
	
	public static Object[] split(String s, char separator, Object[] re) {
		int index =0;
		int p = 0;
		for (int sepIndex=s.indexOf(separator); index<re.length & sepIndex>=0; sepIndex = s.substring(p).indexOf(separator)) {
			re[index++]=s.substring(p,p+sepIndex);
			p += sepIndex+1;
		}
		if (index<re.length)
			re[index++]=s.substring(p);
		for (; index<re.length; index++)
			re[index] = null;
		return re;
	}
	


	public static CharSequence reverse(CharSequence s) {
		return new ReversedCharSequence(s);
	}

	public static class ReversedCharSequence implements CharSequence {

		private CharSequence base;
		private int l;
		public ReversedCharSequence(CharSequence base) {
			this.base = base;
			this.l = base.length();
		}

		@Override
		public char charAt(int index) {
			return base.charAt(l-index-1);
		}

		@Override
		public int length() {
			return l;
		}

		@Override
		public CharSequence subSequence(int start, int end) {
			return new ReversedCharSequence(base.subSequence(l-end, l-start));
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(this);
			return new String(sb);
		}
		
	}

	
	public static String createRandomIdentifier(int length) {
		return createRandomIdentifier(length, new RandomNumbers());
	}
	
	
	public static String createRandomIdentifier(int length, RandomNumbers rnd) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<length; i++) {
			int c = rnd.getUnif(0, 2*('Z'-'A'+1))+'A';
			if (c>'Z')
				c+='a'-'Z'-1;
			sb.append((char)c);
		}
		return sb.toString();
	}

	public static int countChar(CharSequence s, char c) {
		int re = 0;
		for (int i=0; i<s.length(); i++)
			if (s.charAt(i)==c)
				re++;
		return re;
	}

	public static int indexOf(String s, char c, int nr) {
		int i;
		for (i=0; i>=0 && nr>0; i = s.indexOf(c,i)+1, nr--);
		return i-1;
	}
	
	public static String[] trimAll(String[] a, char...chars) {
		for (int i=0; i<a.length; i++)
			a[i] = trim(a[i],chars);
		return a;
	}

	public static String trim(String s) {
		if (s==null) return null;
		return trim(s,' ','\t');
	}
	
	public static String trim(String s, char...chars) {
		char[] c = s.toCharArray();
		BitVector mask = getCharMask(chars);
		int len = c.length;
		int st = 0;

		while ((st < len) && (mask.getQuick(c[st]))) 
		    st++;
		while ((st < len) && (mask.getQuick(c[len - 1]))) 
		    len--;
		return s.substring(st, len);
	}
	
	public static BitVector getCharMask(char...chars) {
		BitVector re = new BitVector(256);
		for (char c : chars)
			re.set(c);
		return re;
	}

	public static String escape(String s, char...chars) {
		
		BitVector cont = new BitVector(256);
		for (char c : chars)
			cont.putQuick(c, true);
		
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<s.length(); i++) {
			if (cont.getQuick(s.charAt(i))) 
				sb.append("\\");
			sb.append(s.charAt(i));
		}
		return sb.toString();
	}
	
	public static String escape(String s) {
		return escape(s,'\'','"','\\');
	}	
	
	public static String unescape(String s) {
		char[] map = new char[256];
		map['t'] = '\t';
		map['b'] = '\b';
		map['n'] = '\n';
		map['f'] = '\f';
		map['r'] = '\r';
		map['0'] = '\0';
		map['"'] = '"';
		map['\''] = '\'';
		map['\\'] = '\\';
		
		char[] c = s.toCharArray();
		StringBuilder re = new StringBuilder(c.length);
		for (int i=0; i<c.length; i++) {
			if (i+1<c.length && c[i]=='\\') 
				re.append(map[c[++i]]==0 ? c[i] : map[c[i]]);
			else
				re.append(c[i]);
		}
		return re.toString();
	}
	
	public static String unescape(char s) {
		return unescape(new String(new char[] {s}));
	}

	
	public static long getMemory(String memory) {
		switch (memory.charAt(memory.length()-1)) {
		case 'k':case 'K':
			return Long.parseLong(memory.substring(0,memory.length()-1))*1000L;
		case 'm':case 'M':
			return Long.parseLong(memory.substring(0,memory.length()-1))*1000000L;
		case 'g':case 'G':
			return Long.parseLong(memory.substring(0,memory.length()-1))*1000000000L;
		default:
			return Long.parseLong(memory.substring(0,memory.length()-1));
		}
	}
	
	public static String getShortHumanReadableMemory(long memory) {
		String[] s = {"B","KB","MB","GB","TB"};
		String[] re = new String[s.length];
		Arrays.fill(re, "");
		long m = (long)Math.pow(2,10);
		
		String unit = "";
		int i;
		for (i=0; i<s.length && memory>0; i++) {
			if(i==s.length-1)
				m=1;
			re[i] = memory%m+"";
			memory/=m;
			unit = s[i];
		}
		if (i==0)
			return "0B";
		if (i==1)
			return re[i-1]+unit;
		return re[i-1]+"."+re[i-2]+unit;
	}
	
	public static String getHumanReadableMemory(long memory) {
		String[] s = {"B","KB","MB","GB","TB"};
		String[] re = new String[s.length];
		Arrays.fill(re, "");
		long m = (long)Math.pow(2,10);
		
		for (int i=0; i<s.length && memory>0; i++) {
			if(i==s.length-1)
				m=1;
			re[i] = memory%m+s[i];
			memory/=m;
		}
		ArrayUtils.reverse(re);
		return ArrayUtils.concat(" ", re).trim();
	}
	
	public static String getHumanReadableTimespan(long timespan) {
		String[] s = {"ms","s","m","h","d"};
		String[] re = new String[s.length];
		Arrays.fill(re, "");
		long[] m = {1000,60,60,24,Long.MAX_VALUE};
		
		for (int i=0; i<s.length && timespan>0; i++) {
			re[i] = timespan%m[i]+s[i];
			timespan/=m[i];
		}
		ArrayUtils.reverse(re);
		return ArrayUtils.concat(" ", re).trim();
	}

	
	public static String getHumanReadableTimespanNano(long timespan) {
		String[] s = {"ns","us","ms","s","m","h","d"};
		String[] re = new String[s.length];
		Arrays.fill(re, "");
		long[] m = {1000,1000,1000,60,60,24,Long.MAX_VALUE};
		
		for (int i=0; i<s.length && timespan>0; i++) {
			re[i] = timespan%m[i]+s[i];
			timespan/=m[i];
		}
		ArrayUtils.reverse(re);
		return ArrayUtils.concat(" ", re).trim();
	}

	public static CharSequence concatSequences(CharSequence... sequences) {
		return new ConcatenatedCharSequence(sequences);
	}

	public static class PrefixCharSequence implements CharSequence {

		private CharSequence base;
		private int prefix;

		public PrefixCharSequence(CharSequence base, int prefix) {
			this.base = base;
			this.prefix = prefix;
		}

		public void increment() {
			if (prefix<base.length()-1)
				prefix++;
		}
		
		@Override
		public char charAt(int index) {
			return base.charAt(index);
		}

		@Override
		public int length() {
			return prefix;
		}

		@Override
		public CharSequence subSequence(int start, int end) {
			return base.subSequence(start, end);
		}
		
		@Override
		public String toString() {
			return base.subSequence(0, prefix).toString();
		}
		
	}
	
	public static class ConcatenatedCharSequence implements CharSequence {
		
		private CharSequence[] sequences;
		private int[] cumLengths;
		
		private int absIndex;
		private int seqIndex;
		private int relIndex;
		
		public ConcatenatedCharSequence(CharSequence[] sequences) {
			this.sequences = sequences;
			cumLengths = new int[sequences.length+1];
			for (int i=1; i<cumLengths.length; i++)
				cumLengths[i] = cumLengths[i-1]+sequences[i-1].length();
		}

		@Override
		public char charAt(int index) {
			determineIndices(index);
			return sequences[this.seqIndex].charAt(this.relIndex);
		}

		private void determineIndices(int index) {
			int s;
			if (index>=absIndex && index<cumLengths[seqIndex+1]) { // linear search from recent one
				s=seqIndex;
			} else if (index<absIndex && index>=cumLengths[seqIndex]) { // linear search from recent one
				s=seqIndex;
			} else {
				s= Arrays.binarySearch(cumLengths, index);
				if (s<0) 
					s = -s-2;
			}
			
			this.absIndex = index;
			this.relIndex = index-cumLengths[s];
			this.seqIndex = s;
		}

		@Override
		public int length() {
			return cumLengths[cumLengths.length-1];
		}

		@Override
		public CharSequence subSequence(int start, int end) {
			if (end<=start)
				return "";
			determineIndices(start);
			int startSeq = seqIndex;
			int startIndex = relIndex;
			determineIndices(end-1);
			int endSeq = seqIndex;
			int endIndex = relIndex;
			
			if (endSeq!=startSeq) {
				CharSequence[] s = new CharSequence[endSeq-startSeq+1];
				s[0] = sequences[startSeq].subSequence(startIndex, sequences[startSeq].length());
				if (endSeq-startSeq-1>0)
					System.arraycopy(sequences, startSeq+1, s, 1, endSeq-startSeq-1);
				s[s.length-1] = sequences[endSeq].subSequence(0, endIndex+1);
				return new ConcatenatedCharSequence(s);
			}
			
			return sequences[startSeq].subSequence(startIndex, endIndex+1);
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (CharSequence s : sequences)
				sb.append(s.toString());
			return sb.toString();
		}
		
	}

	public static String padLeft(String s, int l, char c) {
		if (s.length()>=l)
			return s;
		else
			return repeat(String.valueOf(c), l-s.length())+s;
	}
	
	public static String padRight(String s, int l, char c) {
		if (s.length()>=l)
			return s;
		else
			return s+repeat(String.valueOf(c), l-s.length());
	}
	
	public static String padLeft(String s, int l) {
		if (s.length()>=l)
			return s;
		else
			return repeat(" ", l-s.length())+s;
	}
	
	public static String padRight(String s, int l) {
		if (s.length()>=l)
			return s;
		else
			return s+repeat(" ", l-s.length());
	}

	public static int trailingNumber(String s) {
		int index = s.length()-1;
		for (; index>=0 && Character.isDigit(s.charAt(index)); index--);
		if (index==s.length()-1) return -1;
		return Integer.parseInt(s.substring(index+1));
	}

	public static int leadingNumber(String s) {
		int index = 0;
		for (; index<s.length() && Character.isDigit(s.charAt(index)); index++);
		if (index==0) return -1;
		return Integer.parseInt(s.substring(0,index));
	}

	public static double[] parseDouble(String[] a) {
		return parseDouble(a,0,a.length);
	}
	public static double[] parseDouble(String[] a, int start, int end) {
		double[] re = new double[end-start];
		for (int i=start; i<end; i++)
			re[i-start] = Double.parseDouble(a[i]);
		return re;
	}
	
	public static int[] parseInt(String[] a) {
		int[] re = new int[a.length];
		for (int i=0; i<a.length; i++)
			re[i] = Integer.parseInt(a[i]);
		return re;
	}
	
	



	public static String replace(String s, int start, int end,
			String r) {
		return s.substring(0,start)+r+s.substring(end);
	}

	public static Comparator<String> longestFirstComparator() {
		return new LongestFirstComparator();
	}
	
	public static class LongestFirstComparator implements Comparator<String> {

		@Override
		public int compare(String o1, String o2) {
			int re = o2.length()-o1.length();
			if (re==0)
				re = o1.compareTo(o2);
			return re;
		}
		
	}

	public static ExtendedIterator<String> iteratePermutations(String s) {
		return new PermutationIterator(s);
	}

	
	/**
	 * Lexicographic permutations, based on Knuth 4
	 * @author erhard
	 *
	 */
	public static class PermutationIterator implements ExtendedIterator<String> {

		private int n;
		private	char[] a;
		private boolean hasnext = true;
		
		public PermutationIterator(String s) {
			n = s.length();
			a = new char[n+1];
			System.arraycopy(s.toCharArray(), 0, a, 1, n);
			Arrays.sort(a);
		}
		
		@Override
		public boolean hasNext() {
			return hasnext;
		}

		@Override
		public String next() {
			String re = new String(a,1,n);
			int j,l;
			for (j=n-1;a[j]>=a[j+1]; j--);
			if (j==0)
				hasnext=false;
			else {
				for (l=n;a[j]>=a[l]; l--);
				ArrayUtils.swap(a, j, l);
				ArrayUtils.reverse(a, j+1, n+1);
			}
			
			return re;
		}

		@Override
		public void remove() {}
		
	}
	
	/**
	 * Iterates over all hamming distance less or equal than 1 to s.
	 * @param s
	 * @param alpha
	 * @return
	 */
	public static ExtendedIterator<String> iterateHammingHull(String s, Alphabet alpha) {
		return new HammingIterator(s,alpha);
	}

	
	/**
	 * Lexicographic permutations, based on Knuth 4
	 * @author erhard
	 *
	 */
	public static class HammingIterator implements ExtendedIterator<String> {

		private int pos = 0;
		private int sPos = 0;
		private char[] sigma;
		private String s;
		private	char[] a;
		
		public HammingIterator(String s, Alphabet alpha) {
			this.s = s;
			this.a = s.toCharArray();
			sigma = alpha.toCharArray();
		}
		
		@Override
		public boolean hasNext() {
			return pos<s.length() && sPos<sigma.length;
		}

		@Override
		public String next() {
			a[pos] = sigma[sPos];
			String re = String.valueOf(a); 
			increment();
			// skip s itself for pos>0, i.e. it is returned in the first block, but not afterwards
			if (pos>0  && hasNext() && a[pos]==sigma[sPos]) {
				increment();
			}
			
			return re;
		}

		private void increment() {
			if (++sPos==sigma.length){
				a[pos] = s.charAt(pos);
				sPos = 0;
				pos++;
			}
		}
		
	}

	/**
	 * Returns the id of the unique match of pattern to an entry of a. If pattern cannot be matches, -1 is returned,
	 * if it matches to multiple elements, a.length is returned!
	 * @param a
	 * @param pattern
	 * @return
	 */
	public static int findUnique(String[] a, Pattern pattern) {
		int re = -1;
		for (int i=0; i<a.length; i++)
			if (pattern.matcher(a[i]).find()) {
				if (re!=-1)
					return a.length;
				re = i;
			}
		return re;
	}
	

	/**
	 * Returns the groups of the unique match of pattern to t. If pattern cannot be matched or is matched
	 * multiple times, null is returned.
	 * @param t
	 * @param pattern
	 * @return
	 */
	public static String[] findUnique(String t, Pattern pattern) {
		Matcher re = pattern.matcher(t);
		if (re.find()) {
			String[] r = new String[re.groupCount()];
			for (int i=0; i<r.length; i++)
				r[i] = re.group(i+1);
			if (re.find())
				return null;
			return r;
		}
		return null;
	}

	/**
	 * Returns the groups of the unique match of pattern to t in groups. If pattern cannot be matched zero is returned,
	 * if it is matched multiple times -n is returned and n otherwise, where n is the number of groups.
	 * @param t
	 * @param pattern
	 * @return
	 */
	public static int findUnique(String t, Pattern pattern, String[] groups) {
		Matcher re = pattern.matcher(t);
		if (re.find()) {
			int g = groups.length;
			int n = Math.min(g, re.groupCount());
			for (int i=0; i<n; i++)
				groups[i] = re.group(i+1);
			if (re.find())
				return -g;
			return g;
		}
		return -1;
	}
	
	/**
	 * Creates also reversed (if to<from).
	 * Both indices are inclusive!
	 * @param seq
	 * @param from
	 * @param to
	 * @return
	 */
	public static char[] extract(CharSequence seq, int from, int to) {
		if (to>=from) {
			char[] re = new char[to-from+1];
			for (int i=from; i<=to; i++)
				re[i-from] = seq.charAt(i);
			return re;
		} 
		char[] re = new char[from-to+1];
		for (int i=from; i>=to; i--)
			re[from-i] = seq.charAt(i);
		return re;
		
	}
	
	/**
	 * Creates also reversed (if to<from).
	 * Both indices are inclusive!
	 * @param seq
	 * @param from
	 * @param to
	 * @return
	 */
	public static char[] extract(char[] seq, int from, int to) {
		if (to>=from) {
			char[] re = new char[to-from+1];
			for (int i=from; i<=to; i++)
				re[i-from] = seq[i];
			return re;
		} 
		char[] re = new char[from-to+1];
		for (int i=from; i>=to; i--)
			re[from-i] = seq[i];
		return re;
		
	}

	public static char[] toCharArray(CharSequence chars) {
		if (chars instanceof String)
			return ((String)chars).toCharArray();
		
		char[] re = new char[chars.length()];
		for (int i=0; i<re.length; i++)
			re[i] = chars.charAt(i);
		return re;
	}
	
	public static String toString(Object o, String nullValue) {
		if (o==null) return nullValue;
		try {
			if (o.getClass().getDeclaredMethod("toString")!=null)
				return o.toString();
		} catch (NoSuchMethodException | SecurityException e1) {
		}
		
		if (o.getClass().isArray()) {
			StringBuilder sb = new StringBuilder();
			int l = Array.getLength(o);
			sb.append("[");
			for (int i=0; i<l; i++) {
				if (i>0) sb.append(",");
				sb.append(toString(Array.get(o, i),nullValue));
			}
			sb.append("]");
			return sb.toString();
		}
		if (o instanceof Iterable) {
			StringBuilder sb = new StringBuilder();
			sb.append("[");
			for (Object e : ((Iterable)o)) {
				if (sb.length()>1) sb.append(",");
				sb.append(toString(e,nullValue));
			}
			sb.append("]");
			return sb.toString();
		}
		
		return String.valueOf(o);
	}
	
	public static String toString(Object o) {
		return toString(o,"null");
	}
	
	public static String toString(CharSequence chars) {
		if (chars instanceof String)
			return (String)chars;
		
		StringBuilder sb = new StringBuilder(chars.length());
		for (int i=0; i<chars.length(); i++)
			sb.append(chars.charAt(i));
		return sb.toString();
	}
	
	public static String toString(CharSequence chars, int start, int end) {
		if (chars instanceof String)
			return ((String)chars).substring(start, end);
		
		StringBuilder sb = new StringBuilder(end-start);
		for (int i=start; i<end; i++)
			sb.append(chars.charAt(i));
		return sb.toString();
	}

	/**
	 * Gets a string that can be used as a scale for ascii output. Every step steps |index is printed.
	 * @param i
	 * @return
	 */
	public static String getScale(int step, int length) {
		return getScale(step,0,length-1);
	}
	
	public static String getScale(int step, int from, int to) {
		step = Math.max(step, Math.max((""+from).length(),(""+to).length())+1);
		StringBuilder sb = new StringBuilder();
		if (from<=to)
			for (int i=from; i<=to; i+=step) {
				String s = String.valueOf(i);
				if (sb.length()+s.length()+1>to-from+1) {
					for (;i<=to; i++)
						sb.append(" ");
				} else {
					sb.append("|");
					sb.append(s);
					for (int r=0; r<step-(s.length()+1); r++)
						sb.append(" ");
				}
			}
		else
			for (int i=from; i>=to; i-=step) {
				String s = String.valueOf(i);
				if (sb.length()+s.length()+1>from-to+1) {
					for (;i>=to; i--)
						sb.append(" ");
				} else {
					sb.append("|");
					sb.append(s);
					for (int r=0; r<step-(s.length()+1); r++)
						sb.append(" ");
				}
			}
		return sb.toString();
	}

	/**
	 * Returns a string of given length, that contains s, if possible and removes inner chars from s, if impossible.
	 * Alignment in [-1:1] specifies, where the string is supposed to be (-1f = left, 0f = centered, 1f = right)
	 * @param s
	 * @param length
	 * @return
	 */
	public static String getMatchingString(String s, int length, float alignment) {
		return getMatchingString(s,' ',length,alignment);
	}
	
	/**
	 * Returns a string of given length, that contains s, if possible and removes inner chars from s, if impossible.
	 * Alignment in [-1:1] specifies, where the string is supposed to be (-1f = left, 0f = centered, 1f = right)
	 * @param s
	 * @param length
	 * @return
	 */
	public static String getMatchingString(String s, char fill, int length, float alignment) {
		if (s.length()>length) 
			return s.substring(0,(int)Math.ceil(length/2.0-1))+".."+s.substring(s.length()-(int)Math.floor(length/2.0-1));
		alignment = alignment/2f+.5f;
		length -= s.length();
		return repeat(fill+"",(int)Math.ceil(length*alignment))+s+repeat(fill+"",(int)Math.floor(length*(1-alignment)));
	}

	/**
	 * Converts a string to camelcase
	 * @param s the string
	 * @param firstToUpper if the first letter should be capital
	 * @return camelcase
	 */
	public static String getCamelCase(String s, boolean firstToUpper) {
		StringBuffer sb = new StringBuffer(s.length());
		for (int i=0; i<s.length(); i++)
			if (!Character.isLetter(s.charAt(i)) && !Character.isDigit(s.charAt(i)))
				sb.append(Character.toUpperCase(s.charAt(++i)));
			else
				sb.append(s.charAt(i));
		if (firstToUpper)
			sb.setCharAt(0, Character.toUpperCase(s.charAt(0)));
		else
			sb.setCharAt(0, Character.toLowerCase(s.charAt(0)));
		
		return sb.toString();
	}
	
	/**
	 * Converts a string from camelcase
	 * @param s the string
	 * @param glue the string to insert between words
	 * @param firstToUpper if the first letter should be capital
	 * @return un camelcase
	 */
	public static String getUnCamelCase(String s, String glue, boolean firstToUpper) {
		StringBuffer sb = new StringBuffer(s.length());
		if (firstToUpper)
			sb.append(Character.toUpperCase(s.charAt(0)));
		else 
			sb.append(s.charAt(0));
		
		for (int i=1; i<s.length(); i++) {
			if (Character.isUpperCase(s.charAt(i)))
				sb.append(glue);
			if (s.charAt(i)=='_')
				sb.append(glue);
			else
				sb.append(s.charAt(i));
		}
		
		return sb.toString();
	}

	public static String removeFooter(String s, String footer) {
		if (s.endsWith(footer))
			return s.substring(0,s.length()-footer.length());
		return s;
	}
	
	public static String removeHeader(String s, String header) {
		if (s.startsWith(header))
			return s.substring(header.length());
		return s;
	}

	public static int hamming(CharSequence s1, CharSequence s2) {
		if (s1.length()!=s2.length()) return Math.max(s1.length(), s2.length());
		int re = 0;
		for (int i=0; i<s1.length(); i++)
			if (s1.charAt(i)!=s2.charAt(i)) re++;
		return re;
	}

	public static CharSequence repeatSequence(char c, int length) {
		return new RepeatCharSequence(c,length);
	}


	public static class RepeatCharSequence implements CharSequence {

		private char c;
		private int length;
		
		public RepeatCharSequence(char c, int length) {
			this.c = c;
			this.length = length;
		}

		@Override
		public int length() {
			return length;
		}

		@Override
		public char charAt(int index) {
			return c;
		}

		@Override
		public CharSequence subSequence(int start, int end) {
			return new RepeatCharSequence(c, end-start);
		}
		
		@Override
		public int hashCode() {
			return c+(length<<13);
		}
		
		public boolean equals(Object obj) {
			if (!(obj instanceof RepeatCharSequence)) return false;
			RepeatCharSequence s = (RepeatCharSequence) obj;
			return s.c==c && s.length==length;
		}
		
		@Override
		public String toString() {
			char[] re = new char[length];
			Arrays.fill(re, c);
			return String.valueOf(re);
		}
	}

	public static boolean charsEqual(CharSequence a,CharSequence b) {
		if (a.length()!=b.length())return false;
		int n = a.length();
		for (int i=0; i<n; i++)
			if (a.charAt(i)!=b.charAt(i))
				return false;
		return true;
	}

	public static int getCommonPrefixLength(String a, String b) {
		int re;
		int n = Math.min(a.length(),b.length());
		for (re=0; re<n && a.charAt(re)==b.charAt(re); re++);
		return re;
	}

	public static int hashCode(CharSequence chars) {
		int h=0;
		for (int i = 0; i < chars.length(); i++) {
			h = 31 * h + chars.charAt(i);
		}
		return h;
	}
	
	public static int compare(CharSequence a, CharSequence b) {
		int len1 = a.length();
        int len2 = b.length();
        int lim = Math.min(len1, len2);

        int k = 0;
        while (k < lim) {
            char c1 = a.charAt(k);
            char c2 = b.charAt(k);
            if (c1 != c2) {
                return c1 - c2;
            }
            k++;
        }
        return len1 - len2;
	}

	public static boolean equals(CharSequence chars, Object anObject) {
		if (chars == anObject) {
			return true;
		}
		if (chars==null) return false;
		if (anObject instanceof CharSequence) {
			CharSequence anotherString = (CharSequence) anObject;
			int n = chars.length();
			if (n == anotherString.length()) {
				int i = 0;
				while (n-- != 0) {
					if (chars.charAt(i) != anotherString.charAt(i))
						return false;
					i++;
				}
				return true;
			}
		}
		return false;
	}

	
	public static StringBuilder indent(StringBuilder sb, int indent) {
		return indent(sb,indent,'\t');
	}

	
	public static StringBuilder indent(StringBuilder sb, int indent, char c) {
		for (int i=0; i<indent; i++)
			sb.append(c);
		return sb;
	}

	private static SecureRandom random = new SecureRandom();
	public static String getRandomId() {
		return new BigInteger(130, random).toString(32);
	}

	public static boolean isJavaIdentifier(String s) {
		if (s==null || s.length()==0) return false;
		if (!Character.isJavaIdentifierStart(s.charAt(0))) return false;
		for (int i=1; i<s.length();i++)
			if (!Character.isJavaIdentifierPart(s.charAt(i))) return false;
		return true;
	}

	public static String toJavaIdentifier(String s) {
		if (s==null || s.length()==0) return "V";
		if (!Character.isJavaIdentifierStart(s.charAt(0))) s = "V"+s;
		char[] ca = s.toCharArray();
		for (int i=1; i<ca.length;i++)
			if (!Character.isJavaIdentifierPart(ca[i])) ca[i] = '_';
		return new String(ca);
	}

	public static String[] makeUnique(String[] a) {
		HashSet<String> p = new HashSet<String>();
		for (int i=0; i<a.length; i++) {
			while (!p.add(a[i])) {
				int trnum = trailingNumber(a[i]);
				a[i] = a[i].substring(0, a[i].length()-(trnum+"").length())+(trnum+1);
			}
		}
		return a;
	}

	public static String saveSubstring(String s, int start, int end,
			char c) {
		if (end<0 || start>=s.length())
			return repeat(c, end-start);
		
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<-start; i++)
			sb.append(c);
		sb.append(s.substring(Math.max(0, start), Math.min(end,s.length())));
		for (int i=s.length(); i<end; i++)
			sb.append(c);
		return sb.toString();
	}

	/**
	 * removes all spaces and makes only letters after a space a capital (i.e. the first letter will not be a capital letter) 
	 * @param s
	 * @return
	 */
	public static String toCamelcase(CharSequence s) {
		StringBuilder sb = new StringBuilder();
		boolean capitalize = false;
		for (int i=0; i<s.length(); i++) {
			char c = s.charAt(i);
			if (Character.isWhitespace(c)) {
				capitalize = true;
			}else {
				sb.append(capitalize?Character.toUpperCase(c):Character.toLowerCase(c));
				capitalize = false;
			}
		}
		return sb.toString();
	}

	public static int compare(String a, String b) {
		if (a==null && b==null) return 0;
		if (a==null) return -1;
		if (b==null) return 1;
		return a.compareTo(b);
	}
	
	public static String display(GenomicRegion...regions) {
		return display(EI.wrap(regions));
	}
	
	public static String display(Iterator<GenomicRegion> regions) {
		StringBuilder sb = new StringBuilder();
		int maxl = 0;
		while (regions.hasNext()) {
			GenomicRegion n = regions.next();
			String line = packAscii(new ImmutableReferenceGenomicRegion<>(Chromosome.UNMAPPED, new ArrayGenomicRegion(0,n.getEnd()+1)), EI.singleton(n));
			sb.append(line);
			maxl = Math.max(maxl, line.length());
		}
		return getRuler(0, maxl, 10)+"\n"+sb.toString();
	}

	public static String getRuler(int start, int length, int ticDistance) {
		if (((start+length)+"").length()+2>=ticDistance) throw new RuntimeException("Tic labels do not fit!");
		StringBuilder sb = new StringBuilder();
		
		for (int l=start; l<length; l+=ticDistance) {
			sb.append("|").append(l);
			for (int i=0; i<ticDistance-(l+"").length()-1; i++)
				sb.append(" ");
		}
		
		return sb.toString();
	}
	
	public static <D> String packAscii(ReferenceGenomicRegion<?> parent,
			Iterator<GenomicRegion> regions) {
		return packAscii(parent, regions, '#', '-', '<', '>', '/', '\\', d->"");
	}
	
	public static <D> String packAscii(ReferenceGenomicRegion<?> parent,
			Iterator<GenomicRegion> regions, Function<GenomicRegion,String> stringer) {
		return packAscii(parent, regions, '#', '-', '<', '>', '/', '\\', stringer);
	}
	
	/**
	 * Returns a string representation of the given interval tree in the given parent region
	 * @param cds
	 * @param set
	 * @return
	 */
	public static String packAscii(ReferenceGenomicRegion<?> parent,
			Iterator<GenomicRegion> regions, char regChar, char gapChar, char missingLeft, char missingRight, char compatibleSplitLeft, char compatibleSplitRight, Function<GenomicRegion,String> stringer) {
		
		ArrayList<StringBuilder> lines = new ArrayList<StringBuilder>();
		
		GenomicRegion preg = parent.getRegion();
		boolean plus = parent.getReference().getStrand()!=Strand.Minus;
		
		while (regions.hasNext()) {
			
			GenomicRegion reg = regions.next();
			String d = stringer.apply(reg);
			GenomicRegion inters = preg.intersect(reg);
			if (inters.getTotalLength()==0) continue;
			
			GenomicRegion intersInd = parent.induce(inters);
			char[] chars = new char[intersInd.getEnd()-intersInd.getStart()];
			Arrays.fill(chars, gapChar);
			for (int p=0; p<intersInd.getNumParts(); p++)
				Arrays.fill(chars, intersInd.getStart(p)-intersInd.getStart(),intersInd.getEnd(p)-intersInd.getStart(),regChar);
			
			// fill in inconsistent intron chars
			GenomicRegion removed = reg.subtract(inters);
			if (removed.getTotalLength()>0) {
				GenomicRegion right = parent.induce(removed.extendAll(plus?1:0, plus?0:1).subtract(removed).intersect(preg));
				for (int p=0; p<right.getNumParts(); p++)
					for (int pp=right.getStart(p); pp<right.getEnd(p); pp++)
						if (pp-intersInd.getStart()>=0 && pp-intersInd.getStart()<chars.length)
							chars[pp-intersInd.getStart()] = missingLeft;
				
				GenomicRegion left = parent.induce(removed.extendAll(plus?0:1,plus?1:0).subtract(removed).intersect(preg));
				for (int p=0; p<left.getNumParts(); p++)
					for (int pp=left.getStart(p); pp<left.getEnd(p); pp++)
						if (pp-intersInd.getStart()>=0 && pp-intersInd.getStart()<chars.length)
							chars[pp-intersInd.getStart()] = missingRight;
			}
			
			// fill in consistent intron chars
			GenomicRegion borderRight = parent.induce(preg.extendAll(plus?1:0, plus?0:1).subtract(preg).extendAll(1, 1).intersect(preg));
			for (int p=0; p<borderRight.getNumParts(); p++)
				for (int pp=borderRight.getStart(p); pp<borderRight.getEnd(p); pp++)
					if (pp-intersInd.getStart()>=0 && pp-intersInd.getStart()<chars.length && chars[pp-intersInd.getStart()]==regChar)
						chars[pp-intersInd.getStart()] = compatibleSplitLeft;
			GenomicRegion borderLeft = parent.induce(preg.extendAll(plus?0:1,plus?1:0).subtract(preg).extendAll(1, 1).intersect(preg));
			for (int p=0; p<borderLeft.getNumParts(); p++)
				for (int pp=borderLeft.getStart(p); pp<borderLeft.getEnd(p); pp++)
					if (pp-intersInd.getStart()>=0 && pp-intersInd.getStart()<chars.length && chars[pp-intersInd.getStart()]==regChar)
						chars[pp-intersInd.getStart()] = compatibleSplitRight;
			
			// put it into the next line where there is space
			int line;
			for (line=0; line<lines.size(); line++) {
				if (lines.get(line).length()+1<intersInd.getStart())
					break;
			}
			if (line==lines.size())
				lines.add(new StringBuilder());
			StringBuilder sb = lines.get(line);
			while (sb.length()<intersInd.getStart()) sb.append(' ');
			
			sb.append(chars);
			if (d.length()>0) sb.append(" ").append(d);
			
		}
		
		
		StringBuilder sb = new StringBuilder();
		for (StringBuilder line : lines)
			sb.append(line).append("\n");
		return sb.toString();
		
	}

	public static int getLongestSuffixPosition(String s, Predicate<String> test) {
		for (int i=0; i<s.length(); i++)
			if (test.test(s.substring(i)))
				return i;
		return -1;
	}
	
	public static int getLongestPrefixPosition(String s, Predicate<String> test) {
		for (int i=s.length()-1; i>=0; i--)
			if (test.test(s.substring(0,i)))
				return i;
		return -1;
	}

	public static String removeComments(String src) {
		StringBuilder sb = new StringBuilder();
		String m1 = MaskedCharSequence.maskEscaped(src, '\0', '"','\'').toString();
		String masked = MaskedCharSequence.maskQuotes(m1, ' ').toString();
		
		
		int p = 0;
		for (;;) {
			int single = masked.indexOf("//", p);
			int multi = masked.indexOf("/*", p);
			if (single==-1 && multi==-1) {
				sb.append(src.substring(p));
				return sb.toString();
			}
			int start;
			int end; 
			if (single>-1 && (multi==-1 || single<multi)) {
				start = single;
				end = masked.indexOf("\n",start);
			} else {
				start = multi;
				end = masked.indexOf("*/",start)+2;
			}
			
			sb.append(src.substring(p,start));
			p = end;
		}
	}

	public static String sha1(String s) {
		MessageDigest mDigest;
		try {
			mDigest = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
        byte[] result = mDigest.digest(s.getBytes());
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < result.length; i++) {
            sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
        }
         
        return sb.toString();
	}

	private static Pattern varPattern = Pattern.compile("(\\$[A-Za-z_][A-Za-z0-9_]*)|(\\$\\{[^\\}]+\\})");
	
	/**
	 * Replaces all occurrences of ${var} by the value given by varToVal (or don't if it returns null) 
	 * @param s
	 * @param varToVal
	 * @return
	 */
	public static String replaceVariables(String s, UnaryOperator<String> varToVal) {
		Matcher m = varPattern.matcher(s);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			m.appendReplacement(sb, "");
			
			String rep = m.group();
			if (rep.startsWith("${") && rep.endsWith("}"))
				rep = rep.substring(2, rep.length()-1);
			else
				rep = rep.substring(1);
			
			String v = varToVal.apply(rep);
			if (v!=null)
				rep = v;
			else rep = "${"+rep+"}";
			
			sb.append(rep);
		}
		m.appendTail(sb);

		return sb.toString();
	}
	
	/**
	 * Replaces all occurrences of ${var} by the value given by varToVal (or don't if it returns null) 
	 * @param s
	 * @param varToVal
	 * @return
	 */
	public static String replaceVariablesInQuotes(String s, UnaryOperator<String> varToVal) {
		String m1 = MaskedCharSequence.maskEscaped(s, '\0', '"','\'').toString();
		MaskedCharSequence masked = MaskedCharSequence.maskLeftToRight(m1,'\0', new char[] {'"'}, new char[] {'"'});
		
		
		Matcher m = varPattern.matcher(s);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			m.appendReplacement(sb, "");
			
			String rep = m.group();
			if (rep.startsWith("${") && rep.endsWith("}"))
				rep = rep.substring(2, rep.length()-1);
			else
				rep = rep.substring(1);
			
			if (masked.isAllMasked(m.start(),m.end())) { 
				String v = varToVal.apply(rep);
				if (v!=null)
					rep = v;
				else rep = "${"+rep+"}";
			}
			sb.append(rep);
		}
		m.appendTail(sb);

		return sb.toString();
	}

	public static String createExceptionMessage(Exception e) {
		StringBuilder sb = new StringBuilder();
		for (Throwable t=e; t!=null; t=t.getCause()) {
			if (t.getMessage()!=null && t.getMessage().length()>0) {
				if (sb.length()>0)
					sb.append("\n");
				sb.append(t.getMessage());
			}
		}
		return sb.toString();
	}
	

}
