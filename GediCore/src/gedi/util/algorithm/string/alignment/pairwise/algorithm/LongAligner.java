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

import gedi.util.algorithm.string.alignment.pairwise.AbstractAligner;
import gedi.util.algorithm.string.alignment.pairwise.Alignment;
import gedi.util.algorithm.string.alignment.pairwise.AlignmentMode;
import gedi.util.algorithm.string.alignment.pairwise.gapCostFunctions.AffineGapCostFunction;
import gedi.util.algorithm.string.alignment.pairwise.gapCostFunctions.InfiniteGapCostFunction;
import gedi.util.algorithm.string.alignment.pairwise.gapCostFunctions.LinearGapCostFunction;
import gedi.util.algorithm.string.alignment.pairwise.scoring.LongScoring;
import gedi.util.algorithm.string.alignment.pairwise.scoring.SubstitutionMatrix;



public class LongAligner<A> extends AbstractAligner<A,A> {

	private LongGotoh gotoh;
	private LongNeedlemanWunsch nw;
	private LongGapless gl;
	
	private long longGapOpen;
	private long longGapExtend;
	
	private LongScoring<A> scoring;
	
	
	public LongAligner(LongScoring<A> scoring, float gapOpen, float gapExtend, AlignmentMode mode) {
		this(scoring,gapOpen,gapExtend,Math.max(SubstitutionMatrix.inferPrecision(gapOpen),SubstitutionMatrix.inferPrecision(gapExtend)),mode);
	}
	
	public LongAligner(LongScoring<A> scoring, float gapOpen, float gapExtend, int precision, AlignmentMode mode) {
		super(scoring,new AffineGapCostFunction(gapOpen, gapExtend),mode);
		this.scoring = scoring;
		scoring.ensurePrecision(precision);
		
		if (gapOpen>0 || gapExtend>0)
			throw new RuntimeException("Positive gap scores are not allowed!");
		
		longGapOpen = scoring.correct(gapOpen);
		longGapExtend = scoring.correct(gapExtend);
		gotoh = new LongGotoh(scoring.getMaxLength());
	}
	

	public LongAligner(LongScoring<A> scoring, float gap, AlignmentMode mode) {
		this(scoring,gap,SubstitutionMatrix.inferPrecision(gap),mode);
	}
	
	public LongAligner(LongScoring<A> scoring, float gap, int precision, AlignmentMode mode) {
		super(scoring,new LinearGapCostFunction(gap),mode);
		this.scoring = scoring;
		scoring.ensurePrecision(precision);
		
		if (gap>0)
			throw new RuntimeException("Positive gap scores are not allowed!");
		
		longGapOpen = 0;
		longGapExtend = scoring.correct(gap);
		nw = new LongNeedlemanWunsch(scoring.getMaxLength());
	}
	
	
	public LongAligner(LongScoring<A> scoring, AlignmentMode mode) {
		super(scoring,new InfiniteGapCostFunction(),mode);
		this.scoring = scoring;
		
		longGapOpen = 0;
		longGapExtend = 0;
		gl = new LongGapless(scoring.getMaxLength());
	}
	
	@Override
	public float align() {
		if (gotoh!=null)
			return scoring.correct(gotoh.align(scoring, scoring.getLength1(), scoring.getLength2(), longGapOpen, longGapExtend, mode,null));
		else if (nw!=null)
			return scoring.correct(nw.align(scoring, scoring.getLength1(), scoring.getLength2(), longGapExtend, mode,null));
		else if (gl!=null)
			return scoring.correct(gl.align(scoring, scoring.getLength1(), scoring.getLength2(), mode,null));
		throw new RuntimeException();
	}
	
	@Override
	public float align(Alignment alignment) {
		if (gotoh!=null)
			return scoring.correct(gotoh.align(scoring, scoring.getLength1(), scoring.getLength2(), longGapOpen, longGapExtend, mode,alignment));
		else if (nw!=null)
			return scoring.correct(nw.align(scoring, scoring.getLength1(), scoring.getLength2(), longGapExtend, mode,alignment));
		else if (gl!=null)
			return scoring.correct(gl.align(scoring, scoring.getLength1(), scoring.getLength2(), mode,alignment));
			throw new RuntimeException();
	}
	
	
	public void writeMatrix(Writer out, char[] s1, char[] s2) throws IOException {
		if (gotoh!=null)
			gotoh.printMatrices(s1, s2, out);
		else if (nw!=null)
			nw.printMatrices(s1, s2, out);
		else if (gl!=null)
			gl.printMatrices(s1, s2, out);
		else
			throw new RuntimeException();

		out.flush();
	}
	
	public void writeLatex(Writer out, char[] s1, char[] s2) throws IOException {
		if (gotoh!=null)
			gotoh.printMatrices(s1, s2, out);
		else if (nw!=null)
			nw.printLatex(scoring,longGapExtend,mode,s1, s2, out);
		else if (gl!=null)
			gl.printMatrices(s1, s2, out);
		else
			throw new RuntimeException();

		out.flush();
	}
}
