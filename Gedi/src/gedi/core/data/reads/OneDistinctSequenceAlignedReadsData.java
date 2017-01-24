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

public class OneDistinctSequenceAlignedReadsData implements AlignedReadsData {

	private AlignedReadsData parent;
	private int distinct;
	
	public OneDistinctSequenceAlignedReadsData(AlignedReadsData parent,
			int distinct) {
		this.parent = parent;
		this.distinct = distinct;
	}
	
	public void setParent(AlignedReadsData parent) {
		this.parent = parent;
		hash = -1;
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
		if (distinct!=0) throw new IndexOutOfBoundsException();
		return parent.getWeight(this.distinct);
	}
	
	@Override
	public int getId(int distinct) {
		if (distinct!=0) throw new IndexOutOfBoundsException();
		return parent.getId(this.distinct);
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		throw new UnsupportedOperationException();
	}
	
	public void setDistinct(int distinct) {
		this.distinct = distinct;
		hash = -1;
	}

	@Override
	public int getDistinctSequences() {
		return 1;
	}

	@Override
	public int getNumConditions() {
		return parent.getNumConditions();
	}

	@Override
	public int getCount(int distinct, int condition) {
		if (distinct!=0) throw new IndexOutOfBoundsException();
		return parent.getCount(this.distinct, condition);
	}

	@Override
	public int getVariationCount(int distinct) {
		if (distinct!=0) throw new IndexOutOfBoundsException();
		return parent.getVariationCount(this.distinct);
	}

	@Override
	public boolean isMismatch(int distinct, int index) {
		if (distinct!=0) throw new IndexOutOfBoundsException();
		return parent.isMismatch(this.distinct, index);
	}

	@Override
	public int getMismatchPos(int distinct, int index) {
		if (distinct!=0) throw new IndexOutOfBoundsException();
		return parent.getMismatchPos(this.distinct, index);
	}

	@Override
	public CharSequence getMismatchGenomic(int distinct, int index) {
		if (distinct!=0) throw new IndexOutOfBoundsException();
		return parent.getMismatchGenomic(this.distinct, index);
	}

	@Override
	public CharSequence getMismatchRead(int distinct, int index) {
		if (distinct!=0) throw new IndexOutOfBoundsException();
		return parent.getMismatchRead(this.distinct, index);
	}

	@Override
	public boolean isInsertion(int distinct, int index) {
		if (distinct!=0) throw new IndexOutOfBoundsException();
		return parent.isInsertion(this.distinct, index);
	}

	@Override
	public int getInsertionPos(int distinct, int index) {
		if (distinct!=0) throw new IndexOutOfBoundsException();
		return parent.getInsertionPos(this.distinct, index);
	}

	@Override
	public CharSequence getInsertion(int distinct, int index) {
		if (distinct!=0) throw new IndexOutOfBoundsException();
		return parent.getInsertion(this.distinct, index);
	}

	@Override
	public boolean isDeletion(int distinct, int index) {
		if (distinct!=0) throw new IndexOutOfBoundsException();
		return parent.isDeletion(this.distinct, index);
	}

	@Override
	public int getDeletionPos(int distinct, int index) {
		if (distinct!=0) throw new IndexOutOfBoundsException();
		return parent.getDeletionPos(this.distinct, index);
	}

	@Override
	public CharSequence getDeletion(int distinct, int index) {
		if (distinct!=0) throw new IndexOutOfBoundsException();
		return parent.getDeletion(this.distinct, index);
	}
	
	@Override
	public boolean isSoftclip(int distinct, int index) {
		if (distinct!=0) throw new IndexOutOfBoundsException();
		return parent.isSoftclip(this.distinct, index);
	}

	@Override
	public CharSequence getSoftclip(int distinct, int index) {
		if (distinct!=0) throw new IndexOutOfBoundsException();
		return parent.getSoftclip(this.distinct, index);
	}

	@Override
	public int getSoftclipPos(int distinct, int index) {
		if (distinct!=0) throw new IndexOutOfBoundsException();
		return parent.getSoftclipPos(this.distinct, index);
	}	
	

	@Override
	public int getMultiplicity(int distinct) {
		if (distinct!=0) throw new IndexOutOfBoundsException();
		return parent.getMultiplicity(this.distinct);
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
