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
package gedi.core.processing.sources;

import gedi.core.data.annotation.Transcript;
import gedi.core.data.numeric.GenomicNumericProvider;
import gedi.core.data.numeric.GenomicNumericProvider.PositionNumericIterator;
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericProvider;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.processing.GenomicRegionProcessor;
import gedi.core.processing.OverlapMode;
import gedi.core.processing.ProcessorContext;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.ReferenceSequenceConversion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.datastructure.tree.redblacktree.SimpleInterval;
import gedi.util.mutable.MutableMonad;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class GeneProcessorSource extends ProcessorSource<Transcript> {

	private Set<String> restrictToGenes;
	
	public GeneProcessorSource setRestrict(Set<String> restrict) {
		this.restrictToGenes = restrict;
		return this;
	}
	
	protected boolean removeMultiMapping = false;
	protected boolean strandspecific = false;
	public GeneProcessorSource setRemoveMultiMappingReads(boolean removeMultiMapping, boolean strandspecific) {
		this.removeMultiMapping = removeMultiMapping;
		this.strandspecific = strandspecific;
		return this;
	}

	
	@Override
	public void process(Stream<MutableReferenceGenomicRegion<Transcript>> transcripts, 
			GenomicRegionProcessor processor) throws Exception {

		progress.init();
		// mapping to genes
		HashMap<String,MutableReferenceGenomicRegion<String>> genesToRegon = new HashMap<String, MutableReferenceGenomicRegion<String>>();
		HashMap<String,IntervalTree<SimpleInterval, String>> geneToExons = new HashMap<String,IntervalTree<SimpleInterval, String>>();
		transcripts.forEach(rgr->{
			if (restrictToGenes!=null && !restrictToGenes.contains(rgr.getData().getGeneId()))
				return;

			MutableReferenceGenomicRegion<String> r = genesToRegon.get(rgr.getData().getGeneId());

			if (r==null) {
				genesToRegon.put(rgr.getData().getGeneId(), 
					new MutableReferenceGenomicRegion<String>()
					.setReference(rgr.getReference())
					.setRegion(rgr.getRegion())
					.setData(rgr.getData().getGeneId()));
				geneToExons.put(rgr.getData().getGeneId(), new IntervalTree<>(rgr.getReference()));
			}
			else {
				if (!r.getReference().equals(rgr.getReference()))
					throw new RuntimeException(rgr.getData().getGeneId()+" is located on multiple chromosomes: "+r.getReference()+", "+rgr.getReference());
				r.setRegion(r.getRegion().union(rgr.getRegion()));
			}
			for (int p=0; p<rgr.getRegion().getNumParts(); p++)
				geneToExons.get(rgr.getData().getGeneId()).add(new SimpleInterval(rgr.getRegion().getStart(p), rgr.getRegion().getStop(p)));


		});

		progress.setCount(genesToRegon.size());
		ProcessorContext context = new ProcessorContext();

		processor.begin(context);
		for (MutableReferenceGenomicRegion<String> gene : genesToRegon.values()) {
			progress.setDescription("Processing "+gene);
			progress.incrementProgress();
			IntervalTree<SimpleInterval, String> exontree = geneToExons.get(gene.getData());
			context.putValue(ProcessorContext.EXON_TREE, exontree);
			
			processor.beginRegion(gene,context);
			processor.endRegion(gene,context);
		}
		processor.end(context);
		progress.finish();

	}


	@Override
	public void process(GenomicRegionStorage<AlignedReadsData> reads, 
			ReferenceSequenceConversion readConversion, 
			Stream<MutableReferenceGenomicRegion<Transcript>> transcripts, 
			GenomicRegionProcessor processor) throws Exception {


		progress.init();

		// mapping to genes
		HashMap<String,MutableReferenceGenomicRegion<String>> genesToRegon = new HashMap<String, MutableReferenceGenomicRegion<String>>();
		HashMap<String,IntervalTree<SimpleInterval, String>> geneToExons = new HashMap<String,IntervalTree<SimpleInterval, String>>();
		
		transcripts.forEach(rgr->{
			if (restrictToGenes!=null && !restrictToGenes.contains(rgr.getData().getGeneId()))
				return;

			
			MutableReferenceGenomicRegion<String> r = genesToRegon.get(rgr.getData().getGeneId());

			if (r==null) {
				genesToRegon.put(rgr.getData().getGeneId(), 
					new MutableReferenceGenomicRegion<String>()
					.setReference(rgr.getReference())
					.setRegion(rgr.getRegion())
					.setData(rgr.getData().getGeneId()));
				geneToExons.put(rgr.getData().getGeneId(), new IntervalTree<>(rgr.getReference()));
			}
			else {
				if (!r.getReference().equals(rgr.getReference()))
					throw new RuntimeException(rgr.getData().getGeneId()+" is located on multiple chromosomes: "+r.getReference()+", "+rgr.getReference());
				r.setRegion(r.getRegion().union(rgr.getRegion()));
			}
			
			for (int p=0; p<rgr.getRegion().getNumParts(); p++)
				geneToExons.get(rgr.getData().getGeneId()).put(new SimpleInterval(rgr.getRegion().getStart(p), rgr.getRegion().getStop(p)),rgr.getData().getTranscriptId());

		});

		MemoryIntervalTreeStorage<String> overlapper = new MemoryIntervalTreeStorage<String>(String.class);
		if (removeMultiMapping) {
			if (strandspecific)
				for (MutableReferenceGenomicRegion<String> gene : genesToRegon.values())
					overlapper.add(gene.getReference(), gene.getRegion(), gene.getData());
			else
				for (MutableReferenceGenomicRegion<String> gene : genesToRegon.values()){
					overlapper.add(gene.getReference().toPlusStrand(), gene.getRegion(), gene.getData());
					overlapper.add(gene.getReference().toMinusStrand(), gene.getRegion(), gene.getData());
				}
		}

		progress.setCount(genesToRegon.size());
		ProcessorContext context = new ProcessorContext();
		context.putValue(ProcessorContext.OverlapMode, overlapMode);

		HashSet<String> refNames = new HashSet<String>(); 
		for (ReferenceSequence ref : reads.getReferenceSequences())
			refNames.add(ref.getName());
		
		// sort by chromosome for better performance
		String[] genes = genesToRegon.keySet().toArray(new String[0]);
		Arrays.sort(genes, (a,b)->{
			int re = genesToRegon.get(a).getReference().compareTo(genesToRegon.get(b).getReference());
			if (re==0) re = genesToRegon.get(a).getRegion().compareTo(genesToRegon.get(b).getRegion());
			return re;
		});
		
		
		processor.begin(context);
		for (String g : genes) {
			MutableReferenceGenomicRegion<String> gene = genesToRegon.get(g);
			progress.setDescription("Processing "+gene.getData());
			progress.incrementProgress();

			IntervalTree<SimpleInterval, String> exontree = geneToExons.get(g);
			context.putValue(ProcessorContext.EXON_TREE, exontree);
			
			processor.beginRegion(gene,context);
			ReferenceSequence ref = readConversion.apply(gene.getReference()).correctName(refNames);
			if (ref!=null)
				reads.iterateIntersectingMutableReferenceGenomicRegions(ref, gene.getRegion().getStart(), gene.getRegion().getEnd()).forEachRemaining(rgr->{
					try {
//						System.out.println(rgr + " " + overlapMode.test(rgr.getRegion(), null,rgr.getRegion()));
						if (overlapMode.test(gene.getRegion(),exontree, rgr.getRegion())){
							if (!removeMultiMapping || !findOtherOverlapping(overlapper,ref, gene.getRegion(),rgr.getRegion(),overlapMode, geneToExons))
								processor.read(gene,rgr,context);
						}
					}catch (Exception e) {
						throw new RuntimeException(e);
					}
				});
			processor.endRegion(gene,context);
		}

		processor.end(context);
		progress.finish();

	}


	@Override
	public void process(GenomicNumericProvider numeric, 
			ReferenceSequenceConversion numericConversion, 
			Stream<MutableReferenceGenomicRegion<Transcript>> transcripts,  
			GenomicRegionProcessor processor) throws Exception {

		progress.init();

		// mapping to genes
		HashMap<String,MutableReferenceGenomicRegion<String>> genesToRegon = new HashMap<String, MutableReferenceGenomicRegion<String>>();
		HashMap<String,IntervalTree<SimpleInterval, String>> geneToExons = new HashMap<String,IntervalTree<SimpleInterval, String>>();
		transcripts.forEach(rgr->{
			if (restrictToGenes!=null && !restrictToGenes.contains(rgr.getData().getGeneId()))
				return;

			MutableReferenceGenomicRegion<String> r = genesToRegon.get(rgr.getData().getGeneId());

			if (r==null){ 
				genesToRegon.put(rgr.getData().getGeneId(), 
					new MutableReferenceGenomicRegion<String>()
					.setReference(rgr.getReference())
					.setRegion(rgr.getRegion())
					.setData(rgr.getData().getGeneId()));
				geneToExons.put(rgr.getData().getGeneId(), new IntervalTree<>(rgr.getReference()));
			}else {
				if (!r.getReference().equals(rgr.getReference()))
					throw new RuntimeException(rgr.getData().getGeneId()+" is located on multiple chromosomes: "+r.getReference()+", "+rgr.getReference());
				r.setRegion(r.getRegion().union(rgr.getRegion()));
			}

			for (int p=0; p<rgr.getRegion().getNumParts(); p++)
				geneToExons.get(rgr.getData().getGeneId()).add(new SimpleInterval(rgr.getRegion().getStart(p), rgr.getRegion().getStop(p)));

		});


		progress.setCount(genesToRegon.size());
		double[] buff = null;
		ProcessorContext context = new ProcessorContext();

		processor.begin(context);
		for (MutableReferenceGenomicRegion<String> gene : genesToRegon.values()) {
			progress.setDescription("Processing "+gene);
			progress.incrementProgress();

			IntervalTree<SimpleInterval, String> exontree = geneToExons.get(gene.getData());
			context.putValue(ProcessorContext.EXON_TREE, exontree);
			
			
			processor.beginRegion(gene,context);
			PositionNumericIterator it = numeric.iterateValues(numericConversion.apply(gene.getReference()), gene.getRegion());
			while (it.hasNext()) {
				processor.value(gene,it.nextInt(),buff=it.getValues(buff),context);
			}
			processor.endRegion(gene,context);
		}
		processor.end(context);
		progress.finish();

	}


}
