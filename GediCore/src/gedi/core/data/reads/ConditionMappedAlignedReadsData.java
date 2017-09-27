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

import gedi.util.io.randomaccess.BinaryReader;

import java.io.IOException;

public class ConditionMappedAlignedReadsData implements AlignedReadsData {

	private AlignedReadsData parent;
	private ContrastMapping mapping;
	
	public ConditionMappedAlignedReadsData(AlignedReadsData parent,
			ContrastMapping mapping) {
		this.parent = parent;
		this.mapping = mapping;
	}
	
	public void setParent(AlignedReadsData parent) {
		this.parent = parent;
	}
	
	public AlignedReadsData getParent() {
		return parent;
	}
	
	@Override
	public boolean hasWeights() {
		return parent.hasWeights();
	}
	
	@Override
	public float getWeight(int distinct) {
		return parent.getWeight(distinct);
	}
	
	@Override
	public boolean isVariationFromSecondRead(int distinct, int index) {
		return parent.isVariationFromSecondRead(distinct, index);
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getId(int distinct) {
		return parent.getId(distinct);
	}
	
	@Override
	public int getDistinctSequences() {
		return parent.getDistinctSequences();
	}

	@Override
	public int getNumConditions() {
		return mapping.getNumMergedConditions();
	}

	@Override
	public int getCount(int distinct, int condition) {
		int re = 0;
		for (int c : mapping.getMergeConditions(condition))
			re+=parent.getCount(distinct, c);
		return re;
	}

	@Override
	public int getVariationCount(int distinct) {
		return parent.getVariationCount(distinct);
	}

	@Override
	public boolean isMismatch(int distinct, int index) {
		return parent.isMismatch(distinct, index);
	}

	@Override
	public int getMismatchPos(int distinct, int index) {
		return parent.getMismatchPos(distinct, index);
	}

	@Override
	public CharSequence getMismatchGenomic(int distinct, int index) {
		return parent.getMismatchGenomic(distinct, index);
	}

	@Override
	public CharSequence getMismatchRead(int distinct, int index) {
		return parent.getMismatchRead(distinct, index);
	}

	@Override
	public boolean isInsertion(int distinct, int index) {
		return parent.isInsertion(distinct, index);
	}

	@Override
	public int getInsertionPos(int distinct, int index) {
		return parent.getInsertionPos(distinct, index);
	}

	@Override
	public CharSequence getInsertion(int distinct, int index) {
		return parent.getInsertion(distinct, index);
	}

	@Override
	public boolean isDeletion(int distinct, int index) {
		return parent.isDeletion(distinct, index);
	}

	@Override
	public int getDeletionPos(int distinct, int index) {
		return parent.getDeletionPos(distinct, index);
	}

	@Override
	public CharSequence getDeletion(int distinct, int index) {
		return parent.getDeletion(distinct, index);
	}
	
	@Override
	public boolean isSoftclip(int distinct, int index) {
		return parent.isSoftclip(distinct, index);
	}

	@Override
	public CharSequence getSoftclip(int distinct, int index) {
		return parent.getSoftclip(distinct, index);
	}

	@Override
	public int getSoftclipPos(int distinct, int index) {
		return parent.getSoftclipPos(distinct, index);
	}	
	
	@Override
	public int getMultiplicity(int distinct) {
		return parent.getMultiplicity(distinct);
	}

	transient int hash = -1;
	@Override
	public int hashCode() {
		if (hash==-1) hash = hashCode2();
		return hash;
	}
	@Override
	public boolean equals(Object obj) {
		return equals(obj,true,true);
	}
	
	@Override
	public String toString() {
		return toString2();
	}


	
	
	
}
