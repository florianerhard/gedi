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
package gedi.macoco.javapipeline;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.function.BiConsumer;

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.data.annotation.Transcript;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.genomic.Genomic;
import gedi.core.reference.Strandness;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.math.stat.inference.EquivalenceClassCountEM;
import gedi.util.math.stat.inference.EquivalenceClassEffectiveLengths;
import gedi.util.program.GediParameter;
import gedi.util.program.GediProgram;
import gedi.util.program.GediProgramContext;

public abstract class EstimateTpm extends GediProgram {


	
	public EstimateTpm(MacocoParameterSet params, GediParameter<File> out) {
		addInput(params.countTable);
		addInput(params.lenDistTable);
		addInput(params.genomic);
		addInput(params.mrnas);
		addInput(params.strandness);
		
		addInput(params.prefix);
		addOutput(out);
	}
	
	
	
	public String execute(GediProgramContext context) throws IOException {
		
		File countsf = getParameter(0);
		File len= getParameter(1);
		Genomic genomic = getParameter(2);
		GenomicRegionStorage<NameAnnotation> mRNAs = getParameter(3);
		Strandness strand = getParameter(4);
			
		context.getLog().info("Running "+StringUtils.removeFooter(StringUtils.removeHeader(getOutputSpec().get(0).getName(),"${prefix}."),".tsv")+"...");
		
		
		MemoryIntervalTreeStorage<Transcript> trans;
		if (mRNAs==null) {
			trans = genomic.getTranscripts();
			
		} else {
			trans = new MemoryIntervalTreeStorage<>(Transcript.class);
			trans.fill(mRNAs.ei().map(r->new ImmutableReferenceGenomicRegion<>(r.getReference(), r.getRegion(),new Transcript(r.getData().getName(), r.getData().getName(), -1, -1))));
		}

		// read counts
		EquivalenceClassInfo info = new EquivalenceClassInfo(countsf.getAbsolutePath(), len.getAbsolutePath(), null);
		String[] cond = info.getConditionNames();
		String[][] E = info.getE();
		double[][] counts = info.getCounts();
		double[][] lens = info.getReadLengths();
		
		HashMap<String,double[]> tpms = new HashMap<>();
		for (int i=0; i<counts.length; i++) {
			int ui = i;
			double[] eff = EquivalenceClassEffectiveLengths.preprocessEff(lens[i]);
			estimate(context,genomic,trans,strand,ui,cond[ui],eff,E,counts[i],(t,a)->{
				double[] ar = tpms.get(t);
				if (ar==null) tpms.put(t, ar = new double[counts.length]);
				ar[ui] = a;
			}
			);
			
			
			double sum = EI.wrap(tpms.values()).mapToDouble(t->t[ui]).sum()/1E6;
			EI.wrap(tpms.values()).forEachRemaining(m->m[ui]/=sum);
		}
		
		context.getLog().info("Writing tables...");
		
		LineWriter writer = new LineOrientedFile(getOutputFile(0).getPath()).write();
		writer.write("Transcript");
		for (int i=0; i<cond.length; i++)
			writer.writef("\t%s",cond[i]);
		writer.writeLine();
		
		String[] ts = tpms.keySet().toArray(new String[0]);
		Arrays.sort(ts);
		
		for (String k : ts) {
			writer.write(k);
			double[] t = tpms.get(k);
			for (int i=0; i<t.length; i++)
				writer.writef("\t%.3f",t[i]);
			writer.writeLine();
		}
		
		writer.close();
		
		return null;
	}



	protected abstract void estimate(GediProgramContext context, Genomic genomic, MemoryIntervalTreeStorage<Transcript> trans, Strandness strand, int condition, String conditionName, double[] eff, String[][] E, double[] counts, BiConsumer<String, Double> transUnnorm) throws IOException;
	
	
}
