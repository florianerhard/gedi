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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.BiConsumer;
import java.util.logging.Level;

import gedi.core.data.annotation.Transcript;
import gedi.core.genomic.Genomic;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strandness;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.StringUtils;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.math.stat.inference.EquivalenceClassCountEM;
import gedi.util.math.stat.inference.EquivalenceClassEffectiveLengths;
import gedi.util.math.stat.inference.EquivalenceClassMinimizeFactors;
import gedi.util.program.GediProgramContext;
import gedi.util.r.RRunner;

public class EquivalenceClassesMacoco extends EstimateTpm {



	public EquivalenceClassesMacoco(MacocoParameterSet params) {
		super(params,params.macocoTable);
	}



	@Override
	protected void estimate(GediProgramContext context, Genomic genomic, MemoryIntervalTreeStorage<Transcript> trans, Strandness strand, int cond, String condName, double[] eff, String[][] E, double[] counts,
			BiConsumer<String, Double> transUnnorm) throws IOException {
		
		ExtendedIterator<ImmutableReferenceGenomicRegion<String>> tit = trans.ei().map(r->new ImmutableReferenceGenomicRegion<String>(strand==Strandness.Unspecific?r.getReference().toStrandIndependent():r.getReference(), r.getRegion(), r.getData().getTranscriptId()));

		EquivalenceClassEffectiveLengths<String> len = new EquivalenceClassEffectiveLengths<String>(tit,eff);
		
		double[] el = new double[counts.length];
		double[] cov = new double[counts.length];
		for (int i=0; i<cov.length; i++) {
			el[i] = len.getEffectiveLength(E[i]);
			cov[i] = counts[i]/el[i];
			if (Double.isInfinite(cov[i]))
				cov[i] = 0;//System.out.println(counts[i]+"\t"+len.getEffectiveLength(E[i]));
		}
		
		
		String[] tr = EI.wrap(E).unfold(a->EI.wrap(a)).sort().unique(true).toArray(String.class);
		LineWriter out = new LineOrientedFile(getOutputFile(0).getPath()+"."+condName+".tsv").write();
		for (int i=0; i<tr.length; i++)
			out.writef("%s\t", tr[i]);
		out.writeLine("len\tcov");
		for (int i=0; i<E.length; i++) {
			if (el[i]<5) continue;
			
			HashSet<String> eset = new HashSet<>(Arrays.asList(E[i]));
			for (int j=0; j<tr.length; j++) 
				out.writef("%d\t", eset.contains(tr[j])?1:0);
			out.writef("%.1f\t%.3f\n",len.getEffectiveLength(E[i]),cov[i]);
		}
		out.close();
		
		try {
			context.getLog().info("Running R scripts for fitting glm");
			RRunner r = new RRunner(getOutputFile(0).getPath()+"."+condName+".R");
			r.set("input",getOutputFile(0).getPath()+"."+condName+".tsv");
			r.set("output",getOutputFile(0).getPath()+"."+condName+".coef.tsv");
			r.addSource(getClass().getResourceAsStream("/resources/R/macoco.R"));
			r.run(true);
		} catch (Throwable e) {
			context.getLog().log(Level.SEVERE, "Could not plot!", e);
		}
		
		EI.lines(getOutputFile(0).getPath()+"."+condName+".coef.tsv").skip(1).map(s->StringUtils.split(s, '\t')).forEachRemaining(a->transUnnorm.accept(a[0],Double.parseDouble(a[1])));
		
		
	}
	
	protected void estimateold(Genomic genomic, int cond, String condName, double[] eff, String[][] E, double[] counts,
			BiConsumer<String, Double> transUnnorm) throws IOException {
		
		EquivalenceClassEffectiveLengths<String> len = new EquivalenceClassEffectiveLengths<String>(
				genomic.getTranscripts().ei().map(r->new ImmutableReferenceGenomicRegion<String>(r.getReference().toStrandIndependent(), r.getRegion(), r.getData().getTranscriptId())),
				eff);
		
		String[][] e2 = new String[100][];
		double[] cov2 = new double[100];
		int ind = 0;
		
		double[] el = new double[counts.length];
		double[] cov = new double[counts.length];
		for (int i=0; i<cov.length; i++) {
			el[i] = len.getEffectiveLength(E[i]);
			cov[i] = counts[i]/el[i];
			if (Double.isInfinite(cov[i]))
				cov[i] = 0;//System.out.println(counts[i]+"\t"+len.getEffectiveLength(E[i]));
			if (E[i][0].startsWith("R2_45")) {
				e2[ind] = E[i];
				cov2[ind] = cov[i];
				if (cond==0) {
					System.out.println(StringUtils.toString(e2[ind])+" "+counts[i]+" "+len.getEffectiveLength(E[i])+" "+cov2[ind]);
					len.getEffectiveLength(E[i]);
				}
				ind++;
			}
		}
		
		
		if (cond==0) {
//			System.out.println(new EquivalenceClassMinimizeFactors(e2,cov2).compute((s,d)->System.out.println(s+" "+d)));
		
			String[] tr = EI.wrap(E).unfold(a->EI.wrap(a)).sort().unique(true).toArray(String.class);
			HashMap<String, Integer> index = EI.wrap(tr).indexPosition();
			LineWriter out = new LineOrientedFile("test.macoco").write();
			for (int i=0; i<tr.length; i++)
				out.writef("%s\t", tr[i]);
			out.writeLine("len\tcov");
			for (int i=0; i<E.length; i++) {
				if (el[i]<5) continue;
				
				HashSet<String> eset = new HashSet<>(Arrays.asList(E[i]));
				for (int j=0; j<tr.length; j++) 
					out.writef("%d\t", eset.contains(tr[j])?1:0);
				out.writef("%.1f\t%.3f\n",len.getEffectiveLength(E[i]),cov[i]);
			}
			out.close();
		}
		
		new EquivalenceClassMinimizeFactors(E,cov).compute(transUnnorm);
		
	}
}
