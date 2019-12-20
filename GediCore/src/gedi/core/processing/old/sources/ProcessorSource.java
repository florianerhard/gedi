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
package gedi.core.processing.old.sources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import gedi.core.data.numeric.GenomicNumericProvider;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.processing.old.GenomicRegionProcessor;
import gedi.core.processing.old.OverlapMode;
import gedi.core.processing.old.ProcessorContext;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.ReferenceSequenceConversion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.datastructure.tree.redblacktree.SimpleInterval;
import gedi.util.mutable.MutableMonad;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

public class ProcessorSource<T> {

	protected boolean parallel = false;
	
	protected Progress progress = new NoProgress();
	protected OverlapMode overlapMode = OverlapMode.Always;
	
	public ProcessorSource<T> setProgress(Progress progress) {
		this.progress = progress;
		return this;
	}

	public ProcessorSource<T> setOverlapMode(OverlapMode overlapMode) {
		this.overlapMode = overlapMode;
		return this;
	}

	
	/**
	 * Returns whether there is another overlapping region in index (use in conjunction with setRemoveMultiMappingReads)
	 * @param index
	 * @param ref
	 * @param current
	 * @param read
	 * @param overlapMode
	 * @return
	 */
	protected boolean findOtherOverlapping(
			GenomicRegionStorage<String> index, ReferenceSequence ref, GenomicRegion current,
			GenomicRegion read, OverlapMode overlapMode,HashMap<String,IntervalTree<SimpleInterval, String>> exontrees) {
		
		for (ImmutableReferenceGenomicRegion<String> r : index.getReferenceRegionsIntersecting(ref, read, new ArrayList<ImmutableReferenceGenomicRegion<String>>())) {
			GenomicRegion other = r.getRegion();
			if (!other.equals(current) && overlapMode.test(other, exontrees==null?null:exontrees.get(r.getData()), read)) 
				return true;
		}
		
//		for (GenomicRegion other : index.getRegionsIntersecting(ref, read, new ArrayList<GenomicRegion>())) {
//			if (!other.equals(current) && overlapMode.test(other, exontrees==null?null:exontrees.get(index.getData(ref, other)), read)) 
//				return true;
//		}
	
		return false;
	}
	
	public void process(GenomicRegionStorage<T> regions, 
			GenomicRegionProcessor processor) throws Exception {
		process(StreamSupport.stream(regions.iterateMutableReferenceGenomicRegions(),parallel),processor);
	}
	
	
	
	public void process(Spliterator<MutableReferenceGenomicRegion<T>> regions, 
			GenomicRegionProcessor processor) throws Exception {
		process(StreamSupport.stream(regions, parallel),processor);
	}
	
	
	public void process(Stream<MutableReferenceGenomicRegion<T>> regions, 
			GenomicRegionProcessor processor) throws Exception {

		progress.init();

		ProcessorContext context = new ProcessorContext();

		processor.begin(context);
		regions.forEach(mrgr-> {
			progress.setDescription("Processing "+mrgr);
			progress.incrementProgress();

			try {
				processor.beginRegion(mrgr,context);
				processor.endRegion(mrgr,context);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

		});
		processor.end(context);
		progress.finish();

	}

	public void process(GenomicRegionStorage<AlignedReadsData> reads, 
			ReferenceSequenceConversion readConversion, 
			GenomicRegionStorage<T> regions, 
			GenomicRegionProcessor processor) throws Exception {
		process(reads,readConversion,StreamSupport.stream(regions.iterateMutableReferenceGenomicRegions(), parallel),processor);
	}
	
	public void process(GenomicRegionStorage<AlignedReadsData> reads, 
			ReferenceSequenceConversion readConversion, 
			Spliterator<MutableReferenceGenomicRegion<T>> regions, 
			GenomicRegionProcessor processor) throws Exception {
		process(reads,readConversion,StreamSupport.stream(regions, parallel),processor);
	}

	public void process(GenomicRegionStorage<AlignedReadsData> reads, 
			ReferenceSequenceConversion readConversion, 
			Stream<MutableReferenceGenomicRegion<T>> regions, 
			GenomicRegionProcessor processor) throws Exception {

		progress.init();
		ProcessorContext context = new ProcessorContext();

		processor.begin(context);
		regions.forEach(mrgr-> {
			progress.setDescription("Processing "+mrgr);
			progress.incrementProgress();

			try {
				processor.beginRegion(mrgr,context);
				reads.iterateIntersectingMutableReferenceGenomicRegions(readConversion.apply(mrgr.getReference()), mrgr.getRegion().getStart(), mrgr.getRegion().getEnd()).forEachRemaining(rgr->{
					try {
						if (overlapMode.test(mrgr.getRegion(), null,rgr.getRegion()))
							processor.read(mrgr,rgr,context);
					}catch (Exception e) {
						throw new RuntimeException(e);
					}
				});
				processor.endRegion(mrgr,context);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});


		processor.end(context);
		progress.finish();

	}
	
	public void process(GenomicNumericProvider numeric, 
			ReferenceSequenceConversion numericConversion, 
			GenomicRegionStorage<T> regions, 
			GenomicRegionProcessor processor) throws Exception {
		process(numeric,numericConversion,StreamSupport.stream(regions.iterateMutableReferenceGenomicRegions(), parallel), processor);
	}
	
	public void process(GenomicNumericProvider numeric, 
			ReferenceSequenceConversion numericConversion, 
			Spliterator<MutableReferenceGenomicRegion<T>> regions, 
			GenomicRegionProcessor processor) throws Exception {
		process(numeric,numericConversion,StreamSupport.stream(regions, parallel), processor);
	}

	public void process(GenomicNumericProvider numeric, 
			ReferenceSequenceConversion numericConversion, 
			Stream<MutableReferenceGenomicRegion<T>> regions, 
			GenomicRegionProcessor processor) throws Exception {

		progress.init();


		MutableMonad<double[]> buff = new MutableMonad<double[]>();
		ProcessorContext context = new ProcessorContext();

		processor.begin(context);
		regions.forEach(mrgr-> {
			progress.setDescription("Processing "+mrgr);
			progress.incrementProgress();
			try {
				processor.beginRegion(mrgr,context);

				for (int i=0; i<mrgr.getRegion().getTotalLength(); i++) {
					int pos = mrgr.getRegion().map(i);
					buff.Item = numeric.getValues(numericConversion.apply(mrgr.getReference()), pos,buff.Item);
					processor.value(mrgr,pos,buff.Item,context);
				}
				//				PositionNumericIterator it = numeric.iterateValues(numericConversion.apply(mrgr.getReference()), mrgr.getRegion());
				//				while (it.hasNext()) {
				//					processor.value(mrgr,it.nextInt(),buff.Item=it.getValues(buff.Item),context);
				//				}
				processor.endRegion(mrgr,context);
			}catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		processor.end(context);
		progress.finish();

	}


}
