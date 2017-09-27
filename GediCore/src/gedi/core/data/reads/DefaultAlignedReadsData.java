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

import gedi.util.dynamic.DynamicObject;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import gedi.util.sequence.DnaSequence;

import java.io.IOException;


public class DefaultAlignedReadsData implements AlignedReadsData, BinarySerializable {

	int[][] count;
	short[][] var;
	CharSequence[][] indels;
	int[] multiplicity;
	int[] ids;
	float[] weights;
	
	public DefaultAlignedReadsData() {}

	/**
	 * If data is a {@link DefaultAlignedReadsData}, this will be a shallow copy!
	 * @param data
	 */
	public DefaultAlignedReadsData(AlignedReadsData data) {
		
		if (data instanceof DefaultAlignedReadsData) {
			DefaultAlignedReadsData d = (DefaultAlignedReadsData)data;
			count = d.count;
			var = d.var;
			indels = d.indels;
			multiplicity = d.multiplicity;
			ids = d.ids;
			weights = d.weights;
		} else {
			int distinct = data.getDistinctSequences();
			int condition = data.getNumConditions();
			
			count = new int[distinct][condition];
			var = new short[distinct][];
			indels = new CharSequence[distinct][];
			multiplicity = new int[distinct];
			
			for (int i=0; i<count.length; i++) {
				count[i] = new int[condition];
				for (int j=0; j<condition; j++)
					count[i][j] = data.getCount(i, j);
			}
			for (int i=0; i<count.length; i++) {
				var[i] = new short[data.getVariationCount(i)];
				indels[i] = new CharSequence[var[i].length];
				
				for (int v=0; v<var[i].length; v++) {
					if (data.isMismatch(i, v)) {
						var[i][v] = encodeMismatch(data.getMismatchPos(i, v), data.getMismatchGenomic(i, v).charAt(0), data.getMismatchRead(i, v).charAt(0), data.isVariationFromSecondRead(i, v));
						indels[i][v] = encodeMismatchIndel(data.getMismatchPos(i, v), data.getMismatchGenomic(i, v).charAt(0), data.getMismatchRead(i, v).charAt(0));
					}
					else if (data.isInsertion(i, v)) {
						var[i][v] = encodeInsertion(data.getInsertionPos(i, v), data.getInsertion(i, v), data.isVariationFromSecondRead(i, v));
						indels[i][v] = encodeInsertionIndel(data.getInsertionPos(i, v), data.getInsertion(i, v));
					}
					else if (data.isDeletion(i, v)) {
						var[i][v] = encodeDeletion(data.getDeletionPos(i, v), data.getDeletion(i, v), data.isVariationFromSecondRead(i, v));
						indels[i][v] = encodeDeletionIndel(data.getDeletionPos(i, v), data.getDeletion(i, v));
					} else if (isSoftclip(i, v)) {
						var[i][v] = DefaultAlignedReadsData.encodeSoftclip(getSoftclipPos(i, v), getSoftclip(i, v), data.isVariationFromSecondRead(i, v));
						indels[i][v] = DefaultAlignedReadsData.encodeSoftclipIndel(getSoftclipPos(i, v), getSoftclip(i, v));
					} else throw new RuntimeException("Neither mismatch nor insertion nor deletion!");
				}
			}
			for (int i=0; i<count.length; i++)
				multiplicity[i] = data.getMultiplicity(i);
			
			if (data.hasId()) {
				ids = new int[distinct];
				for (int i=0; i<ids.length; i++)
					ids[i] = data.getId(i);
			}
			
			if (data.hasWeights()) {
				weights = new float[distinct];
				for (int i=0; i<weights.length; i++)
					weights[i] = data.getWeight(i);
			}
		}
	}

	@Override
	public boolean hasWeights() {
		return weights!=null;
	}
	
	@Override
	public float getWeight(int distinct) {
		if (weights!=null)
			return weights[distinct];
		
		int m = getMultiplicity(distinct);
		if (m==0) return 1;
		return 1.0f/m;
	}
	
	@Override
	public boolean hasId() {
		return ids!=null;
	}

	@Override
	public int getId(int distinct) {
		return ids==null?-1:ids[distinct]; 
	}
//
//	@Override
//	public long getLongId(int distinct) {
//		return ids instanceof long[]?((int[])ids)[distinct]:getIntId(distinct);
//	}
//
//	@Override
//	public String getId(int distinct) {
//		return ids instanceof String[]?((String[])ids)[distinct]:getLongId(distinct)+"";
//	}

	
	
//	
//	@Override
//	public void serialize(BinaryWriter out) throws IOException {
//		out.putInt(count.length);// distinct
//		out.putInt(count[0].length);//conditions
//		
//		for (int i=0; i<count.length; i++)
//			FileUtils.writeIntArray(out, count[i]);
//		for (int i=0; i<count.length; i++)
//			FileUtils.writeShortArray(out, var[i]);
//		for (int i=0; i<indels.length; i++)
//			for (int j=0; j<indels[i].length; j++) {
//				out.putInt(indels[i][j].length());
//				out.putAsciiChars(indels[i][j]);
//			}
//		for (int i=0; i<count.length; i++)
//			out.putInt(multiplicity[i]);
//		
//	}
//
//	@Override
//	public void deserialize(BinaryReader in) throws IOException {
//		int distinct = in.getInt();// distinct
//		int condition = in.getInt();//conditions
//		
//		count = new int[distinct][condition];
//		var = new short[distinct][];
//		indels = new CharSequence[distinct][];
//		multiplicity = new int[distinct];
//		
//		for (int i=0; i<count.length; i++)
//			count[i] = FileUtils.readIntArray(in);
//		for (int i=0; i<count.length; i++) {
//			var[i] = FileUtils.readShortArray(in);
//			indels[i] = new CharSequence[var[i].length];
//		}
//		for (int i=0; i<count.length; i++) {
//			for (int j=0; j<var[i].length; j++) {
//				char[] ca = new char[in.getInt()];
//				for (int c=0; c<ca.length; c++)
//					ca[c] = in.getAsciiChar();
//				indels[i][j] = new DnaSequence(ca); 
//			}
//		}
//		
//		for (int i=0; i<count.length; i++)
//			multiplicity[i] = in.getInt();
//	}
	
	
	public double getDensity() {
		int n = 0;
		for (int i=0; i<count.length; i++)
			for (int j=0; j<count[i].length; j++)
				if (count[i][j]>0)
					n++;
		return (double)n/(count.length*count[0].length);
	}
	
	@Override
	public void deserialize(BinaryReader in) throws IOException {
		int d = in.getCInt();// distinct
		int c;
		
		DynamicObject gi = in.getContext().getGlobalInfo();
		if (!gi.hasProperty(CONDITIONSATTRIBUTE))
			c = in.getCInt();//conditions
		else
			c = gi.getEntry(CONDITIONSATTRIBUTE).asInt();
		
		count = new int[d][c];
		var = new short[d][];
		indels = new CharSequence[d][];
		multiplicity = new int[d];
		
		if (!gi.hasProperty(SPARSEATTRIBUTE) || gi.getEntry(SPARSEATTRIBUTE).asInt()==0) {
			for (int i=0; i<d; i++)
				for (int j=0; j<c; j++)
					count[i][j] = in.getCInt();
		}
		else {
			int co = in.getCInt();
			for (int i=0; i<co; i++) {
				int pos = in.getCInt();
				int count = in.getCInt();
				this.count[pos/c][pos%c] = count;
			}
			
		}
		for (int i=0; i<d; i++) {
			int v = in.getCInt();
			var[i] = new short[v];
			indels[i] = new CharSequence[v];
			for (int j=0; j<v; j++) {
				var[i][j] = in.getCShort();
				char[] ca = new char[in.getCInt()];
				for (int cr=0; cr<ca.length; cr++)
					ca[cr] = in.getAsciiChar();
				indels[i][j] = new DnaSequence(ca); 
			}
		}
		
		for (int i=0; i<d; i++)
			multiplicity[i] = in.getCInt();
		
		ids = null;
		if (in.getContext().getGlobalInfo().getEntry(AlignedReadsData.HASIDATTRIBUTE).asInt()==1) {
			ids = new int[d];
			for (int i=0; i<d; i++)
				ids[i] = in.getCInt();
		} 
		
		weights = null;
		if (in.getContext().getGlobalInfo().getEntry(AlignedReadsData.HASWEIGHTATTRIBUTE).asInt()==1) {
			weights = new float[d];
			if (d>0)
				for (int i=0; i<d; i++)
					weights[i] = in.getFloat();
			else
				weights[0] = 1;
		} 
		
	}
	
	private static void checkPositionEncoding(int pos) {
		if (pos>=1<<11) throw new RuntimeException("Cannot encode positions >"+(1<<11));
	}
	
	static short encodeMismatch(int pos, char genomic, char read, boolean secondRead) {
		checkPositionEncoding(pos);
		return (short) (TYPE_MISMATCH<<13 | (secondRead?1:0) << 12 | pos);
	}
	static CharSequence encodeMismatchIndel(int pos, char genomic, char read) {
		if (read=='N') read=genomic;
		return new DnaSequence(genomic, read);
	}
	
	static short encodeDeletion(int pos, CharSequence genomic, boolean secondRead) {
		checkPositionEncoding(pos);
		return (short) (TYPE_DELETION<<13 | (secondRead?1:0) << 12 | pos);
	}
	static CharSequence encodeDeletionIndel(int pos, CharSequence genomic) {
		return new DnaSequence(genomic);
	}
	
	static short encodeInsertion(int pos, CharSequence read, boolean secondRead) {
		checkPositionEncoding(pos);
		return (short) (TYPE_INSERTION<<13 | (secondRead?1:0) << 12 | pos);
	}
	static CharSequence encodeInsertionIndel(int pos, CharSequence read) {
		return new DnaSequence(read);
	}
	
	static short encodeSoftclip(int pos, CharSequence read, boolean secondRead) {
		checkPositionEncoding(pos);
		return (short) (TYPE_SOFTCLIP<<13 | (secondRead?1:0) << 12 | pos);
	}
	static CharSequence encodeSoftclipIndel(int pos, CharSequence read) {
		return new DnaSequence(read);
	}
	
	static boolean isSecondRead(short mask) {
		int smask = mask & 0xFFFF;
		return ((smask>>>12) & 1)==1;
	}
	
	
	private static short bytemask = (1<<12)-1;
	static int pos(int mask) {
		return mask & bytemask;
	}
	private static final int TYPE_MISMATCH = 0;
	private static final int TYPE_INSERTION = 1;
	private static final int TYPE_DELETION = 2;
	private static final int TYPE_SOFTCLIP = 3;
	
	static int type(short mask) {
		int smask = mask & 0xFFFF;
		return smask>>>13;
	}
	@Override
	public int getDistinctSequences() {
		return var.length;
	}
	@Override
	public int getNumConditions() {
		return count[0].length;
	}
	@Override
	public int getCount(int distinct, int condition) {
		return count[distinct][condition];
	}
	@Override
	public int getVariationCount(int distinct) {
		return var[distinct].length;
	}
	
	@Override
	public boolean isVariationFromSecondRead(int distinct, int index) {
		
		if (distinct>=var.length || index>=var[distinct].length)
			System.out.println(isMismatch(distinct, index));
		return isSecondRead(var[distinct][index]);
	}
	
	@Override
	public boolean isMismatch(int distinct, int index) {
		return type(var[distinct][index])==TYPE_MISMATCH;
	}
	@Override
	public int getMismatchPos(int distinct, int index) {
		return pos(var[distinct][index]);
	}
	@Override
	public CharSequence getMismatchGenomic(int distinct, int index) {
		return indels[distinct][index].subSequence(0,1);
	}
	@Override
	public CharSequence getMismatchRead(int distinct, int index) {
		return indels[distinct][index].subSequence(1, 2);
	}
	@Override
	public boolean isInsertion(int distinct, int index) {
		return type(var[distinct][index])==TYPE_INSERTION;
	}
	@Override
	public int getInsertionPos(int distinct, int index) {
		return pos(var[distinct][index]);
	}
	@Override
	public CharSequence getInsertion(int distinct, int index) {
		return indels[distinct][index];
	}
	@Override
	public boolean isDeletion(int distinct, int index) {
		return type(var[distinct][index])==TYPE_DELETION;
	}
	@Override
	public int getDeletionPos(int distinct, int index) {
		return pos(var[distinct][index]);
	}
	@Override
	public CharSequence getDeletion(int distinct, int index) {
		return indels[distinct][index];
	}
	
	@Override
	public boolean isSoftclip(int distinct, int index) {
		return type(var[distinct][index])==TYPE_SOFTCLIP;
	}
	
	@Override
	public int getSoftclipPos(int distinct, int index) {
		return pos(var[distinct][index]);
	}
	
	@Override
	public CharSequence getSoftclip(int distinct, int index) {
		return indels[distinct][index];
	}

	
	@Override
	public int getMultiplicity(int distinct) {
		return multiplicity[distinct];
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
