package gedi.util.io.text.tsv.formats;

import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterators;
import java.util.function.Function;

import cern.colt.Arrays;
import gedi.core.data.annotation.Gff3Element;
import gedi.core.data.annotation.NameAnnotation;
import gedi.core.data.annotation.ScoreNameAnnotation;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.AlignedReadsDataFactory;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.core.sequence.SequenceProvider;
import gedi.util.FunctorUtils;
import gedi.util.FunctorUtils.ParallellIterator;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.fasta.index.FastaIndexFile;
import gedi.util.io.text.tsv.formats.BedEntry.StrandAwareByChromosomeComparator;
import gedi.util.mutable.MutableMonad;
import gedi.util.sequence.MismatchString;
import gedi.util.sequence.MismatchString.Mismatch;
import gedi.util.sequence.MismatchString.MismatchStringPart;

public class Gff extends MemoryIntervalTreeStorage<Gff3Element> {

	public Gff(String file) throws IOException {
		super(Gff3Element.class);
		new Gff3FileReader(file).readIntoMemoryTakeFirst(this);
	}
	
	public Gff(String file, String... features ) throws IOException {
		super(Gff3Element.class);
		new Gff3FileReader(file,features).readIntoMemoryTakeFirst(this);
	}
	
	@Override
	public int getLength(String name) {
		int re = super.getLength(name);
		if (re!=-1) return -re;
		return re;
	}
	
	
	
}
