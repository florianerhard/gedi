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

package gedi.util.algorithm.string.alignment.pairwise.formatter;

import gedi.util.algorithm.string.alignment.pairwise.Alignment;
import gedi.util.algorithm.string.alignment.pairwise.AlignmentMode;
import gedi.util.algorithm.string.alignment.pairwise.gapCostFunctions.GapCostFunction;
import gedi.util.algorithm.string.alignment.pairwise.scoring.Scoring;

public class ArrayAlignmentFormatter<T> implements AlignmentFormatter<T[], Scoring<T[]>, String> {

	private static final char GAP = '-';
	private boolean appendUnaligned;
	private String sep;
	
	public ArrayAlignmentFormatter(String sep){
		this(" ",true);
	}

	public ArrayAlignmentFormatter(String sep, boolean appendUnaligned) {
		this.sep = sep;
		this.appendUnaligned = appendUnaligned;
	}

	
	@Override
	public String format(Alignment alignment, Scoring<T[]> scoring,
			GapCostFunction gap, AlignmentMode mode, T[] s1, T[] s2) {
		
		StringBuilder sb = new StringBuilder();
		
		int end = alignment.length();

		StringBuilder sb1 = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		
		
		int[] point = {s1.length, s2.length};
		if (end>0)
			alignment.getPoint(0, point);
		
		if (appendUnaligned) {
			for (int a=0; a<point[0]; a++) {
				String o = s1[a].toString();
				sb1.append(o);
				for (int i=0; i<o.length(); i++)
					sb2.append(GAP);
				sb1.append(sep);
				sb2.append(sep);
			}
			for (int a=0; a<point[1]; a++) {
				String o = s2[a].toString();
				sb2.append(o);
				for (int i=0; i<o.length(); i++)
					sb1.append(GAP);
				sb1.append(sep);
				sb2.append(sep);
			}
		}

		int li = -1;
		int lj = -1;
		for (int p=0; p<end; p++) {
			alignment.getPoint(p, point);
			
			if (p>0) {
				for (int g=li+1; g<point[0]; g++) {
					String o = s1[g].toString();
					sb1.append(o);
					for (int i=0; i<o.length(); i++)
						sb2.append(GAP);
					sb1.append(sep);
					sb2.append(sep);
				}
				for (int g=lj+1; g<point[1]; g++) {
					String o = s2[g].toString();
					sb2.append(o);
					for (int i=0; i<o.length(); i++)
						sb1.append(GAP);
					sb1.append(sep);
					sb2.append(sep);
				}
			}
			String o1 = s1[point[0]].toString();
			String o2 = s2[point[1]].toString();
			sb1.append(o1);
			sb2.append(o2);
			for (int i=0; i<o2.length()-o1.length(); i++)
				sb1.append(' ');
			for (int i=0; i<o1.length()-o2.length(); i++)
				sb2.append(' ');
			sb1.append(sep);
			sb2.append(sep);
			
			li=point[0]; lj=point[1];
		}
		
		if (appendUnaligned) {
			for (int a=point[0]+1; a<s1.length; a++) {
				String o = s1[a].toString();
				sb1.append(o);
				for (int i=0; i<o.length(); i++)
					sb2.append(GAP);
				sb1.append(sep);
				sb2.append(sep);
			}
			for (int a=point[1]+1; a<s2.length; a++) {
				String o = s2[a].toString();
				sb2.append(o);
				for (int i=0; i<o.length(); i++)
					sb1.append(GAP);
				sb1.append(sep);
				sb2.append(sep);
			}
		}
		sb1.delete(sb1.length()-sep.length(), sb1.length());
		sb2.delete(sb2.length()-sep.length(), sb2.length());
		
		sb.append(sb1.toString());
		sb.append("\n");
		sb.append(sb2.toString());
		sb.append("\n");
		return sb.toString();
	}

}
