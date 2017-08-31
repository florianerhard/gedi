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

package gedi.bam.tools;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.sequence.SequenceProvider;
import gedi.region.bam.FactoryGenomicRegion;
import gedi.region.bam.RecordsGenomicRegion;
import gedi.region.bam.Strandness;
import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.sequence.MismatchString;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.Progress;
import htsjdk.samtools.AlignmentBlock;
import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceRecord;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class BamUtils {

	
	public static final String FIRST_PAIR_SUFFIX = "/1";
	public static final String SECOND_PAIR_SUFFIX = "/2";
	public static final ReferenceSequence SAMUNMAPPEDREFERENCE = Chromosome.obtain("Unmapped",true);
	
	
	public static IntArrayList getGenomicRegionCoordinates(SAMRecord r) {
		IntArrayList coords = new IntArrayList(4);
		int l = r.getAlignmentStart()-1;
		coords.add(l);
		List<CigarElement> ele = r.getCigar().getCigarElements();
		for (int i=0; i<ele.size(); i++) {
			CigarElement e = ele.get(i);
			CigarOperator op = e.getOperator();
			if (op==CigarOperator.N)
				coords.add(l);
			if (op==CigarOperator.N || op==CigarOperator.D || op==CigarOperator.M)
				l+=e.getLength();
			else if (op!=CigarOperator.I && op!=CigarOperator.S) // softclipped parts will not be part of the genomic region!
				throw new RuntimeException("CIGAR operator "+op+" not supported!");
			if (op==CigarOperator.N)
				coords.add(l);
		}
		coords.add(l);
		return coords;
	}

	
	public static IntArrayList getGenomicRegionCoordinatesByBlocks(SAMRecord r, int minIntronLength) {
		IntArrayList coords = new IntArrayList(4);
		for (AlignmentBlock b : r.getAlignmentBlocks()) {
			coords.add(b.getReferenceStart()-1);
			coords.add(b.getReferenceStart()-1+b.getLength());
		}
		for (int i=2; i<coords.size(); i+=2) {
			if (coords.getInt(i)-coords.getInt(i-1)<minIntronLength)
				coords.set(i-1,coords.getInt(i));
		}
		return coords;
	}

	
	public static RecordsGenomicRegion getRecordsGenomicRegion(SAMRecord r) {
		IntArrayList coords = getGenomicRegionCoordinates(r);
		RecordsGenomicRegion re = new RecordsGenomicRegion(coords, r);
		return re;
	}
	
	public static ArrayGenomicRegion getArrayGenomicRegion(SAMRecord r) {
		IntArrayList coords = getGenomicRegionCoordinates(r);
		ArrayGenomicRegion re = new ArrayGenomicRegion(coords);
		return re;
	}
	
	public static ArrayGenomicRegion getArrayGenomicRegionByBlocks(SAMRecord r, int minIntronLength) {
		IntArrayList coords = getGenomicRegionCoordinatesByBlocks(r, minIntronLength);
		ArrayGenomicRegion re = new ArrayGenomicRegion(coords);
		return re;
	}
	
	public static FactoryGenomicRegion getFactoryGenomicRegion(SAMRecord r, int[] cumNumCond, boolean ignoreVariation, boolean needReadNames) {
		return getFactoryGenomicRegion(r, cumNumCond, ignoreVariation, needReadNames, -1);
	}
		
		
	public static FactoryGenomicRegion getFactoryGenomicRegion(SAMRecord r, int[] cumNumCond, boolean ignoreVariation, boolean needReadNames, int minIntronLength) {
		IntArrayList coords = minIntronLength>=0?getGenomicRegionCoordinatesByBlocks(r, minIntronLength):getGenomicRegionCoordinates(r);
		FactoryGenomicRegion re = new FactoryGenomicRegion(coords, cumNumCond,ignoreVariation,needReadNames);
		re.setUseBlocks(minIntronLength);
		return re;
	}
	
	/**
	 * join is true-> the returned region will not contain a whole, when the two record do not overlap
	 * @param r1
	 * @param r2
	 * @param cumNumCoord
	 * @param join
	 * @return
	 */
	public static FactoryGenomicRegion getFactoryGenomicRegion(SAMRecord r1, SAMRecord r2, int[] cumNumCoord, boolean join, boolean throwOnNonconsistent, boolean ignoreVariation, boolean needReadNames) {
		return getFactoryGenomicRegion(r1, r2, cumNumCoord, join, throwOnNonconsistent, ignoreVariation, needReadNames, -1);
	}
	public static FactoryGenomicRegion getFactoryGenomicRegion(SAMRecord r1, SAMRecord r2, int[] cumNumCoord, boolean join, boolean throwOnNonconsistent, boolean ignoreVariation, boolean needReadNames, int minIntronLength) {
		if (r2==null){
			// missing information
			IntArrayList reg = minIntronLength>=0?getGenomicRegionCoordinatesByBlocks(r1, minIntronLength):getGenomicRegionCoordinates(r1);
			FactoryGenomicRegion re = new FactoryGenomicRegion(reg.toIntArray(), cumNumCoord, 
					true,ignoreVariation,needReadNames,-1,r1.getMateAlignmentStart()<r1.getAlignmentStart()?-1:1);
			return re;
		}
		if (r1.getAlignmentStart()>r2.getAlignmentStart()) {
			SAMRecord s = r1;
			r1 = r2;
			r2 = s;
		}
		IntArrayList coords1 = minIntronLength>=0?getGenomicRegionCoordinatesByBlocks(r1, minIntronLength):getGenomicRegionCoordinates(r1);
		IntArrayList coords2 = minIntronLength>=0?getGenomicRegionCoordinatesByBlocks(r2, minIntronLength):getGenomicRegionCoordinates(r2);
		GenomicRegion re1 = new ArrayGenomicRegion(coords1);
		GenomicRegion re2 = new ArrayGenomicRegion(coords2);
		boolean cons = re1.isIntronConsistent(re2);
		if (!cons && throwOnNonconsistent)
			throw new RuntimeException("Pair "+r1+" "+r2+" are not intron consistent:"+re1+" "+re2);
		
		int pairedEndIntron = -1;
		ArrayGenomicRegion reg = re1.union(re2);
		if (join && !re1.intersects(re2)) reg = reg.union(new ArrayGenomicRegion(
				Math.min(re1.getEnd(),re2.getEnd()),
				Math.max(re1.getStart(),re2.getStart())
				));
		else {
			pairedEndIntron = reg.getEnclosingPartIndex(re1.getStop());
			if (reg.getStop(pairedEndIntron)!=re1.getStop() || pairedEndIntron==reg.getNumParts()-1) 
				pairedEndIntron = -1;
			
			int r2p = re2.getEnclosingPartIndex(re1.getStop());
			if (r2p>=0 && re1.getStop()==re2.getStop(r2p))
				pairedEndIntron = -1;
		}
		
		FactoryGenomicRegion re = new FactoryGenomicRegion(reg.getBoundaries(), cumNumCoord, cons,ignoreVariation,needReadNames,pairedEndIntron,0);
		re.setUseBlocks(minIntronLength);
		return re;
	}
	
	public static String getPairId(SAMRecord rec) {
		String re = rec.getReadName();
		if (re.endsWith("/1") || re.endsWith("/2")) return re.substring(0, re.length()-2);
		return re;
	}

	
	public static boolean isValidStrand(Strand strand, Strandness strandness, SAMRecord first, SAMRecord second) {
		if (strand==Strand.Independent || strandness==Strandness.Unspecific) return true;
		if (strandness==Strandness.Specific) {
			if (first!=null)
				return first.getReadNegativeStrandFlag()==(strand==Strand.Minus);
			else
				return second.getReadNegativeStrandFlag()!=(strand==Strand.Minus);
		} else {
			if (first!=null)
				return first.getReadNegativeStrandFlag()!=(strand==Strand.Minus);
			else
				return second.getReadNegativeStrandFlag()==(strand==Strand.Minus);
		}
			
	}


	public static String getCigarString(GenomicRegion region, List<CigarElement> oldElements) {
		StringBuilder sb = new StringBuilder();
		int p = 0;
		for (CigarElement e : oldElements) {
			CigarOperator op = e.getOperator();

			if (e.getOperator()==CigarOperator.N) continue; // skip old introns
			
			if (op==CigarOperator.D) {
				sb.append(e.getLength());
				sb.append('D');
				p+=e.getLength();
			} else if(op==CigarOperator.M) {
				GenomicRegion m = new ArrayGenomicRegion(p,p+e.getLength());
				m = region.map(m);
				for (int i=0; i<m.getNumParts(); i++) {
					if (i>0) {
						sb.append(m.getIntronLength(i-1));
						sb.append('N');
					}
					sb.append(m.getLength(i));
					sb.append('M');
				}
				p+=e.getLength();
			}
			else if (op==CigarOperator.I) {
				sb.append(e.getLength());
				sb.append('I');
			}
			else if (op==CigarOperator.S){
				sb.append(e.getLength());
				sb.append('S');
			}
			else throw new RuntimeException("CIGAR operator "+op+" not supported!");
			
		}
		return sb.toString();
	}
	
	public static SAMFileHeader createHeader(SequenceProvider s) {
		SAMFileHeader re = new SAMFileHeader();
		for (String n : s.getSequenceNames()) 
			re.addSequence(new SAMSequenceRecord(n, s.getLength(n)));
		return re;
	}
	
	public static ArrayList<SAMRecord> toSamRecords(ReferenceGenomicRegion<? extends AlignedReadsData> read, SAMFileHeader header, int condition, Supplier<String> readname, SequenceProvider seq) {
		
		StringBuilder cigar = new StringBuilder();
		cigar.append(read.getRegion().getLength(0)).append("M");
		for (int i=1; i<read.getRegion().getNumParts(); i++) 
			cigar.append(read.getRegion().getIntronLength(i-1)).append("N").append(read.getRegion().getLength(i)).append("M");
		
		ArrayList<SAMRecord> re = new ArrayList<SAMRecord>();
		for (int d=0; d<read.getData().getDistinctSequences(); d++) {
			int c = condition>=0?read.getData().getCount(d, condition):read.getData().getTotalCountForDistinctInt(d, ReadCountMode.All);
			int edit = read.getData().getVariationCount(d);
			for (int i=0; i<c; i++) {
			
				SAMRecord rec = new SAMRecord(header);
				
				rec.setAlignmentStart(read.getRegion().getStart()+1);
				rec.setCigarString(cigar.toString());
				rec.setReadName(readname.get());
				rec.setReadNegativeStrandFlag(read.getReference().getStrand().equals(Strand.Minus));
				rec.setReferenceName(read.getReference().getName());
				rec.setMappingQuality(255);
				rec.setAttribute("NM", edit);
				
				MismatchString mm = MismatchString.from(read.getData().getVariations(d), read.getReference().getStrand(), read.getRegion().getTotalLength(),false);
				rec.setAttribute("MD", mm.toString());
				rec.setAttribute("NH", read.getData().getMultiplicity(d));

				if (seq!=null) {
					String reference = seq.getPlusSequence(read.getReference().getName(), read.getRegion()).toString();
					mm = MismatchString.from(read.getData().getVariations(d), read.getReference().getStrand(), read.getRegion().getTotalLength(),true);
					rec.setReadString(mm.reconstitute(reference));
					rec.setBaseQualityString(StringUtils.repeat("I", rec.getReadString().length()));
				}
				
				re.add(rec);
			}
		}
		
		return re;
		
	}
	
	public static void toSam(String path, SAMFileHeader header, Iterator<SAMRecord> it, boolean progress) {
		SAMFileWriterFactory fac = new SAMFileWriterFactory();
		SAMFileWriter writer = fac.makeSAMWriter(header, false, new File(path));;
		Progress prog = progress?new ConsoleProgress():null;
		
		
		if (prog!=null)	prog.init();
		if (prog!=null)prog.setDescription("Writing SAM file "+path);
		while (it.hasNext()) {
			SAMRecord r = it.next();
			writer.addAlignment(r);
			
			if (prog!=null)prog.incrementProgress();
		}
		writer.close();
		
		if (prog!=null)	prog.finish();
		
	}
	public static void toBam(String path, SAMFileHeader header, Iterator<SAMRecord> it, boolean sort, boolean index, boolean progress) {
		toBam(path, header, it, sort, index, progress, null);
	}
	public static void toBam(String path, SAMFileHeader header, Iterator<SAMRecord> it, boolean sort, boolean index, boolean progress, Map<String,String> chromosomeMapping) {
		if (sort) header.setSortOrder(SortOrder.coordinate);
		
		SAMFileWriterFactory fac = new SAMFileWriterFactory();
		fac.setCreateIndex(index);
		fac.setTempDirectory(new File(path).getParentFile());
		
		if (chromosomeMapping!=null) {
			header = header.clone();
			ArrayList<SAMSequenceRecord> seqs = new ArrayList<SAMSequenceRecord>();
			for (SAMSequenceRecord r : header.getSequenceDictionary().getSequences()) {
				String to = chromosomeMapping.get(r.getSequenceName());
				if (to!=null)
					r = new SAMSequenceRecord(to, r.getSequenceLength());
				seqs.add(r);
			}
			header.getSequenceDictionary().setSequences(seqs);
		}
		
		
		SAMFileWriter writer = fac.makeBAMWriter(header, false, new File(path));
		Progress prog = progress?new ConsoleProgress():null;
		
		
		if (prog!=null)	prog.init();
		if (prog!=null)prog.setDescription("Writing BAM file "+path);
		while (it.hasNext()) {
			SAMRecord r = it.next();
			
			
			if (chromosomeMapping!=null) {
				r.setHeader(header);
					String to = chromosomeMapping.get(r.getReferenceName());
				if (to!=null) 
					r.setReferenceName(to);
				else
					r.setReferenceName(r.getReferenceName());
			}
			
				
			writer.addAlignment(r);
			
			if (prog!=null)prog.incrementProgress();
		}
		writer.close();
		
		if (prog!=null)	prog.finish();
		
	}
	
	/**
	 * In 5' to 3' direction!
	 * @param r
	 * @return
	 */
	public static String restoreSequence(SAMRecord r, boolean correctStrand) {
		String seq = r.getReadString();
		if (seq.equals("*")) 
			throw new RuntimeException("Sequence not available in SAMRecord!");
		if (r.getStringAttribute("MD")==null) 
			throw new RuntimeException("MD tag not available in SAMRecord!");
		if (r.getCigarString()=="*")  
			throw new RuntimeException("CIGAR string not available in SAMRecord!");
		
		StringBuilder sb = new StringBuilder();
		
		// handle deletions
		int p = 0;
		for (CigarElement e:r.getCigar().getCigarElements()) {
			switch (e.getOperator()){
			case M:
				sb.append(seq.substring(p,p+e.getLength()));
				p+=e.getLength();
				break;
			case I:
			case S:
				p+=e.getLength();
				break;
			case D:
			case N:
				break;
			default:
				throw new RuntimeException("CIGAR operator "+e.getOperator()+" not supported!");
			}
		}
		seq = sb.toString();
		
		// handle insertions and mismatches
		seq = new MismatchString(r.getStringAttribute("MD")).reconstitute(seq);
		
		if (correctStrand &&  r.getReadNegativeStrandFlag())
			seq = SequenceUtils.getDnaReverseComplement(seq);
		return seq;
	}

	/**
	 * Not properly tested yet!!
	 * @param header
	 * @param reference
	 * @param region
	 * @param data
	 * @param distinct
	 * @param template
	 * @return
	 */
	public static SAMRecord createRecord(SAMFileHeader header, ReferenceSequence reference, GenomicRegion region, AlignedReadsData data, int distinct, String readName, int qual) {
		SAMRecord re = new SAMRecord(header);
		re.setReadName(readName);
		re.setReferenceName(reference.getName());
		re.setFlags(0);
		re.setMappingQuality(qual);
		re.setReadNegativeStrandFlag(reference.getStrand()==Strand.Minus);
		re.setAlignmentStart(region.getStart()+1);
		re.setCigar(createCigar(region));
		re.setReadString("*");
		re.setBaseQualityString("*");
		re.setMateAlignmentStart(0);
		re.setMateReferenceName("*");
		re.setInferredInsertSize(0);
		re.setAttribute("NM", data.getVariationCount(distinct));
		re.setAttribute("XV", toSamXV(data,distinct));
		return re;
	}

	private static String toSamXV(AlignedReadsData data, int distinct) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<data.getVariationCount(distinct); i++) {
			if (i>0) sb.append(",");
			sb.append(data.getVariation(distinct, i));
		}
		return sb.toString();
	}

//	private static Object createMDTag(ReferenceSequence reference, GenomicRegion region, AlignedReadsData data, int distinct) {
//		if (data.getVariationCount(distinct)==0) 
//			return region.getTotalLength()+"";
//		
//		ArrayList<String> re = new ArrayList<String>();
//		int plast = 0;
//		for (int v=0; v<data.getVariationCount(distinct); v++) {
//			AlignedReadsVariation var = data.getVariation(distinct, v);
//			if (var.isMismatch() || var.isDeletion()) {
//				if (var.getPosition()!=plast)
//					re.add(var.getPosition()-plast+"");
//				if (var.isMismatch())
//					re.add(var.getReferenceSequence().toString());
//				else
//					re.add("^"+var.getReferenceSequence().toString());
//				plast = var.getPosition()+var.getReferenceSequence().length();
//			}
//		}
//		
//		if (reference.getStrand()==Strand.Minus)
//			ArrayUtils.reverse(re);
//		
//		StringBuilder sb = new StringBuilder();
//		for (String s : re)
//			sb.append(s);
//		
//		return sb.toString();
//	}
//
//	private static String createReadSequence(ReferenceSequence reference, GenomicRegion region, AlignedReadsData data, int distinct) {
//		if (data.getVariationCount(distinct)==0) 
//			return StringUtils.repeat(".", region.getTotalLength());
//		
//		StringBuilder sb = new StringBuilder();
//		int plast = 0;
//		for (int v=0; v<data.getVariationCount(distinct); v++) {
//			AlignedReadsVariation var = data.getVariation(distinct, v);
//			for (int i=plast; i<var.getPosition(); i++)
//				sb.append('.');
//			
//			sb.append(var.getReadSequence());
//			
//			plast = var.getPosition();
//		}
//		for (int i=plast; i<region.getTotalLength(); i++)
//			sb.append('.');
//	
//		
//		if (reference.getStrand()==Strand.Minus)
//			sb.reverse();
//		
//		return sb.toString();
//	}

//	private static Cigar createCigar(ReferenceSequence reference, GenomicRegion region,
//			AlignedReadsData data, int distinct) {
//		ArrayList<CigarElement> re = new ArrayList<CigarElement>();
//		
//		for (int p=0, v=0; p<region.getNumParts()-1 || v<data.getVariationCount(distinct); ) {
//			
//			AlignedReadsVariation var = v<data.getVariationCount(distinct)?data.getVariation(distinct, v):null;
//			int p1 = p<region.getNumParts()-1?region.getStart(p+1):Integer.MAX_VALUE;
//			int p2 = var!=null?var.getPosition():Integer.MAX_VALUE;
//			
//			// add initial M
//			if (re.isEmpty()) {
//				int min = Math.min(p1,p2);
//				if (min==Integer.MAX_VALUE) {
//					re.add(new CigarElement(region.getTotalLength(), CigarOperator.M));
//					break;
//				}
//				re.add(new CigarElement(min, CigarOperator.M));
//			}
//			
//			if (p1<=p2) {
//				re.add(new CigarElement(region.getIntronLength(p), CigarOperator.I));
//				p++;
//			}
//			if (p2<=p1) {
//				if (var.isInsertion()) re.add(new CigarElement(var.getReadSequence().length(), CigarOperator.I));
//				else if (var.isDeletion()) re.add(new CigarElement(var.getReferenceSequence().length(), CigarOperator.D));
//				v++;
//			}
//			
//			if (p>=region.getNumParts()-1 && v>=data.getVariationCount(distinct)) {
//				// add terminal M
//				int l = region.getTotalLength()-Math.min(p1,p2);
//				if (re.get(re.size()-1).getOperator()==CigarOperator.M) 
//					l+=re.remove(re.size()-1).getLength();
//				re.add(new CigarElement(l, CigarOperator.M));
//			}
//		}
//		
//		if (reference.getStrand()==Strand.Minus)
//			ArrayUtils.reverse(re);
//		return new Cigar(re);
//	}
	
	private static Cigar createCigar(GenomicRegion region) {
		ArrayList<CigarElement> re = new ArrayList<CigarElement>();
		
		for (int p=0; p<region.getNumParts(); p++) {
			
			re.add(new CigarElement(region.getLength(p), CigarOperator.M));
			if (p<region.getNumParts()-1)
				re.add(new CigarElement(region.getIntronLength(p), CigarOperator.I));
		}
		return new Cigar(re);
	}

	public static ReferenceSequence getReference(SAMRecord r) {
		if (r.getReadUnmappedFlag()) return SAMUNMAPPEDREFERENCE;
		return Chromosome.obtain(r.getReferenceName(), !r.getReadNegativeStrandFlag());
	}


	public static <T> MutableReferenceGenomicRegion<T> getReferenceGenomicRegion(SAMRecord r, T data) {
		return new MutableReferenceGenomicRegion<T>().set(getReference(r),getArrayGenomicRegion(r),data);
	}

	/**
	 * Convert single softclipped bases to mismatches (adapt alignments start when leading, adapt nM and NM tags when available, adapt or introduce MD tag) 
	 * @param r
	 * @return
	 */
	public static SAMRecord softClipToMismatch(SAMRecord r) {
		if (r.getReadPairedFlag())throw new RuntimeException("Cannot convert soft clips to mismatches for paired end reads!");
		String cigar = r.getCigarString();
		int len = getGenomicRegionLengthFromCigar(cigar);
		
		int add = 0;
		
		if (cigar.startsWith("1S")) {
			cigar = cigar.substring(2);
			int l = StringUtils.leadingNumber(cigar);
			cigar = (l+1)+cigar.substring((l+"").length());
			add++;
			r.setAlignmentStart(r.getAlignmentStart()-1);
			if (r.getStringAttribute("MD")!=null)
				r.setAttribute("MD", "0A"+r.getStringAttribute("MD"));
			else
				r.setAttribute("MD","0A"+len);
		}
		
		if (cigar.endsWith("M1S")) {
			cigar = cigar.substring(0,cigar.length()-3);
			
			int l = StringUtils.trailingNumber(cigar);
			cigar = cigar.substring(0,cigar.length()-(l+"").length())+(l+1)+"M";
			
			add++;
			if (r.getStringAttribute("MD")!=null)
				r.setAttribute("MD", r.getStringAttribute("MD")+"A0");
			else
				r.setAttribute("MD",len+"A0");
		}
		
		r.setCigarString(cigar);
		
		if (add==0) return r;
		
		if (r.getAttribute("nM")!=null)
			r.setAttribute("nM", r.getIntegerAttribute("nM")+add);
		if (r.getAttribute("NM")!=null)
			r.setAttribute("NM", r.getIntegerAttribute("NM")+add);
		
		return r;
	}


	public static int getGenomicRegionLengthFromCigar(String cigar) {
		int re = 0;
		int lenStart = -1;
		for (int index = 0; index<cigar.length(); index++) {
			if (cigar.charAt(index)=='M' || cigar.charAt(index)=='D') 
				re+=Integer.parseInt(cigar.substring(lenStart, index));
			if (!Character.isDigit(cigar.charAt(index)))
				lenStart = -1;
			else if (lenStart==-1)
				lenStart = index;
		}
		return re;
	}
	
}


