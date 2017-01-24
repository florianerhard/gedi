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

import java.util.Locale;

import gedi.util.algorithm.string.alignment.pairwise.Alignment;
import gedi.util.algorithm.string.alignment.pairwise.AlignmentMode;
import gedi.util.algorithm.string.alignment.pairwise.scoring.Scoring;


public class FloatGotoh {

	
	private float[][] A;
	private float[][] I;
	private float[][] D;
	
	private float[] a;
	private float[] i;
	
	private int max_i;
	private int max_j;

	public FloatGotoh() {
		this((1<<8)-1);
	}
	
	public FloatGotoh(int maxLength) {
		A = new float[(maxLength+1)][(maxLength+1)];
		I = new float[(maxLength+1)][(maxLength+1)];
		D = new float[(maxLength+1)][(maxLength+1)];
	}
	
	public void freeSpace() {
		A = null;
		I = null;
		D = null;
	}

	private void ensureSize(int n, int m) {
		if (A==null || A.length<n || A[0].length<m) {
			int nn = 1, nm = 1;
			for (;nn<n; nn<<=1);
			for (;nm<m; nm<<=1);
			A = new float[nn][nm];
			I = new float[nn][nm];
			D = new float[nn][nm];
		}
	}
	
	private void ensureSize(int m) {
		if (a==null || a.length<m) {
			int nm = 1;
			for (;nm<m; nm<<=1);
			a = new float[nm];
			i = new float[nm];
		}
	}

	
	public float align(final Scoring<?> scoring, final int n, final int m, float gapOpen, final float gapExtend, final AlignmentMode mode) {

		final float INF = Float.NEGATIVE_INFINITY;
		
		ensureSize(m+1);
		
		final float gapFirst = gapOpen+gapExtend;

		final boolean initZeroFirst = mode!=AlignmentMode.Global && mode!=AlignmentMode.PrefixSuffix;
		final boolean initZeroSecond = mode!=AlignmentMode.Global;

		float max = 0;
		int max_i = 0;
		int max_j = 0;

		final float[] A = this.a;
		final float[] I = this.i;
		float d,a,as;

		I[0] = 0;
		A[0] = 0;

		A[1] = initZeroSecond?0:gapFirst;
		I[1] = INF;

		for (int j=2; j<=m; j++) {
			A[j] = initZeroSecond?0:A[j-1]+gapExtend;
			I[j] = INF;
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
					A[j] = Math.max(Math.max(0,a+scoring.getFloat(i-1,j-1)), Math.max(I[j], d));
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
					A[j] = Math.max(a+scoring.getFloat(i-1,j-1), Math.max(I[j], d));
					a = as;
				}
			}
			else if (mode==AlignmentMode.Freeshift)
				for (int j=1; j<=m; j++) {
					I[j] = Math.max(I[j]+gapExtend, A[j]+gapFirst);
					d = Math.max(d+gapExtend, A[j-1]+gapFirst);
					as = A[j];
					A[j] = Math.max(a+scoring.getFloat(i-1,j-1), Math.max(I[j], d));
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
					A[j] = Math.max(a+scoring.getFloat(i-1,j-1), Math.max(I[j], d));
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
	
	
	public float align(final Scoring<?> scoring, final int n, final int m, float gapOpen, final float gapExtend, final AlignmentMode mode, final Alignment alignment) {

		final float INF = Float.NEGATIVE_INFINITY;
		
		ensureSize(n+1, m+1);
		
		final float gapFirst = gapOpen+gapExtend;

		final boolean initZeroFirst = mode!=AlignmentMode.Global && mode!=AlignmentMode.PrefixSuffix;
		final boolean initZeroSecond = mode!=AlignmentMode.Global;

		float max = 0;
		int max_i = 0;
		int max_j = 0;

		final float[][] A = this.A;
		final float[][] I = this.I;
		final float[][] D = this.D;

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
		}


		for (int i=1; i<=n; i++) {

			D[i][0] = INF;
			A[i][0] = initZeroFirst?0:(i==1?gapFirst:A[i-1][0]+gapExtend);
			I[i][0] = A[i][0];
			
			if (mode==AlignmentMode.Local)
				for (int j=1; j<=m; j++) {
					I[i][j] = Math.max(I[i-1][j]+gapExtend, A[i-1][j]+gapFirst);
					D[i][j] = Math.max(D[i][j-1]+gapExtend, A[i][j-1]+gapFirst);
					A[i][j] = Math.max(Math.max(0,A[i-1][j-1]+scoring.getFloat(i-1,j-1)), Math.max(I[i][j], D[i][j]));
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
					A[i][j] = Math.max(A[i-1][j-1]+scoring.getFloat(i-1,j-1), Math.max(I[i][j], D[i][j]));
				}
			}
			else if (mode==AlignmentMode.Freeshift)
				for (int j=1; j<=m; j++) {
					I[i][j] = Math.max(I[i-1][j]+gapExtend, A[i-1][j]+gapFirst);
					D[i][j] = Math.max(D[i][j-1]+gapExtend, A[i][j-1]+gapFirst);
					A[i][j] = Math.max(A[i-1][j-1]+scoring.getFloat(i-1,j-1), Math.max(I[i][j], D[i][j]));
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
					A[i][j] = Math.max(A[i-1][j-1]+scoring.getFloat(i-1,j-1), Math.max(I[i][j], D[i][j]));
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
//	public void backtrack(Alignment alignment, floatScoring scoring, float gapOpen, float gapExtend, AlignmentMode mode) {

		alignment.clear();
		
		int i = this.max_i;
		int j = this.max_j;

//		float gapFirst = gapOpen+gapExtend;

//		float[][] A = this.A;
//		float[][] I = this.I;
//		float[][] D = this.D;
		
		if (i>0 || j>0)
			do {
				float s = A[i][j];
				if (s == A[i-1][j-1]+scoring.getFloat(i-1,j-1)) {
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


	public void printMatrices(char[] s1, char[] s2) {
		System.out.println("A");
		printMatrix(A,s1,s2);
		System.out.println("I");
		printMatrix(I,s1,s2);
		System.out.println("D");
		printMatrix(D,s1,s2);
	}
	
	private void printMatrix(float[][] mat, char[] s1, char[] s2) {
		
		System.out.print("\t");
		for (int j=0; j<s2.length; j++) {
			System.out.print("\t");
			System.out.print(s2[j]);
		}
		System.out.println();
		
		for (int i=0; i<=s1.length; i++) {
			if (i>0)
				System.out.print(s1[i-1]);
			for (int j=0; j<=s2.length; j++) {
				System.out.print("\t");
				System.out.printf(Locale.US,"%.2f",mat[i][j]);
			}
			System.out.println();
		}
	}



	

}
