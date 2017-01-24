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


import gedi.util.mutable.MutablePair;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.function.Function;




/**
 * General model class for sequences starting with lower case part (5' flank)
 * then a uppercase part (the actual sequence) and again a lower case part (the 3' flank)
 * @author erhard
 *
 */
public class WithFlankingSequence implements CharSequence {


	public static final int MODE_5_FLANK = 1;
	public static final int MODE_SEQUENCE = 1<<1;
	public static final int MODE_3_FLANK = 1<<2;
	public static final int MODE_FULL = MODE_3_FLANK | MODE_5_FLANK | MODE_SEQUENCE;




	private char[] chars;
	//	private int offset;
	//	private int length;

	protected int start = -1;
	protected int end = -1;
	
	public WithFlankingSequence(String sequence) {
		this(sequence,true);
	}

	public WithFlankingSequence(String sequence, boolean toRNA) {
		char[] chars = sequence.toCharArray();
		this.chars = chars;

		start = Character.isUpperCase(chars[0]) ? 0 : -1;
		for (int i=0; i<chars.length; i++) {
			boolean isUpper = Character.isUpperCase(chars[i]);
			if (isUpper && start<0)
				start = i;
			else if (!isUpper && end<0 && start>=0)
				end = i;
			if (!isUpper)
				chars[i] = Character.toUpperCase(chars[i]);
			if (toRNA && chars[i]=='T')
				chars[i] = 'U';
		}
		if (start==-1)
			start=chars.length;
		if (end==-1)
			end=chars.length;
		//		offset = 0;
		//		length = chars.length;
	}

	public WithFlankingSequence(String sequence, int cdsStart, int cdsEnd) {
		this.chars = sequence.toCharArray();
		this.start = cdsStart;
		this.end = cdsEnd;
	}
	
	public WithFlankingSequence(String s5flank, String sa, String s3flank) {
		this.chars = (s5flank+sa+s3flank).toUpperCase().toCharArray();
		this.start = s5flank.length();
		this.end = s5flank.length()+sa.length();
	}

	private WithFlankingSequence(WithFlankingSequence parent, int start, int length) {
		this.chars = new char[length];
		System.arraycopy(parent.chars, start, this.chars, 0, length);
		this.start = Math.max(0, parent.start-start);
		this.end = Math.min(chars.length,parent.end-start);
		//		this.offset = start;
		//		this.length = length;
	}

	public boolean isWithFlankingSequence() {
		return start<chars.length;
	}

	public int getActualSequenceStart() {
		return start;
	}

	public int get3FlankStart() {
		return end;
	}

	public int getStart(int mode) {
		if ((mode&MODE_5_FLANK)!=0) return 0;
		if ((mode&MODE_SEQUENCE)!=0) return start;
		if ((mode&MODE_3_FLANK)!=0) return end;
		throw new NoSuchElementException();
	}

	public String get5Flank() {
		return new String(chars,0,start);
	}

	public String getActualSequence() {
		return new String(chars,start,end-start);
	}

	public String get3Flank() {
		return new String(chars,end,chars.length-end);
	}

	public int get3FlankLength()  {
		return chars.length-end;
	}

	public int get5FlankLength()  {
		return start;
	}

	public int getActualSequenceLength()  {
		return end-start;
	}

	/**
	 * Positions are measured from the actual sequence start, i.e. start may be negative.
	 * @param start
	 * @param end
	 * @return
	 */
	public WithFlankingSequence extract(int start, int end) {
		return subSequence(start+this.start, end+this.start);
	}

	public String getLowerUpperLowerSequence() {
		return get5Flank().toLowerCase()+getActualSequence().toUpperCase()+get3Flank().toLowerCase();
	}

	@Override
	public char charAt(int index) {
		//		return chars[index+offset];
		return chars[index];
	}

	public String getSequence(int mode) {
		return getSequence(mode,false);
	}
	
	public String getSequence(int mode, boolean lowerUpperLower) {
		StringBuilder sb = new StringBuilder();
		if ((mode&MODE_5_FLANK)!=0) sb.append(lowerUpperLower?get5Flank().toLowerCase():get5Flank());
		if ((mode&MODE_SEQUENCE)!=0) sb.append(lowerUpperLower?getActualSequence().toUpperCase():getActualSequence());
		if ((mode&MODE_3_FLANK)!=0) sb.append(lowerUpperLower?get3Flank().toLowerCase():get3Flank());
		return sb.toString();
	}

	@Override
	public int length() {
		return chars.length;
	}

	@Override
	public WithFlankingSequence subSequence(int start, int end) {
		return new WithFlankingSequence(this,start,end-start);
	}

	@Override
	public String toString() {
		return getLowerUpperLowerSequence();
	}

	/**
	 * Low level access: Comparison is case insensitive. 
	 * @param sequence
	 * @return
	 */
	public int compareSequenceTo(char[] sequence, int mode) {
		int start = chars.length, end = 0;
		if ((mode&MODE_5_FLANK)!=0) {
			start=Math.min(0,start);
			end=Math.max(this.start, end);
		}
		if ((mode&MODE_SEQUENCE)!=0){
			start=Math.min(this.start,start);
			end=Math.max(this.end, end);
		}
		if ((mode&MODE_3_FLANK)!=0) {
			start=Math.min(this.end,start);
			end=Math.max(chars.length, end);
		}
		if (end<start) throw new IllegalArgumentException("Invalid mode!");
		
		int len1 = end-start;
		int len2 = sequence.length;
		int n = Math.min(len1, len2);
		int i = start;
		int j = 0;

		if (i == j) {
			int k = i;
			while (k < n) {
				char c1 = chars[k];
				char c2 = Character.toUpperCase(sequence[k]);
				if (c1 != c2) {
					return c1 - c2;
				}
				k++;
			}
		} else {
			while (n-- != 0) {
				char c1 = chars[i++];
				char c2 = Character.toUpperCase(sequence[j++]);
				if (c1 != c2) {
					return c1 - c2;
				}
			}
		}
		return len1 - len2;
	}

	public static class LengthComparator implements Comparator<WithFlankingSequence> {
		private int mode;
		public LengthComparator(int mode) {
			this.mode = mode;
		}

		@Override
		public int compare(WithFlankingSequence o1, WithFlankingSequence o2) {
			int l1 = 0;
			int l2 = 0;
			if ((mode&MODE_5_FLANK)!=0) {
				l1+=o1.get5Flank().length();
				l2+=o2.get5Flank().length();
			}
			if ((mode&MODE_SEQUENCE)!=0) {
				l1+=o1.getActualSequence().length();
				l2+=o2.getActualSequence().length();
			}
			if ((mode&MODE_3_FLANK)!=0) {
				l1+=o1.get3Flank().length();
				l2+=o2.get3Flank().length();
			}
			return l1-l2;
		}

	}


	public static class ToFlankingSequenceTransformer implements Function<String,WithFlankingSequence> {
		@Override
		public WithFlankingSequence apply(String s) {
			if (s.toLowerCase().startsWith("sequence unavailable"))
				return null;
			else
				return new WithFlankingSequence(s);
		}

	}


	public static class To3FlankTransformer implements Function<String,String> {
		@Override
		public String apply(String s) {
			String re = new WithFlankingSequence(s).get3Flank();
			if (re.toLowerCase().startsWith("sequence unavailable"))
				return null;
			else
				return re;
		}

	}

	public static class Length3FlankComparator implements Comparator<WithFlankingSequence> {

		@Override
		public int compare(WithFlankingSequence o1, WithFlankingSequence o2) {
			return o1.get3FlankLength()-o2.get3FlankLength(); 
		}

	}

	public static class TakeLonger3UTR implements Function<MutablePair<WithFlankingSequence,WithFlankingSequence>,WithFlankingSequence> {

		@Override
		public WithFlankingSequence apply(MutablePair<WithFlankingSequence, WithFlankingSequence> p) {
			return p.Item1.get3FlankLength()>p.Item2.get3FlankLength()?p.Item1:p.Item2;
		}

	}

	public static class TakeFirst implements Function<MutablePair<WithFlankingSequence,WithFlankingSequence>,WithFlankingSequence> {

		@Override
		public WithFlankingSequence apply(MutablePair<WithFlankingSequence, WithFlankingSequence> p) {
			return p.Item1;
		}

	}

	public static class SequenceComparator<T extends WithFlankingSequence> implements Comparator<T> {
		private int mode;
		public SequenceComparator(int mode) {
			this.mode = mode;
		}

		@Override
		public int compare(T o1, T o2) {
			return o1.getSequence(mode).compareTo(o2.getSequence(mode));
		}

	}


	public static final String modeToString(int mrnaMode) {
		StringBuilder sb = new StringBuilder();
		if ((mrnaMode&MODE_5_FLANK)!=0)
			sb.append("3' flank + ");
		if ((mrnaMode&MODE_SEQUENCE)!=0)
			sb.append("sequence + ");
		if ((mrnaMode&MODE_3_FLANK)!=0)
			sb.append("5' flank + ");
		if (sb.length()>0)
			sb.delete(sb.length()-3, sb.length());
		return sb.toString();
	}

	public static final String modesToString(String sep) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<8; i++) {
			if (i>0)
				sb.append(sep);
			if ((i&MODE_5_FLANK)!=0)
				sb.append("6");
			if ((i&MODE_SEQUENCE)!=0)
				sb.append("S");
			if ((i&MODE_3_FLANK)!=0)
				sb.append("3");
		}
		return sb.toString();
	}

	public char getActualSequenceChar(int pos) {
		if (pos<0) return getSequence(MODE_5_FLANK, false).charAt(pos+get5FlankLength());
		if (pos>=getActualSequenceLength()) return getSequence(MODE_3_FLANK,false).charAt(pos-getActualSequenceLength());
		return getSequence(MODE_SEQUENCE,false).charAt(pos);
	}

	public int getFirstValidActualSequenceIndex() {
		return -get5FlankLength();
	}

	public int getLastValidActualSequenceIndex() {
		return getActualSequenceLength()+get3FlankLength()-1;
	}

	public char charAtRelativeToActual(int pos) {
		return isWithFlankingSequence()?charAt(pos+start):charAt(pos);
	}

}
