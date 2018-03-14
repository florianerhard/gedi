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

import gedi.util.StringUtils;
import gedi.util.algorithm.string.alignment.pairwise.Alignment;
import gedi.util.algorithm.string.alignment.pairwise.AlignmentMode;
import gedi.util.algorithm.string.alignment.pairwise.gapCostFunctions.GapCostFunction;
import gedi.util.algorithm.string.alignment.pairwise.scoring.Scoring;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;


public class SimpleAlignmentFormatter implements AlignmentFormatter<CharSequence,Scoring<CharSequence>,String> {
	
	private static final char GAP = '-';
	
	private boolean appendUnaligned = true;
	private boolean appendToLower = true;
	private int width = 80;

	private boolean writeBetween = true;
	
	
	public SimpleAlignmentFormatter(){}

	public SimpleAlignmentFormatter(boolean appendUnaligned) {
		this.appendUnaligned = appendUnaligned;
	}
	
	public SimpleAlignmentFormatter setWidth(int width) {
		this.width = width;
		return this;
	}
	
	public SimpleAlignmentFormatter setWriteBetween(boolean writeBetween) {
		this.writeBetween = writeBetween;
		return this;
	}
	
	public SimpleAlignmentFormatter setAppendUnaligned(boolean appendUnaligned) {
		this.appendUnaligned = appendUnaligned;
		return this;
	}

	public String format(Alignment alignment, Scoring<CharSequence> scoring, GapCostFunction gap, AlignmentMode mode, CharSequence s1, CharSequence s2) {
		StringWriter sw = new StringWriter();
		try {
			format(alignment, scoring, gap, mode, s1, s2,sw);
		} catch (IOException e) {}
		return sw.toString();
	}
	
	public void format(Alignment alignment, Scoring<CharSequence> scoring, GapCostFunction gap, AlignmentMode mode, CharSequence s1, CharSequence s2, Writer writer) throws IOException {
		StringBuilder sb1 = new StringBuilder();
		StringBuilder sbb = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		
		scoring.setSubjects(s1, s2);
		
		boolean appendToLower = this.appendToLower && mode!=AlignmentMode.Global;
		boolean appendUnaligned = this.appendUnaligned || mode==AlignmentMode.Global;
		int off1 = 0;
		int off2 = 0;
		
		if (alignment.length()==0) {
			if (appendUnaligned) {
				sb1.append(appendToLower?s1.toString().toLowerCase():s1.toString());
				sb1.append(StringUtils.repeat(String.valueOf(GAP), s2.length()));
				sb2.append(StringUtils.repeat(String.valueOf(GAP), s1.length()));
				sb2.append(appendToLower?s2.toString().toLowerCase():s2.toString());
				sbb.append(StringUtils.repeat(" ", s1.length()));
			}
		} else {
		
			int end = alignment.length();
			
			int[] point = new int[2];
			alignment.getPoint(0, point);
			off1 = point[0];
			off2 = point[1];
			
			if (appendUnaligned) {
				for (int a=0; a<point[0]; a++) {
					sb1.append(appendToLower?Character.toLowerCase(s1.charAt(a)):s1.charAt(a));
					sb2.append(GAP);
					sbb.append(" ");
				}
				for (int a=0; a<point[1]; a++) {
					sb1.append(GAP);
					sb2.append(appendToLower?Character.toLowerCase(s2.charAt(a)):s2.charAt(a));
					sbb.append(" ");
				}
			}
	
			int li = -1;
			int lj = -1;
			for (int p=0; p<end; p++) {
				alignment.getPoint(p, point);
				
				if (p>0) {
					for (int g=li+1; g<point[0]; g++) {
						sb1.append(s1.charAt(g));
						sb2.append(GAP);
						sbb.append(" ");
					}
					for (int g=lj+1; g<point[1]; g++) {
						sb1.append(GAP);
						sb2.append(s2.charAt(g));
						sbb.append(" ");
					}
				}
				sb1.append(s1.charAt(point[0]));
				sb2.append(s2.charAt(point[1]));
				
				if (s1.charAt(point[0])==s2.charAt(point[1]))
					sbb.append("|");
				else if (scoring.getFloat(point[0], point[1])>0)
					sbb.append("+");
				else
					sbb.append(" ");
	
				li=point[0]; lj=point[1];
			}
			
			if (appendUnaligned) {
				for (int a=point[0]+1; a<s1.length(); a++) {
					sb1.append(appendToLower?Character.toLowerCase(s1.charAt(a)):s1.charAt(a));
					sb2.append(GAP);
					sbb.append(" ");
				}
				for (int a=point[1]+1; a<s2.length(); a++) {
					sb1.append(GAP);
					sb2.append(appendToLower?Character.toLowerCase(s2.charAt(a)):s2.charAt(a));
					sbb.append(" ");
				}
//			} else {
//				sb1.append(" ").append(alignment.getFirst(0)).append("-").append(alignment.getFirst(end-1));
//				sb2.append(" ").append(alignment.getSecond(0)).append("-").append(alignment.getSecond(end-1));
			}
		}
		
		if (width<0) {
			writer.write(sb1.toString());
			writer.write("\n");
			if (writeBetween) {
				writer.write(sbb.toString());
				writer.write("\n");
			}
			writer.write(sb2.toString());
			writer.write("\n");
			writer.flush();
		} else {
			for (int s=0; s<sb1.length();s+=width) {
				if (s>0) writer.write("\n");
				String after = StringUtils.repeat(' ', 1+Math.max(0, s+width-sb1.length()));
				writer.write(sb1.substring(s, Math.min(sb1.length(), s+width)));
				writer.write(after+(off1+Math.min(sb1.length(), s+width)-StringUtils.countChar(sb1.substring(0, Math.min(sb1.length(), s+width)), GAP)));
				writer.write("\n");
				if (writeBetween) {
					writer.write(sbb.substring(s, Math.min(sb1.length(), s+width)));
					writer.write("\n");
				}
				writer.write(sb2.substring(s, Math.min(sb1.length(), s+width)));
				writer.write(after+(off2+Math.min(sb2.length(), s+width)-StringUtils.countChar(sb2.substring(0, Math.min(sb2.length(), s+width)), GAP)));
				writer.write("\n");
				writer.flush();
			}
		}
		
	}
	
	public void format(Alignment alignment, Scoring<CharSequence> scoring, GapCostFunction gap, AlignmentMode mode, CharSequence s1, CharSequence s2, OutputStream out) throws IOException {
		format(alignment,scoring,gap,mode,s1,s2,new OutputStreamWriter(out));
	}
	
}
