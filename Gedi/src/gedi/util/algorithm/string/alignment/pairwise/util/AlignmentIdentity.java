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

package gedi.util.algorithm.string.alignment.pairwise.util;

import java.lang.reflect.Array;

import gedi.util.algorithm.string.alignment.pairwise.Alignment;
import gedi.util.algorithm.string.alignment.pairwise.AlignmentMode;
import gedi.util.algorithm.string.alignment.pairwise.formatter.AlignmentFormatter;
import gedi.util.algorithm.string.alignment.pairwise.gapCostFunctions.GapCostFunction;
import gedi.util.algorithm.string.alignment.pairwise.scoring.Scoring;

public class AlignmentIdentity<A> implements AlignmentFormatter<A, Scoring<A>,Float> {

	private IdentityRelation<A> relation = new EqualsIdentityRelation<A>();
	
	
	public AlignmentIdentity() {}
	public AlignmentIdentity(IdentityRelation<A> relation) {
		this.relation = relation;
	}

	public void setRelation(IdentityRelation<A> relation) {
		this.relation = relation;
	}
	
	public IdentityRelation<A> getRelation() {
		return relation;
	}
	
	@Override
	public Float format(Alignment alignment, Scoring<A> scoring,
			GapCostFunction gap, AlignmentMode mode, A s1, A s2) {
		float meanLength = (scoring.getLength1()+scoring.getLength2())/2f;
		int[] point = new int[2];
		int numId = 0;
		for (int i=0; i<alignment.length(); i++) {
			alignment.getPoint(i, point);
			if (relation.isIdentical(scoring, s1, s2, point[0],point[1]))
				numId++;
		}
		float identity = numId/meanLength;
		return identity;
	}
	
	
	
	public static interface IdentityRelation<A> {
		boolean isIdentical(Scoring<A> scoring, A s1, A s2, int p1, int p2);
	}

	/**
	 * Subjects must be arrays
	 * @author erhard
	 *
	 * @param <A>
	 */
	public static class EqualsIdentityRelation<A> implements IdentityRelation<A> {
		@Override
		public boolean isIdentical(Scoring<A> scoring, A s1, A s2, int p1, int p2) {
			Object o1 = Array.get(s1, p1);
			Object o2 = Array.get(s2, p2);
			return o1.equals(o2);
		}
	}
	
	public static class CharSequenceIdentityRelation implements IdentityRelation<CharSequence> {
		@Override
		public boolean isIdentical(Scoring<CharSequence> scoring, CharSequence s1, CharSequence s2, int p1, int p2) {
			return s1.charAt(p1)==s2.charAt(p2);
		}
	}
	
	public static class ScoreThresholdIdentityRelation<A> implements IdentityRelation<A> {
		private float threshold;

		public ScoreThresholdIdentityRelation(float threshold) {
			this.threshold = threshold;
		}

		@Override
		public boolean isIdentical(Scoring<A> scoring, A s1, A s2, int p1, int p2) {
			return scoring.getFloat(p1, p2)>=threshold;
		}
		
		
	}
	
}
