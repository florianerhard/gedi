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
package executables;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.logging.Level;

import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.annotation.Transcript;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.genomic.Genomic;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strandness;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionPosition;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.feature.special.Downsampling;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.MemoryDoubleArray;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.tree.Trie;
import gedi.util.datastructure.tree.Trie.AhoCorasickResult;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.functions.IterateIntoSink;
import gedi.util.io.text.HeaderLine;
import gedi.util.io.text.LineWriter;
import gedi.util.math.stat.descriptive.WeightedMeanVarianceOnline;
import gedi.util.mutable.MutablePair;
import gedi.util.mutable.MutableTriple;
import gedi.util.program.CommandLineHandler;
import gedi.util.program.GediParameter;
import gedi.util.program.GediParameterSet;
import gedi.util.program.GediProgram;
import gedi.util.program.GediProgramContext;
import gedi.util.program.parametertypes.BooleanParameterType;
import gedi.util.program.parametertypes.EnumParameterType;
import gedi.util.program.parametertypes.FileParameterType;
import gedi.util.program.parametertypes.GenomicParameterType;
import gedi.util.program.parametertypes.IntParameterType;
import gedi.util.program.parametertypes.StorageParameterType;
import gedi.util.program.parametertypes.StringParameterType;
import gedi.util.r.RRunner;


public class Polya {
	
	




	public static void main(String[] args) throws IOException {
		
//		ImmutableReferenceGenomicRegion<Transcript> t1 = new ImmutableReferenceGenomicRegion<Transcript>(Chromosome.obtain("1+"),new ArrayGenomicRegion(0,5,10,20),new Transcript("g","t1",-1,-1));
//		
//		ImmutableReferenceGenomicRegion[] peaks = {
//		         new ImmutableReferenceGenomicRegion<>(Chromosome.obtain("1+"), new ArrayGenomicRegion(15,16),new Peak(t1)),
//		         new ImmutableReferenceGenomicRegion<>(Chromosome.obtain("1+"), new ArrayGenomicRegion(15,16),new Peak(null))
//		};
//		
//		PeaksEM em = new PeaksEM(PeakModel.getGaussian(3),peaks);
////		em.addRead(ImmutableReferenceGenomicRegion.parse("1+:15-20",1), d->d.intValue());
//		em.addRead(ImmutableReferenceGenomicRegion.parse("1+:9-20",1), d->d.intValue());
//		em.addRead(ImmutableReferenceGenomicRegion.parse("1+:2-5|10-15",10), d->d.intValue());
//		em.finishReads();
//		
//		double[] w = em.em(100,1000,1E-8);
//		System.out.println(StringUtils.toString(w));
		
		
		PolyaParameterSet params = new PolyaParameterSet();
		GediProgram pipeline = GediProgram.create("Polya",
				new PolyaClusterPositionInferenceProgram(params),
				new PolyaClusterProgram(params),
				new PolyaAnnotateClusterProgram(params),
				new PolyaAnalyzeClusterProgram(params)
				);
		GediProgram.run(pipeline, null, new CommandLineHandler("Polya","Polya analyzes 3' seq data.",args));

	}
	
	public enum PeakCategory {
		Exonic,Spliced,ExonIntronic,Intronic,Flank,Intergenic
	}

	public static class PolyaAnalyzeClusterProgram extends GediProgram {

		public PolyaAnalyzeClusterProgram(PolyaParameterSet params) {
			addInput(params.prefix);
			addInput(params.genomic);
			addInput(params.annotation);
			
			addOutput(params.pairwise);
		}

		@Override
		public String execute(GediProgramContext context) throws Exception {
			String prefix = getParameter(0);
			Genomic g = getParameter(1);
			File tab = getParameter(2);
			
			
			HeaderLine h = new HeaderLine();
			Predicate<String[]> isNormal = a->a[h.get("ThreePrime")].equals("1");
			Predicate<String[]> isInternal = a->!a[h.get("ThreePrime")].equals("1") && !a[h.get("Signal")].equals("-") && a[h.get("Priming")].equals("0") && a[h.get("Antisense")].equals("0");
			Comparator<String[]> gcomp = (a,b)->a[h.get("Gene")].compareTo(b[h.get("Gene")]);
			
			LineWriter out = null;
			for (String[][] gene : EI.lines(tab.getPath()).header(h).split("\t")
										.filter(a->isNormal.test(a)||isInternal.test(a))
										.sort(gcomp)
										.multiplex(gcomp, String[].class).loop()) {
				if (out==null) {
					out = getOutputWriter(0);
					out.writeLine("Gene\tLocation\tType A\tType B\t"+EI.wrap(h.getFields()).map(f->f+".A").concat("\t")+"\t"+EI.wrap(h.getFields()).map(f->f+".B").concat("\t"));
				}
				
				String symbol = g.getGeneTable("symbol").apply(gene[0][h.get("Gene")]);
				
				ArrayList<String[]> normals = EI.wrap(gene).filter(isNormal).list();
				ArrayList<String[]> internals = EI.wrap(gene).filter(isInternal).list();
				
				for (int i=0; i<normals.size(); i++) {
					for (int j=i+1; j<normals.size(); j++) {
						double sumi = 0;
						double sumj = 0;
						for (int c=h.get("Location")+1; c<h.get("Gene"); c++){
							sumi+=Double.parseDouble(normals.get(i)[c]);
							sumj+=Double.parseDouble(normals.get(j)[c]);
						}
						if (sumi/sumj>0.05 && sumj/sumj>0.05) {
							MutableReferenceGenomicRegion<Object> loc = ImmutableReferenceGenomicRegion.parse(normals.get(i)[h.get("Location")]).toMutable();
							loc.setRegion(loc.getRegion().extendBack(150).extendFront(150).union(ImmutableReferenceGenomicRegion.parse(normals.get(j)[h.get("Location")]).getRegion().extendBack(150).extendFront(150)));
							out.writef("%s\t%s\tThreePrime\tThreePrime\t%s\t%s\n", symbol,loc.toLocationString(),StringUtils.concat("\t", normals.get(i)),StringUtils.concat("\t", normals.get(j)));
						}
					}
				}
				
				for (int i=0; i<normals.size(); i++) {
					for (int j=0; j<internals.size(); j++) {
						double sumi = 0;
						double sumj = 0;
						for (int c=h.get("Location")+1; c<h.get("Gene"); c++){
							sumi+=Double.parseDouble(normals.get(i)[c]);
							sumj+=Double.parseDouble(internals.get(j)[c]);
						}
						if (sumi/sumj>0.05 && sumj/sumi>0.05) {
							MutableReferenceGenomicRegion<Object> loc = ImmutableReferenceGenomicRegion.parse(normals.get(i)[h.get("Location")]).toMutable();
							loc.setRegion(loc.getRegion().extendBack(150).extendFront(150).union(ImmutableReferenceGenomicRegion.parse(internals.get(j)[h.get("Location")]).getRegion().extendBack(150).extendFront(150)));
							out.writef("%s\t%s\tThreePrime\tInternal\t%s\t%s\n", symbol,loc.toLocationString(),StringUtils.concat("\t", normals.get(i)),StringUtils.concat("\t", internals.get(j)));
						}
					}
				}

				
			}

			out.close();
			
			return null;
		}
	}
	
	public static class PolyaAnnotateClusterProgram extends GediProgram {

		public PolyaAnnotateClusterProgram(PolyaParameterSet params) {
			addInput(params.prefix);
			addInput(params.genomic);
			addInput(params.nthreads);
			addInput(params.clustercit);
			addInput(params.reads);
			
			addOutput(params.annotation);
			addOutput(params.motifs);
		}

		@Override
		public String execute(GediProgramContext context) throws Exception {
			String prefix = getParameter(0);
			Genomic g = getParameter(1);
			int nthreads = getIntParameter(2);
			File cl = getParameter(3);
			GenomicRegionStorage<DefaultAlignedReadsData> reads = getParameter(4);
			
			int downstreamAnnotated = 120;
			int polyaUp = 20;
			int polyadown = 20;
			
			int pasRegionUpstream = 50;
			
			String[] signals = {"AATAAA","ATTAAA","AGTAAA","TATAAA","-"};
			Trie<Integer> strie = new Trie<>();
			for (int i=0; i<signals.length-1; i++)
				strie.put(signals[i], i);
		
			String[] conditions = reads.getMetaDataConditions();
			int gene_tolerance = 10_000;
			
			CenteredDiskIntervalTreeStorage<NumericArray> clusters = new CenteredDiskIntervalTreeStorage<>(cl.getPath());
			
			MemoryIntervalTreeStorage<String> extendedGenes = new MemoryIntervalTreeStorage<>(String.class);
			extendedGenes.fill(g.getGenes().ei().map(ge->ge.toMutable().alterRegion(reg->reg.extendBack(gene_tolerance).extendFront(gene_tolerance)).toImmutable()));
			
			HashMap<String,int[][]> motif = new HashMap<>();
			
			
			LineWriter out = getOutputWriter(0);
			out.writef("Location\t%s\tGene\tCategory\tThreePrime\tAntisense\tSignal\tSignal.dist\tPriming\n",StringUtils.concat("\t", conditions));
			
			for (ImmutableReferenceGenomicRegion<NumericArray> p : clusters.ei().progress(context.getProgress(), (int)clusters.size(), d->d.toLocationString()).loop()) {
				
				PeakCategory cat = PeakCategory.Intergenic;
				String gene = null;
				boolean threeprime = false;
				boolean antisense = false;

				for (ImmutableReferenceGenomicRegion<Transcript> t : g.getTranscripts().ei(p).loop()) {
					if (t.getRegion().containsUnspliced(p.getRegion())) {
						cat = p.getRegion().getNumParts()==1?PeakCategory.Exonic:PeakCategory.Spliced;
						gene = t.getData().getGeneId();
						if (t.induce(p.getRegion()).getEnd()+downstreamAnnotated*2>t.getRegion().getTotalLength())
							threeprime = true;
					}
				}
				if (cat==PeakCategory.Intergenic) {
					for (ImmutableReferenceGenomicRegion<String> gg : g.getGenes().ei(p).loop()) {
						if (gg.getRegion().contains(p.getRegion())) {
							boolean exin = g.getTranscripts().ei(gg).filter(t->t.getRegion().intersects(p.getRegion())).count()>0;
							PeakCategory ccat = exin?PeakCategory.ExonIntronic:PeakCategory.Intronic;
							if (cat.ordinal()>ccat.ordinal()) cat = ccat;
							gene = gg.getData();
						}
					}
				}
				if (cat==PeakCategory.Intergenic) {
					if (extendedGenes.ei(p).count()>0) {
						cat = PeakCategory.Flank;
						gene = extendedGenes.ei(p).first().getData();
					}
				}
				
				if (cat==PeakCategory.Intergenic) {
					for (ImmutableReferenceGenomicRegion<Transcript> t : g.getTranscripts().ei(p.toMutable().toOppositeStrand()).loop()) {
						if (t.getRegion().containsUnspliced(p.getRegion())) {
							cat = t.getRegion().getNumParts()==1?PeakCategory.Exonic:PeakCategory.Spliced;
							gene = t.getData().getGeneId();
							antisense = true;
						}
					}
				}
				if (cat==PeakCategory.Intergenic) {
					for (ImmutableReferenceGenomicRegion<String> gg : g.getGenes().ei(p.toMutable().toOppositeStrand()).loop()) {
						if (gg.getRegion().contains(p.getRegion())) {
							boolean exin = g.getTranscripts().ei(gg).filter(t->t.getRegion().intersects(p.getRegion())).count()>0;
							PeakCategory ccat = exin?PeakCategory.ExonIntronic:PeakCategory.Intronic;
							if (cat.ordinal()>ccat.ordinal()) cat = ccat;
							gene = gg.getData();
							antisense = true;
						}
					}
				}
				if (cat==PeakCategory.Intergenic) {
					if (extendedGenes.ei(p.toMutable().toOppositeStrand()).count()>0) {
						cat = PeakCategory.Flank;
						gene = extendedGenes.ei(p.toMutable().toOppositeStrand()).first().getData();
						antisense = true;
					}
				}
				
				
				int fs = signals.length-1;
				int dist = 0;
				
				CharSequence seq = g.getSequenceSave(p.toMutable().getUpstream(polyaUp)).toString()+g.getSequenceSave(p).toString()+g.getSequenceSave(p.toMutable().getDownstream(polyadown)).toString();
				for (int i=0; i<seq.length(); i++) {
					int nuc = SequenceUtils.inv_nucleotides[seq.charAt(i)];
					if (nuc>=0 && nuc<4) {
						int[][] m = motif.computeIfAbsent(cat+(threeprime||antisense?"_":"")+(threeprime?"3":"")+(antisense?"A":""), x->new int[4][polyaUp+1+polyadown]);
						m[nuc][i]++;
					}
				}
				// A/G stretch start in 0-10? (len 10, >50%A)
				boolean stretch = false;
				int[] cumA = new int[seq.length()];
				int[] cumG = new int[seq.length()];
				for (int i=0; i<seq.length(); i++) {
					if (seq.charAt(i)=='A')
						cumA[i]++;
					else if (seq.charAt(i)=='G')
						cumG[i]++;
				}
				ArrayUtils.cumSumInPlace(cumA, 1);
				ArrayUtils.cumSumInPlace(cumG, 1);
				for (int i=10; i<cumA.length; i++) {
					int na=cumA[i]-cumA[i-10];
					int ng=cumG[i]-cumG[i-10];
					if (na+ng>=9 && na>ng)
						stretch = true;
				}
				
				
				seq = g.getSequenceSave(p.toMutable().getUpstream(pasRegionUpstream));
				for (AhoCorasickResult<Integer> hit : strie.iterateAhoCorasick(seq).loop()) {
					if (hit.getValue()<fs) {
						fs = hit.getValue();
						dist = hit.getStart()-50;
					}
				}
				
				out.writef("%s\t%s\t%s\t%s\t%d\t%d\t%s\t%d\t%d\n", p.toLocationString(),p.getData().formatArray("\t"),gene,cat,threeprime?1:0,antisense?1:0,signals[fs],dist,stretch?1:0);
				
			}
			
			out.close();
			
			
			out = getOutputWriter(1);
			for (String n : motif.keySet()) {
				
				int[][] mat = motif.get(n);
				for (int i=0; i<mat.length; i++) {
					out.writef("%s\t%s\n", n, EI.wrap(mat[i]).concat("\t"));
				}
			}

			out.close();
			
			return null;
		}
	}
	
	
	public static class PolyaClusterProgram extends GediProgram {



		public PolyaClusterProgram(PolyaParameterSet params) {
			addInput(params.prefix);
			addInput(params.reads);
			addInput(params.genomic);
			addInput(params.nthreads);
			addInput(params.minReads);
			addInput(params.flank);
			addInput(params.test);
			addInput(params.strandness);
			addInput(params.infer3pparam);

			addOutput(params.clustercit);
		}

		@Override
		public String execute(GediProgramContext context) throws Exception {
			String prefix = getParameter(0);
			GenomicRegionStorage<DefaultAlignedReadsData> reads = getParameter(1);
			Genomic genomic = getParameter(2);
			int nthreads = getIntParameter(3);
			int minReads = getIntParameter(4);
			int flank = getIntParameter(5);
			boolean test = getBooleanParameter(6);
			Strandness strandness = getParameter(7);
			
			genomic.getTranscriptTable("source");
			
			int halfing = 15;
			Downsampling down = Downsampling.No;
			
			context.getLog().info("Identifying clusters...");
			
			
			TreeSet<ReferenceSequence> refs = new TreeSet<ReferenceSequence>(reads.getReferenceSequences());
			refs.removeIf(r->!genomic.getSequenceNames().contains(r.getName()));
			
			CenteredDiskIntervalTreeStorage<MemoryDoubleArray> out = new CenteredDiskIntervalTreeStorage<>(getOutputFile(0).getPath(),MemoryDoubleArray.class);
			
			int block = 16;
			IterateIntoSink<ImmutableReferenceGenomicRegion<ArrayList<ImmutableReferenceGenomicRegion<DefaultAlignedReadsData>>>> sink = new IterateIntoSink<>(
					ei->out.fill(ei.parallelized(nthreads, block, xei->
									xei.unfold(x->findPolya(x, flank, halfing, down,strandness)
											))
										.removeNulls()
										.filter(r->r.getData().sum()>=minReads)
										.progress(context.getProgress(), -1, x->"Detecting Poly-A sites "+x.toLocationString())
										,context.getProgress()),
					nthreads*block
					);
			
			
			IntervalTree<GenomicRegion,ArrayList<ImmutableReferenceGenomicRegion<DefaultAlignedReadsData>>> openClusters = null;
			TreeMap<Integer,GenomicRegion> openClustersByStop = null;
			
			MemoryIntervalTreeStorage<Transcript> transcripts = genomic.getTranscripts();
			
			for (ImmutableReferenceGenomicRegion<DefaultAlignedReadsData> read : (test?reads.ei("18+:46501748-46501749").sort():reads.ei()) //reads.ei("10+")   
										.progress(context.getProgress(), (int)reads.size(), r->"Collecting reads "+r.toLocationString())
										.filter(r->r.getData().getMultiplicity(0)<=1)
										.checkOrder((a,b)->a.compareTo(b))
										.loop()) {
				
				if (read.getData().getNumParts(read,0)>1 && transcripts.ei(read).chain(transcripts.ei(read.toMutable().toOppositeStrand()))
							.filter(t->read.getData().isConsistentlyContained(read, t, 0))
							.count()==0)
					continue;
				
				ArrayList<ImmutableReferenceGenomicRegion<DefaultAlignedReadsData>> re;
				if (openClusters==null) {
					// first cluster
					openClusters = new IntervalTree<GenomicRegion, ArrayList<ImmutableReferenceGenomicRegion<DefaultAlignedReadsData>>>(read.getReference());
					openClusters.put(read.getRegion(),re = new ArrayList<>());
					openClustersByStop = new TreeMap<>();
					openClustersByStop.put(read.getRegion().getStop(), read.getRegion());
				}
				else if (!openClusters.getReference().equals(read.getReference())) {
					// first cluster on this chromosome
					
					for (GenomicRegion clreg : openClusters.keySet()) 
						sink.accept(new ImmutableReferenceGenomicRegion<>(openClusters.getReference(), clreg, openClusters.get(clreg)));

					openClusters = new IntervalTree<GenomicRegion, ArrayList<ImmutableReferenceGenomicRegion<DefaultAlignedReadsData>>>(read.getReference());
					openClusters.put(read.getRegion(),re = new ArrayList<>());
					openClustersByStop = new TreeMap<>();
					openClustersByStop.put(read.getRegion().getStop(), read.getRegion());
				} else {
					ArrayList<GenomicRegion> overlaps = openClusters
						.keys(read.getRegion().getStart(), read.getRegion().getStop())
						.filter(reg->reg.intersects(read.getRegion())).list();
					
					if (overlaps.size()>0) {
						// merge clusters 
						re = openClusters.get(overlaps.get(0));
						GenomicRegion r = overlaps.get(0);
						for (int i=1; i<overlaps.size(); i++) {
							re.addAll(openClusters.get(overlaps.get(i)));
							r = r.union(overlaps.get(i));
						}
						r = r.union(read.getRegion());
						
						for (GenomicRegion reg : overlaps) {
							openClusters.remove(reg);
							openClustersByStop.remove(reg.getStop());
						}
						
						openClusters.put(r, re);
						openClustersByStop.put(r.getStop(), r);

					} else {
						openClusters.put(read.getRegion(), re = new ArrayList<>());
						openClustersByStop.put(read.getRegion().getStop(), read.getRegion());
					}
					
					// prune interval tree (everything that ends left of the current start
					NavigableMap<Integer, GenomicRegion> head = openClustersByStop.headMap(read.getRegion().getStart(), false);
					for (GenomicRegion clreg : head.values()) 
						sink.accept(new ImmutableReferenceGenomicRegion<>(openClusters.getReference(), clreg, openClusters.get(clreg)));

					openClusters.keySet().removeAll(head.values());
					head.clear();
				}
				re.add(read);
				
				
			}
			
			if (openClusters!=null)
				for (GenomicRegion clreg : openClusters.keySet()) 
					sink.accept(new ImmutableReferenceGenomicRegion<>(openClusters.getReference(), clreg, openClusters.get(clreg)));

			
			sink.finish();
			

			return null;
		}
		
		

		private static ExtendedIterator<ImmutableReferenceGenomicRegion<MemoryDoubleArray>> findPolya(
				ImmutableReferenceGenomicRegion<ArrayList<ImmutableReferenceGenomicRegion<DefaultAlignedReadsData>>> or, 
				int flank, int halfingDist, Downsampling down,
				Strandness strandness) {
			
			double[] endCounter = new double[or.getRegion().getTotalLength()];
			MemoryDoubleArray[] endCounterPerCond = new MemoryDoubleArray[or.getRegion().getTotalLength()];
				
			GenomicRegionPosition poser;
			boolean opp;
			switch (strandness) {
			case Sense: poser = GenomicRegionPosition.ThreePrime; opp = false; break;
			case Antisense: poser = GenomicRegionPosition.FivePrime; opp=true; break;
			default: throw new RuntimeException("Strandness must be sense or antisense!");
			}
			
			// determine weight for each barcode
			for (ImmutableReferenceGenomicRegion<DefaultAlignedReadsData> read : or.getData()) {
				int p = or.induce(poser.position(read));
				double tot = read.getData().getTotalCountOverall(ReadCountMode.Weight);
				endCounter[p] += tot;

				MemoryDoubleArray harr = (MemoryDoubleArray)down.downsample(read.getData().getTotalCountsForConditions(null, ReadCountMode.Weight));
				if (endCounterPerCond[p]==null)
					endCounterPerCond[p] = harr;
				else
					endCounterPerCond[p].add(harr);
			}
			
			ArrayList<ImmutableReferenceGenomicRegion<MemoryDoubleArray>> re = new ArrayList<ImmutableReferenceGenomicRegion<MemoryDoubleArray>>();
			
			double sum = ArrayUtils.sum(endCounter);
			while (sum>=1) {
				int argmax = ArrayUtils.argmax(endCounter);
				double v = endCounter[argmax];
				if (v<1) break;
				
				int hflank = (int) Math.ceil(halfingDist*Math.log(v)/Math.log(2)); // such that it's smaller than 1 outside!

				for (int i=Math.max(0, argmax-flank); i<=Math.min(endCounter.length-1, argmax+flank); i++) {
					if (i!=argmax && endCounterPerCond[i]!=null)
						endCounterPerCond[argmax].add(endCounterPerCond[i]);
					sum-=endCounter[i];
					endCounter[i] = 0;
				}
				for (int i=Math.max(0, argmax-hflank); i<=Math.min(endCounter.length-1, argmax+hflank); i++) {
					double d = Math.pow(2, Math.abs(argmax-i)/(double)halfingDist);
					double min = v/d;
					if (endCounter[i]<min) {
						sum-=endCounter[i];
						endCounter[i] = 0;
					} 
				}
				re.add(new ImmutableReferenceGenomicRegion<MemoryDoubleArray>(
								opp?or.getReference().toOppositeStrand():or.getReference(), 
								or.map(new ArrayGenomicRegion(argmax,argmax+1)), 
								endCounterPerCond[argmax]));
				
			}

			return EI.wrap(re);
		}
		
	}
	
	
	public static class PolyaClusterPositionInferenceProgram extends GediProgram {

		public PolyaClusterPositionInferenceProgram(PolyaParameterSet params) {
			addInput(params.prefix);
			addInput(params.reads);
			addInput(params.genomic);
			addInput(params.nthreads);
			addInput(params.plot);
			
			addOutput(params.infer3pparam);
			addOutput(params.infer3ptsv);
		}

		@Override
		public String execute(GediProgramContext context) throws Exception {
			String prefix = getParameter(0);
			GenomicRegionStorage<DefaultAlignedReadsData> reads = getParameter(1);
			Genomic genomic = getParameter(2);
			int nthreads = getIntParameter(3);
			boolean plot = getParameter(4);

			
			String[] signals = {"AATAAA","ATTAAA","AGTAAA","TATAAA"};
			Trie<Integer> strie = new Trie<>();
			for (int i=0; i<signals.length; i++)
				strie.put(signals[i], i);
			
			int[] signalrange = {12,30}; // most of the ccds transcripts have the start of the aataaa in there! 
			int minDistOther = 1000;
			
			int[] count = new int[6];
			
			ArrayList<ImmutableReferenceGenomicRegion<Transcript>> niceTranscripts = genomic.getTranscripts().ei()
				.progress(context.getProgress(), (int)genomic.getTranscripts().size(), t->t.getData().getTranscriptId())
				.sideEffect(x->count[0]++)
				.filter(t->t.getData().isCoding())
				.sideEffect(x->count[1]++)
				.filter(t->{
					int t3p = GenomicRegionPosition.ThreePrime.position(t);
					int t3p1 = GenomicRegionPosition.ThreePrime.position(t,1);
					return genomic.getTranscripts()
						.ei(new ImmutableReferenceGenomicRegion<>(t.getReference(), new ArrayGenomicRegion(Math.min(t3p1,t3p),Math.max(t3p1,t3p))))
						.filter(o->!o.equals(t))
						.filter(o->Math.abs(GenomicRegionPosition.ThreePrime.position(o)-t3p)<minDistOther)
						.count()==0;
				})
				.sideEffect(x->count[2]++)
				.map(t->new MutableTriple<>(t,genomic.getSequence(t.getData().get3Utr(t)).toString(),genomic.getSequence(t.getDownstream(50)).toString()))
				.filter(tr->{
					String expectPas = tr.Item2.substring(Math.max(0, tr.Item2.length()-signalrange[1]));
					int paspos = expectPas.indexOf(signals[0]);
					return paspos>=0 && paspos<signalrange[1]-signalrange[0];
				})
				.sideEffect(x->count[3]++)
				.filter(tr->tr.Item2.length()>signalrange[1]?
					strie.iterateAhoCorasick(tr.Item2.substring(0,tr.Item2.length()-signalrange[1])).count()==0
					:true
				)
				.sideEffect(x->count[4]++)
				.filter(tr->
					SequenceUtils.getPolyAStretches(tr.Item2+tr.Item3).size()==0
				)
				.sideEffect(x->count[5]++)
				.map(tr->tr.Item1)
				.list();
			
			context.logf("Initial transcripts:  %d",count[0]);
			context.logf("Removed non-coding:   %d",count[1]);
			context.logf("Removed unclear 3':   %d",count[2]);
			context.logf("Removed no PAS:       %d",count[3]);
			context.logf("Removed another PAS:  %d",count[4]);
			context.logf("Removed pA stretches: %d",count[5]);
			
			Collections.shuffle(niceTranscripts);
			
			ClusterPositionStatistics stat = EI.wrap(niceTranscripts)
				.parallelized(nthreads, 8, ei->ei.map(t->{
					ClusterPositionStatistics tmvo = new ClusterPositionStatistics();
					for (ImmutableReferenceGenomicRegion<DefaultAlignedReadsData> read : reads.ei(t).filter(r->t.getRegion().contains(r.map(0))).loop()) {
						int s = read.map(0);
						if (t.getRegion().contains(s)) {
							int dist = t.getRegion().getTotalLength()-t.induce(s);
							if (dist<=1000) 
								tmvo.add(dist, read.getData().getTotalCountOverall(ReadCountMode.Weight));
						}
					}
					return new MutablePair<>(tmvo,t);
				}))
				.progress(context.getProgress(),(int)niceTranscripts.size(), t->t.Item2.toLocationString())
				.reduce(new ClusterPositionStatistics(),(a,b)->b.add(a.Item1));

			FileUtils.writeAllLines(new String[] {"Name\tValue","Mean\t"+stat.mvo.getMean(),"Sd\t"+stat.mvo.getStandardDeviation(),""}, getOutputFile(0));

			EI.seq(0, stat.histo.length).filterInt(i->stat.histo[i]>0).map(i->i+"\t"+stat.histo[i]).print("Distance\tFrequency", getOutputFile(1).getPath());
			
			
			if (plot) {
				try {
					context.getLog().info("Running R scripts for plotting");
					RRunner r = new RRunner(prefix+".plotclusterdist.R");
					r.set("prefix",prefix);
					r.addSource(getClass().getResourceAsStream("/resources/R/plotclusterdist.R"));
					r.run(true);
				} catch (Throwable e) {
					context.getLog().log(Level.SEVERE, "Could not plot!", e);
				}
			}
			
			return null;
		}
		
		
	}

	private static class ClusterPositionStatistics {
		private WeightedMeanVarianceOnline mvo = new WeightedMeanVarianceOnline();
		private double[] histo = new double[1000];
		public void add(int d, double w) {
			mvo.add(d,w);
			histo[d]+=w;
		}
		
		
		public ClusterPositionStatistics add(ClusterPositionStatistics other) {
			this.mvo.add(other.mvo);
			ArrayUtils.add(this.histo, other.histo);
			return this;
		}
	}
	
	public static class PolyaParameterSet extends GediParameterSet {

		public GediParameter<Integer> minReads = new GediParameter<Integer>(this,"min", "minimal number of reads to count", false, new IntParameterType(), 1);
		public GediParameter<Integer> flank = new GediParameter<Integer>(this,"flank", "Add counts within this window!", false, new IntParameterType(), 10);
		public GediParameter<Integer> nthreads = new GediParameter<Integer>(this,"nthreads", "The number of threads to use for computations", false, new IntParameterType(), Runtime.getRuntime().availableProcessors());
		public GediParameter<String> prefix = new GediParameter<String>(this,"prefix", "Output prefix", true, new StringParameterType());
		public GediParameter<Genomic> genomic = new GediParameter<Genomic>(this,"g", "Genomic name", true, new GenomicParameterType());
		public GediParameter<GenomicRegionStorage<AlignedReadsData>> reads = new GediParameter<GenomicRegionStorage<AlignedReadsData>>(this,"reads", "The mapped reads from the ribo-seq experiment.", false, new StorageParameterType<AlignedReadsData>());
		public GediParameter<Strandness> strandness = new GediParameter<Strandness>(this,"strandness", "Whether sequencing protocol was stranded (Sensse), strand unspecific (Unspecific), or opposite strand (Antisense).", false, new EnumParameterType<>(Strandness.class));
		public GediParameter<Boolean> plot = new GediParameter<Boolean>(this,"plot", "Produce plots", false, new BooleanParameterType());

		public GediParameter<Boolean> test = new GediParameter<Boolean>(this,"test", "Only search against chromosome 22", false, new BooleanParameterType());
		
		
		public GediParameter<File> clustercit = new GediParameter<File>(this,"${prefix}.cluster.cit", "Clusters for the viewer", false, new FileParameterType());
		public GediParameter<File> annotation = new GediParameter<File>(this,"${prefix}.annot.tsv", "Clusters for the viewer", false, new FileParameterType());
		public GediParameter<File> motifs = new GediParameter<File>(this,"${prefix}.motif.tsv", "Clusters for the viewer", false, new FileParameterType());
		public GediParameter<File> pairwise = new GediParameter<File>(this,"${prefix}.pairwise.tsv", "Clusters for the viewer", false, new FileParameterType());
		
		public GediParameter<File> infer3ptsv = new GediParameter<File>(this,"${prefix}.3p.tsv", "Table of 3' end information.", false, new FileParameterType());
		public GediParameter<File> infer3pparam = new GediParameter<File>(this,"${prefix}.3p.parameter", "3' parameter file.", false, new FileParameterType());

	}

	
}
