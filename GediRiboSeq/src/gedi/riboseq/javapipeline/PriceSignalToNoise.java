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

package gedi.riboseq.javapipeline;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;

import gedi.core.data.annotation.Transcript;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.genomic.Genomic;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.core.sequence.SequenceProvider;
import gedi.riboseq.cleavage.RiboModel;
import gedi.riboseq.inference.clustering.RiboClusterInfo;
import gedi.riboseq.inference.codon.Codon;
import gedi.riboseq.inference.codon.CodonInference;
import gedi.riboseq.inference.orf.OrfInference;
import gedi.riboseq.inference.orf.PriceOrf;
import gedi.riboseq.utils.RiboUtils;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.datastructure.tree.redblacktree.IntervalTreeSet;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.mutable.MutableInteger;
import gedi.util.program.GediProgram;
import gedi.util.program.GediProgramContext;
import gedi.util.program.parametertypes.StorageParameterType;
import gedi.util.r.RRunner;
import gedi.util.userInteraction.progress.Progress;

public class PriceSignalToNoise extends GediProgram {

	public PriceSignalToNoise(PriceParameterSet params) {
		addInput(params.prefix);
		addInput(params.genomic);
		addInput(params.delta);
		addInput(params.nthreads);
		addInput(params.codons);
		addInput(params.estimateData);
		setRunFlag(params.plot);

		addOutput(params.signaldata);
	}
	
	public String execute(GediProgramContext context) throws IOException {
		
		String prefix = getParameter(0);
		Genomic g = getParameter(1);
		double delta = getDoubleParameter(2);
		int nthreads = getIntParameter(3);
		int chunk = 10;
		
		
		context.getLog().log(Level.INFO, "Collect signal to noise data");
		double[] all = new double[2];
		
		RiboUtils.processCodonsSink(prefix+".codons.bin", "Collecting signal to noise data", context.getProgress(), null,nthreads, chunk, double[].class, 
				ei->ei.map(o->getSignalAndNoise(g,o)),
				n->ArrayUtils.add(all, n.getData())
				);
		
		LineWriter tab = new LineOrientedFile(prefix+".signal.tsv").write();
		tab.write("Delta\tSignal\tNoise\n");
		tab.writef("%.3f\t%.0f\t%.0f\n", delta, all[0], all[1]);
		tab.close();

		context.getLog().info("Running R scripts for plotting");
		RRunner r = new RRunner(prefix+".signaltonoise.R");
		r.set("prefix",prefix);
		r.addSource(getClass().getResourceAsStream("/resources/R/signaltonoise.R"));
		r.run(true);
		
		return null;
	}

	private ImmutableReferenceGenomicRegion<double[]> getSignalAndNoise(Genomic g,
			ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> o) {
		
		IntervalTree<GenomicRegion,double[]> cdss = new IntervalTree<GenomicRegion,double[]>(o.getReference());
		for (GenomicRegion r : g.getTranscripts().ei(o).filter(r->SequenceUtils.checkCompleteCodingTranscript(g, r)).map(r->r.getData().getCds(r).getRegion()).loop())
			cdss.put(r, new double[2]);
		
		for (Codon codon : o.getData()) {
			GenomicRegion c = o.map(codon);
			cdss.forEachIntervalsIntersecting(c.getStart(), c.getStop(), e->{
				GenomicRegion tr = e.getKey();
				if (tr.containsUnspliced(c)) {
					int f = tr.induce(c).getStart()%3;
					e.getValue()[f==0?0:1]+=codon.getTotalActivity();
				}
			});
		}
		
		double[] re2 = new double[2];
		for (GenomicRegion r : cdss.keySet()) {
			double[] t = cdss.get(r);
			if (t[0]/r.getTotalLength()*3>1 && re2[0]<t[0])
				re2 = t;
		}
		return new ImmutableReferenceGenomicRegion<>(o.getReference(), o.getRegion(), re2);
	}
	
	

}
