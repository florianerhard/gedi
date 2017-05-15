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

package gedi.util.io.text.tsv.formats;

import gedi.core.data.annotation.NameProvider;
import gedi.core.data.annotation.ScoreNameAnnotation;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.AlignedReadsDataFactory;
import gedi.core.reference.Chromosome;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.core.sequence.SequenceProvider;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.io.text.tsv.formats.BedEntry.StrandAwareByChromosomeComparator;
import gedi.util.sequence.MismatchString;
import gedi.util.sequence.MismatchString.Mismatch;
import gedi.util.sequence.MismatchString.MismatchStringPart;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class Bed extends MemoryIntervalTreeStorage<ScoreNameAnnotation> {

	public Bed(String file) throws IOException {
		super(ScoreNameAnnotation.class);
		new ScoreNameBedFileReader(file).readIntoMemoryTakeFirst(this);
	}
	
	@Override
	public int getLength(String name) {
		int re = super.getLength(name);
		if (re!=-1) return -re;
		return re;
	}
	
	public static void sort(String path) throws IOException {
		StrandAwareByChromosomeComparator comp = new StrandAwareByChromosomeComparator();
		new LineOrientedFile(path).sort((a,b)->{
			BedEntry ea = BedEntry.parseValues(a);
			BedEntry eb = BedEntry.parseValues(b);
			return comp.compare(ea, eb);
		});
	}
	
	public static ExtendedIterator<ImmutableReferenceGenomicRegion<ScoreNameAnnotation>> iterateScoreNameEntries(String file) throws IOException {
		return iterateEntries(file, f->new ScoreNameAnnotation(f[3], Double.parseDouble(f[4])));
	}
	
	
	public static ExtendedIterator<ImmutableReferenceGenomicRegion<AlignedReadsData>> iterateReads(SequenceProvider genome, String... files) throws IOException {
		AlignedReadsDataFactory fac = new AlignedReadsDataFactory(1);
		
		Function<List<ImmutableReferenceGenomicRegion<String[]>>,ImmutableReferenceGenomicRegion<AlignedReadsData>> applyer = l->{
			fac.start();
			for (ImmutableReferenceGenomicRegion<String[]> r : l) {
				
				String[] s = StringUtils.split(r.getData()[3],'|');
				fac.newDistinctSequence();		
				
				fac.setCount(0, Integer.parseInt(s[0]));
				fac.setMultiplicity(Integer.parseInt(s[2]));
				
				MismatchString mm = new MismatchString(s[1]);
				
				int le = 0;
				for (MismatchStringPart p : mm) {
					if (p instanceof Mismatch){
						int pos = r.getReference().getStrand()==Strand.Plus?le:r.getRegion().getTotalLength()-1-le;
						pos = r.getRegion().map(pos);
						fac.addMismatch(le, genome.getSequence(r.getReference(), pos), ((Mismatch)p).getGenomicChar());
					}
					le+=p.getGenomicLength();
				}
			}
			return new ImmutableReferenceGenomicRegion<AlignedReadsData>(l.get(0).getReference(),l.get(0).getRegion(),fac.create());
		};
		Comparator<ImmutableReferenceGenomicRegion<String[]>> comp = (a,b)->{
			int re = a.getReference().compareTo(b.getReference());
			if (re==0) re = a.getRegion().compareTo(b.getRegion());
			return re;
		};
		
		return iterateEntries(files[0], f->f).multiplex(comp,applyer);
	}
	
	
	public static <T> ExtendedIterator<ImmutableReferenceGenomicRegion<T>> iterateEntries(String file, Function<String[],T> data) throws IOException {
		IntArrayList coo = new IntArrayList();
		return new LineOrientedFile(file).lineIterator().skip(l->l.startsWith("#")).map(l->{
			String[] f = StringUtils.split(l, '\t', new String[12]);
			
			Chromosome ref = Chromosome.obtain(f[0],f[5]);
			GenomicRegion region;
			if (f[11]==null) region = new ArrayGenomicRegion(Integer.parseInt(f[1]),Integer.parseInt(f[2]));
			else {
				coo.clear();
				int s = Integer.parseInt(f[1]);
				String[] off = StringUtils.split(f[11], ',');
				String[] len = StringUtils.split(f[10], ',');
				
				for (int i=0; i<off.length; i++) {
					if (off[i].length()>0) {
						coo.add(s+Integer.parseInt(off[i]));	
						coo.add(coo.getLastInt()+Integer.parseInt(len[i]));
					}
				}
				region = new ArrayGenomicRegion(coo);
				
			}
			
			T d = data.apply(f);
			
			return new ImmutableReferenceGenomicRegion(ref, region, d);
		});
	}

	public static void save(String file,
			ExtendedIterator<? extends ReferenceGenomicRegion<? extends NameProvider>> it) throws IOException {
		LineWriter w = new LineOrientedFile(file).write();
		for (String line : it.map(r->new BedEntry(r).toString()).loop())
			w.writeLine(line);
		w.close();
	}
	
	
}
