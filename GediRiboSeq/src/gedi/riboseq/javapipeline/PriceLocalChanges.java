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
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ToDoubleFunction;

import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.annotation.ScoreNameAnnotation;
import gedi.core.data.annotation.Transcript;
import gedi.core.genomic.Genomic;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.feature.special.Downsampling;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.ArrayUtils;
import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.array.SparseMemoryFloatArray;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.functions.EI;
import gedi.util.functions.IterateIntoSink;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.math.stat.testing.DirichletLikelihoodRatioTest;
import gedi.util.mutable.MutableDouble;
import gedi.util.mutable.MutablePair;
import gedi.util.program.GediProgram;
import gedi.util.program.GediProgramContext;
import gedi.util.userInteraction.progress.Progress;
import jdistlib.Beta;
import jdistlib.math.MathFunctions;

public class PriceLocalChanges extends GediProgram {

	public PriceLocalChanges(PriceParameterSet params) {
		addInput(params.prefix);
		addInput(params.optcodons);
		addInput(params.genomic);
		addInput(params.localTableMinCodonFraction);
		
		addOutput(params.localTable);
		addOutput(params.localCit);
	}
	
	public String execute(GediProgramContext context) throws IOException, InterruptedException {
		
		File cfile = getParameter(1);
		Genomic g = getParameter(2);
		double minFrac = getDoubleParameter(3);
		
		CenteredDiskIntervalTreeStorage<SparseMemoryFloatArray> codons = new CenteredDiskIntervalTreeStorage<>(cfile.getAbsolutePath());
		MemoryIntervalTreeStorage<Transcript> cds = new MemoryIntervalTreeStorage<>(Transcript.class);
		
		cds.fill(
				g.getTranscripts().ei().filter(r->SequenceUtils.checkCompleteCodingTranscript(g, r)).map(r->r.getData().getCds(r))
				);
		
		
		HashMap<String, String> t2g = g.getTranscripts().ei().index(r->r.getData().getTranscriptId(),r->r.getData().getGeneId());
		
		LineWriter writer = new LineOrientedFile(getOutputFile(0).getPath()).write();
		writer.write("Location\tGene\tSymbol\tTranscript\tGenomic position\tPosterior\n");

		
		
		CenteredDiskIntervalTreeStorage<ScoreNameAnnotation> cit = new CenteredDiskIntervalTreeStorage<>(getOutputFile(1).getPath(),ScoreNameAnnotation.class);
		IterateIntoSink<ImmutableReferenceGenomicRegion<ScoreNameAnnotation>> sink = new IterateIntoSink<>(cit::fill);
		
		Progress progress = context.getProgress();
		progress.init();
		int n =0;
		
		for (ReferenceSequence ref : cds.getReferenceSequences()) {//Arrays.asList(Chromosome.obtain("14+"))){//cds.getReferenceSequences()) {
			MutableReferenceGenomicRegion<Void> orf = new MutableReferenceGenomicRegion<Void>().setReference(ref);
			
			progress.setDescription("Processing gene clusters on "+ref.toString()+" found: "+n);
			
			IntervalTree<GenomicRegion, Transcript> tree = cds.getTree(ref);
			IntervalTree<GenomicRegion, Transcript>.GroupIterator git = tree.groupIterator();
			while (git.hasNext()) {
				IntervalTree<GenomicRegion,Transcript> group = new IntervalTree<>(git.next(),ref);
			
				// load data into structure
				HashMap<GenomicRegion,NumericArray[]> map = loadData(ref,group,codons);

				// identify orf with most codons >=1 (or the one with maximimal reads if ambiguous)
				double fraction = removeNonMaximal(map,a->a==null?0:1);
				if (map.size()>1)
					removeNonMaximal(map,a->a==null?0:a.sum());

				if (map.size()>0 && fraction>=minFrac){
					GenomicRegion region = map.keySet().iterator().next();
					NumericArray[] profile = map.get(region);
					
					String trans = group.get(region).getTranscriptId();
					
//					if (g.getGeneTable("symbol").apply(t2g.get(trans)).equals("APEX1"))
//						System.out.println();
//					ArrayList<MutablePair<GenomicRegion, double[]>> regions = computeLocalPvalues(profile,1E-5,0,Downsampling.No,Downsampling.Max);
					ArrayList<MutablePair<GenomicRegion,MutableDouble>> regions = computeLocalPosterior(profile,0.9,0.01,Downsampling.No,Downsampling.No);
					ImmutableReferenceGenomicRegion<Void> parent = new ImmutableReferenceGenomicRegion<>(ref, region);
					
					for (MutablePair<GenomicRegion,MutableDouble> reg : regions) {
						ImmutableReferenceGenomicRegion<ScoreNameAnnotation> rgr = new ImmutableReferenceGenomicRegion<>(ref, 
								parent.map(reg.Item1.pep2dna()), new ScoreNameAnnotation(trans, reg.Item2.N));
						sink.put(rgr);
						
						writer.writef2("%s\t%s\t%s\t%s\t%s\t%.5f\n", 
								orf.setRegion(region),
								t2g.get(trans),
								g.getGeneTable("symbol").apply(t2g.get(trans)),
								trans,
								rgr.toLocationString(),
								reg.Item2.N
								);
						n++;
					}
					
				}
				progress.incrementProgress();
			}
		}
		
		sink.finish();
		progress.finish();
		
		writer.close();
		
		
		return null;
	}

	public static double posteriorDifferentProportions(double a, double b, double c,double d, double priorDifferent) {
		double bf = betabetaBF(a, b, c, d);
		double odds = bf*(priorDifferent/(1-priorDifferent));
		return odds/(odds+1);
	}
	
	public static double betabetaBF(double a, double b, double c,double d) {
		double p1=(a+c)/(a+b+c+d);
		double p2=(b+d)/(a+b+c+d);
		return 1/Math.exp(MathFunctions.lbeta(p1*2+a+c,p2*2+b+d)-MathFunctions.lbeta(p1*2,p2*2)-
			(MathFunctions.lbeta(p1+a,p2+b)-MathFunctions.lbeta(p1,p2)+
					MathFunctions.lbeta(c+p1,d+p2)-MathFunctions.lbeta(p1,p2)));
	}

	
	private ArrayList<MutablePair<GenomicRegion,MutableDouble>> computeLocalPosterior(NumericArray[] profile, double cutoff, double priorDifferent, Downsampling innerDownsampling, Downsampling outerDownsampling) {
		
		IntervalTree<GenomicRegion, Double> re = new IntervalTree<>(null);
		
		NumericArray all = EI.wrap(profile).removeNulls().reduce(NumericArray.createMemory(2, NumericArrayType.Double),(a,ret)->{ret.add(outerDownsampling.downsample(a.copy())); return ret;});
		NumericArray buffer = NumericArray.createMemory(2, NumericArrayType.Double);
		
		NumericArray inner = NumericArray.createMemory(2, NumericArrayType.Double);
		
		for (int i=0; i<profile.length; i++) {
			
			inner.clear();
			all.copyRange(0, buffer, 0, buffer.length());
			
			for (int l=0; l<maxWindow && i+l<profile.length; l++) {
				if (profile[i+l]!=null) {
					buffer.subtract(outerDownsampling.downsample(profile[i+l].copy()));
					inner.add(innerDownsampling.downsample(profile[i+l].copy()));
//					double pval = DirichletLikelihoodRatioTest.testMultinomials(inner.toDoubleArray(),buffer.toDoubleArray());
//					double es = DirichletLikelihoodRatioTest.effectSizeMultinomials(inner.toDoubleArray(),buffer.toDoubleArray());
					double post = posteriorDifferentProportions(inner.getDouble(0), inner.getDouble(1), buffer.getDouble(0), buffer.getDouble(1), priorDifferent);
					if (post>=cutoff)
						re.put(new ArrayGenomicRegion(i,i+l+1),post);
				}
					
			}
		}
		
		ArrayList<MutablePair<GenomicRegion, MutableDouble>> re2 = new ArrayList<>();
		IntervalTree<GenomicRegion, Double>.GroupIterator git = re.groupIterator();
		while (git.hasNext()) {
			IntervalTree<GenomicRegion, Double> g = new IntervalTree<>(git.next(),null);
			
			GenomicRegion rereg = null;
			double max = -1;
			for (GenomicRegion r : g.keySet())
				if (g.get(r)>max) {
					max = g.get(r);
					rereg = r;
				}
			
			re2.add(new MutablePair<>(rereg,new MutableDouble(max)));
			
		}
		
		return re2;
	}
	
	
	int maxWindow = 25;
	private ArrayList<MutablePair<GenomicRegion, double[]>> computeLocalPvalues(NumericArray[] profile, double pvalcutoff, double effectCutoff, Downsampling innerDownsampling, Downsampling outerDownsampling) {
		ArrayList<MutablePair<GenomicRegion, double[]>> re = new ArrayList<>();
		
		NumericArray proto = EI.wrap(profile).removeNulls().first();
		if (proto==null) return re;
		
		int d = proto.length();
		
		
		
		NumericArray all = EI.wrap(profile).removeNulls().reduce(NumericArray.createMemory(d, NumericArrayType.Double),(a,ret)->{ret.add(outerDownsampling.downsample(a.copy())); return ret;});
		NumericArray buffer = NumericArray.createMemory(d, NumericArrayType.Double);
		
		NumericArray inner = NumericArray.createMemory(d, NumericArrayType.Double);
		
		int wall = 0;
		for (int i=0; i<profile.length; i++) {
			
			inner.clear();
			all.copyRange(0, buffer, 0, buffer.length());
			for (int b=0; b<maxWindow && i-b>=wall; b++) {
				
				if (profile[i-b]!=null) {
					buffer.subtract(outerDownsampling.downsample(profile[i-b].copy()));
					inner.add(innerDownsampling.downsample(profile[i-b].copy()));
					double pval = DirichletLikelihoodRatioTest.testMultinomials(inner.toDoubleArray(),buffer.toDoubleArray());
					double es = DirichletLikelihoodRatioTest.effectSizeMultinomials(inner.toDoubleArray(),buffer.toDoubleArray());
					
					if (pval<=pvalcutoff && Math.abs(es)>=effectCutoff) {
						// extend in both directions in window steps to find the region maximizing the effectsize
						double maxes = es;
						double minp = pval;
						// left
						for (int ad=0; ad<maxWindow && i-b-ad>=wall; ad++) {
							if (profile[i-b-ad]!=null) {
								buffer.subtract(outerDownsampling.downsample(profile[i-b-ad].copy()));
								inner.add(innerDownsampling.downsample(profile[i-b-ad].copy()));
								pval = DirichletLikelihoodRatioTest.testMultinomials(inner.toDoubleArray(),buffer.toDoubleArray());
								es = DirichletLikelihoodRatioTest.effectSizeMultinomials(inner.toDoubleArray(),buffer.toDoubleArray());
								if (Math.abs(es)>=Math.abs(maxes)) {
									b+=ad;
									ad=0;
									maxes = es;
									minp = Math.min(minp, pval);
								}
							}
						}
						
						int a = 0;
						// right
						for (int ad=0; ad<maxWindow && i+a+ad<profile.length; ad++) {
							if (profile[i+a+ad]!=null) {
								buffer.subtract(outerDownsampling.downsample(profile[i+a+ad].copy()));
								inner.add(innerDownsampling.downsample(profile[i+a+ad].copy()));
								pval = DirichletLikelihoodRatioTest.testMultinomials(inner.toDoubleArray(),buffer.toDoubleArray());
								es = DirichletLikelihoodRatioTest.effectSizeMultinomials(inner.toDoubleArray(),buffer.toDoubleArray());
								if (Math.abs(es)>=Math.abs(maxes)) {
									a+=ad;
									ad=0;
									maxes = es;
									minp = Math.min(minp, pval);
								}
							}
						}
						
						re.add(new MutablePair<>(new ArrayGenomicRegion(i-b,i+a+1),new double[]{minp,maxes}));
						i=i+a+maxWindow;
						wall=i+a;
						
					}
				}
			}
			
		}
		
		return re;
	}
	
	private double[] computeChangePointPvalues(NumericArray[] profile) {
		double[] re = new double[profile.length];
		Arrays.fill(re, Double.POSITIVE_INFINITY);
		
		NumericArray proto = EI.wrap(profile).removeNulls().first();
		if (proto==null) return re;
		
		int d = proto.length();
		
		
		
		NumericArray left = NumericArray.createMemory(d, NumericArrayType.Double);
		NumericArray right = EI.wrap(profile).removeNulls().reduce(NumericArray.createMemory(d, NumericArrayType.Double),(a,ret)->{ret.add(Downsampling.Max.downsample(a.copy())); return ret;});
		
		
		for (int i=0; i<profile.length; i++) {
			if (profile[i]!=null) {
				NumericArray a = Downsampling.Max.downsample(profile[i].copy());
				left.add(a);
				right.subtract(a);
				re[i] = DirichletLikelihoodRatioTest.testMultinomials(left.toDoubleArray(),right.toDoubleArray());
			}
		}
		
		return re;
	}

	private <K,T> double removeNonMaximal(HashMap<K, T[]> map, ToDoubleFunction<T> fun) {
		HashMap<K,T[]> re = new HashMap<>();
		double max = Double.NEGATIVE_INFINITY;
		
		for (K k : map.keySet()) {
			double val = 0;
			for (T a : map.get(k))
				val+=fun.applyAsDouble(a);
			
			if (val>max) {
				re.clear();
				max = val;
			}
			if (val>=max) re.put(k, map.get(k));
		}
		map.clear();
		map.putAll(re);
		return max;
	}

	private HashMap<GenomicRegion, NumericArray[]> loadData(ReferenceSequence ref, IntervalTree<GenomicRegion, Transcript> group, CenteredDiskIntervalTreeStorage<SparseMemoryFloatArray> codons) {
		HashMap<GenomicRegion, NumericArray[]> re = new HashMap<>();
		
		MutableReferenceGenomicRegion<Void> orf = new MutableReferenceGenomicRegion<Void>().setReference(ref);
		
		for (ReferenceGenomicRegion<SparseMemoryFloatArray> c : codons.ei(ref,new ArrayGenomicRegion(group.getStart(),group.getEnd())).loop()) {
			for (GenomicRegion orff : group.keys(c.getRegion().getStart(),c.getRegion().getStop()).filter(orff->orff.containsUnspliced(c.getRegion())).loop()) {
				int pos = orf.setRegion(orff).induce(c.getRegion()).getStart();
				
				if (pos%3==0) {
					pos/=3;
					NumericArray[] profile = re.computeIfAbsent(orff, x->new NumericArray[orff.getTotalLength()/3]);
					
					if (profile[pos]!=null) throw new RuntimeException("Already occupied, cannot be !");
						
					
					profile[pos] = c.getData();
				}
			}
		}
		return re;
	}
	
	

}

