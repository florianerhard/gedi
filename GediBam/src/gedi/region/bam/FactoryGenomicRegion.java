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
package gedi.region.bam;

import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.MissingInformationIntronInformation;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import htsjdk.samtools.SAMRecord;

import java.util.Arrays;
import java.util.function.BiFunction;

public class FactoryGenomicRegion extends ArrayGenomicRegion implements MissingInformationIntronInformation {

	private BamAlignedReadDataFactory factory;
	private boolean consistent;
	private int pairedEndIntron = -1;
	private int missingEnd = 0;
	
	
	/**
	 * Missingend is 0 if nothing is missing, -1 if left part is missing and 1 if right part is missing
	 * @param coords
	 * @param cumNumCond
	 * @param consistent
	 * @param ignoreVariation
	 * @param needReadNames
	 * @param pairedEndIntron
	 * @param missingEnd
	 */
	public FactoryGenomicRegion(int[] coords, int[] cumNumCond, boolean consistent, boolean ignoreVariation, boolean needReadNames, int pairedEndIntron, int missingEnd, boolean join, BiFunction<SAMRecord, SAMRecord, String> barcode) {
		super(coords);
		this.pairedEndIntron = pairedEndIntron;
		if (pairedEndIntron+1>=getNumParts())
			throw new RuntimeException();
		factory = new BamAlignedReadDataFactory(this,cumNumCond,ignoreVariation,needReadNames,join, barcode);
		factory.start();
		this.consistent = consistent;
		this.missingEnd = missingEnd;
	}
	
	public FactoryGenomicRegion(IntArrayList coords, int[] cumNumCond, boolean ignoreVariation, boolean needReadNames, boolean join, BiFunction<SAMRecord, SAMRecord, String> barcode) {
		super(coords);
		factory = new BamAlignedReadDataFactory(this,cumNumCond,ignoreVariation,needReadNames, join, barcode);
		factory.start();
	}
	
	public boolean isOverlapping() {
		return factory.isOverlapping();
	}
	
	public void setUseBlocks(int minIntronLength) {
		factory.setUseBlocks(minIntronLength);
	}
	

	public FactoryGenomicRegion add(SAMRecord b, int file) {
		factory.addRecord(b, file);
		return this;
	}
	
	public FactoryGenomicRegion add(SAMRecord a, SAMRecord b,int file) {
		factory.addRecord(a,b, file);
		return this;
	}
	
	public DefaultAlignedReadsData create() {
		if (factory.barcodeFun!=null)
			return factory.createBarcode();
		return factory.create();
	}
	
	
	public IntArrayList getReadIds(int distinct) {
		return factory.getReadIds(distinct);
	}
	
	public boolean isConsistent() {
		return consistent;
	}

	@Override
	public GenomicRegion[] getInformationGenomicRegions() {
		if (pairedEndIntron==-1) return new GenomicRegion[] {this};
		GenomicRegion[] re = new GenomicRegion[] {
				new ArrayGenomicRegion(Arrays.copyOfRange(getBoundaries(), 0, pairedEndIntron*2+2)), 
				new ArrayGenomicRegion(Arrays.copyOfRange(getBoundaries(), pairedEndIntron*2+2, getNumBoundaries()))};
		
//		System.out.println(super.toRegionString()+" = "+re[0].toRegionString()+" + "+re[1].toRegionString());
		return re;
	}
	
	@Override
	public boolean isMissingInformationIntron(int intronIndex) {
		return intronIndex==pairedEndIntron;
	}
	
	@Override
	public boolean isLeftPartMissing() {
		return missingEnd==-1;
	}
	@Override
	public boolean isRightPartMissing() {
		return missingEnd==1;
	}
	
	@Override
	public String toRegionString() {
		if (pairedEndIntron==-1) return super.toRegionString();
		GenomicRegion[] two = getInformationGenomicRegions();
		return two[0].toRegionString()+"#"+two[1].toRegionString();
	}
	
	@Override
	public String toString() {
		return toRegionString();
	}
	
	@Override
	public int hashCode() {
		int re = super.hashCode();
		re = 31 * re + pairedEndIntron;
		return re;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj)) return false;
		if (! (obj instanceof FactoryGenomicRegion))
			return false;
		FactoryGenomicRegion r = (FactoryGenomicRegion) obj;
		return r.pairedEndIntron==pairedEndIntron;
	}
	
}
