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

package gedi.core.data.reads;

import gedi.app.extension.GlobalInfoProvider;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.util.FunctorUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.dynamic.DynamicObject;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import gedi.util.math.stat.Ranking;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;


/**
 * <p>
 * Usually used as data for {@link GenomicRegion}. The GenomicRegion should indicate where a read originated, i.e. gaps due to biological reasons
 * like introns should be modelled using an intron in the GenomicRegion. The Variations here should be used to indicate gaps and mismatches due
 * to technical reasons.
 * </p>
 * 
 * <p>
 * Positions refer to the covered part of the genome sequence (not the alignment nor the read sequence) in 5' to 3' direction.
 * 
 * <p><pre>
 * Example:
 * Genome            AAAAAAAAAATTTTTTTTAAATTTTTTTTTTT-CCCCCCCCCCCCCAAAAAAAAAAAATTTTTTTTTTTTTTACCCCCCCCCCCCCCCCAAAAAAAAAAAAAA
 * Read 1 alignment            TTTTTTTT---TTTTTTTTTTTACCCCCCCCCCCCC------------TTTTTTTTTTTTTTGCCCCCCCCCCCCCCCC
 * Read 2 alignment            TTTTTTTT---TTTTTTTTTTT-CCCCCCCCCCCCC------------TTTTTTTTTTTTTTACCCCCCCCCCCCCCCC
 * Read 3 alignment            TTTTTTTTAAATTTTTTTTTTT-CCCCCCCCCCCCC------------TTTTTTTTTTTTTTACCCCCCCCCCCCCCCC
 * </pre></p>
 * 
 * <p>
 * The long gap in the read is an intron, so the {@link GenomicRegion} of the read should be 10-46|58-81 (assuming plus strand).
 * Here, we have 3 distinct sequences ({@link #getDistinctSequences()} is 3). 
 * The first has 3 variations ({@link #getVariationCount(int)} is 3 for index 0). The first is a deletion (isDeletion(0,0) is true) at position
 * 8 (getDeletionPos(0,0) is 8) with length 3 (getDeletionLength(0,0) is 3). The second is an insertion
 * at position 22 with sequence A  and the third is a mismatch at position 49 with genomic char A and read char G (positions refer to covered parts of the genome)
 * The second read has one variation (the deletion), the last none.
 * </p>
 * 
 * <p>
 * If reads are aligned to the negative genomic strand, everything is considered in 5' to 3' direction. I.e. in genomic direction, variation indices
 * are right-to-left and positions are alignmentendposition-genomic position (introns must also be considered)
 * </p>
 * 
 * <p>12/9/15: Each distinct sequence may have an integer id. If read from BinaryFile, having or not having an id is decided based on an attribte in the reader's context.
 * The returned id is -1 otherwise.
 * </p>
 * 
 * If N is sequenced, a A->A mismatch is stored (if the genomic base is an A)!
 * 
 * @author erhard
 *
 */
public interface AlignedReadsData extends BinarySerializable, GlobalInfoProvider {

	public static final String HASIDATTRIBUTE = "HASIDATTRIBUTE";
	public static final String HASWEIGHTATTRIBUTE = "HASWEIGHT";
	public static final String CONDITIONSATTRIBUTE = "CONDITIONS";
	public static final String SPARSEATTRIBUTE = "SPARSE";
	
	
	
	default DynamicObject getGlobalInfo() {
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
		map.put(HASIDATTRIBUTE, hasId()?1:0);
		map.put(HASWEIGHTATTRIBUTE, hasWeights()?1:0);
		map.put(CONDITIONSATTRIBUTE, getNumConditions());
		map.put(SPARSEATTRIBUTE, getNumConditions()>5?1:0);
		return DynamicObject.from(map);
	}
	
	/**
	 * If this read is only observed w/o mismatches, 1 is returned.
	 * @return
	 */
	int getDistinctSequences();
	int getNumConditions();	

	int getCount(int distinct, int condition);
	int getVariationCount(int distinct); 
	
	int getId(int distinct);

	float getWeight(int distinct);
	boolean hasWeights();
	
	boolean isMismatch(int distinct, int index);
	int getMismatchPos(int distinct, int index);
	CharSequence getMismatchGenomic(int distinct, int index);
	CharSequence getMismatchRead(int distinct, int index);

	boolean isSoftclip(int distinct, int index);
	int getSoftclipPos(int distinct, int index);
	/**
	 * This is a read sequence
	 * @param distinct
	 * @param index
	 * @return
	 */
	CharSequence getSoftclip(int distinct, int index);

	
	/**
	 * Gap in reference sequence
	 * @param distinct
	 * @param index
	 * @return
	 */
	boolean isInsertion(int distinct, int index);
	int getInsertionPos(int distinct, int index);
	
	/**
	 * This is a read sequence
	 * @param distinct
	 * @param index
	 * @return
	 */
	CharSequence getInsertion(int distinct, int index);

	boolean isDeletion(int distinct, int index);
	int getDeletionPos(int distinct, int index);
	/**
	 * This is a genomic sequence!
	 * @param distinct
	 * @param index
	 * @return
	 */
	CharSequence getDeletion(int distinct, int index);


	/**
	 * 0 means unknown
	 * @param distinct
	 * @return
	 */
	int getMultiplicity(int distinct);

	
	
	default boolean hasId() {
		return getId(0)>=0;
	}
	
	default boolean isAmbigousMapping(int distinct) {
		return getMultiplicity(distinct)!=1;
	}
	
	default int positionToGenomic(int pos, ReferenceSequence ref, GenomicRegion region) {
		if (ref.getStrand()==Strand.Minus)
			return region.map(region.getTotalLength()-1-pos);
		return region.map(pos);
	}
	
	
	default double getCount(int distinct, int condition, ReadCountMode mode) {
		return mode.computeCount(getCount(distinct, condition), getMultiplicity(distinct), getWeight(distinct));
	}
	
	default double getTotalCountForDistinct(int distinct, ReadCountMode mode) {
		double re = 0;
		for (int i=0; i<getNumConditions(); i++)
			re += getCount(distinct, i, mode);
		return re;
	}
	
	default double getTotalCountForCondition(int condition, ReadCountMode mode) {
		double re = 0;
		for (int i=0; i<getDistinctSequences(); i++)
			re += getCount(i, condition, mode);
		return re;
	}
	
	default double[] getCountsForDistinct(int distinct, ReadCountMode mode) {
		double[] re = new double[getNumConditions()];
		for (int i=0; i<re.length; i++)
			re[i] = getCount(distinct, i, mode);
		return re;
	}
	
	default int[] getCountsForDistinctInt(int distinct, ReadCountMode mode) {
		int[] re = new int[getNumConditions()];
		for (int i=0; i<re.length; i++)
			re[i] = getCountInt(distinct, i, mode);
		return re;
	}
	
	default int[] getCountsForDistinctFloor(int distinct, ReadCountMode mode) {
		int[] re = new int[getNumConditions()];
		for (int i=0; i<re.length; i++)
			re[i] = getCountFloor(distinct, i, mode);
		return re;
	}
	
	
	default double[] addCountsForDistinct(int distinct, double[] re, ReadCountMode mode) {
		if (re==null || re.length!=getNumConditions())
			re = new double[getNumConditions()];
		for (int i=0; i<re.length; i++)
			re[i] += getCount(distinct, i, mode);
		return re;
	}
	
	default int[] addCountsForDistinctInt(int distinct, int[] re, ReadCountMode mode) {
		if (re==null || re.length!=getNumConditions())
			re = new int[getNumConditions()];
		for (int i=0; i<re.length; i++)
			re[i] += getCountInt(distinct, i, mode);
		return re;
	}
	
	default int[] addCountsForDistinctFloor(int distinct, int[] re, ReadCountMode mode) {
		if (re==null || re.length!=getNumConditions())
			re = new int[getNumConditions()];
		for (int i=0; i<re.length; i++)
			re[i] += getCountFloor(distinct, i, mode);
		return re;
	}
	
	default NumericArray getCountsForDistinct(NumericArray re, int distinct, ReadCountMode mode) {
		if (re==null || re.length()!=getNumConditions() || re.getType().getType()!=mode.getType())
			re = NumericArray.createMemory(getNumConditions(), NumericArrayType.fromType(mode.getType()));
		
		for (int i=0; i<re.length(); i++)
			mode.getCount(re,i,getCount(distinct, i),getMultiplicity(distinct),getWeight(distinct));
		return re;
	}
	
	
	default double[] addCountsForCondition(int condition, double[] re, ReadCountMode mode) {
		if (re==null || re.length!=getDistinctSequences())
			re = new double[getDistinctSequences()];
		for (int i=0; i<re.length; i++)
			re[i] += getCount(i, condition, mode);
		return re;
	}
	
	default int[] addCountsForConditionInt(int condition, int[] re, ReadCountMode mode) {
		if (re==null || re.length!=getDistinctSequences())
			re = new int[getDistinctSequences()];
		for (int i=0; i<re.length; i++)
			re[i] += getCountInt(i, condition, mode);
		return re;
	}
	
	default int[] addCountsForConditionFloor(int condition, int[] re, ReadCountMode mode) {
		if (re==null || re.length!=getDistinctSequences())
			re = new int[getDistinctSequences()];
		for (int i=0; i<re.length; i++)
			re[i] += getCountFloor(i, condition, mode);
		return re;
	}
	
	default double[] getCountsForCondition(int condition, ReadCountMode mode) {
		double[] re = new double[getDistinctSequences()];
		for (int i=0; i<re.length; i++)
			re[i] = getCount(i, condition, mode);
		return re;
	}
	
	default int[] getCountsForConditionInt(int condition, ReadCountMode mode) {
		int[] re = new int[getDistinctSequences()];
		for (int i=0; i<re.length; i++)
			re[i] = getCountInt(i, condition, mode);
		return re;
	}
	
	default int[] getCountsForConditionFloor(int condition, ReadCountMode mode) {
		int[] re = new int[getDistinctSequences()];
		for (int i=0; i<re.length; i++)
			re[i] = getCountFloor(i, condition, mode);
		return re;
	}
	
	default NumericArray getCountsForCondition(NumericArray re, int condition, ReadCountMode mode) {
		if (re==null || re.length()!=getDistinctSequences() || re.getType().getType()!=mode.getType())
			re = NumericArray.createMemory(getDistinctSequences(), NumericArrayType.fromType(mode.getType()));
		
		for (int distinct=0; distinct<re.length(); distinct++)
			mode.getCount(re,condition,getCount(distinct, condition),getMultiplicity(distinct),getWeight(distinct));
		return re;
	}
	default NumericArray addCountsForCondition(NumericArray re, int condition, ReadCountMode mode) {
		if (re==null || re.length()!=getDistinctSequences() || re.getType().getType()!=mode.getType())
			re = NumericArray.createMemory(getDistinctSequences(), NumericArrayType.fromType(mode.getType()));
		
		for (int distinct=0; distinct<re.length(); distinct++)
			mode.addCount(re,condition,getCount(distinct, condition),getMultiplicity(distinct),getWeight(distinct));
		return re;
	}
	
	/**
	 * Gets the counts summed over all conditions for each distinct sequence
	 * @param mode
	 * @return
	 */
	default double[] getTotalCountsForDistincts(ReadCountMode mode) {
		double[] re = new double[getDistinctSequences()];
		for (int d=0; d<re.length; d++)
			for (int i=0; i<getNumConditions(); i++)
				re[d] += getCount(d, i, mode);
		return re;
	}
	
	/**
	 * Gets the counts summed over all distinct sequences for each condition
	 * @param mode
	 * @return
	 */
	default double[] getTotalCountsForConditions(ReadCountMode mode) {
		double[] re = new double[getNumConditions()];
		for (int c=0; c<getNumConditions(); c++)
			for (int i=0; i<getDistinctSequences(); i++)
				re[c] += getCount(i, c, mode);
		return re;
	}
	
	default double getTotalCountOverall(ReadCountMode mode) {
		double re = 0;
		for (int c=0; c<getNumConditions(); c++)
			for (int i=0; i<getDistinctSequences(); i++)
				re += getCount(i, c, mode);
		return re;
	}
	
	
	
	default int getCountInt(int distinct, int condition, ReadCountMode mode) {
		return mode.computeCountInt(getCount(distinct, condition), getMultiplicity(distinct), getWeight(distinct));
	}
	
	default int getTotalCountForDistinctInt(int distinct, ReadCountMode mode) {
		int re = 0;
		for (int i=0; i<getNumConditions(); i++)
			re += getCountInt(distinct, i, mode);
		return re;
	}
	
	default int getTotalCountForConditionInt(int condition, ReadCountMode mode) {
		int re = 0;
		for (int i=0; i<getDistinctSequences(); i++)
			re += getCountInt(i, condition, mode);
		return re;
	}
	
	
	/**
	 * Gets the counts summed over all conditions for each distinct sequence
	 * @param mode
	 * @return
	 */
	default int[] getTotalCountsForDistinctsInt(ReadCountMode mode) {
		int[] re = new int[getDistinctSequences()];
		for (int d=0; d<re.length; d++)
			for (int i=0; i<getNumConditions(); i++)
				re[d] += getCountInt(d, i, mode);
		return re;
	}
	
	
	/**
	 * Gets the counts summed over all distinct sequences for each condition
	 * @param mode
	 * @return
	 */
	default int[] getTotalCountsForConditionsInt(ReadCountMode mode) {
		int[] re = new int[getNumConditions()];
		for (int c=0; c<getNumConditions(); c++)
			for (int i=0; i<getDistinctSequences(); i++)
				re[c] += getCountInt(i, c, mode);
		return re;
	}
	
	
	default int getTotalCountOverallInt(ReadCountMode mode) {
		int re = 0;
		for (int c=0; c<getNumConditions(); c++)
			for (int i=0; i<getDistinctSequences(); i++)
				re += getCountInt(i, c, mode);
		return re;
	}
	
	
	
	default int getCountFloor(int distinct, int condition, ReadCountMode mode) {
		return mode.computeCountFloor(getCount(distinct, condition), getMultiplicity(distinct), getWeight(distinct));
	}
	
	default int getTotalCountForDistinctFloor(int distinct, ReadCountMode mode) {
		int re = 0;
		for (int i=0; i<getNumConditions(); i++)
			re += getCountFloor(distinct, i, mode);
		return re;
	}
	
	default int getTotalCountForConditionFloor(int condition, ReadCountMode mode) {
		int re = 0;
		for (int i=0; i<getDistinctSequences(); i++)
			re += getCountFloor(i, condition, mode);
		return re;
	}
	
	/**
	 * Gets the counts summed over all conditions for each distinct sequence
	 * @param mode
	 * @return
	 */
	default int[] getTotalCountsForDistinctsFloor(ReadCountMode mode) {
		int[] re = new int[getDistinctSequences()];
		for (int d=0; d<re.length; d++)
			for (int i=0; i<getNumConditions(); i++)
				re[d] += getCountFloor(d, i, mode);
		return re;
	}
	
	
	/**
	 * Gets the counts summed over all distinct sequences for each condition
	 * @param mode
	 * @return
	 */
	default int[] getTotalCountsForConditionsFloor(ReadCountMode mode) {
		int[] re = new int[getNumConditions()];
		for (int c=0; c<getNumConditions(); c++)
			for (int i=0; i<getDistinctSequences(); i++)
				re[c] += getCountFloor(i, c, mode);
		return re;
	}
	
	
	default int getTotalCountOverallFloor(ReadCountMode mode) {
		int re = 0;
		for (int c=0; c<getNumConditions(); c++)
			for (int i=0; i<getDistinctSequences(); i++)
				re += getCountFloor(i, c, mode);
		return re;
	}
	
	/**
	 * Gets the counts summed over all conditions for each distinct sequence
	 * 
	 * Counts are added to re; re can be null; if re.length or its type do not match, a new one is created
	 * @param re
	 * @param mode
	 * @return
	 */
	default NumericArray getTotalCountsForDistincts(NumericArray re, ReadCountMode mode) {
		if (re==null || re.length()!=getDistinctSequences() || re.getType().getType()!=mode.getType())
			re = NumericArray.createMemory(getDistinctSequences(), NumericArrayType.fromType(mode.getType()));
		
		for (int i=0; i<getDistinctSequences(); i++)
			for (int c=0; c<getNumConditions(); c++)
				mode.getCount(re,i,getCount(i, c),getMultiplicity(i),getWeight(i));
		return re;
	}
	
	default NumericArray addTotalCountsForDistincts(NumericArray re, ReadCountMode mode) {
		if (re==null || re.length()!=getDistinctSequences() || re.getType().getType()!=mode.getType())
			re = NumericArray.createMemory(getDistinctSequences(), NumericArrayType.fromType(mode.getType()));
		
		for (int i=0; i<getDistinctSequences(); i++)
			for (int c=0; c<getNumConditions(); c++)
				mode.addCount(re,i,getCount(i, c),getMultiplicity(i),getWeight(i));
		return re;
	}
	
	
	/**
	 * 
	 * Gets the counts summed over all distinct sequences for each condition.
	 * 
	 * Counts are added to re; re can be null; if re.length or its type do not match, a new one is created
	 * @param re
	 * @param mode
	 * @return
	 */
	default NumericArray getTotalCountsForConditions(NumericArray re, ReadCountMode mode) {
		if (re==null || re.length()!=getNumConditions() || re.getType().getType()!=mode.getType())
			re = NumericArray.createMemory(getNumConditions(), NumericArrayType.fromType(mode.getType()));
	
		for (int c=0; c<getNumConditions(); c++)
			for (int i=0; i<getDistinctSequences(); i++)
				mode.getCount(re,c,getCount(i, c),getMultiplicity(i),getWeight(i));
		return re;
	}
	
	default NumericArray addTotalCountsForConditions(NumericArray re, ReadCountMode mode) {
		if (re==null || re.length()!=getNumConditions() || re.getType().getType()!=mode.getType())
			re = NumericArray.createMemory(getNumConditions(), NumericArrayType.fromType(mode.getType()));
	
		for (int c=0; c<getNumConditions(); c++)
			for (int i=0; i<getDistinctSequences(); i++)
				mode.addCount(re,c,getCount(i, c),getMultiplicity(i),getWeight(i));
		return re;
	}
	
//	
//	
//	
//	default int getTotalCountUniqueMappings(int condition) {
//		int re = 0;
//		for (int i=0; i<getDistinctSequences(); i++)
//			if (getMultiplicity(i)==1)
//				re+=getCount(i, condition);
//		return re;
//	}
//	default double getTotalCountDivide(int condition) {
//		double re = 0;
//		for (int i=0; i<getDistinctSequences(); i++)
//			re+=(double)getCount(i, condition)/getMultiplicity(i);
//		return re;
//	}
//	default int getTotalCount(int condition) {
//		int re = 0;
//		for (int i=0; i<getDistinctSequences(); i++)
//			re+=getCount(i, condition);
//		return re;
//	}
//	
//
//	
//	default int[] getTotalCount(int[] re) {
//		if (re==null||re.length!=getNumConditions()) re = new int[getNumConditions()];
//		for (int i=0; i<getNumConditions(); i++)
//			re[i] = getTotalCount(i);
//		return re;
//	}
//	
//	default double[] getTotalCountDouble(double[] re) {
//		if (re==null||re.length!=getNumConditions()) re = new double[getNumConditions()];
//		for (int i=0; i<getNumConditions(); i++)
//			re[i] = getTotalCount(i);
//		return re;
//	}
//	
//	default int[] getCounts(int distinct, int[] re) {
//		if (re==null||re.length!=getNumConditions()) re = new int[getNumConditions()];
//		for (int i=0; i<getNumConditions(); i++)
//			re[i] = getCount(distinct,i);
//		return re;
//	}
//	
//	default int[] getCounts(int distinct) {
//		return getCounts(distinct,null);
//	}
//	default int[] getTotalCount() {
//		return getTotalCount(null);
//	}
//	default int[] addTotalCount(int[] re) {
//		if (re==null||re.length!=getNumConditions()) re = new int[getNumConditions()];
//		for (int i=0; i<getNumConditions(); i++)
//			re[i] += getTotalCount(i);
//		return re;
//	}
//	
//	default double[] addTotalCount(double[] re) {
//		if (re==null||re.length!=getNumConditions()) re = new double[getNumConditions()];
//		for (int i=0; i<getNumConditions(); i++)
//			re[i] += getTotalCount(i);
//		return re;
//	}
//	
//	default int[] addCount(int distinct, int[] re) {
//		if (re==null||re.length!=getNumConditions()) re = new int[getNumConditions()];
//		for (int i=0; i<getNumConditions(); i++)
//			re[i] += getCount(distinct, i);
//		return re;
//	}
//	
//	default double[] addCount(int distinct, double[] re, boolean divideByMulti) {
//		if (re==null||re.length!=getNumConditions()) re = new double[getNumConditions()];
//		for (int i=0; i<getNumConditions(); i++)
//			re[i] += (double)getCount(distinct, i)/(divideByMulti?getMultiplicity(distinct):1);
//		return re;
//	}
//	
//	default NumericArray addCount(int distinct, NumericArray re, boolean divideByMulti) {
//		if (re==null||re.length()!=getNumConditions()) re = NumericArray.createMemory(getNumConditions(), divideByMulti?NumericArrayType.Double:NumericArrayType.Integer);
//		for (int i=0; i<getNumConditions(); i++)
//			if (divideByMulti)
//				re.add(i, (double)getCount(distinct, i)/Math.max(1, getMultiplicity(distinct)));
//			else
//				re.add(i,getCount(distinct,i));
//		return re;
//	}
//	
//	
//	default NumericArray addSumCount(NumericArray re, boolean divideByMulti) {
//		for (int d=0; d<getDistinctSequences(); d++) 
//			re = addCount(d, re, divideByMulti);
//		return re;
//	}
//	
//	default NumericArray toNumericArray(boolean divideByMulti) {
//		return addSumCount(null, divideByMulti);
//	}
//	
//	default NumericArray addTotalCount(NumericArray re) {
//		if (re==null||re.length()!=getNumConditions()) re = NumericArray.createMemory(getNumConditions(),NumericArrayType.Integer);
//		for (int i=0; i<getNumConditions(); i++)
//			re.add(i,getTotalCount(i));
//		return re;
//	}
//	
//	default int getSumTotalCount() {
//		int re = 0;
//		for (int i=0; i<getNumConditions(); i++)
//			re += getTotalCount(i);
//		return re;
//	}
//	default int getSumTotalCountUniqueMappings() {
//		int re = 0;
//		for (int i=0; i<getNumConditions(); i++)
//			re += getTotalCountUniqueMappings(i);
//		return re;
//	}
//	default double getSumTotalCountDivide() {
//		double re = 0;
//		for (int i=0; i<getNumConditions(); i++)
//			re += getTotalCountDivide(i);
//		return re;
//	}
//	default int getSumCount(int distinct) {
//		int re = 0;
//		for (int i=0; i<getNumConditions(); i++)
//			re += getCount(distinct,i);
//		return re;
//	}
//	default double getSumCount(int distinct, boolean divideByMulti) {
//		double re = 0;
//		for (int i=0; i<getNumConditions(); i++)
//			re += (double)getCount(distinct,i)/(divideByMulti?getMultiplicity(distinct):1);
//		return re;
//	}
//
//	default int[] getTotalCountForDistinctSequence(int distinct, int[] re) {
//		if (re==null||re.length!=getNumConditions()) re = new int[getNumConditions()];
//		for (int i=0; i<getNumConditions(); i++)
//			re[i] = getCount(distinct,i);
//		return re;
//	}
//	default int[] getTotalCountForDistinctSequence(int distinct) {
//		return getTotalCountForDistinctSequence(distinct,null);
//	}

	/**
	 * Checks whether mismatches are consistent with the genomic sequence and inserts N if not.
	 * @param distinct
	 * @param genomic
	 * @return
	 */
	default CharSequence genomeToRead(int distinct, CharSequence genomic) {
		int c = getVariationCount(distinct);
		if (c==0) return genomic;
		int p = 0;
		StringBuilder sb = new StringBuilder();
		
		for (int v=0; v<c; v++) {
			if (isDeletion(distinct, v)) {
				int d = getDeletionPos(distinct, v);
				sb.append(genomic.subSequence(p, d));
				sb.append(getDeletion(distinct, v));
				p = d;
				throw new RuntimeException("Not tested!");
			} else if (isInsertion(distinct, v)) {
				int d = getInsertionPos(distinct, v);
				sb.append(genomic.subSequence(p, d));
				p = d+getInsertion(distinct, v).length();
				throw new RuntimeException("Not tested!");
			} else if (isMismatch(distinct, v)) {
				int d = getMismatchPos(distinct, v);
				sb.append(genomic.subSequence(p, d));
				if (getMismatchGenomic(distinct, v).charAt(0)==genomic.charAt(d))
					sb.append(getMismatchRead(distinct, v));
				else
					sb.append('N');
				p = d+1;
			} else if (isSoftclip(distinct, v)) {
				int d = getSoftclipPos(distinct, v);
				sb.append(getSoftclip(distinct, v));
				p = d;
				throw new RuntimeException("Not tested!");
			}
		}
		sb.append(genomic.subSequence(p,genomic.length()));
		
		return sb.toString();
	}
	
	default AlignedReadsVariation getVariation(int distinct, int index) {
		if (isMismatch(distinct, index)) return new AlignedReadsMismatch(getMismatchPos(distinct, index), getMismatchGenomic(distinct, index), getMismatchRead(distinct, index));
		if (isDeletion(distinct, index)) return new AlignedReadsDeletion(getDeletionPos(distinct, index), getDeletion(distinct, index));
		if (isInsertion(distinct, index)) return new AlignedReadsInsertion(getInsertionPos(distinct, index), getInsertion(distinct, index));
		if (isSoftclip(distinct, index)) return new AlignedReadsSoftclip(getSoftclipPos(distinct, index), getSoftclip(distinct, index));
		return null;
	}

	default AlignedReadsVariation[] getVariations(int distinct) {
		AlignedReadsVariation[] re = new AlignedReadsVariation[getVariationCount(distinct)];
		for (int i=0; i<re.length; i++)
			re[i] = getVariation(distinct, i);
		return re;
	}
	
	default String toString2() {
		StringBuilder sb = new StringBuilder();
		for (int d=0; d<getDistinctSequences(); d++) {
			if (hasId()) 
				sb.append(getId(d)).append(": ");
			sb.append(Arrays.toString(getCountsForDistinctInt(d, ReadCountMode.All)));
			sb.append(" x");
			sb.append(getMultiplicity(d));
			if (hasWeights()) 
				sb.append(" (w=").append(String.format("%.2f", getWeight(d))).append(")");
			for (AlignedReadsVariation var : getVariations(d))
				sb.append("\t"+var);
			if (d<getDistinctSequences()-1) sb.append(" ~ ");
		}
		return sb.toString();
	}

	default int hashCode2() {
		int result = 1;
		for (int i=0; i<getDistinctSequences(); i++) {
			int cs = 1;
			for (int j=0; j<getNumConditions(); j++) {
				int e = getCount(i, j);
				
				int elementHash = (int)(e ^ (e>>> 32));
				cs = 31 * cs + elementHash;
			}
			result += cs;
			if (hasId())
				result += Integer.hashCode(getId(i))<<13;
			
			int vs = 1;
			for (int j=0; j<getVariationCount(i); j++) {
				int e = getVariation(i, j).hashCode();
				vs = e;
			}
			result+=31*vs;
		}
		return result;
	}
	
	default boolean equals(Object obj, boolean considerMultiplicity, boolean considerId) {
		return equals(obj,considerMultiplicity,considerId,0);
	}
	default boolean equals(Object obj, boolean considerMultiplicity, boolean considerId, double countTolerance) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof AlignedReadsData))
			return false;
		AlignedReadsData other = (AlignedReadsData) obj;
		
		if (getNumConditions()!=other.getNumConditions()) return false;
		if (getDistinctSequences()!=other.getDistinctSequences()) return false;
		
		AlignedReadsVariation[][] a = new AlignedReadsVariation[getDistinctSequences()][];
		AlignedReadsVariation[][] b = new AlignedReadsVariation[getDistinctSequences()][];
		for (int i=0; i<a.length; i++) {
			a[i] = getVariations(i);
			b[i] = other.getVariations(i);
			Arrays.sort(a[i]);
			Arrays.sort(b[i]);
		}
		Ranking<AlignedReadsVariation[]> ranka = new Ranking<AlignedReadsVariation[]>(a,FunctorUtils.arrayComparator()).sort(true);
		Ranking<AlignedReadsVariation[]> rankb = new Ranking<AlignedReadsVariation[]>(b,FunctorUtils.arrayComparator()).sort(true);
		for (int i=0; i<a.length; i++) {
			if (getVariationCount(ranka.getOriginalIndex(i))!=other.getVariationCount(rankb.getOriginalIndex(i))) return false;
			if (considerMultiplicity && getMultiplicity(ranka.getOriginalIndex(i))!=other.getMultiplicity(rankb.getOriginalIndex(i))) return false;
			if (considerId && getId(ranka.getOriginalIndex(i))!=other.getId(rankb.getOriginalIndex(i))) return false;
			if (countTolerance==0) {
				for (int j=0; j<getNumConditions(); j++)
					if (getCount(ranka.getOriginalIndex(i),j)!=other.getCount(rankb.getOriginalIndex(i),j)) return false;
			} else {
				for (int j=0; j<getNumConditions(); j++){
					int ca = getCount(ranka.getOriginalIndex(i),j);
					int cb = other.getCount(rankb.getOriginalIndex(i),j);
					if (Math.abs(ca-cb)>Math.max(ca,cb)*countTolerance) return false;
				}
			}
			
			if (!Arrays.equals(a[i], b[i])) return false;
		}
		
		return true;
	}

	
//	static final byte INT_ID_MAGIC = 0;
//	static final byte LONG_ID_MAGIC = 1;
//	static final byte STRING_ID_MAGIC = 2;
//	static final byte NO_ID_MAGIC = 2;
	
	default void serialize(BinaryWriter out) throws IOException {
		int d = getDistinctSequences();
		int c = getNumConditions();
		
		out.putCInt(d);// distinct
		
		DynamicObject gi = out.getContext().getGlobalInfo();
		if (!gi.hasProperty(CONDITIONSATTRIBUTE))
			out.putCInt(c);//conditions
		
		if (!gi.hasProperty(SPARSEATTRIBUTE) || gi.getEntry(SPARSEATTRIBUTE).asInt()==0) {
			for (int i=0; i<d; i++)
				for (int j=0; j<c; j++)
					out.putCInt(getCount(i, j));
		}
		else {
			int co = 0;
			for (int i=0; i<d; i++)
				for (int j=0; j<c; j++) 
					if (getCount(i, j)>0)
						co++;
			out.putCInt(co);
			int ind = 0;
			for (int i=0; i<d; i++)
				for (int j=0; j<c; j++) {
					if (getCount(i, j)>0) {
						out.putCInt(ind);
						out.putCInt(getCount(i, j));
					}
					ind++;
				}
		}
		
		for (int i=0; i<d; i++) {
			int v = getVariationCount(i);
			out.putCInt(v);
			for (int j=0; j<v; j++) {
				CharSequence ch;
				if (isMismatch(i, j)) {
					out.putCShort(DefaultAlignedReadsData.encodeMismatch(getMismatchPos(i, j), getMismatchGenomic(i, j).charAt(0), getMismatchRead(i, j).charAt(0)));
					ch = DefaultAlignedReadsData.encodeMismatchIndel(getMismatchPos(i, j), getMismatchGenomic(i, j).charAt(0), getMismatchRead(i, j).charAt(0));
				} else if (isDeletion(i, j)) {
					out.putCShort(DefaultAlignedReadsData.encodeDeletion(getDeletionPos(i, j), getDeletion(i, j)));
					ch = DefaultAlignedReadsData.encodeDeletionIndel(getDeletionPos(i, j), getDeletion(i, j));
				} else if (isInsertion(i, j)) {
					out.putCShort(DefaultAlignedReadsData.encodeInsertion(getInsertionPos(i, j), getInsertion(i, j)));
					ch = DefaultAlignedReadsData.encodeInsertionIndel(getInsertionPos(i, j), getInsertion(i, j));
				} else if (isSoftclip(i, j)) {
					out.putCShort(DefaultAlignedReadsData.encodeSoftclip(getSoftclipPos(i, j), getSoftclip(i, j)));
					ch = DefaultAlignedReadsData.encodeSoftclipIndel(getSoftclipPos(i, j), getSoftclip(i, j));
				} else throw new RuntimeException("Neither mismatch nor deletion nor insertion!");
				out.putCInt(ch.length());
				out.putAsciiChars(ch);
			}
		}
			
		for (int i=0; i<d; i++)
			out.putCInt(getMultiplicity(i));
		
		
		if (out.getContext().getGlobalInfo().getEntry(AlignedReadsData.HASIDATTRIBUTE).asInt()==1) {
			for (int i=0; i<d; i++)
				out.putCInt(getId(i));
		} 
		
		if (out.getContext().getGlobalInfo().getEntry(AlignedReadsData.HASWEIGHTATTRIBUTE).asInt()==1) {
			for (int i=0; i<d; i++)
				out.putFloat(getWeight(i));
		} 
		
//		if (hasIntId()) {
//			out.put(INT_ID_MAGIC);
//			for (int i=0; i<d; i++)
//				out.putCInt(getIntId(i));
//		}
//		else if (hasLongId()) {
//			out.put(LONG_ID_MAGIC);
//			for (int i=0; i<d; i++)
//				out.putCLong(getLongId(i));
//		}
//		else if (hasId()) {
//			out.put(STRING_ID_MAGIC);
//			for (int i=0; i<d; i++)
//				out.putString(getId(i));
//		}
//		else
//			out.put(NO_ID_MAGIC);

		
	}
	default int getNumConditionsWithCounts() {
		int re = 0;
		for (int i=0; i<getNumConditions(); i++)
			if (getTotalCountForCondition(i, ReadCountMode.All)>0)
				re++;
		return re;
	}

	default boolean isAnyAmbigousMapping() {
		for (int i=0; i<getDistinctSequences(); i++)
			if (isAmbigousMapping(i)) return true;
		return false;
	}
	
	default boolean isAnyUniqueMapping() {
		for (int i=0; i<getDistinctSequences(); i++)
			if (!isAmbigousMapping(i)) return true;
		return false;
	}

	public static String[] getConditionNames(GenomicRegionStorage<AlignedReadsData> reads) {
		int numCond = reads.getRandomRecord().getNumConditions();
		String[] conditions = new String[numCond];
		if (reads.getMetaData().isNull()) {
			for (int c=0; c<conditions.length; c++)
				conditions[c] = c+"";
		} else {
			for (int c=0; c<conditions.length; c++) {
				conditions[c] = reads.getMetaData().getEntry("conditions").getEntry(c).getEntry("name").asString();
				if ("null".equals(conditions[c])) conditions[c] = c+"";
			}
		}
		return conditions;
	}
	
	
}

