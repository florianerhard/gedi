package gedi.util.algorithm.string.search;

import gedi.util.datastructure.collections.intcollections.IntIterator;


public class BoyerMoore {
	private char[] p;
	private CharSequence t;       // pattern, text
	private int m, n;          // pattern length, text length
	private static int alphabetsize=256;
	private int[] occ;         // occurence function
	private int[] f;
	private int[] s;

	public BoyerMoore()	{
		occ=new int[alphabetsize];
	}

	public IntIterator searchIterate(String tt, String pp)	{
		setText(tt);
		setPattern(pp);
		return searchIterate();
	}
	
	public String getPattern() {
		return new String(p);
	}
	public String getText() {
		return t.toString();
	}
	
	
	public int[] search(String tt, String pp) {
		return search(tt,pp,0);
	}
	
	public int[] search(String tt, String pp, int offset) {
		setText(tt);
		setPattern(pp);
		int[] re = new int[tt.length()];
		int index = 0;
		int i=0, j;
		while (i<=n-m)
		{
			j=m-1;
			while (j>=0 && p[j]==t.charAt(i+j)) j--;
			if (j<0)
			{
				re[index++]=i+offset;
				i+=s[0];
			}
			else
				i+=Math.max(s[j+1], j-occ[t.charAt(i+j)]);
		}
		int[] re2 = new int[index];
		System.arraycopy(re, 0, re2, 0, index);
		return re2;
	}


	public BoyerMoore setPattern(char[] pp) {
		this.p = pp;
		this.m = pp.length;
		f=new int[m+1];
		s=new int[m+1];
		bmPreprocess();
		return this;
	}
	
	public BoyerMoore setText(char[] tt)	 {
		n=tt.length;
		t=String.valueOf(tt);
		return this;
	}
	
	
	/** sets the text
	 */ 
	public BoyerMoore setText(CharSequence tt)	 {
		n=tt.length();
		t=tt;
		return this;
	}

	/** sets the pattern
	 */ 
	public BoyerMoore setPattern(String pp) {
		m=pp.length();
		p=pp.toCharArray();
		f=new int[m+1];
		s=new int[m+1];
		bmPreprocess();
		return this;
	}

	/** computation of the occurrence function
	 */ 
	private void bmInitocc() {
		char a;
		int j;

		for (a=0; a<alphabetsize; a++)
			occ[a]=-1;

		for (j=0; j<m; j++) {
			a=p[j];
			occ[a]=j;
		}
	}

	/** preprocessing according to the pattern (part 1)
	 */ 
	private void bmPreprocess1() {
		int i=m, j=m+1;
		f[i]=j;
		while (i>0) {
			while (j<=m && p[i-1]!=p[j-1]) {
				if (s[j]==0) s[j]=j-i;
				j=f[j];
			}
			i--; j--;
			f[i]=j;
		}
	}

	/** preprocessing according to the pattern (part 2)
	 */ 
	private void bmPreprocess2() {
		int i, j;
		j=f[0];
		for (i=0; i<=m; i++) {
			if (s[i]==0) s[i]=j;
			if (i==j) j=f[j];
		}
	}

	/** preprocessing according to the pattern
	 */ 
	private void bmPreprocess() {
		bmInitocc();
		bmPreprocess1();
		bmPreprocess2();
	}
	
	public boolean contains() {
		return searchIterate().hasNext();
	}
	
	public int count(String pp, String tt) {
		IntIterator it = searchIterate(tt,pp);
		int re = 0;
		while (it.hasNext()) {
			it.next();
			re++;
		}
		return re;
	}
	
	public int count() {
		IntIterator it = searchIterate();
		int re = 0;
		while (it.hasNext()) {
			it.next();
			re++;
		}
		return re;
	}
	
	/** searches the text for all occurences of the pattern
	 */ 
	public IntIterator searchIterate() {
		return new BmIterator();
	}
	
	public int search() {
		return search(0,n);
	}
	
	public int search(int start) {
		return search(start,n);
	}
	public int search(int start, int end) {
		int i=start, j;
		while (i<=end-m)
		{
			j=m-1;
			while (j>=0 && p[j]==t.charAt(i+j)) j--;
			if (j<0)
				return i;
			else
				i+=Math.max(s[j+1], j-occ[t.charAt(i+j)]);
		}
		
		return -1;
	}

	private class BmIterator implements IntIterator {

		private int next;
		private boolean hasNext = false;
		private int i=0, j;
		
		@Override
		public boolean hasNext() {
			lookAhead();
			return hasNext;
		}

		@Override
		public Integer next() {
			lookAhead();
			Integer re = next;
			hasNext = false;
			return re;
		}
		
		@Override
		public int nextInt() {
			lookAhead();
			hasNext = false;
			return next;
		}

		private void lookAhead() {
			while (!hasNext && i<=n-m) {
				j=m-1;
				while (j>=0 && p[j]==t.charAt(i+j)) j--;
				if (j<0)
				{
					next = i;
					hasNext = true;
					i+=s[0];
				}
				else
					i+=Math.max(s[j+1], j-occ[t.charAt(i+j)]);
			}
		}
		
		@Override
		public void remove() {}

	}



}
