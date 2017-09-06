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

package gedi.core.region.feature;

import gedi.core.data.annotation.Transcript;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.AlignedReadsDataFactory;
import gedi.core.data.reads.OneDistinctSequenceAlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.reference.Chromosome;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.feature.index.WriteCoverageRmq;
import gedi.core.region.feature.index.WriteJunctionCit;
import gedi.core.region.feature.output.FeatureListOutput;
import gedi.core.region.feature.output.FeatureStatisticOutput;
import gedi.util.functions.EI;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AlignedReadsDataToFeatureProgram implements Consumer<ReferenceGenomicRegion<AlignedReadsData>> {

	private GenomicRegionFeatureProgram<AlignedReadsData> program;
	private MutableReferenceGenomicRegion<AlignedReadsData> mut = new MutableReferenceGenomicRegion<AlignedReadsData>();
	
	private Progress progress = new NoProgress();

	private ReadCountMode readCountMode = ReadCountMode.Weight;
	private Supplier<CharSequence> descr;
	
	public AlignedReadsDataToFeatureProgram(
			GenomicRegionFeatureProgram<AlignedReadsData> program) {
		this.program = program;
		
	}
	
	
	public static AlignedReadsDataToFeatureProgram getSimpleProgram(String statistic, String list, GenomicRegionFeature...features) {
		GenomicRegionFeatureProgram<AlignedReadsData> re = new GenomicRegionFeatureProgram<>();
		
		for (GenomicRegionFeature f : features) {
			if (f.getId()==null)
				f.setId(f.getClass().getSimpleName().replaceAll("Feature$", ""));
			re.add(f);
		}
		
		
		if (statistic!=null)
			re.add(new FeatureStatisticOutput(statistic), EI.wrap(features).map(f->f.getId()).toArray(String.class));
		if (list!=null)
			re.add(new FeatureListOutput(list), EI.wrap(features).map(f->f.getId()).toArray(String.class));
		re.setThreads(2);
		
		return new AlignedReadsDataToFeatureProgram(re);
	}


	public static AlignedReadsDataToFeatureProgram getCoverageProgram(String coverageRmq, String junctionCoverage) {
		GenomicRegionFeatureProgram<AlignedReadsData> re = new GenomicRegionFeatureProgram<>();
		if (coverageRmq!=null) {
			WriteCoverageRmq f = new WriteCoverageRmq(coverageRmq);
			f.setId("coverage");
			re.add(f);
		}
		if (junctionCoverage!=null) {
			WriteJunctionCit f = new WriteJunctionCit(junctionCoverage);
			f.setId("junction");
			re.add(f);
		}
		re.setThreads(1);
		return new AlignedReadsDataToFeatureProgram(re);
	}


	public GenomicRegionFeatureProgram<AlignedReadsData> getProgram() {
		return program;
	}
	
	public AlignedReadsDataToFeatureProgram setProgress(Progress progress) {
		this.progress = progress;
		return this;
	}

	public AlignedReadsDataToFeatureProgram setProgress(Progress progress, Supplier<CharSequence> descr) {
		this.progress = progress;
		this.descr = descr;
		return this;
	}

	public AlignedReadsDataToFeatureProgram setConsoleProgress() {
		this.progress = new ConsoleProgress(System.err);
		return this;
	}
	
	public void setReadCountMode(ReadCountMode readCountMode) {
		this.readCountMode = readCountMode;
	}
	
	public ReadCountMode getReadCountMode() {
		return readCountMode;
	}

	public void setSize(int size) {
		this.progress.setCount(size);
	}

	public void test(GenomicRegionStorage<Transcript> anno) {
		Chromosome ref = Chromosome.obtain("chr1-");
		MutableReferenceGenomicRegion<Transcript> rgr = EI.wrap(anno.iterateMutableReferenceGenomicRegions(ref))
				.filter(t->t.getData().isCoding() && t.getData().get3Utr(t.getReference(), t.getRegion()).getTotalLength()>500)
				.skip(100)
				.next();
		GenomicRegion reg = new ImmutableReferenceGenomicRegion(ref, rgr.getData().get3Utr(rgr.getReference(), rgr.getRegion())).map(new ArrayGenomicRegion(10,38));
		MutableReferenceGenomicRegion<AlignedReadsData> read = new MutableReferenceGenomicRegion<AlignedReadsData>();
		read.set(ref, reg, new AlignedReadsDataFactory(1).start().newDistinctSequence().setMultiplicity(1).setCount(0, 1).create());
		
		program.setDataToCounts((ard,b)->ard.getCountsForDistinct(b, 0, readCountMode));//ard.addCount(0, b, true));
		program.begin();
		accept(read);
		program.end();
	}
	
	/**
	 * r itself can be mutable, but the data should not be changed (by reflection or orm), as processing may be concurrent!
	 */
	@Override
	public void accept(ReferenceGenomicRegion<AlignedReadsData> r) {
		
		for (int d=0; d<r.getData().getDistinctSequences(); d++) {
			program.accept(mut.set(r.getReference(),r.getRegion(),new OneDistinctSequenceAlignedReadsData(r.getData(),d)));
		}
		if (descr!=null)
			progress.setDescription(descr);
		progress.incrementProgress();
	}
	
	public void processStorage(GenomicRegionStorage<AlignedReadsData> storage) {
		program.setDataToCounts((ard,b)->ard.getCountsForDistinct(b, 0, readCountMode));//ard.addCount(0, b, true));
		
		program.begin();
		progress.init();
		progress.setCount((int) storage.size());
		storage.iterateReferenceGenomicRegions().forEachRemaining(this);
		progress.finish();
		program.end();

	}
	
	public void process(Iterator<? extends ReferenceGenomicRegion<AlignedReadsData>> it) {
		process(it,-1);
	}
	
	public void process(Iterator<? extends ReferenceGenomicRegion<AlignedReadsData>> it, int count) {
		program.setDataToCounts((ard,b)->ard.getCountsForDistinct(b, 0, readCountMode));//ard.addCount(0, b, true));
		
		program.begin();
		progress.init();
		if (count>0)
			progress.setCount(count);
		while (it.hasNext())
			accept(it.next());
		progress.finish();
		program.end();

	}

}
