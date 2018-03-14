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
package gedi.util.algorithm.string.alignment.pairwise.algorithm;

import java.io.IOException;
import java.io.Writer;
import java.util.Locale;

import gedi.util.algorithm.string.alignment.pairwise.Alignment;
import gedi.util.algorithm.string.alignment.pairwise.AlignmentMode;
import gedi.util.algorithm.string.alignment.pairwise.scoring.LongScoring;



public class LongGotoh {

	
	private long[][] A;
	private long[][] I;
	private long[][] D;
	
	private long[] a;
	private long[] i;
	
	private int max_i;
	private int max_j;

	public LongGotoh() {
		this((1<<8)-1);
	}
	
	public LongGotoh(int maxLength) {
		A = new long[(maxLength+1)][(maxLength+1)];
		I = new long[(maxLength+1)][(maxLength+1)];
		D = new long[(maxLength+1)][(maxLength+1)];
		a = new long[maxLength+1];
		i = new long[maxLength+1];
	}
	
	public void freeSpace() {
		A = null;
		I = null;
		D = null;
		a = null;
		i = null;
	}

	private void ensureSize(int n, int m) {
		if (A==null || A.length<n || A[0].length<m) {
			int nn = 1, nm = 1;
			for (;nn<n; nn<<=1);
			for (;nm<m; nm<<=1);
			A = new long[nn][nm];
			I = new long[nn][nm];
			D = new long[nn][nm];
		}
	}
	
	private void ensureSize(int m) {
		if (a==null || a.length<m) {
			int nm = 1;
			for (;nm<m; nm<<=1);
			a = new long[nm];
			i = new long[nm];
		}
	}

	
	public long align(final LongScoring<?> scoring, final int n, final int m, long gapOpen, final long gapExtend, final AlignmentMode mode) {

		final long INF = Long.MIN_VALUE-gapExtend;
		
		ensureSize(m+1);
		
		final long gapFirst = gapOpen+gapExtend;

		final boolean initZeroFirst = mode!=AlignmentMode.Global && mode!=AlignmentMode.BothPrefix && mode!=AlignmentMode.PrefixSuffix;
		final boolean initZeroSecond = mode!=AlignmentMode.Global && mode!=AlignmentMode.BothPrefix;

		long max = INF;
		int max_i = 0;
		int max_j = 0;

		final long[] A = this.a;
		final long[] I = this.i;
		long d,a,as;

		I[0] = 0;
		A[0] = 0;

		A[1] = initZeroSecond?0:gapFirst;
		I[1] = INF;

		for (int j=2; j<=m; j++) {
			A[j] = initZeroSecond?0:A[j-1]+gapExtend;
			I[j] = INF;
			if (mode==AlignmentMode.Freeshift && A[j]>max) {
				max = A[j];
				max_i = 0; 
				max_j = j;
			}
		}
		
		if (mode==AlignmentMode.PrefixSuffix && A[m]>max) {
			max = A[m];
			max_i = 0; 
			max_j = m;
		}

		for (int i=1; i<=n; i++) {
			I[0] = Math.max(I[0]+gapExtend, A[0]+gapFirst);
			d = INF;
			a = A[0];
			A[0] = initZeroFirst?0:(i==1?gapFirst:A[0]+gapExtend);
			
			if (mode==AlignmentMode.Local)
				for (int j=1; j<=m; j++) {
					I[j] = Math.max(I[j]+gapExtend, A[j]+gapFirst);
					d = Math.max(d+gapExtend, A[j-1]+gapFirst);
					as = A[j];
					A[j] = Math.max(Math.max(0,a+scoring.getLong(i-1,j-1)), Math.max(I[j], d));
					a = as;
					if (A[j]>max) {
						max = A[j];
						max_i = i; 
						max_j = j;
					}
				}
			else if (mode==AlignmentMode.Global) {
				for (int j=1; j<=m; j++) {
					I[j] = Math.max(I[j]+gapExtend, A[j]+gapFirst);
					d = Math.max(d+gapExtend, A[j-1]+gapFirst);
					as = A[j];
					A[j] = Math.max(a+scoring.getLong(i-1,j-1), Math.max(I[j], d));
					a = as;
				}
			}
			else if (mode==AlignmentMode.BothPrefix) {
				for (int j=1; j<=m; j++) {
					I[j] = Math.max(I[j]+gapExtend, A[j]+gapFirst);
					d = Math.max(d+gapExtend, A[j-1]+gapFirst);
					as = A[j];
					A[j] = Math.max(a+scoring.getLong(i-1,j-1), Math.max(I[j], d));
					a = as;
					if ((j==m || i==n) && A[j]>max) {
						max = A[j];
						max_i = i; 
						max_j = j;
					}
				}
			}
			else if (mode==AlignmentMode.Freeshift)
				for (int j=1; j<=m; j++) {
					I[j] = Math.max(I[j]+gapExtend, A[j]+gapFirst);
					d = Math.max(d+gapExtend, A[j-1]+gapFirst);
					as = A[j];
					A[j] = Math.max(a+scoring.getLong(i-1,j-1), Math.max(I[j], d));
					a = as;
					if ((j==m || i==n) && A[j]>max) {
						max = A[j];
						max_i = i; 
						max_j = j;
					}
				}
			else 
				for (int j=1; j<=m; j++) {
					I[j] = Math.max(I[j]+gapExtend, A[j]+gapFirst);
					d = Math.max(d+gapExtend, A[j-1]+gapFirst);
					as = A[j];
					A[j] = Math.max(a+scoring.getLong(i-1,j-1), Math.max(I[j], d));
					a = as;
					if (j==m && A[j]>max) {
						max = A[j];
						max_i = i; 
						max_j = j;
					}
				}
			
		}
		
		if (mode==AlignmentMode.Global) {
			max = A[m];
			max_i = n; 
			max_j = m;
		}

		this.max_i = max_i;
		this.max_j = max_j;

		return max;
	}
	
	
	public long align(final LongScoring<?> scoring, final int n, final int m, long gapOpen, final long gapExtend, final AlignmentMode mode, final Alignment alignment) {

		final long INF = Long.MIN_VALUE-gapExtend;
		
		ensureSize(n+1, m+1);
		
		final long gapFirst = gapOpen+gapExtend;

		final boolean initZeroFirst = mode!=AlignmentMode.Global && mode!=AlignmentMode.BothPrefix && mode!=AlignmentMode.PrefixSuffix;
		final boolean initZeroSecond = mode!=AlignmentMode.Global && mode!=AlignmentMode.BothPrefix;

		long max = INF;
		int max_i = 0;
		int max_j = 0;

		final long[][] A = this.A;
		final long[][] I = this.I;
		final long[][] D = this.D;

		I[0][0] = 0;
		D[0][0] = 0;
		A[0][0] = 0;

		A[0][1] = initZeroSecond?0:gapFirst;
		D[0][1] = A[0][1];
		I[0][1] = INF;

		D[1][0] = INF;
		A[1][0] = initZeroFirst?0:gapFirst;
		I[1][0] = A[1][0];

		for (int j=2; j<=m; j++) {
			A[0][j] = initZeroSecond?0:A[0][j-1]+gapExtend;
			D[0][j] = A[0][j];
			I[0][j] = INF;
			if (mode==AlignmentMode.Freeshift && A[0][j]>max) {
				max = A[0][j];
				max_i = 0; 
				max_j = j;
			}
		}
		
		if (mode==AlignmentMode.PrefixSuffix && A[0][m]>max) {
			max = A[0][m];
			max_i = 0; 
			max_j = m;
		}


		for (int i=1; i<=n; i++) {

			D[i][0] = INF;
			A[i][0] = initZeroFirst?0:(i==1?gapFirst:A[i-1][0]+gapExtend);
			I[i][0] = A[i][0];
			
			if (mode==AlignmentMode.Local)
				for (int j=1; j<=m; j++) {
					I[i][j] = Math.max(I[i-1][j]+gapExtend, A[i-1][j]+gapFirst);
					D[i][j] = Math.max(D[i][j-1]+gapExtend, A[i][j-1]+gapFirst);
					A[i][j] = Math.max(Math.max(0,A[i-1][j-1]+scoring.getLong(i-1,j-1)), Math.max(I[i][j], D[i][j]));
					if (A[i][j]>max) {
						max = A[i][j];
						max_i = i; 
						max_j = j;
					}
				}
			else if (mode==AlignmentMode.Global) {
				for (int j=1; j<=m; j++) {
					I[i][j] = Math.max(I[i-1][j]+gapExtend, A[i-1][j]+gapFirst);
					D[i][j] = Math.max(D[i][j-1]+gapExtend, A[i][j-1]+gapFirst);
					A[i][j] = Math.max(A[i-1][j-1]+scoring.getLong(i-1,j-1), Math.max(I[i][j], D[i][j]));
				}
			}
			else if (mode==AlignmentMode.BothPrefix) {
				for (int j=1; j<=m; j++) {
					I[i][j] = Math.max(I[i-1][j]+gapExtend, A[i-1][j]+gapFirst);
					D[i][j] = Math.max(D[i][j-1]+gapExtend, A[i][j-1]+gapFirst);
					A[i][j] = Math.max(A[i-1][j-1]+scoring.getLong(i-1,j-1), Math.max(I[i][j], D[i][j]));
					if ((j==m || i==n) && A[i][j]>max) {
						max = A[i][j];
						max_i = i; 
						max_j = j;
					}
				}
			}
			else if (mode==AlignmentMode.Freeshift)
				for (int j=1; j<=m; j++) {
					I[i][j] = Math.max(I[i-1][j]+gapExtend, A[i-1][j]+gapFirst);
					D[i][j] = Math.max(D[i][j-1]+gapExtend, A[i][j-1]+gapFirst);
					A[i][j] = Math.max(A[i-1][j-1]+scoring.getLong(i-1,j-1), Math.max(I[i][j], D[i][j]));
					if ((j==m || i==n) && A[i][j]>max) {
						max = A[i][j];
						max_i = i; 
						max_j = j;
					}
				}
			else
				for (int j=1; j<=m; j++) {
					I[i][j] = Math.max(I[i-1][j]+gapExtend, A[i-1][j]+gapFirst);
					D[i][j] = Math.max(D[i][j-1]+gapExtend, A[i][j-1]+gapFirst);
					A[i][j] = Math.max(A[i-1][j-1]+scoring.getLong(i-1,j-1), Math.max(I[i][j], D[i][j]));
					if (j==m && A[i][j]>max) {
						max = A[i][j];
						max_i = i; 
						max_j = j;
					}
				}
		}
		
		if (mode==AlignmentMode.Global) {
			max = A[n][m];
			max_i = n; 
			max_j = m;
		}

		this.max_i = max_i;
		this.max_j = max_j;

		if (alignment==null)
			return max;
		
//		return max;
//	}
//
//	public void backtrack(Alignment alignment, LongScoring scoring, long gapOpen, long gapExtend, AlignmentMode mode) {

		alignment.clear();
		
		int i = this.max_i;
		int j = this.max_j;

//		long gapFirst = gapOpen+gapExtend;

//		long[][] A = this.A;
//		long[][] I = this.I;
//		long[][] D = this.D;
		
		if (i==0 ||j==0) return max;

		do {
			long s = A[i][j];
			if (s == A[i-1][j-1]+scoring.getLong(i-1,j-1)) {
				alignment.add(i-1,j-1);
				i--;
				j--;
			} else if (s == I[i][j]) {
				for (i--; A[i][j]+gapFirst!=I[i+1][j]; i--);
			} else {
				for (j--; A[i][j]+gapFirst!=D[i][j+1]; j--);
			}

		} while (i!=0 && j!=0 && (mode!=AlignmentMode.Local || A[i][j]!=0));
		
		return max;
	}

	
	public void printMatrices(char[] s1, char[] s2, Writer out) throws IOException {
		out.write("A\n");
		printMatrix(A,s1,s2,out);
		out.write("I\n");
		printMatrix(I,s1,s2,out);
		out.write("D\n");
		printMatrix(D,s1,s2,out);
	}
	
	private void printMatrix(long[][] mat, char[] s1, char[] s2, Writer out) throws IOException {
		
		out.write("\t");
		for (int j=0; j<s2.length; j++) {
			out.write("\t");
			out.write(s2[j]);
		}
		out.write("\n");
		
		for (int i=0; i<=s1.length; i++) {
			if (i>0)
				out.write(s1[i-1]);
			for (int j=0; j<=s2.length; j++) {
				out.write("\t");
				out.write(String.format(Locale.US,"%d",mat[i][j]));
			}
			out.write("\n");
		}
	}
//
//	public void printMatrices(char[] s1, char[] s2) {
//		System.out.println("A");
//		printMatrix(A,s1,s2);
//		System.out.println("I");
//		printMatrix(I,s1,s2);
//		System.out.println("D");
//		printMatrix(D,s1,s2);
//	}
//	
//	private void printMatrix(long[][] mat, char[] s1, char[] s2) {
//		
//		System.out.print("\t");
//		for (int j=0; j<s2.length; j++) {
//			System.out.print("\t");
//			System.out.print(s2[j]);
//		}
//		System.out.println();
//		
//		for (int i=0; i<=s1.length; i++) {
//			if (i>0)
//				System.out.print(s1[i-1]);
//			for (int j=0; j<=s2.length; j++) {
//				System.out.print("\t");
//				System.out.printf(Locale.US,"%.2f",mat[i][j]);
//			}
//			System.out.println();
//		}
//	}



	

}
