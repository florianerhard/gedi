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
package gedi.util.algorithm.string.alignment.pairwise;

import java.io.IOException;

import gedi.util.algorithm.string.alignment.pairwise.formatter.AlignmentFormatter;
import gedi.util.algorithm.string.alignment.pairwise.gapCostFunctions.GapCostFunction;
import gedi.util.algorithm.string.alignment.pairwise.scoring.Scoring;


public abstract class AbstractAligner<A,P> implements Aligner<A> {

	protected Scoring<A> scoring;
	protected GapCostFunction gap;
	protected AlignmentMode mode;
	
	/**
	 * Last alignment given to alignCache
	 */
	protected Alignment alignment;
	protected A s1;
	protected A s2;
	
	public AbstractAligner(Scoring<A> scoring, GapCostFunction gap,
			AlignmentMode mode) {
		this.scoring = scoring;
		this.gap = gap;
		this.mode = mode;
	}
	
	/**
	 * Call this only if you called {@link #alignCache(String, String, Alignment)} first!
	 * @param <R>
	 * @param formatter
	 * @return
	 * @throws IOException
	 */
	public <R> R format(AlignmentFormatter<A, Scoring<A>,R> formatter) {
		return formatter.format(alignment, scoring, gap, mode, s1, s2);
	}

	@Override
	public float alignCache(String id1, String id2) {
		scoring.loadCachedSubjects(id1, id2);
		return align();
	}
	
	@Override
	public float alignCache(String id1, String id2,
			Alignment alignment) {
		scoring.loadCachedSubjects(id1, id2);
		this.alignment = alignment;
		s1 = scoring.getCachedSubject(id1);
		s2 = scoring.getCachedSubject(id2);
		return align(alignment);
	}

	@Override
	public float align(A s1, A s2) {
		scoring.setSubjects(s1, s2);
		alignment = null;
		this.s1 = s1;
		this.s2 = s2;
		return align();
	}
	
	@Override
	public float align(A s1, A s2, Alignment alignment) {
		scoring.setSubjects(s1, s2);
		this.alignment = alignment;
		this.s1 = s1;
		this.s2 = s2;
		return align(alignment);
	}

	@Override
	public GapCostFunction getGapCostFunction() {
		return gap;
	}
	@Override
	public Scoring<A> getScoring() {
		return scoring;
	}
	@Override
	public AlignmentMode getMode() {
		return mode;
	}
	public abstract float align();
	public abstract float align(Alignment alignment);

}
