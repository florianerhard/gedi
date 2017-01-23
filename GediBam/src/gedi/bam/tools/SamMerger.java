package gedi.bam.tools;

import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.util.ArrayUtils;
import gedi.util.FunctorUtils;
import gedi.util.FunctorUtils.MappedIterator;
import gedi.util.FunctorUtils.MergeIterator;
import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import gedi.util.functions.EI;
import gedi.util.sequence.MismatchString;
import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileReader;
import htsjdk.samtools.SAMProgramRecord;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SAMTextHeaderCodec;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.logging.Logger;

public class SamMerger {

	private static final Logger log = Logger.getLogger( SamMerger.class.getName() );
	
	private ArrayList<SamMapping> mappings = new ArrayList<SamMerger.SamMapping>();

	private HashMap<String, Integer> additionalReferences = new HashMap<String, Integer>();
	private HashMap<String, String> mappedReferences = new HashMap<String, String>();
	
	private boolean warnUnknown = true;
	
	private int head = -1;
	
	public SamMerger() {
	}
	public SamMerger(boolean warnUnknown) {
		this.warnUnknown = warnUnknown;
	}
	
	public void addAdditionalReference(String name, int length) {
		additionalReferences.put(name,length);
	}
	

	public void setHead(int head) {
		this.head = head;
	}
	
	public void mapReference(String from, String to) {
		mappedReferences.put(from, to);
	}
	
	public void addSamMapping(String file) {
		if (header!=null) throw new RuntimeException("Header already retrieved!");
		mappings.add(new SamMapping(new SAMFileReader(new File(file)), null));
	}
	public void addSamMapping(String file,Function<ImmutableReferenceGenomicRegion, ImmutableReferenceGenomicRegion> mapper) {
		if (header!=null && mapper!=null) throw new RuntimeException("Header already retrieved!");
		mappings.add(new SamMapping(new SAMFileReader(new File(file)), mapper));
	}
	
	private SAMFileHeader header;
	
	public SAMFileHeader getHeader() {
		if (header==null) {
			HashMap<String,Integer> map = new HashMap<String, Integer>(this.additionalReferences);
			for (SamMapping m : mappings)
				if (m.mapper==null)
					for (SAMSequenceRecord s : m.file.getFileHeader().getSequenceDictionary().getSequences()) {
						Integer l = map.get(s.getSequenceName());
						if (l!=null && l.intValue()!=s.getSequenceLength())
							throw new RuntimeException("Sequence length inconsistent for "+s.getSequenceName());
						map.put(s.getSequenceName(),s.getSequenceLength());
					}
			
			for (String from : mappedReferences.keySet()) {
				if (map.containsKey(from)) {
					map.put(mappedReferences.get(from), map.remove(from));
				}
			}
			
			SAMSequenceRecord[] seq = new SAMSequenceRecord[map.size()];
			int index = 0;
			for (String n : map.keySet())
				seq[index++] = new SAMSequenceRecord(n, map.get(n));
			
			Arrays.sort(seq,(a,b)->a.getSequenceName().compareTo(b.getSequenceName()));
			
			header = new SAMFileHeader() {
				public String toString() {
					final StringWriter headerTextBuffer = new StringWriter();
			        new SAMTextHeaderCodec().encode(headerTextBuffer, this);
			        String re = headerTextBuffer.toString();
			        re = re.substring(0,re.length()-1);
			        return re;
				}
			};
			for (SAMSequenceRecord s : seq)
				header.addSequence(s);
			SAMProgramRecord prog = new SAMProgramRecord("Gedi");
			prog.setProgramName("GediBam");
			header.addProgramRecord(prog);
		}
		return header;
	}
	
	public Iterator<SAMRecord> merge() {
		return merge(null);
	}
	
	public Iterator<SAMRecord> merge(UnaryOperator<Iterator<SAMRecord>> map) {
		SamRecordNameComparator comp = new SamRecordNameComparator();
		SamRecordPosComparator comp2 = new SamRecordPosComparator();
		
		@SuppressWarnings("unchecked")
		MappedIterator<SAMRecord,SAMRecord>[] its = new MappedIterator[mappings.size()];
		for (int i=0; i<its.length; i++) {
			Iterator<SAMRecord> it = mappings.get(i).file.iterator();
			
			if (map!=null)
				it = map.apply(it);
			its[i] = FunctorUtils.mappedIterator(it,mappings.get(i));
			
			
			if (mappedReferences.size()>0) {
				its[i] = FunctorUtils.mappedIterator(its[i], s->{
					String to = mappedReferences.get(s.getReferenceName());
					if (to!=null)
						s.setReferenceName(to);
					return s;
				});
			}
		}
		
		MergeIterator<SAMRecord> merged = FunctorUtils.mergeIterator(its, comp);
		Iterator<SAMRecord[]> rid = FunctorUtils.multiplexIterator(merged, comp,SAMRecord.class);
		MappedIterator<SAMRecord[], SAMRecord[]> re = FunctorUtils.mappedIterator(rid, block->{
			Arrays.sort(block,comp2);
			int offset = 0;
			if (block[0].getReadUnmappedFlag() && !block[block.length-1].getReadUnmappedFlag()) 
				for (;block[offset].getReadUnmappedFlag(); offset++);
			
			int end = ArrayUtils.unique(block,offset,block.length, (a,b)->a.getSAMString().equals(b.getSAMString()));
			return ArrayUtils.slice(block, offset, end);
		});
		
//		for (int i=0; i<its.length; i++)
//			((SAMRecordIterator)its[i].getParent()).close();
		// the iterators close themselves when completely consumed!
		
		
		Iterator<SAMRecord> re2 = FunctorUtils.demultiplexIterator(re, a->FunctorUtils.arrayIterator(a));
		if (head!=-1) {
			return EI.wrap(re2).head(head);
		}
		
		return re2;
	}
	
	
	private HashSet<String> unknownRef = new HashSet<String>();
	
	private class SamMapping implements UnaryOperator<SAMRecord> {
		private SAMFileReader file;
		private Function<ImmutableReferenceGenomicRegion, ImmutableReferenceGenomicRegion> mapper;

		public SamMapping(
				SAMFileReader file,
				Function<ImmutableReferenceGenomicRegion, ImmutableReferenceGenomicRegion> mapper) {
			this.file = file;
			this.mapper = mapper;
		}

		@Override
		public SAMRecord apply(SAMRecord t) {
			if (t.getReadUnmappedFlag()) return t;
			
			t.setHeader(getHeader());
			// samtools are incredibly stupid!
			t.setReferenceName(t.getReferenceName());
			
			if (mapper!=null) {
				ReferenceSequence r = Chromosome.obtain(t.getReferenceName(), !t.getReadNegativeStrandFlag());
				GenomicRegion reg = BamUtils.getArrayGenomicRegion(t);
				ImmutableReferenceGenomicRegion rgr = new ImmutableReferenceGenomicRegion(r, reg);
				rgr = mapper.apply(rgr);
				if (rgr==null) {
					if (unknownRef.add(r.getName()) && warnUnknown)
						log.warning("Reference sequence "+r.getName()+" unknown!");
					return null;
				}
				
				boolean strandSwitch = r.getStrand()!=rgr.getReference().getStrand();
				t.setReferenceName(rgr.getReference().getName());
				t.setReadNegativeStrandFlag(rgr.getReference().getStrand()==Strand.Minus);
				t.setAlignmentStart(rgr.getRegion().getStart()+1);
				
				if (strandSwitch) {
//					t.setAttribute("XS", t.getCigarString()+","+t.getStringAttribute("MD"));
					String md = t.getStringAttribute("MD");
					if (md!=null) 
						t.setAttribute("MD", new MismatchString(md).reverseComplement().toString());
					CigarElement[] cigarElements = t.getCigar().getCigarElements().toArray(new CigarElement[0]);
					ArrayUtils.reverse(cigarElements);
					t.setCigar(new Cigar(Arrays.asList(cigarElements)));
					if (!t.getReadString().equals("*"))
						t.setReadString(SequenceUtils.getDnaReverseComplement(t.getReadString()));
					t.setBaseQualityString(StringUtils.reverse(t.getBaseQualityString()).toString());
				}
				
				t.setCigarString(BamUtils.getCigarString(rgr.getRegion(),t.getCigar().getCigarElements()));
			}
			return t;
		}
		
		
	}
	
}
