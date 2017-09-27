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
import gedi.util.io.randomaccess.BinaryWriter;

import java.io.IOException;

import cern.colt.bitvector.BitVector;


/**
 * Space efficient representation, no variations nor ids are allowed, in addition the count is either 0 or 1!
 * @author erhard
 *
 */
public class DigitalAlignedReadsData implements AlignedReadsData {
	
	BitVector count;

	public DigitalAlignedReadsData() {}

	
	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putCInt(count.size());
		for (int b=0; b<count.size(); b+=8)
			out.putByte((int)count.getLongFromTo(b, Math.min(b+7,count.size()-1)));
	}
	
	@Override
	public boolean hasWeights() {
		return false;
	}
	
	@Override
	public float getWeight(int distinct) {
		int m = getMultiplicity(distinct);
		if (m==0) return 1;
		return 1.0f/m;
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		int size = in.getCInt();
		count = new BitVector(size);
		for (int b=0; b<count.size(); b+=8)
			count.putLongFromTo(in.getByte(), b, Math.min(b+7,count.size()-1));
	}

	@Override
	public int getId(int distinct) {
		return -1;
	}
	
	@Override
	public int getDistinctSequences() {
		return 1;
	}

	@Override
	public int getNumConditions() {
		return count.size();
	}

	@Override
	public int getCount(int distinct, int condition) {
		return count.getQuick(condition)?1:0;
	}

	@Override
	public int getVariationCount(int distinct) {
		return 0;
	}
	
	@Override
	public boolean isVariationFromSecondRead(int distinct, int index) {
		return false;
	}

	@Override
	public boolean isMismatch(int distinct, int index) {
		return false;
	}

	@Override
	public int getMismatchPos(int distinct, int index) {
		return 0;
	}

	@Override
	public CharSequence getMismatchGenomic(int distinct, int index) {
		return null;
	}

	@Override
	public CharSequence getMismatchRead(int distinct, int index) {
		return null;
	}

	@Override
	public boolean isInsertion(int distinct, int index) {
		return false;
	}

	@Override
	public int getInsertionPos(int distinct, int index) {
		return 0;
	}

	@Override
	public CharSequence getInsertion(int distinct, int index) {
		return null;
	}

	@Override
	public boolean isDeletion(int distinct, int index) {
		return false;
	}

	@Override
	public int getDeletionPos(int distinct, int index) {
		return 0;
	}

	@Override
	public CharSequence getDeletion(int distinct, int index) {
		return null;
	}

	@Override
	public int getMultiplicity(int distinct) {
		return 0;
	}

	@Override
	public String toString() {
		return toString2();
	}



	@Override
	public boolean isSoftclip(int distinct, int index) {
		return false;
	}


	@Override
	public int getSoftclipPos(int distinct, int index) {
		return 0;
	}


	@Override
	public CharSequence getSoftclip(int distinct, int index) {
		return null;
	}



}
