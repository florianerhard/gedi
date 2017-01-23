package gedi.riboseq.inference.orf;

import gedi.core.data.annotation.Transcript;
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericBuilder;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.riboseq.cleavage.RiboModel;
import gedi.riboseq.inference.codon.Codon;
import gedi.riboseq.inference.codon.CodonInference;
import gedi.riboseq.inference.codon.CodonType;
import gedi.riboseq.inference.orf.SpliceGraph.Intron;
import gedi.util.ArrayUtils;
import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.datastructure.graph.SimpleDirectedGraph;
import gedi.util.datastructure.graph.SimpleDirectedGraph.AdjacencyNode;
import gedi.util.datastructure.tree.Trie;
import gedi.util.datastructure.tree.redblacktree.IntervalTreeSet;
import gedi.util.datastructure.unionFind.UnionFind;
import gedi.util.functions.EI;
import gedi.util.math.stat.inference.EquivalenceClassCountEM;
import gedi.util.math.stat.inference.EquivalenceClassMinimizeFactors;
import gedi.util.mutable.MutableMonad;
import gedi.util.mutable.MutablePair;
import gedi.util.userInteraction.progress.Progress;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.ToDoubleFunction;

import jdistlib.Binomial;
import jdistlib.ChiSquare;

import org.apache.commons.math3.stat.inference.MannWhitneyUTest;

import cern.colt.bitvector.BitVector;


/**
 * Orfs with less than minUniqueCodons (3) and lower total activity than minOrfActivity (10) are filtered and not even considered!
 * 
 * All other orfs are reported and a passes all filters flag is set if
 * - it's not to short (minAaLength=5)
 * - it has a start and a stop codon
 * - its isoform fraction is not too small (minIsoformFraction=0.01)
 * - its trimmed mean coverage is not too small (minTmCov=0.1)
 * - it exceeds the internal pvalue of 0.01
 * 
 * @author erhard
 *
 */
public class OrfFinder {


	private GenomicRegionStorage<Transcript> annotation;
	private MemoryIntervalTreeStorage<ImmutableReferenceGenomicRegion<Transcript>> intronless;
	private GenomicRegionStorage<AlignedReadsData> reads;
	private CodonInference inference;

	private HashMap<String,ArrayList<ImmutableReferenceGenomicRegion<Transcript>>> geneMap;
	private boolean useReadSplits = false;

	private int miniter = 100;
	private int maxiter = 10000;
	private int maxAminoDist = 25;
	private double minCodonActivity = 0.1;
	private double minOrfTotalActivity = 50;
	private double minOrfExperimentActivity = 20;
	private double minIsoformFraction = 0.01;
//	private double minClusterFraction = 0.01;
	private double minTmCov = 0.1;
	private int minAaLength = 5;
	private int minUniqueCodons = 3;
	private double minUniqueActivity = 20;
	private boolean useEM = false;
	
	private double minStitchCoverage = 0.5;
	private int maxStitchDistance = 50;
	private double orffrac =0.8;
	private double orffrac2 =0.4;
	private int minRi = 0;
	
	private boolean filterByGap = false;
	private boolean assembleAnnotationFirst = true;
	
	public OrfFinder(GenomicRegionStorage<Transcript> annotation,
			GenomicRegionStorage<AlignedReadsData> reads,
			CodonInference inference) {
		intronless = new MemoryIntervalTreeStorage<ImmutableReferenceGenomicRegion<Transcript>>((Class)ImmutableReferenceGenomicRegion.class);
		annotation.ei()
			.map(i->new ImmutableReferenceGenomicRegion<ImmutableReferenceGenomicRegion<Transcript>>(i.getReference(),i.getRegion().removeIntrons(),i))
			.forEachRemaining(i->intronless.add(i.getReference(), i.getRegion(), i.getData()));
		
		this.annotation = annotation;
		this.reads = reads;
		this.inference = inference;
	}

	

	private HashMap<String, ArrayList<ImmutableReferenceGenomicRegion<Transcript>>> getGeneMap(Progress progress) {
		if (geneMap==null) {
			geneMap = new HashMap<String, ArrayList<ImmutableReferenceGenomicRegion<Transcript>>>();
			progress.init().setDescription("Collecting transcripts per gene...");
			annotation.iterateReferenceGenomicRegions().forEachRemaining(rgr-> {
				if (rgr.getData().isCoding())
					geneMap.computeIfAbsent(rgr.getData().getGeneId(), g->new ArrayList<ImmutableReferenceGenomicRegion<Transcript>>()).add(rgr);
			});
			progress.finish();
		}
		return geneMap;
	}
	
	public void setAssembleAnnotationFirst(boolean assembleAnnotationFirst) {
		this.assembleAnnotationFirst = assembleAnnotationFirst;
	}
	
	public void setFilterByGap(boolean filterByGap) {
		this.filterByGap = filterByGap;
	}
	
	public void setUseEM(boolean em) {
		this.useEM = em;
	}
	
	public void setMinimalReproducibilityIndex(int minRi) {
		this.minRi  = minRi;
	}
	
	public void setUseReadSplits(boolean useReadSplits) {
		this.useReadSplits = useReadSplits;
	}

	public MemoryIntervalTreeStorage<Orf> computeGene(String gene, Progress progress) throws IOException {

		HashMap<String,ArrayList<ImmutableReferenceGenomicRegion<Transcript>>> geneMap = getGeneMap(progress);


		progress.setDescriptionf("Processing "+gene).incrementProgress();

		int start = -1;
		int end = -1;
		ReferenceSequence ref = null;
		for (ImmutableReferenceGenomicRegion<Transcript> tr : geneMap.get(gene)) {
			if (ref==null) {
				start = tr.getRegion().getStart();
				end = tr.getRegion().getEnd();
				ref = tr.getReference();
			} else {
				if (!ref.equals(tr.getReference())) throw new RuntimeException("Gene on more than one chromosome: "+gene);
				start = Math.min(start, tr.getRegion().getStart());
				end = Math.max(end, tr.getRegion().getEnd());
			}
		}
		return computeChunk(-1, ref, start, end, null);
	}
	
//	private void setRmq(DiskGenomicNumericBuilder codon, ReferenceSequence ref, int genomic, int offset, double act, float[] buff) {
//		buff[(genomic-offset+3)%3] = (float)act;
//		codon.addValueEx(ref, genomic, buff); // FIX: spliced codons!
//		buff[(genomic-offset+3)%3] = 0;
//	}
	
	public MemoryIntervalTreeStorage<Orf> computeChunk(int index, ReferenceSequence ref, int start, int end, Consumer<ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>>> codonOutProc) { //DiskGenomicNumericBuilder codon, DiskGenomicNumericBuilder[] perCondCodon

		
		MutableMonad<ToDoubleFunction<Collection<Codon>>> gofComp = new MutableMonad<ToDoubleFunction<Collection<Codon>>>();
		ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons = inference.inferCodons(reads, ref, start, end, maxAminoDist*3, gofComp);

		if (codonOutProc!=null)
			codonOutProc.accept(codons);
		
//		if (codon!=null) {
//			synchronized (codon) {
//				float[] data = new float[3];
//				for (Codon c : codons.getData()) {
//					if (c.getTotalActivity()==0) continue;
//					
//					setRmq(codon, ref, 
//							codons.map(c.map(ref.getStrand().equals(Strand.Minus)?2:0)),0,
//							c.getTotalActivity(), data);
//					setRmq(codon, ref, 
//							codons.map(c.map(1)),1,
//							c.getTotalActivity(), data);
//					setRmq(codon, ref, 
//							codons.map(c.map(ref.getStrand().equals(Strand.Minus)?0:2)),2,
//							c.getTotalActivity(), data);
//					
////					int genomic = codons.map(ref.getStrand().equals(Strand.Minus)?c.getStop():c.getStart());
////					data[genomic%3] = (float) c.getTotalActivity();
////					codon.addValueEx(ref, genomic, data); // FIX: spliced codons!
////					codon.addValueEx(ref, genomic+1, data);
////					codon.addValueEx(ref, genomic+2, data);
////					data[genomic%3] = 0;
//				}				
//			}
//		}
//		
//		if (perCondCodon!=null) {
//			synchronized (perCondCodon) {
//				float[] data = new float[3];
//				for (int i=0; i<perCondCodon.length; i++) {
//					for (Codon c : codons.getData()) {
//						if (c.getActivity()[i]==0) continue;
//						
//						setRmq(perCondCodon[i], ref, 
//								codons.map(c.map(ref.getStrand().equals(Strand.Minus)?2:0)),0,
//								c.getActivity()[i], data);
//						setRmq(perCondCodon[i], ref, 
//								codons.map(c.map(1)),1,
//								c.getActivity()[i], data);
//						setRmq(perCondCodon[i], ref, 
//								codons.map(c.map(ref.getStrand().equals(Strand.Minus)?0:2)),2,
//								c.getActivity()[i], data);
//						
////						int genomic = codons.map(ref.getStrand().equals(Strand.Minus)?c.getStop():c.getStart());
////						data[genomic%3] = (float) c.getActivity()[i];
////						perCondCodon[i].addValueEx(ref, genomic, data);
////						perCondCodon[i].addValueEx(ref, genomic+1, data);
////						perCondCodon[i].addValueEx(ref, genomic+2, data);
////						data[genomic%3] = 0;
//					}
//				}
//			}
//		}
		
		SpliceGraph sg = new SpliceGraph(0, end-start);

		CharSequence sequence = inference.getGenome().getSequence(codons);

		if (useReadSplits )
			reads.iterateIntersectingMutableReferenceGenomicRegions(ref,start,end).forEachRemaining(read->sg.addIntrons(codons.induce(read.getRegion())));
		annotation.iterateIntersectingMutableReferenceGenomicRegions(ref,start,end).forEachRemaining(tr->{
			sg.addIntrons(codons.induce(tr.getRegion()));
		});
		
		if (!useReadSplits) {
			Iterator<Codon> it = codons.getData().iterator();
			while (it.hasNext()) {
				Codon c = it.next();
				for (int i=1; i<c.getNumParts(); i++) {
					if (!sg.contains(c.getEnd(i-1),c.getStart(i))) {
						it.remove();
						break;
					}
				}
			}
		}
		
//		// codons region spans 1 - l-1
//		double[] codTotal = new double[codons.getRegion().getTotalLength()+2];
//		for (Codon c : codons.getData())
//			codTotal[c.getStart()+1]+=c.getTotalActivity();
		
		RiboModel scoring = inference.getModels()[0];
		
		MemoryIntervalTreeStorage<Orf> re = new MemoryIntervalTreeStorage<Orf>(Orf.class);
		ArrayList<OrfWithCodons> orfs = findOrfs(index, sequence.toString(), sg, codons);
		
		// stitch orfs if all of them have a coverage of minStitchCoverage and are minStichDistance away from each other (and there is no stop codon...)
		tryToStitch(orfs, sg, sequence.toString());
		
		double totalClu = EI.wrap(codons.getData()).mapToDouble(m->m.getTotalActivity()).sum();
		
		for (OrfWithCodons orf : orfs) {
				
//			if (codons.map(orf.getRegion()).getStart()==143233)
//				System.out.println(codons.map(orf.getRegion()));
			Orf oorf = orf.toOrf();
			oorf.clusterFraction = oorf.getActivityUnique()/totalClu;
			
			oorf.sequence = SequenceUtils.extractSequence(orf.getRegion(), sequence);
			
			oorf.hasStop = orf.hasStopCodon();
			
			
			oorf.startScores = new double[orf.getRegion().getTotalLength()/3];
			oorf.changePointScores = new double[orf.getRegion().getTotalLength()/3];
			double[][] accc = oorf.uniquePval<0.01?oorf.estCodonsEach:oorf.estCodonsUnique;
			double[] ccc = null;
			for (int c=0; c<accc.length; c++)
				ccc = ArrayUtils.add(ccc, accc[c]);
			double[] cccc = ccc.clone();
			ArrayUtils.cumSumInPlace(cccc, 1);
			
			for (int p=0; p<accc[0].length; p++) {
//				oorf.startScores[p] = scoring.computeStartScore(accc,p,accc[0].length-1, useSingleStartScores);
				if (!scoring.hasLfc())
					oorf.startScores[p] = scoring.computeSvmStartProbability(accc,p,accc[0].length-1);
				oorf.changePointScores[p] = scoring.computeChangePointScore(ccc,p,ccc.length-1);
			}
			
			if (scoring.hasLfc())
				oorf.startScores = scoring.computeSvmLfcProbabilities(accc);
			
			GenomicRegion eorf = orf.getRegion().extendFront(1).extendBack(1);
			double[] codTotal = new double[eorf.getTotalLength()];
			for (Codon c : codons.getData())
				if (eorf.containsUnspliced(c))
					codTotal[eorf.induce(c.getStart())]+=c.getTotalActivity();
			
			
			BitVector disallowed = new BitVector(accc[0].length);
			oorf.inferredStartPosition = -1;
			boolean thisistheend = false;
			while (oorf.inferredStartPosition<0 && !thisistheend) {
				
				int startCand = inferStart(oorf,ccc,cccc, disallowed,scoring.hasLfc());
				
				if (startCand==-1) {
					// try first and exit
					disallowed.clear();
					startCand = inferStart(oorf,ccc,cccc, disallowed,scoring.hasLfc());
					thisistheend = true;
				}
				
				if (startCand>=0) {
					
					
	
					oorf.tmCov = NumericArrayFunction.trimmedMean(scoring.getTrim()).applyAsDouble(NumericArray.wrap(ccc, startCand, ccc.length));//,ngaps,ccc.length));
					
					double[] sccc = ArrayUtils.slice(ccc, startCand, ccc.length);
					
					DoubleArrayList x = new DoubleArrayList(sccc.length);
					DoubleArrayList y = new DoubleArrayList(sccc.length);
					for (int i=0; i<sccc.length; i++)
						if (sccc[i]>scoring.getThreshold())
							x.add(i);
						else
							y.add(i);
					double uniformity = x.isEmpty()||y.isEmpty()?1:new MannWhitneyUTest().mannWhitneyUTest(x.toDoubleArray(), y.toDoubleArray());
					
					
					boolean stopPresent = sccc[sccc.length-1]>scoring.getThreshold();
					Arrays.sort(sccc);
					int ngaps = 0;
					for (;ngaps<sccc.length && sccc[ngaps]<=scoring.getThreshold(); ngaps++);
	
//					double tmCov = NumericArrayFunction.trimmedMean(model.getTrim()).applyAsDouble(NumericArray.wrap(ccc,ngaps,ccc.length));
					double meanCov = ArrayUtils.sum(sccc)/sccc.length;
					
					int present = sccc.length-1-ngaps+(stopPresent?1:0);
					
					
					oorf.internalPval = scoring.computeErrorOrf(codTotal, startCand*3+1, codTotal.length-3);
					oorf.uniformityPval = uniformity;
					oorf.gapPval = (ngaps-1.0)/(oorf.startScores.length-1-startCand)>scoring.getGapThreshold(meanCov)?0:1;//getGapPvalue(oorf.startScores.length-1-startCand, meanCov, ngaps-1);
					oorf.presentPval = scoring.getPresentPvalue(sccc.length-1, present); 
					oorf.stopScore = scoring.computeStopScore(accc, startCand, accc[0].length-1);
	
					double total = ArrayUtils.sum(sccc);
					boolean allok =  //orf.getRegion().getTotalLength()/3-1-startCand>=minAaLength &&
							total>minOrfTotalActivity && oorf.hasStop && 
							oorf.activityFraction>minIsoformFraction && 
							oorf.tmCov>minTmCov && 
							oorf.internalPval<0.01
							;
					
					if (thisistheend && startCand>=0 && total<=minOrfTotalActivity)
						startCand = -1;
									
					if (allok || thisistheend) 
						oorf.inferredStartPosition = startCand;
					else
						disallowed.putQuick(startCand, true);
				} else {
					oorf.tmCov = 0;
					oorf.internalPval = 1;
					oorf.presentPval = 1;
					oorf.gapPval = 1;
					oorf.stopScore = 0;
				}
				
				
			}
			
			if (oorf.inferredStartPosition>-1) {
				// recompute actitivies!
				oorf.activityUnique = ArrayUtils.sum(ccc,oorf.inferredStartPosition,ccc.length);
				for (int c=0; c<oorf.estCodonsEach.length; c++) 
					oorf.activities[c] = ArrayUtils.sum(oorf.estCodonsEach[c],oorf.inferredStartPosition,oorf.estCodonsEach[c].length);
			}
			oorf.ri = 0;
			for (int c=0; c<oorf.estCodonsEach.length; c++) 
				if (oorf.activities[c]>=minOrfExperimentActivity)
					oorf.ri++;
			
			int bigger1 = 0;
			int missedgof = 0;
			double total = 0;
			ArrayList<Codon> orfCodons = new ArrayList<Codon>();
			for (Codon c : orf.getEstCodons())
				if (orf.getRegion().induce(c.getStart())/3>=oorf.inferredStartPosition) {
					orfCodons.add(new Codon(codons.map(c),c));
					total+=c.getTotalActivity();
					if (c.getTotalActivity()>=1) {
						bigger1++;
						if (c.getGoodness()>scoring.getCodonGofThreshold(c.getTotalActivity()))
							missedgof++;
					}
				}
			
			
			double gof = gofComp.Item.applyAsDouble(orfCodons); 
			oorf.gof = Binomial.cumulative(missedgof, bigger1, 0.01, false, false);
			oorf.gof = gof>scoring.getOrfGofThreshold(total)?0:1;
			
			
//			System.out.println(codons.map(orf.getRegion()));
			// odds scores are not filtered!
			oorf.passesAllFilters = orf.getRegion().getTotalLength()/3-1-oorf.inferredStartPosition>=minAaLength &&
					oorf.inferredStartPosition>=0 && oorf.hasStop && 
					oorf.activityFraction>minIsoformFraction && 
					oorf.tmCov>minTmCov && 
					oorf.internalPval<0.01 &&
					(!filterByGap || oorf.gapPval>0.01) && 
					oorf.presentPval<0.01 &&
//					oorf.clusterFraction>minClusterFraction &&
					oorf.ri>=minRi && 
					(!scoring.hasLfc() || oorf.startScores[oorf.inferredStartPosition]>inference.getModels()[0].getSvmLfcCutoff());
			
			
			double offframe = 0;
			double inframe = 0;
			
			ArrayGenomicRegion cds = orf.getRegion().map(new ArrayGenomicRegion(oorf.inferredStartPosition*3,orf.getRegion().getTotalLength()));
			for (Codon c : codons.getData().getIntervalsIntersecting(cds.getStart(), cds.getStop(), new ArrayList<Codon>())) {
				if (cds.containsUnspliced(c)) {
					int frame = cds.induce(c).getStart()%3;
					if (frame==0) inframe+=c.getTotalActivity();
					else offframe+=c.getTotalActivity();
				}
			}
			oorf.inframefraction = inframe/(inframe+offframe);
			
			ImmutableReferenceGenomicRegion<Orf> r = new ImmutableReferenceGenomicRegion<Orf>(ref, codons.map(orf.getRegion()), oorf);
			

			annotate(r);		
			re.add(r);
			
//			if (codOut!=null && r.getData().getOrfType()==OrfType.CDS) {
//				double[] d = ArrayUtils.slice(codTotal, r.getData().getInferredStartPosition()*3+1, codTotal.length-1);
//				try {
//					codOut.writef("%s\t%s\t%.1f\t%s\n", r.getData().getReference().getData().getTranscriptId(),r.toLocationString(), r.getData().getActivityUnique(), StringUtils.concat(",", d));
//				} catch (IOException e) {
//					throw new RuntimeException("Could not write codons!",e);
//				}
//			}
		}
		
		return re;
	}

//	private LineWriter codOut;
//	public OrfFinder setCodonOut(LineWriter codOut) {
//		this.codOut = codOut;
//		return this;
//	}

//	private double maxStart(double[] startScores, int start, int end) {
//		double re = startScores[start];
//		for (; start<end; start++)
//			re = Math.max(re,startScores[start]);
//		return re;
//	}



	private int inferStart(Orf oorf, double[] ccc, double[] cccc, BitVector disallowed, boolean score) {

		int best = -1;
		if (score) {
			// first look for the best canonical start codon with positive start score containing >orffrac of the total activity
			double bestScore = inference.getModels()[0].getSvmStartCutoff();
			for (int p=0; p<oorf.startScores.length; p++) {
				if (cccc[p]-ccc[p]>(1-orffrac)*cccc[cccc.length-1])
					break;
				if (oorf.getCodonType(p)==CodonType.Start && oorf.startScores[p]>bestScore && !disallowed.get(p)) {
					best = p;
					bestScore = oorf.startScores[p];
				}
			}
			if (best!=-1) return best;
			
			// then look for the best non-canonical start codon with positive start score containing >orffrac of the total activity
			for (int p=0; p<oorf.startScores.length; p++) {
				if (cccc[p]-ccc[p]>(1-orffrac)*cccc[cccc.length-1])
					break;
				if (oorf.getCodonType(p)==CodonType.NoncanonicalStart && oorf.startScores[p]>bestScore && !disallowed.get(p)) {
					best = p;
					bestScore = oorf.startScores[p];
				}
			}
			if (best!=-1) return best;
			
			// first look for the best canonical start codon with positive start score containing >orffrac2 of the total activity
			bestScore = inference.getModels()[0].getSvmStartCutoff();
			for (int p=0; p<oorf.startScores.length; p++) {
				if (cccc[p]-ccc[p]>(1-orffrac2)*cccc[cccc.length-1])
					break;
				if (oorf.getCodonType(p)==CodonType.Start && oorf.startScores[p]>bestScore && !disallowed.get(p)) {
					best = p;
					bestScore = oorf.startScores[p];
				}
			}
			if (best!=-1) return best;
			
			// then look for the best non-canonical start codon with positive start score containing >orffrac2 of the total activity
			for (int p=0; p<oorf.startScores.length; p++) {
				if (cccc[p]-ccc[p]>(1-orffrac2)*cccc[cccc.length-1])
					break;
				if (oorf.getCodonType(p)==CodonType.NoncanonicalStart && oorf.startScores[p]>bestScore && !disallowed.get(p)) {
					best = p;
					bestScore = oorf.startScores[p];
				}
			}
			if (best!=-1) return best;
		}
		
		// then look for the first canonical start codon containing >orffrac of the total activity
		for (int p=0; p<oorf.startScores.length; p++) {
			if (cccc[p]-ccc[p]>(1-orffrac)*cccc[cccc.length-1])
				break;
			if (oorf.getCodonType(p)==CodonType.Start) {
				if (!disallowed.get(p)) return p;
			}
		}
		
		// then look for the first noncanonical start codon containing >90% of the total activity
		for (int p=0; p<oorf.startScores.length; p++) {
			if (cccc[p]-ccc[p]>(1-orffrac)*cccc[cccc.length-1])
				break;
			if (oorf.getCodonType(p)==CodonType.NoncanonicalStart) {
				if (!disallowed.get(p)) return p;
			}
		}

		return -1;
	}



	private void tryToStitch(ArrayList<OrfWithCodons> orfs, SpliceGraph sg, String sequence) {
		if (orfs.size()<=1) return;
		
		Collections.sort(orfs,(a,b)->a.getRegion().compareTo(b.getRegion()));
		
		
		
		for (int f=0; f<orfs.size(); f++) {
			if (orfs.get(f).getEstimatedTotalActivity()/(orfs.get(f).getRegion().getTotalLength()/3)>=minStitchCoverage)
				for (int t=f+1; t<orfs.size(); t++) 
					if (orfs.get(t).getEstimatedTotalActivity()/(orfs.get(t).getRegion().getTotalLength()/3)>=minStitchCoverage && orfs.get(f).getRegion().getEnd()<orfs.get(t).getRegion().getStart()) {
						OrfWithCodons from = orfs.get(f);
						OrfWithCodons to = orfs.get(t);
						MutableMonad<ArrayGenomicRegion> inbetween = new MutableMonad<>(new ArrayGenomicRegion(from.getRegion().getEnd(), to.getRegion().getStart()));
						if (inbetween.Item.getTotalLength()%3!=0 || inbetween.Item.getTotalLength()/3>=maxStitchDistance || containsInframeStop(SequenceUtils.extractSequence(inbetween.Item.extendFront(3), sequence)))
							inbetween.Item = null;
						
						sg.forEachIntronStartingBetween(from.getRegion().getEnd(),maxStitchDistance, intron->{
							ArrayGenomicRegion reg = new ArrayGenomicRegion(from.getRegion().getEnd(), intron.getStart(), intron.getEnd(), to.getRegion().getStart());
							if (reg.getTotalLength()%3!=0 || reg.getTotalLength()>=maxStitchDistance || containsInframeStop(SequenceUtils.extractSequence(reg.extendFront(3), sequence))) {
								if (inbetween.Item==null || inbetween.Item.getTotalLength()>reg.getTotalLength())
									inbetween.Item = reg;
							}
						});
						
						if (inbetween.Item!=null) {
							// yeah, I can stitch them!
							from.stitchWith(to, inbetween.Item);
							f--;
							orfs.remove(t);
							break; // continue with from and the check all possible to again!
						}
					
					}
		}
		
		
	}



	private void annotate(ImmutableReferenceGenomicRegion<Orf> r) {
		ArrayList<ImmutableReferenceGenomicRegion<Transcript>> tr = intronless.ei(r).map(i->i.getData()).toCollection(new ArrayList<>());
		Comparator<ImmutableReferenceGenomicRegion<Transcript>> comp = (a,b)->Integer.compare(a.getRegion().getTotalLength(), b.getRegion().getTotalLength());
		comp = comp.thenComparing((a,b)->a.getData().getTranscriptId().compareTo(b.getData().getTranscriptId())); // just to make it stable for two different orfs
		Collections.sort(tr,comp);
		
		
		for (OrfType type : OrfType.values()) {
			for (ImmutableReferenceGenomicRegion<Transcript> t : tr) {
				if (type.is(r, t)) {
					r.getData().reference = t;
					r.getData().orfType = type;
					return;
				}
			}
		}
		r.getData().orfType = OrfType.oORF;
	}


	public MemoryIntervalTreeStorage<Orf> computeByGenes(Progress progress) throws IOException {
		HashMap<String,ArrayList<ImmutableReferenceGenomicRegion<Transcript>>> geneMap = getGeneMap(progress);

		MemoryIntervalTreeStorage<Orf> re = new MemoryIntervalTreeStorage<Orf>(Orf.class);
		progress.init().setCount(geneMap.size());
		for (String gene : geneMap.keySet()) {
			re.fill(computeGene(gene,progress));
		}

		progress.finish();
		return re;
	}


	/**
	 * Coordinates are in codonsRegion space!
	 * @param index
	 * @param sequence
	 * @param sg
	 * @param codonsRegion
	 * @return
	 */
	public ArrayList<OrfWithCodons> findOrfs(int index, String sequence, SpliceGraph sg, 
			ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codonsRegion) {
		SimpleDirectedGraph<Codon> fg = new SimpleDirectedGraph<Codon>("Codongraph");

//		if (!codonsRegion.getReference().toString().equals("chr4+") || !codonsRegion.getRegion().contains(140_283_087))
//			return 0;
		
		LeftMostInFrameAndClearList buff = new LeftMostInFrameAndClearList();

		IntervalTreeSet<Codon> codons = codonsRegion.getData();
		codons.removeIf(c->c.getTotalActivity()<minCodonActivity );
		if (codons.size()==0) return new ArrayList<OrfWithCodons>();
		
		// add stop codons for easy orf inference
		HashSet<Codon> stopCodons = new HashSet<Codon>();
		Trie<String> stop = new Trie<String>();
		stop.put("TAG", "TAG");
		stop.put("TGA", "TGA");
		stop.put("TAA", "TAA");
		stop.iterateAhoCorasick(sequence).map(r->new Codon(new ArrayGenomicRegion(r.getStart(),r.getEnd()), r.getValue())).toCollection(stopCodons);

		for (Intron intr : sg.iterateIntrons().loop()) {
			ArrayGenomicRegion reg = new ArrayGenomicRegion(intr.getStart()-2, intr.getStart(), intr.getEnd(), intr.getEnd()+1);
			String cod = stop.get(SequenceUtils.extractSequence(reg, sequence));
			if (cod!=null)
				stopCodons.add(new Codon(reg,cod));
			
			reg = new ArrayGenomicRegion(intr.getStart()-1, intr.getStart(), intr.getEnd(), intr.getEnd()+2);
			cod = stop.get(SequenceUtils.extractSequence(reg, sequence));
			if (cod!=null)
				stopCodons.add(new Codon(reg,cod));
		}
		stopCodons.removeAll(codons);
		codons.addAll(stopCodons);
		
		ArrayList<OrfWithCodons> re = new ArrayList<OrfWithCodons>();
		HashSet<Codon> usedForAnno = new HashSet<Codon>();
		
		
		if (assembleAnnotationFirst ) {
			// new: first use annotated transcripts in a greedy fashion
			ArrayList<ImmutableReferenceGenomicRegion<Transcript>> transcripts = annotation.ei(codonsRegion)
					.filter(t->t.getData().isCoding())
					.map(t->codonsRegion.induce(t, "T"))
					.list();
			
			int acount = 0;
			LinkedList<OrfWithCodons> orfs = new LinkedList<OrfWithCodons>();
			GenomicRegion best;
			HashSet<Codon> aremoved = new HashSet<Codon>();
			
			do {
				best = null;
				double bestSum = 0;
				for (ImmutableReferenceGenomicRegion<Transcript> tr : transcripts) {
					double[] a = new double[tr.getRegion().getTotalLength()];
					for (Codon c : codons) {
						if (tr.getRegion().containsUnspliced(c)) {
							int p = tr.induce(c.getStart());
							
							assert a[p]==0;
							
							if (!aremoved.contains(c))
								a[p]=c.totalActivity;
							if (c.isStop())
								a[p] = -1;
						}
					}
					for (int f=0; f<3; f++) {
						int s = -1;
						double sum = 0;
						
						for (int p=f; p<a.length; p+=3) {
							if (a[p]==-1) {//stop
								if (sum>bestSum) {
									bestSum = sum;
									best = tr.getRegion().map(new ArrayGenomicRegion(s,p+3));
								}
								s = -1;
								sum = 0;
							}
							else 
								sum+=a[p];
							
							if (a[p]>0 && s==-1)
								s=p;
						}
					}
				}
				if (best!=null) {
					ArrayList<Codon> cods = new ArrayList<>();
					int uniqueCodons = 0;
					double uniqueActivity = 0;
					double totalActivity = 0;
					
					for (Codon c : codons) {
						if (best.containsUnspliced(c) && best.induce(c.getStart())%3==0) {
							if (aremoved.add(c)) {
								uniqueActivity+=c.totalActivity;
								uniqueCodons++;
							}
							totalActivity+=c.totalActivity;
							if (c.totalActivity>0)
								cods.add(c);
						}
					}
					
					if ((uniqueCodons>=minUniqueCodons || uniqueCodons==cods.size()) && uniqueActivity>minUniqueActivity && totalActivity>minOrfTotalActivity) {
						
						Collections.sort(cods);
						usedForAnno.addAll(cods);
						
						OrfWithCodons orf = new OrfWithCodons(index,0, acount++, best.toArrayGenomicRegion(),cods, true);
						orfs.add(orf);
					}
					
				}
			} while (best!=null);
			
			if (orfs.size()>1) {
				
				// they are not necessarily connected!
				LinkedList<OrfWithCodons>[] connected = findConnectedOrfs(orfs);
				orfs.clear();
				
				for (LinkedList<OrfWithCodons> corfs : connected) {
					for (boolean changed = true; changed && corfs.size()>1; ){
						changed = false;
						
						if (useEM)
							inferOverlappingOrfActivitiesEM(corfs);
						else
							overlapUniqueCoverage(corfs);
						
						Iterator<OrfWithCodons> it = corfs.iterator();
						while (it.hasNext()) {
							OrfWithCodons orf = it.next();
							if (orf.getEstimatedTotalActivity()<minOrfTotalActivity) {
								it.remove();
								changed = true;
							}
						}
					}
					
					if (corfs.size()>1) 
						distributeCodons(corfs);
					orfs.addAll(corfs);
				}
			}
			re.addAll(orfs);
		}
					
					
					
		// as edges only are represented in the splice graph, singleton codons are discarded (which does make sense anyway)
		for (Codon c : codons) {
 			if (!c.isStop()) {
				// find unspliced successors (can be more than one, when the successor codon itself is spliced! all of them have the same start!)
				int max = c.getEnd()+maxAminoDist*3;
				for (Codon n : codons.getIntervalsIntersecting(c.getEnd(), c.getEnd()+maxAminoDist*3, buff.startAndClear(c)).get()) {
					if (!containsInframeStop(sequence.substring(c.getEnd(), n.getStart()))) 
						fg.addInteraction(c,n);
					max = n.getStart()+2;
				}

				// find all spliced successors for each splice junction that comes before n or maxAminoDist
				sg.forEachIntronStartingBetween(c.getEnd(),max+1, intron->{
					for (Codon n : codons.getIntervalsIntersecting(intron.getEnd(), intron.getEnd()+maxAminoDist*3 - (intron.getStart()-c.getEnd()), buff.startAndClear(c,intron)).get())
						if (!containsInframeStop(SequenceUtils.extractSequence(new ArrayGenomicRegion(c.getStart(),intron.getStart(),intron.getEnd(),n.getStart()), sequence))) 
							fg.addInteraction(c, n, intron);
				});
			}
		}

			
			
		int cc = 1;
		for (SimpleDirectedGraph<Codon> g : fg.getWeaklyConnectedComponents()) {
			if (EI.wrap(g.getSources()).mapToDouble(c->c.getTotalActivity()).sum()==0) 
				continue;
			
			// iterate longest paths in g
			LinkedList<Codon> topo = g.getTopologicalOrder();
			HashSet<Codon> remInTopo = new HashSet<Codon>(topo);
			remInTopo.removeIf(c->!stopCodons.contains(c) && !usedForAnno.contains(c));
			HashSet<Codon> removed = new HashSet<Codon>(remInTopo);
			
//			double maxPathScore = 0;
			
			LinkedList<OrfWithCodons> orfs = new LinkedList<OrfWithCodons>();

			int count = 0;
			while (removed.size()<topo.size()) {
				HashMap<Codon,MutablePair<GenomicRegion, Double>> longestPrefixes = new HashMap<Codon, MutablePair<GenomicRegion,Double>>();
				for (Codon c : topo)
					longestPrefixes.put(c, new MutablePair<GenomicRegion, Double>(c, removed.contains(c)?0:(c.getTotalActivity())));

				Codon longestEnd = null;
				HashMap<Codon,Codon> backtracking = new HashMap<Codon,Codon>();

				for (Codon c : topo) {
//					if (codonsRegion.map(c).getStart()==100_466_118)
//						System.out.println(c);
//					
//					if (codonsRegion.map(c).getStart()==100_465_842)
//						System.out.println(c);
					
					double len = longestPrefixes.get(c).Item2;
					for (AdjacencyNode<Codon> n = g.getTargets(c); n!=null; n=n.next) {
						MutablePair<GenomicRegion, Double> pref = longestPrefixes.get(n.node);

						double nnact = removed.contains(n.node)?0:(n.node.getTotalActivity());
						if (pref.Item2<=len+nnact) {
							pref.set(extendFullPath(longestPrefixes.get(c).Item1,c,n.node,n.getLabel()), len+nnact);
							backtracking.put(n.node, c);
						}
					}
					if (longestEnd==null || longestPrefixes.get(longestEnd).Item2<=len)
						longestEnd = c;

				}

				// determine longest path by backtracking and mark all codons on the path as removed
				ArrayList<Codon> orfCodons = new ArrayList<Codon>();
				double totalActivity = 0;
				double uniqueActivity = 0;
				int uniqueCodons = 0;
				for (Codon c=longestEnd; c!=null; c=backtracking.get(c)) {
					if (removed.add(c) && c.getTotalActivity()>0) {
						uniqueCodons++;
						uniqueActivity+=c.getTotalActivity();
					}
					
					if (c.getTotalActivity()>0) // to remove dummy stop codons
						orfCodons.add(c);
					totalActivity+=c.getTotalActivity();
				}

//				System.out.println(codonsRegion.map(longestPrefixes.get(longestEnd).Item1));
				
				if ((uniqueCodons>=minUniqueCodons || uniqueCodons==orfCodons.size()) && uniqueActivity>minUniqueActivity && totalActivity>minOrfTotalActivity) {
					Collections.reverse(orfCodons);
	
					MutablePair<GenomicRegion, Double> triple = longestPrefixes.get(longestEnd);
					ArrayGenomicRegion region = triple.Item1.toArrayGenomicRegion();
					String lastCodon = SequenceUtils.extractSequence(region.map(new ArrayGenomicRegion(region.getTotalLength()-3, region.getTotalLength())), sequence);
					
					OrfWithCodons orf = new OrfWithCodons(index,cc, count++, region,orfCodons, stop.containsKey(lastCodon));
					orfs.add(orf);
				}
				
//				maxPathScore = Math.max(maxPathScore,totalActivity);
			}
			
			
			if (orfs.size()>1) {
				
				// they are not necessarily connected!
				
				LinkedList<OrfWithCodons>[] connected = findConnectedOrfs(orfs);
				orfs.clear();
				
				for (LinkedList<OrfWithCodons> corfs : connected) {
					for (boolean changed = true; changed && corfs.size()>1; ){
						changed = false;
						
						if (useEM)
							inferOverlappingOrfActivitiesEM(corfs);
						else
							overlapUniqueCoverage(corfs);
						
						Iterator<OrfWithCodons> it = corfs.iterator();
						while (it.hasNext()) {
							OrfWithCodons orf = it.next();
							if (orf.getEstimatedTotalActivity()<minOrfTotalActivity) {
								it.remove();
								changed = true;
							}
						}
					}
					
					if (corfs.size()>1) 
						distributeCodons(corfs);
					orfs.addAll(corfs);
				}
				
				
			}
			
			re.addAll(orfs);
			
			cc++;
		}

		return re;

	}
	
	
	private LinkedList<OrfWithCodons>[] findConnectedOrfs(
			LinkedList<OrfWithCodons> orfs) {
		
		HashMap<Codon,HashSet<OrfWithCodons>> cod2Orf = new HashMap<Codon, HashSet<OrfWithCodons>>();
		for (OrfWithCodons orf : orfs) 
			for (Codon c : orf.getCodons()) 
				cod2Orf.computeIfAbsent(c, x->new HashSet<>()).add(orf);

		UnionFind<OrfWithCodons> uf = new UnionFind<OrfWithCodons>(orfs);
		for (Codon c : cod2Orf.keySet()) 
			uf.unionAll(cod2Orf.get(c));
				
		return uf.getGroups(new LinkedList<OrfWithCodons>());
	}



	private void overlapUniqueCoverage(List<OrfWithCodons> orfs) {
		
		HashMap<Codon,HashSet<OrfWithCodons>> cod2Orf = new HashMap<Codon, HashSet<OrfWithCodons>>();
		int numCond = -1;
		for (OrfWithCodons orf : orfs) 
			for (Codon c : orf.getCodons()) {
				cod2Orf.computeIfAbsent(c, x->new HashSet<>()).add(orf);
				numCond = c.getActivity().length;
			}
		
		// now equivalence classes: gives you all codons that are consistent with a specific combination of orfs
		HashMap<HashSet<OrfWithCodons>,HashSet<Codon>> equi = new HashMap<HashSet<OrfWithCodons>, HashSet<Codon>>();
		for (Codon c : cod2Orf.keySet()) {
			equi.computeIfAbsent(cod2Orf.get(c), x->new HashSet<>()).add(c);
		}
		
		// compute equi regions for their length
		HashMap<HashSet<OrfWithCodons>,Integer> equiLengths = new HashMap<HashSet<OrfWithCodons>, Integer>();
		for(HashSet<OrfWithCodons> e: equi.keySet()) {
			LinkedList<ArrayGenomicRegion> equiCodons = null;
			for(OrfWithCodons orf: e) {
				if (equiCodons == null) {
					equiCodons = new LinkedList<ArrayGenomicRegion>();
					for (int i=0; i<orf.getRegion().getTotalLength(); i+=3)
						equiCodons.add(orf.getRegion().map(new ArrayGenomicRegion(i,i+3)));
				}
				else {
					Iterator<ArrayGenomicRegion> it = equiCodons.iterator();
					while (it.hasNext()) {
						ArrayGenomicRegion cod = it.next();
						if (!orf.getRegion().containsUnspliced(cod) || orf.getRegion().induce(cod.getStart())%3!=0)
							it.remove();
					}
				}
			}
			for(OrfWithCodons orf: orfs) {
				if (!e.contains(orf)) {
					
					Iterator<ArrayGenomicRegion> it = equiCodons.iterator();
					while (it.hasNext()) {
						ArrayGenomicRegion cod = it.next();
						if (orf.getRegion().containsUnspliced(cod) && orf.getRegion().induce(cod.getStart())%3==0)
							it.remove();
					}
				}
					
			}
			equiLengths.put(e,equiCodons.size());
		}
			
		HashMap<OrfWithCodons, double[]> total = estimateByCoverage(equi, equiLengths, c->c.getTotalActivity());
		double sum = EI.wrap(total.values()).mapToDouble(a->a[0]).sum();
		for (OrfWithCodons orf : total.keySet())
			orf.setEstimatedTotalActivity(total.get(orf)[0], total.get(orf)[0]/sum);
		
			
		for (int i=0; i<numCond; i++) {
			int ei = i;
			total = estimateByCoverage(equi, equiLengths, c->c.getActivity()[ei]);
			sum = EI.wrap(total.values()).mapToDouble(a->a[0]).sum();
			for (OrfWithCodons orf : total.keySet())
				orf.setEstimatedTotalActivity(i,total.get(orf)[0], total.get(orf)[0]/sum);
		}
		
	}


	/**
	 * orf->[total-activity,fraction-by-coverage]
	 * @param equi
	 * @param uniqueRegions
	 * @param acti
	 * @return
	 */
	private HashMap<OrfWithCodons, double[]> estimateByCoverage(
			HashMap<HashSet<OrfWithCodons>, HashSet<Codon>> equi,
			HashMap<HashSet<OrfWithCodons>,Integer> equiLengths,
			ToDoubleFunction<Codon> acti) {
		
		HashMap<HashSet<OrfWithCodons>, Double> vv = new HashMap<HashSet<OrfWithCodons>, Double>();
		double[] v = new double[equi.size()];
		OrfWithCodons[][] E = new OrfWithCodons[equi.size()][];
		HashSet<Codon>[] codons = new HashSet[E.length];
		int ind = 0;
		for (HashSet<OrfWithCodons> e : equi.keySet()) {
			codons[ind] = equi.get(e);
			E[ind] = e.toArray(new OrfWithCodons[0]);
			for (Codon c : equi.get(e))
				v[ind]+=acti.applyAsDouble(c);
			vv.put(e, v[ind]);
			v[ind]/=equiLengths.get(e);
			ind++;
		}
		HashMap<OrfWithCodons, double[]> re = new HashMap<OrfWithCodons, double[]>(); 
		new EquivalenceClassMinimizeFactors<OrfWithCodons>(E, v).compute((orf,pi)->re.put(orf, new double[] {0,pi}));
	
		for (HashSet<OrfWithCodons> e : equi.keySet()) {
			double sum = EI.wrap(e).mapToDouble(i->re.get(i)[1]).sum();
			for (OrfWithCodons i : e) {
				double[] r = re.get(i);
				r[0] += vv.get(e)*r[1]/sum;
			}
		}		
		
		return re;
	}





	private void distributeCodons(LinkedList<OrfWithCodons> orfs) {
		HashMap<Codon,HashSet<OrfWithCodons>> cod2Orf = new HashMap<Codon, HashSet<OrfWithCodons>>();
		for (OrfWithCodons orf : orfs)
			for (Codon c : orf.getCodons()) 
				cod2Orf.computeIfAbsent(c, x->new HashSet<>()).add(orf);
		
		for (OrfWithCodons orf : orfs) {
			ArrayList<Codon> estCodonsUnique = new ArrayList<Codon>();
			ArrayList<Codon> estCodonsEach = new ArrayList<Codon>();
			for (Codon c : orf.getCodons()) {
				estCodonsUnique.add(c.createProportionalUnique(orf,cod2Orf.get(c)));
				estCodonsEach.add(c.createProportionalEach(orf,cod2Orf.get(c)));
			}
			orf.setEstimatedCodons(estCodonsUnique,estCodonsEach);
		}
		
	}

	


	private void inferOverlappingOrfActivitiesEM(List<OrfWithCodons> orfs) {
		
		HashMap<Codon,HashSet<OrfWithCodons>> cod2Orf = new HashMap<Codon, HashSet<OrfWithCodons>>();
		int numCond = -1;
		for (OrfWithCodons orf : orfs)
			for (Codon c : orf.getCodons()) {
				cod2Orf.computeIfAbsent(c, x->new HashSet<>()).add(orf);
				numCond = c.getActivity().length;
			}
		
		// now equivalence classes: gives you all codons that are consistent with a specific combination of orfs
		HashMap<HashSet<OrfWithCodons>,HashSet<Codon>> equi = new HashMap<HashSet<OrfWithCodons>, HashSet<Codon>>();
		for (Codon c : cod2Orf.keySet()) {
			equi.computeIfAbsent(cod2Orf.get(c), x->new HashSet<>()).add(c);
		}
		
		OrfWithCodons[][] E = new OrfWithCodons[equi.size()][];
		HashSet<Codon>[] codons = new HashSet[E.length];
		int ind = 0;
		for (HashSet<OrfWithCodons> e : equi.keySet()) {
			codons[ind] = equi.get(e);
			E[ind++] = e.toArray(new OrfWithCodons[0]);
		}
		
		
		int dfEach = (numCond-1)*orfs.size();
		int dfUnique = (numCond-1);
		double llEach = 0;
		double llUnique = 0;
		
		
		double[] alpha = new double[E.length];
		for (int i=0; i<alpha.length; i++) {
			for (Codon codon : codons[i])
				alpha[i] += codon.getTotalActivity();
		}
		double sum = EI.wrap(alpha).sum();
		
		
		llUnique = new EquivalenceClassCountEM<OrfWithCodons>(E, alpha, orf->orf.getEffectiveLength()).compute(miniter, maxiter, (orf,pi)->orf.setEstimatedTotalActivity(pi*sum,pi));
		
		for (int c=0; c<numCond; c++) {
			Arrays.fill(alpha, 0);
			for (int i=0; i<alpha.length; i++)
				for (Codon codon : codons[i])
					alpha[i] += codon.getActivity()[c];
			int uc = c;
			
			double csum = EI.wrap(alpha).sum();
			double lla = new EquivalenceClassCountEM<OrfWithCodons>(E, alpha, orf->orf.getEffectiveLength()).compute(miniter, maxiter, (orf,pi)->orf.setEstimatedTotalActivity(uc,pi*csum,pi));
			if (!Double.isNaN(lla))
				llEach += lla;
		}
	
		double p = ChiSquare.cumulative(2*llEach-2*llUnique, dfEach-dfUnique, false, false);

		for (OrfWithCodons o : orfs)
			o.setUniqueProportionPval(p);
		
	}


	private static boolean containsInframeStop(String s) {
		for (int i=0; i<s.length(); i+=3)
			if (SequenceUtils.translate(StringUtils.saveSubstring(s, i, i+3, 'N')).equals(SequenceUtils.STOP_CODON))
				return true;
		return false;
	}


	static int findFirstStart(String s, boolean near) {
		for (int i=0; i<s.length(); i+=3)
			if (StringUtils.hamming(s.substring(i, i+3),"ATG")<=(near?1:0))
				return i;
		return -1;
	}

	static int findLastStartWoStop(String s, boolean near) {
		for (int i=s.length()-3; i>=0; i-=3)
			if (StringUtils.hamming(s.substring(i, i+3),"ATG")<=(near?1:0))
				return containsInframeStop(s.substring(i))?-1:i;
		return -1;
	}



	private GenomicRegion extendFullPath(GenomicRegion path, Codon from, Codon to,
			Intron intron) {

		ArrayGenomicRegion between = from.getEnd()<=to.getStart()?new ArrayGenomicRegion(from.getEnd(),to.getStart()):new ArrayGenomicRegion(to.getEnd(),from.getStart());
		if (intron!=null) 
			between = between.subtract(intron.asRegion());

		return path.union(between).union(to);
	}

	private GenomicRegion extendCorePath(GenomicRegion path, Codon from, Codon to,
			Intron intron, HashSet<Codon> removed) {

		if (removed.contains(from) || removed.contains(to)) {
			if (!removed.contains(from))
				return path.union(from);
			if (!removed.contains(to))
				return path.union(to);
			return path;
		}

		return extendFullPath(path, from, to, intron);
	}

	public static enum StartCodonType {
		Annotated,ATG,Near
	}

	private static class LeftMostInFrameAndClearList extends ArrayList<Codon> {

		private Intron intron;
		private Codon ref;

		public LeftMostInFrameAndClearList startAndClear(Codon reference, Intron intron) {
			this.ref = reference;
			this.intron = intron;
			clear();
			return this;
		}
		public LeftMostInFrameAndClearList startAndClear(Codon reference) {
			this.ref = reference;
			this.intron = null;
			clear();
			return this;
		}

		public ArrayList<Codon> get() {
			int size = 0;
			for (int i=0; i<size(); i++) {
				Codon c = get(i);
				if (c.getStart()>=ref.getEnd() && (intron==null || c.getStart()>=intron.getEnd()) && inFrame(c) && (size==0 || c.getStart()<=get(0).getStart())) {
					if (size>0 && c.getStart()<get(0).getStart())
						size=0;
					set(size++,c);
				}
			}
			while (size()>size)
				remove(size()-1);
			return this;
		}

		private boolean inFrame(Codon c) {
			if (intron==null) return (c.getStart()-ref.getEnd())%3==0;
			int beforeIntron = intron.getStart()-ref.getEnd();
			int afterIntron = c.getStart()-intron.getEnd();
			return (beforeIntron+afterIntron)%3==0;
		}

	}


	private NumericArray computeActivityVector(ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons) {
		double[] re = new double[codons.getRegion().getTotalLength()/3];
		for (Codon c : codons.getData()) {
			int s = c.getStart();
			if (s%3!=0 || re[s/3]!=0) throw new RuntimeException();
			re[s/3] = c.getTotalActivity();
		}
		return NumericArray.wrap(re);
	}



	

}
