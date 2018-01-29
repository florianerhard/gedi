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

import gedi.bam.tools.BamUtils;
import gedi.core.data.reads.AlignedReadsDataFactory;
import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.region.GenomicRegion;
import gedi.util.FunctorUtils;
import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.sequence.MismatchString;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.SAMRecord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class BamAlignedReadDataFactory extends AlignedReadsDataFactory {

	private CharSequence genomicSequence;
	private HashMap<String,Integer> map = new HashMap<String, Integer>();
	private ArrayList<IntArrayList> distinctToIds = new ArrayList<IntArrayList>();
	private int[] cumNumCond;
	private GenomicRegion region;
	private boolean ignoreVariation;
	private boolean needReadNames;
	
	private int minIntronLength = -1;
	
	public BamAlignedReadDataFactory(GenomicRegion region, int[] cumNumCond, boolean ignoreVariation, boolean needReadNames) {
		super(cumNumCond[cumNumCond.length-1]);
		this.cumNumCond = cumNumCond;
		this.region = region;
		this.ignoreVariation = ignoreVariation;
		this.needReadNames = needReadNames;
	}
	
	public void setUseBlocks(int minIntronLength) {
		if (minIntronLength>=0 && !ignoreVariation) throw new RuntimeException("Cannot use blocks when recording variations!");
		this.minIntronLength = minIntronLength;
	}
	
	@Override
	public AlignedReadsDataFactory start() {
		genomicSequence = null;
		map.clear();
		return super.start();
	}
	
	public BamAlignedReadDataFactory setGenomicSequence(CharSequence genomicSequence) {
		this.genomicSequence = genomicSequence;
		return this;
	}
	
	public IntArrayList getReadIds(int distinct) {
		IntArrayList re = distinctToIds.get(distinct);
		re.sort();
		re.unique();
		return re;
	}
	
	public void addRecord(SAMRecord record, int file) {
		if (getCoveredGenomicLength(record)!=region.getTotalLength())
			throw new RuntimeException("Record and region do not match!");

		String key = getKey(record);
		Integer s = map.get(key);
		if (s==null) {
			// start a new distinct
			map.put(key, s = map.size());
			newDistinctSequence();
			
			Integer m = record.getIntegerAttribute("NH");
			if (m!=null) 
				setMultiplicity(s, m);
			else
				setMultiplicity(s, 0);
			
			if (!ignoreVariation)
				addVariations(record,record.getReadNegativeStrandFlag(),record.getReadNegativeStrandFlag(),0,getCurrentVariationBuffer());
			else
				setMultiplicity(s, 0);
			
			if (needReadNames)
				distinctToIds.add(new IntArrayList());
		}
		
		if (needReadNames) {
//			if (record.getReadPairedFlag() && record.getFirstOfPairFlag())
//				distinctToIds.get(s).add(record.getReadName()+BamUtils.FIRST_PAIR_SUFFIX);
//			else if (record.getReadPairedFlag() && record.getSecondOfPairFlag())
//				distinctToIds.get(s).add(record.getReadName()+BamUtils.SECOND_PAIR_SUFFIX);
//			else 
			if (!StringUtils.isInt(record.getReadName()))
				throw new RuntimeException("Can only keep integer read names!");
			
			distinctToIds.get(s).add(Integer.parseInt(record.getReadName()));
			
		}

		// handle counts
		int start = file==0?0:cumNumCond[file-1];
		int end = cumNumCond[file];
		
		String c = record.getAttribute("XR") instanceof String?record.getStringAttribute("XR"):null;
		if (c==null && end-start!=1) throw new RuntimeException("XR counts do not match BAM header descriptor");
		if (c==null)
			incrementCount(s, start, 1);
		else {
			String[] f = StringUtils.split(c,',');
			if (end-start!=f.length) throw new RuntimeException("XR counts do not match BAM header descriptor");
			for (int i=0; i<f.length; i++)
				incrementCount(s, start+i, Integer.parseInt(f[i]));
		}
		
		
	}

	@Override
	public DefaultAlignedReadsData create() {
		if (needReadNames) {
			for (int d=0; d<distinctToIds.size(); d++)
				setId(d, getReadIds(d).getInt(0));
		}
		return super.create();
	}
	
	private String getKey(SAMRecord record) {
		if (ignoreVariation) return "SAM";
		
		String xv = record.getStringAttribute("XV");
		String secondary = xv==null?record.getReadString():xv;
		
		return record.getCigarString()+secondary;
	}

	/**
	 * For mate pairs; The region of this must match the two records: intron consistent, contained, start with record1 and end with record2
	 * 
	 * If mappings overlap, variations are resolved optimistically (i.e. mismatch only in one mate pair -> to variation); if there are are 
	 * inconsistent variations, the variation of the first mate is taken.
	 * @param record1
	 * @param record2
	 * @param file
	 */
	public void addRecord(SAMRecord record1, SAMRecord record2, int file) {
//		if (record1.getAlignmentStart()>record2.getAlignmentStart() ||
//				(record1.getAlignmentStart()==record2.getAlignmentStart() && record1.getAlignmentEnd()>record2.getAlignmentEnd())
//				) {
//			SAMRecord d = record2;
//			record2 = record1;
//			record1 = d;
//		}
		// this would break the calculation of the mismatch positions; this was here for the key (now handled below!)
		
		GenomicRegion reg1 = minIntronLength>=0?BamUtils.getArrayGenomicRegionByBlocks(record1,minIntronLength):BamUtils.getArrayGenomicRegion(record1);
		GenomicRegion reg2 = minIntronLength>=0?BamUtils.getArrayGenomicRegionByBlocks(record2,minIntronLength):BamUtils.getArrayGenomicRegion(record2);
		
		if (!region.isIntronConsistent(reg1) || !region.isIntronConsistent(reg2) || !reg1.isIntronConsistent(reg2) 
				|| !region.contains(reg1) || !region.contains(reg2) )
//				|| region.induce(reg1.getStart())!=0 || region.induce(reg2.getStop())!=region.getTotalLength()-1)
			// can be for softclipped reads
			throw new RuntimeException("Record and regions do not match: \n"
					+ "Region: "+region+"\nRecord1: "+reg1+"\t"+record1.getSAMString()
					+"Record2: "+reg2+"\t"+record2.getSAMString());
		
		String key = getKey(record1)+getKey(record2);
		if (record1.getAlignmentStart()>record2.getAlignmentStart() ||
				(record1.getAlignmentStart()==record2.getAlignmentStart() && record1.getAlignmentEnd()>record2.getAlignmentEnd())
				) {
			key = getKey(record2)+getKey(record1);
		}
		
		Integer s = map.get(key);
		if (s==null) {
			// start a new distinct
			map.put(key, s = map.size());
			newDistinctSequence();
			if (needReadNames)
				distinctToIds.add(new IntArrayList());
			
			Integer m = record1.getIntegerAttribute("NH");
			if (m!=null) 
				setMultiplicity(s, m);
			else
				setMultiplicity(s, 0);
			
			if (reg1.intersects(reg2)) {
				
				GenomicRegion overlap = region.induce(reg1.intersect(reg2));
				if (overlap.getNumParts()!=1)
					throw new RuntimeException("Cannot be!");
				GenomicRegion off = region.induce(reg1.subtract(reg2));

				if (!ignoreVariation) {
					addVariations(record1,record1.getReadNegativeStrandFlag(),record1.getReadNegativeStrandFlag(),0,getCurrentVariationBuffer());
					addVariations(record2,record1.getReadNegativeStrandFlag(),record2.getReadNegativeStrandFlag(),off.getTotalLength(),getCurrentVariationBuffer());
					
					// to merge variations in the overlap;
					/*
					ArrayList<VarIndel> buffer1 = new ArrayList<VarIndel>();
					ArrayList<VarIndel> buffer2 = new ArrayList<VarIndel>();
					addVariations(record1,record1.getReadNegativeStrandFlag(),record1.getReadNegativeStrandFlag(),0,buffer1);
					addVariations(record2,record1.getReadNegativeStrandFlag(),record2.getReadNegativeStrandFlag(),off.getTotalLength(),buffer2);
					buffer1.sort(FunctorUtils.naturalComparator());
					buffer2.sort(FunctorUtils.naturalComparator());
					
					for (int i1=0, i2=0; i1<buffer1.size() || i2<buffer2.size(); ) {
						int p1 = i1<buffer1.size()?buffer1.get(i1).getPosition():Integer.MAX_VALUE;
						int p2 = i2<buffer2.size()?buffer2.get(i2).getPosition():Integer.MAX_VALUE;
						
						if (p1==p2) {
							getCurrentVariationBuffer().add(buffer1.get(i1));
							i1++; i2++;
						}
						else if (p1<p2) {
							if (!overlap.contains(p1))
								getCurrentVariationBuffer().add(buffer1.get(i1));
							i1++;
						}
						else {
							if (!overlap.contains(p2))
								getCurrentVariationBuffer().add(buffer2.get(i2));
							i2++;
						}
					}
					*/
				}
				else
					setMultiplicity(s, 0);
			} else {
				if (!ignoreVariation) {
					addVariations(record1,record1.getReadNegativeStrandFlag(),record1.getReadNegativeStrandFlag(),0,getCurrentVariationBuffer());
					addVariations(record2,record1.getReadNegativeStrandFlag(),record2.getReadNegativeStrandFlag(),reg1.getTotalLength(),getCurrentVariationBuffer());
				}
				else
					setMultiplicity(s, 0);
			}
			
		}

		if (!BamUtils.getPairId(record1).equals(BamUtils.getPairId(record2)))
				throw new RuntimeException("Read names do not match for mate pairs!");
		if (needReadNames) {
			if (!StringUtils.isInt(record1.getReadName()))
				throw new RuntimeException("Can only keep integer read names!");
			distinctToIds.get(s).add(Integer.parseInt(record1.getReadName()));
		}
		
		// handle counts
		int start = file==0?0:cumNumCond[file-1];
		int end = cumNumCond[file];
		
		String c = record1.getAttribute("XR") instanceof String?record1.getStringAttribute("XR"):null;
		if (c==null && end-start!=1) throw new RuntimeException("XR counts to not match BAM header descriptor");
		if (c==null)
			incrementCount(s, start, 1);
		else {
			String[] f = StringUtils.split(c,',');
			if (end-start!=f.length) throw new RuntimeException("XR counts to not match BAM header descriptor");
			for (int i=0; i<f.length; i++)
				incrementCount(s, start+i, Integer.parseInt(f[i]));
		}
		
		
	}
	
	
	/**
	 * Offset is for paired end reads to specify the position, where thes second records starts within the genomic region.
	 * @param record
	 * @param offset
	 */
	private void addVariations(SAMRecord record, boolean invertPos, boolean complement, int offset, Collection<VarIndel> to) {
		
		String xv = record.getStringAttribute("XV");
		if (xv!=null) {
			for (String v : StringUtils.split(xv, ','))
				to.add(createVarIndel(v));
			return;
		}
		
		int coveredGenomic = getCoveredGenomicLength(record);
		
		int pos;
		CharSequence vread;
		
		int ls = 0;
		int lr = 0;
		for (CigarElement e : record.getCigar().getCigarElements()) {
			switch (e.getOperator()){
			case I: 
				pos = (invertPos?coveredGenomic-lr:lr);
				vread = complement?SequenceUtils.getDnaReverseComplement(getReadSequence(record,ls,ls+e.getLength())):getReadSequence(record,ls,ls+e.getLength());;
				ls+=e.getLength(); 
				to.add(createInsertion(pos+offset, vread, record.getReadPairedFlag() && record.getSecondOfPairFlag()));
				break;
			case D:
				pos = (invertPos?coveredGenomic-lr-e.getLength():lr);
				vread = complement?SequenceUtils.getDnaReverseComplement(getReferenceSequence(record,lr,lr+e.getLength())):getReferenceSequence(record,lr,lr+e.getLength());;
				to.add(createDeletion(pos+offset, vread, record.getReadPairedFlag() && record.getSecondOfPairFlag()));
				lr+=e.getLength(); 
				break;
			case M: 
				CharSequence read = getReadSequence(record,ls,ls+e.getLength());
				CharSequence ref = getReferenceSequence(record,lr,lr+e.getLength());
				for (int i=0; i<read.length(); i++)
					if (read.charAt(i)!=ref.charAt(i)) {
						pos = (invertPos?coveredGenomic-1-lr-i:lr+i);
						char g = complement?SequenceUtils.getDnaComplement(ref.charAt(i)):ref.charAt(i);
						char r = complement?SequenceUtils.getDnaComplement(read.charAt(i)):read.charAt(i);
						to.add(createMismatch(pos+offset, g, r, record.getReadPairedFlag() && record.getSecondOfPairFlag()));
					}
				ls+=e.getLength();
				lr+=e.getLength();
				break;
			case S:
				pos = (invertPos?coveredGenomic-lr:lr);
				vread = complement?SequenceUtils.getDnaReverseComplement(getReadSequence(record,ls,ls+e.getLength())):getReadSequence(record,ls,ls+e.getLength());;
				to.add(createSoftclip(pos+offset, vread, record.getReadPairedFlag() && record.getSecondOfPairFlag()));
				
				ls+=e.getLength();
				
				break;
			case N: break;
			default: throw new IllegalArgumentException("Cigar operator "+e.getOperator()+" unknown!");
			}
		}
		
	}


	private int getCoveredGenomicLength(SAMRecord record) {
		int re = 0;
		for (CigarElement e : record.getCigar().getCigarElements()) {
			switch (e.getOperator()){
			case D: 
			case M: 
				re+=e.getLength();
				break;
			case N:
			case S:
			case I: 
				break;
			default: throw new IllegalArgumentException("Cigar operator "+e.getOperator()+" unknown!");
			}
		}
		return re;
	}

	private CharSequence getReferenceSequence(SAMRecord r, int s, int e) {
		String md = r.getStringAttribute("MD");
		if (md==null || r.getReadString().equals("*")) {
			
			
			if (genomicSequence==null) {
				if (md!=null) {
					MismatchString mm = new MismatchString(r.getStringAttribute("MD"));
					String seq = StringUtils.repeatSequence('N',mm.getGenomicLength()).toString();
					seq = mm.reconstitute(seq);
					return seq.substring(s, e);
				}
				return StringUtils.repeatSequence('N', e-s);
			}
			return genomicSequence.subSequence(s, e);
		}
		return BamUtils.restoreSequence(r,false).substring(s, e);
	}
	private CharSequence getReadSequence(SAMRecord r, int s, int e) {
		if (r.getReadString().length()==0 || r.getReadString().equals("*")) return StringUtils.repeatSequence('N', e-s);
		return r.getReadString().substring(s,e);
	}
	
	
	
}
