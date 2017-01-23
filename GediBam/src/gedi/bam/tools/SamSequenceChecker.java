package gedi.bam.tools;

import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.sequence.CompositeSequenceProvider;
import gedi.core.sequence.FastaIndexSequenceProvider;
import gedi.core.sequence.SequenceProvider;
import gedi.util.FunctorUtils;
import gedi.util.io.text.fasta.index.FastaIndexFile;
import gedi.util.io.text.fasta.index.FastaIndexFile.FastaIndexEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.logging.Logger;

import htsjdk.samtools.SAMRecord;

public class SamSequenceChecker implements UnaryOperator<Iterator<SAMRecord>> {

	private static final Logger log = Logger.getLogger( SamSequenceChecker.class.getName() );
	
	
	private CompositeSequenceProvider seq = new CompositeSequenceProvider();
	private boolean ignoreChrM = false;
	
	
	public SamSequenceChecker(SequenceProvider seq) {
		this.seq.add(seq);
	}

	public SamSequenceChecker(String... paths) throws IOException {
		this(new FastaIndexSequenceProvider(paths));
	}

	public void addFastaIndexFile(String path) throws IOException {
		this.seq.add(new FastaIndexSequenceProvider(path));
	}
	
	public SamSequenceChecker setIgnoreChrM(boolean ignoreChrM) {
		this.ignoreChrM = ignoreChrM;
		return this;
	}

	private HashSet<String> unknown = new HashSet<String>();
	
	public Iterator<SAMRecord> apply(Iterator<SAMRecord> it) {
		
		return FunctorUtils.sideEffectIterator(it,r->{
			if (r.getReadUnmappedFlag())return;
			
			if (r.getReferenceIndex()==-1)
				log.severe("Reference index is -1:\n"+r.getSAMString());
			
			String restored = BamUtils.restoreSequence(r,true);
			CharSequence reference = seq.getSequence(Chromosome.obtain(r.getReferenceName(), !r.getReadNegativeStrandFlag()), BamUtils.getRecordsGenomicRegion(r));
			if (reference==null) {
				if (unknown.add(r.getReferenceName()))
					log.warning("No index for reference sequence "+r.getReferenceName()+"!");
			} else if (!restored.equals(reference.toString())) {
				if (r.getReferenceName().equals("chrM")) {
					r.setAlignmentStart(r.getAlignmentStart()+1);
					reference = seq.getSequence(Chromosome.obtain(r.getReferenceName(), !r.getReadNegativeStrandFlag()), BamUtils.getRecordsGenomicRegion(r));
				}
				if (!restored.equals(reference.toString())) {
					if (!ignoreChrM || !r.getReferenceName().equals("chrM"))
						log.severe("Sequences inconsistent\n"+r.getSAMString()+"Restored:  "+restored+"\nReference: "+reference);
				}
			}
		});
	}
	
	
}