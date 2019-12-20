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
package gedi.util.rna.secondary;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;

import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.intcollections.IntIterator;


public class EnergyUtils {

	

	public static int[] getOneBasedBasepairTable(CharSequence bracketNotation) {
		int i,j,hx;
		int length = bracketNotation.length();
		int[] stack = new int[length+1];
		int[] table = new int[length+1];

		table[0] = length;
		int br = 0;
		
		for (hx=0, i=1; i<=length; i++) {
			switch (bracketNotation.charAt(i-1)) {
			case '(':
				stack[hx++]=i-br;
				break;
			case ')':
				j = stack[--hx];
				if (hx<0) 
					throw new IllegalArgumentException("Unbalanced brackets!");
				table[i-br]=j;
				table[j]=i-br;
				break;
			case '&':
				br = 1;
				table[i] = 0;
				break;
			default:   /* unpaired base, usually '.' */
				table[i]= 0;
				break;
			}
		}
		if (hx!=0) 
			throw new IllegalArgumentException("Unbalanced brackets!");
		return(table);
	}

	public static int[] getZeroBasedBasepairTable(CharSequence bracketNotation) {
		int i,j,hx;
		int length = bracketNotation.length();
		int[] stack = new int[length];
		int[] table = new int[length];


		for (hx=0, i=0; i<length; i++) {
			switch (bracketNotation.charAt(i)) {
			case '(':
				stack[hx++]=i;
				break;
			case ')':
				j = stack[--hx];
				if (hx<0) 
					throw new IllegalArgumentException("Unbalanced brackets!");
				table[i]=j;
				table[j]=i;
				break;
			default:   /* unpaired base, usually '.' */
				table[i]= -1;
				break;
			}
		}
		if (hx!=0) 
			throw new IllegalArgumentException("Unbalanced brackets!");
		return(table);
	}
	
	
	
	public static final int[] encodeSequence(CharSequence sequence, boolean leadingSize, Character correct) {
		final int offset = leadingSize?1:0;
		int[] re = new int[sequence.length()+offset];
		if (leadingSize)
			re[0]=sequence.length();
		for (int i=offset; i<re.length; i++) {
			re[i] = encode_char(Character.toUpperCase(sequence.charAt(i-offset)));
			if (correct!=null && re[i]==0)
				re[i] = encode_char(Character.toUpperCase(correct));
		}
		return re;
	}

	public static final void encodeSequence(CharSequence sequence,int[] S, int[] S1) {
		int i,l;

		l = sequence.length();
		/* S1 exists only for the special X K and I bases and energy_set!=0 */
		S[0] = l;

		for (i=1; i<=l; i++) { /* make numerical encoding of sequence */
			S[i]= encode_char(Character.toUpperCase(sequence.charAt(i-1)));
			if (S1!=null)
				S1[i] = alias[S[i]];   /* for mismatches of nostandard bases */
		}
		/* for circular folding add first base at position n+1 and last base at
			position 0 in S1	*/
		S[l+1] = S[1];
		if (S1!=null) {
			S1[l+1]=S1[1]; 
			S1[0] = S1[l];
		}
	}

	
	public static double GASCONST = 1.98717;  /* in [cal/K] */
	public static double K0 = 273.15;
	public static int INF = Integer.MAX_VALUE/2;
	public static int FORBIDDEN = 9999;
	public static int BONUS = 10000;
	public static int NBPAIRS = 7;
	public static int NBASES = 8;
	public static int TURN = 3;
	public static int MAXLOOP = 30;
	public static int NST = 0;
	public static int NSM = 0;
	public static double Tmeasure = 37+K0;  /* temperature of param measurements */
	public static double lxc37=107.856;     /* parameter for logarithmic loop
					    energy extrapolation            */
	public static double _RT = (((Tmeasure) * GASCONST) / 1000.0);

	public static final String Law_and_Order = "_ACGUTXKI";
	public static final char[] Law_and_Order_ARRAY = Law_and_Order.toCharArray();
	public static final int[][] BP_pair=
	   /* _  A  C  G  U  X  K  I */
	   {{ 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 5, 0, 0, 5},
		{ 0, 0, 0, 1, 0, 0, 0, 0},
		{ 0, 0, 2, 0, 3, 0, 0, 0},
		{ 0, 6, 0, 4, 0, 0, 0, 6},
		{ 0, 0, 0, 0, 0, 0, 2, 0},
		{ 0, 0, 0, 0, 0, 1, 0, 0},
		{ 0, 6, 0, 0, 5, 0, 0, 0}};

	public static int[][] pair = new int[Law_and_Order.length()+1][Law_and_Order.length()+1];
	/* rtype[pair[i][j]]:=pair[j][i] */
	public static int[] rtype = {0, 2, 1, 4, 3, 6, 5, 7}; 
	public static int[] alias = new int[Law_and_Order.length()+1];
	public static char[] rpair1;
	public static char[] rpair2;
	static {
		for (int i=0; i<5; i++) alias[i] = (short) i;
		alias[5] = 3; /* X <-> G */
		alias[6] = 2; /* K <-> C */
		alias[7] = 0; /* I <-> default base '@' */
		for (int i=0; i<NBASES; i++) {
			for (int j=0; j<NBASES; j++) 
				pair[i][j] = BP_pair[i][j];
		}      
		for (int i=0; i<NBASES; i++) {
			for (int j=0; j<NBASES; j++) 
				rtype[pair[i][j]] = pair[j][i];
		}      
		rpair1 = new char[ArrayUtils.max(pair)+1];
		rpair2 = new char[rpair1.length];
		for (int i=0; i<6; i++)
			for (int j=0; j<6; j++) {
				rpair1[pair[i][j]] = Law_and_Order_ARRAY[i];
				rpair2[pair[i][j]] = Law_and_Order_ARRAY[j];
			}
				
	}
	public static final int encode_char(char c) {
		/* return numerical representation of base used e.g. in pair[][] */
		//	  if (energy_set>0) code = (int) (c-'A')+1;
		//	  else 
		{
			int index = Law_and_Order.indexOf(c);
			if (index==-1) index=0;
			if (index>4) index--; /* make T and U equivalent */
			return index;
		}
	}
	
	public static boolean canPair(char c1, char c2) {
		return pair[encode_char(c1)][encode_char(c2)]>0;
	}
	
	/**
	 * Returns true, iff in all sequences, base at p1 can pair with base at p2.
	 * @param s1
	 * @param s2
	 * @param p1
	 * @param p2
	 * @return
	 */
	public static boolean canPair(CharSequence[] s, int p1, int p2) {
		int n= s.length;
		
		for (int i=0; i<n; i++)
			if (pair[encode_char(s[i].charAt(p1))][encode_char(s[i].charAt(p2))]==0)
				return false;
		return true;
	}


	public static boolean isHairpin(String s) {
		boolean cl = false;
		for (int i=0; i<s.length(); i++) {
			if (s.charAt(i)==')') 
				cl = true;
			else if (cl && s.charAt(i)=='(')
				return false;
		}
		return cl;
	}

	public static Comparator<int[]> basepairComparator() {
		return new Comparator<int[]>() {
			@Override
			public int compare(int[] o1, int[] o2) {
				int re = o1[0]-o2[0];
				if (re==0)
					re = o2[1]-o1[1];
				return re;
			}
			
		};
	}

	public static Iterator<int[]> basepairIterator(final int[] bp, final boolean zeroBased, final int... additionalInfo) {
		return new BasepairIterator(bp,zeroBased,additionalInfo);
	}
	
	private static class BasepairIterator implements Iterator<int[]> {
		int p;
		int[] n;
		int[] bp;
		boolean next = false;
		
		public BasepairIterator(int[] bp, boolean zeroBased,
				int[] additionalInfo) {
			this.bp = bp;
			p=zeroBased?-1:0;
			n = new int[2+additionalInfo.length];
			System.arraycopy(additionalInfo, 0, n, 2, additionalInfo.length);
		}

		@Override
		public boolean hasNext() {
			lookAhead();
			return p<bp.length;
		}

		@Override
		public int[] next() {
			lookAhead();
			next = false;
			if (p<bp.length)
				return n;
			else
				return null;
		}

		private void lookAhead() {
			if (!next) {
				next = true;
				for (p++; p<bp.length && bp[p]<p; p++);
				if (p<bp.length){
					n[0]=p;
					n[1]=bp[p];
				}
			}
		}

		@Override
		public void remove() {}
	}
	
	public static IntIterator leftBasepairIterator(final int[] bp, final boolean zeroBased) {
		return new LeftBasepairIterator(bp,zeroBased);
	}
	
	private static class LeftBasepairIterator implements IntIterator {
		int p;
		int[] bp;
		boolean next = false;
		
		public LeftBasepairIterator(int[] bp, boolean zeroBased) {
			this.bp = bp;
			p=zeroBased?-1:0;
		}

		@Override
		public boolean hasNext() {
			lookAhead();
			return p<bp.length;
		}

		@Override
		public int nextInt() {
			lookAhead();
			next = false;
			return p;
		}
		@Override
		public Integer next() {
			lookAhead();
			next = false;
			if (p<bp.length)
				return p;
			else
				return null;
		}

		private void lookAhead() {
			if (!next) {
				next = true;
				for (p++; p<bp.length && bp[p]<p; p++);
			}
		}

		@Override
		public void remove() {}
	}

	/**
	 * Extends all loops if possible, e.g. an internal loop GG-UU is closed 
	 * @param fullBracketNotation
	 * @return the bracket notation of the extended structure
	 */
	public static String extendLoops(CharSequence sequence, CharSequence bracketNotation) {
		char[] re = StringUtils.toCharArray(bracketNotation);
		int[] bps = getZeroBasedBasepairTable(bracketNotation);
		IntIterator it = leftBasepairIterator(bps, true);
		while (it.hasNext()) {
			int p = it.next();
			int q=bps[p];
			for  (int i=p-1, j=q+1; i>0 && j<re.length && bps[i]==-1 && bps[j]==-1 && canPair(sequence.charAt(i), sequence.charAt(j)); i--, j++) 
				if (re[i]=='.' && re[j]=='.') {
					re[i] = '(';
					re[j] = ')';
				}
			for  (int i=p+1, j=q-1; j>0 && i<re.length && bps[i]==-1 && bps[j]==-1 && canPair(sequence.charAt(i), sequence.charAt(j)); i--, j++)
				if (re[i]=='.' && re[j]=='.') {
					re[i] = '(';
					re[j] = ')';
				}
			
		}
		return String.valueOf(re);
	}
	

	/**
	 * Removes all illegal basepairs
	 * @param fullBracketNotation
	 * @return the bracket notation of the corrected structure
	 */
	public static String correctLoops(CharSequence sequence, CharSequence bracketNotation) {
		char[] re = StringUtils.toCharArray(bracketNotation);
		int[] bps = getZeroBasedBasepairTable(bracketNotation);
		IntIterator it = leftBasepairIterator(bps, true);
		while (it.hasNext()) {
			int p = it.nextInt();
			int q = bps[p];
			
			if (pair[encode_char(sequence.charAt(p))][encode_char(sequence.charAt(q))]==0) 
				re[p] = re[q] = '.';
		}
		return String.valueOf(re);
	}
	


	public static CharSequence getBracketNotation(int[] bps,boolean zeroBased) {
		return new BasepairsBrackets(bps,zeroBased);
	}
	
	
	private static class BasepairsBrackets implements CharSequence {
		private int[] bps;
		private boolean zeroBased;
		
		public BasepairsBrackets(int[] bps, boolean zeroBased) {
			this.bps = bps;
			this.zeroBased = zeroBased;
		}

		@Override
		public int length() {
			return zeroBased?bps.length:bps.length-1;
		}

		@Override
		public char charAt(int index) {
			int bp = zeroBased?bps[index]:bps[index+1];
			if (bp==(zeroBased?-1:0))
				return '.';
			if (bp<index)
				return ')';
			return '(';
		}
		
		@Override
		public String toString() {
			return StringUtils.toString(this);
		}

		@Override
		public CharSequence subSequence(int start, int end) {
			return StringUtils.toString(this).substring(start,end);
		}
		
	}

	

}