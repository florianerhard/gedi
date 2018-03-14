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


/**
 * CIGAR and MD(or supplied sequence) are used to infer variations; NH is used to infer multiplicity (-2 if not present); XR is used as the (comma-separated conditions) read count (1 if not present)
 * @author erhard
 *
 */
//public class BamAlignedReadsData implements AlignedReadsData {
//	
//	private int[] cumNumCond;
//	private SAMRecord[][] records;
//	private Cigar[] cigars;
//	private String[] seq;
//	private CharSequence genomicSequence;
//	private boolean minusStrand;
//	
//	public BamAlignedReadsData(SAMRecord[][] records, int[] cumNumCond, boolean minusStrand) {
//		this(records,cumNumCond,minusStrand,null);
//	}
//	/**
//	 * GenomicSequence of the alignment in 5' to 3' direction. If this is data to a GenomicRegion, it should be exactly the part of the genome corresponding to the GenomicRegion.
//	 * @param records
//	 * @param genomicSequence
//	 */
//	public BamAlignedReadsData(SAMRecord[][] records, int[] cumNumCond, boolean minusStrand, CharSequence genomicSequence) {
//		if (records==null) throw new IllegalArgumentException("Records may not be null!");
//		if (records.length!=cumNumCond.length)  throw new IllegalArgumentException("Records and condition counts do not match!");
//		
//		
//		this.cumNumCond = cumNumCond;
//		this.genomicSequence = genomicSequence;
//		this.minusStrand = minusStrand;
//		this.records = records;
//		HashMap<String,SAMRecord> cigar = new HashMap<String, SAMRecord>();
//		for (int i=0; i<records.length; i++)
//			if (records[i]!=null) {
//				for (SAMRecord r : records[i]) {
//					if (r.getReadPairedFlag())
//						throw new RuntimeException("Paired reads not supported without convert!");
//					if (r.getStringAttribute("XV")!=null)
//						throw new RuntimeException("XV tag not supported without convert!");
//					
//					cigar.put(r.getCigarString()+"#"+r.getReadString(),r);
//				}
//			}
//		
//		cigars = new Cigar[cigar.size()];
//		seq = new String[cigar.size()];
//		int index = 0;
//		for (SAMRecord r : cigar.values()) {
//			cigars[index] = r.getCigar();
//			seq[index++] = r.getReadString();
//		}
//		
//	}
//
//	
//	@Override
//	public void deserialize(BinaryReader in) throws IOException {
//		throw new UnsupportedOperationException();
//	}
//	
//	@Override
//	public int getDistinctSequences() {
//		return cigars.length;
//	}
//
//	@Override
//	public int getNumConditions() {
//		return cumNumCond[cumNumCond.length-1];
//	}
//	
//	private int getRecordIndex(int condition) {
//		if (cumNumCond[cumNumCond.length-1]==cumNumCond.length) return condition;
//		int p = Arrays.binarySearch(cumNumCond, condition);
//		if (p>=0) return p;
//		return -p-1;
//	}
//	
//	private int getRecordXROffset(int condition) {
//		if (cumNumCond[cumNumCond.length-1]==cumNumCond.length) return 0;
//		int p = Arrays.binarySearch(cumNumCond, condition);
//		if (p>=0) return 0;
//		int below = p==-1?0:cumNumCond[-p-2];
//		return condition-below;
//	}
//
//	@Override
//	public int getCount(int distinct, int condition) {
//		int offset = getRecordXROffset(condition);
//		condition = getRecordIndex(condition);
//		int re = 0;
//		for (SAMRecord r : records[condition])
//			if (r.getCigar().equals(cigars[distinct]) && r.getReadString().equals(seq[distinct])) {
//				String c = r.getStringAttribute("XR");
//				if (c==null && offset>0) throw new RuntimeException("XR counts to not match BAM header descriptor");
//				if (c==null)
//					re++;
//				else
//					re+=Integer.parseInt(StringUtils.splitField(c,',',offset));
//			}
//		return re;
//	}
//	
//	private int getCoveredGenomicLength() {
//		int lr = 0;
//		for (CigarElement e : cigars[0].getCigarElements()) {
//			switch (e.getOperator()){
//			case D: 
//			case M: 
//				lr+=e.getLength();
//				break;
//			case N:
//			case S:
//			case I: break;
//			default: throw new IllegalArgumentException("Cigar operator "+e.getOperator()+" unknown!");
//			}
//		}
//		return lr;
//	}
//
//	@Override
//	public int getVariationCount(int distinct) {
//		int re = 0;
//		int ls = 0;
//		int lr = 0;
//		for (CigarElement e : cigars[distinct].getCigarElements()) {
//			switch (e.getOperator()){
//			case I: re++; ls+=e.getLength(); break;
//			case D: re++; lr+=e.getLength(); break;
//			case M: 
//				CharSequence read = getReadSequence(distinct,ls,ls+e.getLength());
//				CharSequence ref = getReferenceSequence(distinct,lr,lr+e.getLength());
//				ls+=e.getLength();
//				lr+=e.getLength();
//				re+=StringUtils.hamming(read, ref);
//				break;
//			case S:
//			case N: break;
//			default: throw new IllegalArgumentException("Cigar operator "+e.getOperator()+" unknown!");
//			}
//		}
//		return re;
//	}
//
//	private CharSequence getReferenceSequence(int distinct, int s, int e) {
//		SAMRecord r = getAnyRecord(distinct);
//		String md = r.getStringAttribute("MD");
//		if (md==null) {
//			if (genomicSequence==null) return StringUtils.repeatSequence('N', e-s);//throw new RuntimeException("No MD tag and no sequence given!");
//			return genomicSequence.subSequence(s, e);
//		}
//		return BamUtils.restoreSequence(r,false).substring(s, e);
//	}
//	private CharSequence getReadSequence(int distinct, int s, int e) {
//		SAMRecord r = getAnyRecord(distinct);
//		if (r.getReadString().length()==0 || r.getReadString().equals("*")) return StringUtils.repeatSequence('N', e-s);
//		return r.getReadString().substring(s,e);
//	}
//	
//	@Override
//	public boolean isMismatch(int distinct, int index) {
//		int re = 0;
//		int ls = 0;
//		int lr = 0;
//		if (minusStrand) index = getVariationCount(distinct)-1-index;
//		for (CigarElement e : cigars[distinct].getCigarElements()) {
//			switch (e.getOperator()){
//			case I: 
//				if (re++==index) return false; 
//				ls+=e.getLength(); 
//				break;
//			case D: 
//				if (re++==index) return false; 
//				lr+=e.getLength();
//				break;
//			case M: 
//				CharSequence read = getReadSequence(distinct,ls,ls+e.getLength());
//				CharSequence ref = getReferenceSequence(distinct,lr,lr+e.getLength());
//				ls+=e.getLength();
//				lr+=e.getLength();
//				re+=StringUtils.hamming(read, ref);
//				if (index<re) return true;
//				break;
//			case S:
//			case N: break;
//			default: throw new IllegalArgumentException("Cigar operator "+e.getOperator()+" unknown!");
//			}
//		}
//		throw new RuntimeException("Not possible");
//	}
//
//	@Override
//	public int getMismatchPos(int distinct, int index) {
//		int re = 0;
//		int ls = 0;
//		int lr = 0;
//		if (minusStrand) index = getVariationCount(distinct)-1-index;
//		for (CigarElement e : cigars[distinct].getCigarElements()) {
//			switch (e.getOperator()){
//			case I: 
//				re++; 
//				ls+=e.getLength(); 
//				break;
//			case D: 
//				re++; 
//				lr+=e.getLength();
//				break;
//			case M: 
//				CharSequence read = getReadSequence(distinct,ls,ls+e.getLength());
//				CharSequence ref = getReferenceSequence(distinct,lr,lr+e.getLength());
//				for (int i=0; i<read.length(); i++)
//					if (read.charAt(i)!=ref.charAt(i)) {
//						if (re++==index)
//							return (minusStrand?getCoveredGenomicLength()-1-lr-i:lr+i);
//					}
//				ls+=e.getLength();
//				lr+=e.getLength();
//				break;
//			case S:
//			case N: break;
//			default: throw new IllegalArgumentException("Cigar operator "+e.getOperator()+" unknown!");
//			}
//		}
//		throw new RuntimeException("Not possible");
//	}
//
//	@Override
//	public CharSequence getMismatchGenomic(int distinct, int index) {
//		int re = 0;
//		int ls = 0;
//		int lr = 0;
//		if (minusStrand) index = getVariationCount(distinct)-1-index;
//		for (CigarElement e : cigars[distinct].getCigarElements()) {
//			switch (e.getOperator()){
//			case I: 
//				re++; 
//				ls+=e.getLength(); 
//				break;
//			case D: 
//				re++; 
//				lr+=e.getLength();
//				break;
//			case M: 
//				CharSequence read = getReadSequence(distinct,ls,ls+e.getLength());
//				CharSequence ref = getReferenceSequence(distinct,lr,lr+e.getLength());
//				ls+=e.getLength();
//				lr+=e.getLength();
//				for (int i=0; i<read.length(); i++)
//					if (read.charAt(i)!=ref.charAt(i)) {
//						if (re++==index)
//							return minusStrand?SequenceUtils.getDnaReverseComplement(ref.subSequence(i,i+1)):ref.subSequence(i,i+1);
//					}
//				break;
//			case S:
//			case N: break;
//			default: throw new IllegalArgumentException("Cigar operator "+e.getOperator()+" unknown!");
//			}
//		}
//		throw new RuntimeException("Not possible");
//	}
//
//	@Override
//	public CharSequence getMismatchRead(int distinct, int index) {
//		int re = 0;
//		int ls = 0;
//		int lr = 0;
//		if (minusStrand) index = getVariationCount(distinct)-1-index;
//		for (CigarElement e : cigars[distinct].getCigarElements()) {
//			switch (e.getOperator()){
//			case I: 
//				re++; 
//				ls+=e.getLength(); 
//				break;
//			case D: 
//				re++; 
//				lr+=e.getLength();
//				break;
//			case M: 
//				CharSequence read = getReadSequence(distinct,ls,ls+e.getLength());
//				CharSequence ref = getReferenceSequence(distinct,lr,lr+e.getLength());
//				ls+=e.getLength();
//				lr+=e.getLength();
//				for (int i=0; i<read.length(); i++)
//					if (read.charAt(i)!=ref.charAt(i)) {
//						if (re++==index)
//							return minusStrand?SequenceUtils.getDnaReverseComplement(read.subSequence(i,i+1)):read.subSequence(i,i+1);
//					}
//				break;
//			case S:
//			case N: break;
//			default: throw new IllegalArgumentException("Cigar operator "+e.getOperator()+" unknown!");
//			}
//		}
//		throw new RuntimeException("Not possible");
//	}
//
//	@Override
//	public boolean isInsertion(int distinct, int index) {
//		int re = 0;
//		int ls = 0;
//		int lr = 0;
//		if (minusStrand) index = getVariationCount(distinct)-1-index;
//		for (CigarElement e : cigars[distinct].getCigarElements()) {
//			switch (e.getOperator()){
//			case I: 
//				if (re++==index) return true; 
//				ls+=e.getLength(); 
//				break;
//			case D: 
//				if (re++==index) return false; 
//				lr+=e.getLength();
//				break;
//			case M: 
//				CharSequence read = getReadSequence(distinct,ls,ls+e.getLength());
//				CharSequence ref = getReferenceSequence(distinct,lr,lr+e.getLength());
//				ls+=e.getLength();
//				lr+=e.getLength();
//				re+=StringUtils.hamming(read, ref);
//				if (index<re) return false;
//				break;
//			case S:
//			case N: break;
//			default: throw new IllegalArgumentException("Cigar operator "+e.getOperator()+" unknown!");
//			}
//		}
//		throw new RuntimeException("Not possible");
//	}
//
//	@Override
//	public int getInsertionPos(int distinct, int index) {
//		int re = 0;
//		int ls = 0;
//		int lr = 0;
//		if (minusStrand) index = getVariationCount(distinct)-1-index;
//		for (CigarElement e : cigars[distinct].getCigarElements()) {
//			switch (e.getOperator()){
//			case I: 
//				if (re++==index) return (minusStrand?getCoveredGenomicLength()-lr:lr); 
//				ls+=e.getLength(); 
//				break;
//			case D: 
//				re++; 
//				lr+=e.getLength();
//				break;
//			case M: 
//				CharSequence read = getReadSequence(distinct,ls,ls+e.getLength());
//				CharSequence ref = getReferenceSequence(distinct,lr,lr+e.getLength());
//				ls+=e.getLength();
//				lr+=e.getLength();
//				re+=StringUtils.hamming(read, ref);
//				break;
//			case S:
//			case N: break;
//			default: throw new IllegalArgumentException("Cigar operator "+e.getOperator()+" unknown!");
//			}
//		}
//		throw new RuntimeException("Not possible");
//	}
//
//	@Override
//	public CharSequence getInsertion(int distinct, int index) {
//		int re = 0;
//		int ls = 0;
//		int lr = 0;
//		if (minusStrand) index = getVariationCount(distinct)-1-index;
//		for (CigarElement e : cigars[distinct].getCigarElements()) {
//			switch (e.getOperator()){
//			case I: 
//				if (re++==index) return minusStrand?SequenceUtils.getDnaReverseComplement(getReadSequence(distinct,ls,ls+e.getLength())):getReadSequence(distinct,ls,ls+e.getLength()); 
//				ls+=e.getLength(); 
//				break;
//			case D: 
//				re++; 
//				lr+=e.getLength();
//				break;
//			case M: 
//				CharSequence read = getReadSequence(distinct,ls,ls+e.getLength());
//				CharSequence ref = getReferenceSequence(distinct,lr,lr+e.getLength());
//				ls+=e.getLength();
//				lr+=e.getLength();
//				re+=StringUtils.hamming(read, ref);
//				break;
//			case S:
//			case N: break;
//			default: throw new IllegalArgumentException("Cigar operator "+e.getOperator()+" unknown!");
//			}
//		}
//		throw new RuntimeException("Not possible");
//	}
//
//	@Override
//	public boolean isDeletion(int distinct, int index) {
//		int re = 0;
//		int ls = 0;
//		int lr = 0;
//		if (minusStrand) index = getVariationCount(distinct)-1-index;
//		for (CigarElement e : cigars[distinct].getCigarElements()) {
//			switch (e.getOperator()){
//			case I: 
//				if (re++==index) return false; 
//				ls+=e.getLength(); 
//				break;
//			case D: 
//				if (re++==index) return true; 
//				lr+=e.getLength();
//				break;
//			case M: 
//				CharSequence read = getReadSequence(distinct,ls,ls+e.getLength());
//				CharSequence ref = getReferenceSequence(distinct,lr,lr+e.getLength());
//				ls+=e.getLength();
//				lr+=e.getLength();
//				re+=StringUtils.hamming(read, ref);
//				if (index<re) return false;
//				break;
//			case S:
//			case N: break;
//			default: throw new IllegalArgumentException("Cigar operator "+e.getOperator()+" unknown!");
//			}
//		}
//		throw new RuntimeException("Not possible");
//	}
//
//	@Override
//	public int getDeletionPos(int distinct, int index) {
//		int re = 0;
//		int ls = 0;
//		int lr = 0;
//		if (minusStrand) index = getVariationCount(distinct)-1-index;
//		for (CigarElement e : cigars[distinct].getCigarElements()) {
//			switch (e.getOperator()){
//			case I: 
//				re++; 
//				ls+=e.getLength(); 
//				break;
//			case D: 
//				if (re++==index) return (minusStrand?getCoveredGenomicLength()-lr-e.getLength():lr); 
//				lr+=e.getLength();
//				break;
//			case M: 
//				CharSequence read = getReadSequence(distinct,ls,ls+e.getLength());
//				CharSequence ref = getReferenceSequence(distinct,lr,lr+e.getLength());
//				ls+=e.getLength();
//				lr+=e.getLength();
//				re+=StringUtils.hamming(read, ref);
//				break;
//			case S:
//			case N: break;
//			default: throw new IllegalArgumentException("Cigar operator "+e.getOperator()+" unknown!");
//			}
//		}
//		throw new RuntimeException("Not possible");
//	}
//
//	@Override
//	public CharSequence getDeletion(int distinct, int index) {
//		int re = 0;
//		int ls = 0;
//		int lr = 0;
//		if (minusStrand) index = getVariationCount(distinct)-1-index;
//		for (CigarElement e : cigars[distinct].getCigarElements()) {
//			switch (e.getOperator()){
//			case I: 
//				re++; 
//				ls+=e.getLength(); 
//				break;
//			case D: 
//				if (re++==index) return minusStrand?SequenceUtils.getDnaReverseComplement(getReferenceSequence(distinct,lr,lr+e.getLength())):getReferenceSequence(distinct,lr,lr+e.getLength());
//				lr+=e.getLength();
//				break;
//			case M: 
//				CharSequence read = getReadSequence(distinct,ls,ls+e.getLength());
//				CharSequence ref = getReferenceSequence(distinct,lr,lr+e.getLength());
//				ls+=e.getLength();
//				lr+=e.getLength();
//				re+=StringUtils.hamming(read, ref);
//				break;
//			case S:
//			case N: break;
//			default: throw new IllegalArgumentException("Cigar operator "+e.getOperator()+" unknown!");
//			}
//		}
//		throw new RuntimeException("Not possible");
//	}
//
//	@Override
//	public int getMultiplicity(int distinct) {
//		SAMRecord r = getAnyRecord(distinct);
//		if (r!=null) {
//			Integer m = r.getIntegerAttribute("NH");
//			if (m!=null) return m;
//		}
//		return 0;
//	}
//
//	private SAMRecord getAnyRecord(int distinct) {
//		for (int i=0; i<records.length; i++)
//			for (SAMRecord r : records[i])
//				if (r.getCigar().equals(cigars[distinct]) && r.getReadString().equals(seq[distinct])) {
//					return r;
//				}
//		
//		return null;
//	}
//	
//	
//	transient int hash = -1;
//	@Override
//	public int hashCode() {
//		if (hash==-1) hash = hashCode2();
//		return hash;
//	}
//	@Override
//	public boolean equals(Object obj) {
//		return equals2(obj);
//	}
//	
//	@Override
//	public String toString() {
//		return toString2();
//	}
//	
//	
//}
