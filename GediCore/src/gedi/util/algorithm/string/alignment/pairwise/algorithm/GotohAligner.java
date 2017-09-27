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

import gedi.util.algorithm.string.alignment.pairwise.AbstractAligner;
import gedi.util.algorithm.string.alignment.pairwise.Alignment;
import gedi.util.algorithm.string.alignment.pairwise.AlignmentMode;
import gedi.util.algorithm.string.alignment.pairwise.gapCostFunctions.AffineGapCostFunction;
import gedi.util.algorithm.string.alignment.pairwise.scoring.Scoring;



public class GotohAligner<A> extends AbstractAligner<A,A> {

	private FloatGotoh aligner = new FloatGotoh();
	
	private float gapOpen;
	private float gapExtend;
	
	
	public GotohAligner(Scoring<A> scoring, float gapOpen, float gapExtend, AlignmentMode mode) {
		super(scoring,new AffineGapCostFunction(gapOpen, gapExtend),mode);
		
		if (gapOpen>0 || gapExtend>0)
			throw new RuntimeException("Positive gap scores are not allowed!");
		
		this.gapOpen = gapOpen;
		this.gapExtend = gapExtend;
	}
	
	@Override
	public float align() {
		return aligner.align(scoring, scoring.getLength1(), scoring.getLength2(), gapOpen, gapExtend, mode);
	}
	
	@Override
	public float align(Alignment alignment) {
		return aligner.align(scoring, scoring.getLength1(), scoring.getLength2(), gapOpen, gapExtend, mode,alignment);
	}
	
	
}
