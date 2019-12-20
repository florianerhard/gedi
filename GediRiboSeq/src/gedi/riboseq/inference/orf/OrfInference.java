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
package gedi.riboseq.inference.orf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

import cern.colt.bitvector.BitVector;
import gedi.core.data.annotation.Transcript;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.genomic.Genomic;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.SpliceGraph;
import gedi.core.region.SpliceGraph.Intron;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.riboseq.inference.codon.Codon;
import gedi.riboseq.inference.codon.CodonInference;
import gedi.riboseq.inference.orf.NoiseModel.SingleNoiseModel;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.datastructure.graph.SimpleDirectedGraph;
import gedi.util.datastructure.graph.SimpleDirectedGraph.AdjacencyNode;
import gedi.util.datastructure.graph.SimpleDirectedGraph.DfsVisitor;
import gedi.util.datastructure.tree.Trie;
import gedi.util.datastructure.tree.Trie.AhoCorasickResult;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.datastructure.tree.redblacktree.IntervalTreeSet;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import gedi.util.math.stat.counting.Counter;
import gedi.util.math.stat.distributions.PoissonBinomial;
import gedi.util.math.stat.inference.EquivalenceClassCountEM;
import gedi.util.mutable.MutableInteger;
import gedi.util.mutable.MutableMonad;
import gedi.util.mutable.MutablePair;


/**
 * New, more simplified version of OrfFinder, that infers ORFs (which are {@link ImmutableReferenceGenomicRegion<PriceORF>} objects).
 * These are its main functions:
 * 
 * - {@link #codonInference(int, ReferenceSequence, int, int)}: Infer the codons for the genomic chunk (in the coordinate system of this chunk) with activity exceeding <minActivity>
 * - {@link #computeNoise(ImmutableReferenceGenomicRegion)}: Computes the noise model (which can provide the probability of observing an in- or off-frame codon with a given level within an ORF of given translation strength)
 * - {@link #findAnnotated(ImmutableReferenceGenomicRegion)}: Identifies the annotated CDS within a given chunk, that has maximal total activity as {@link ImmutableReferenceGenomicRegion<PriceORF>} object
 * - {@link #inferOrfs(ImmutableReferenceGenomicRegion)}: does several steps to identify actively translated ORFs for a given genomic chunk:
 * 
 * 1. Determine transcripts from the annotation in a greedy fashion such that each new transcript has at least <filterMinUniqueReads> reads that cannot be explained by any other previously identified transcript.
 * 2. Find ORF candidates: each Candidate starts at an active codon, ends at one of the stop codons, has no other in-frame stop codon, has at least <minReads> reads and <minCodonFraction> of the codons covered by active codons
 * 3. Predict start codons: Each of the defined <startCandidates> with start score exceeding <minOverallStartScore> is predicted.
 * 4. Statistical test for active translation: For each predicted start codon in each ORF (ordered by total activity), perform the poisson-binomial test of observing at least as the observed codons above transformed mean * <testFactor>; keep all ORFs below p value <testThreshold> as present ORFs for subsequent tests.  
 * 5. Filter N terminal extensions:  Start with the most C-terminal start candidate and test for each N-terminal extensions using hypergeometric test.
 * 6. Deconvolve remaining overlapping ORFs using EM.
 * 7. Annotate ORF type, start codon
 * 8. Map to genomic coordinates
 * 
 * 
 * Parameters:
 * -delta:				Codon inference regularization parameter (default: 0, no regularization)
 * -minActivity:		When is a codon treated as present						(default: 0.1)
 * -filterMinUniqueReads:			each transcript needs that many unique reads				(default: 5)
 * -minReads:			minimal reads mapped to codons to consider ORF			(default: 5)
 * -minCodonFraction:	minimal fraction of codons covered to consider ORF		(default: 0.25)
 * -startCandidates:	which codon tripletts to consider as start codons		(default: all codons with hamming distance <2 to ATG)
 * -minOverallStartScore:	minimal start score to consider start codon			(default: 0.1)
 * -testFactor:			Factor of the transformed mean to test for presence		(default: 0.25)
 * -testThreshold:		Keep ORFs below this p value as present for subsequent tests (default: 0.01)
 * -miniter/maxiter:	parameters for isoform deconvolution
 * 
 * 
 * @author erhard
 *
 */
public class OrfInference {
	private static final Logger log = Logger.getLogger( OrfInference.class.getName() );
	
	private Genomic genomic;
	private MemoryIntervalTreeStorage<Transcript> allTranscripts;
	private MemoryIntervalTreeStorage<Void> spliceJunctions = new MemoryIntervalTreeStorage<>(Void.class);
	private HashMap<String,ImmutableReferenceGenomicRegion<Transcript>> tindex;
	private GenomicRegionStorage<AlignedReadsData> reads;

	private boolean allowNovelTranscripts = false;
	
	private double minActivity = 0.9;
	
	private double filterMinUniqueReads = 5;
	
	private int minReads = 5;
	private double minCodonFraction = 0.25;
	
	private HashSet<String> startCandidates = new HashSet<>();
	private double minOverallStartScore = 0.1;
	
	private double testThreshold = 0.01;
	private double testFactor = 0.1;
	private double ntermMinFold = 0.1;

	private int miniter = 100;
	private int maxiter = 10000;
	
	private int abortiveLength = 4;

	private int maxAminoDist = 25;
	
	private Trie<String> stops = new Trie<>();
	
	private ReferenceSequence codonChr = Chromosome.obtain("CODONS+");

	private StartCodonScorePredictor startPredictor;
	private NoiseModel noiseModel;
	
	private HashMap<String,MemoryIntervalTreeStorage<MutablePair<?,CheckOrfStatus>>> checkOrfs = new HashMap<>();

	private boolean removeAnno = true;
	
	private AtomicInteger orphanCounter = new AtomicInteger();
	
	{
		startCandidates.add("ATG");
		char[] ATG = "ATG".toCharArray();
		for (int i=0; i<3; i++)
			for (int n=0; n<4; n++)
				if (ATG[i]!=SequenceUtils.nucleotides[n]) {
					ATG[i] = SequenceUtils.nucleotides[n];
					startCandidates.add(String.valueOf(ATG));
					ATG[i] = "ATG".charAt(i);
				}
		stops.put("TAA","TAA");
		stops.put("TAG","TAG");
		stops.put("TGA","TGA");
		
	}
	
	public OrfInference(Genomic genomic,
			GenomicRegionStorage<AlignedReadsData> reads) {
		this.genomic = genomic;
		this.reads = reads;
		
		allTranscripts = new MemoryIntervalTreeStorage<Transcript>(Transcript.class);
		allTranscripts.fill(genomic.getTranscripts());
		tindex = allTranscripts.ei().index(r->r.getData().getTranscriptId());
	}
	
	public void addCheckOrfs(String name, Iterator<? extends ReferenceGenomicRegion<?>> checkOrfs) {
		MemoryIntervalTreeStorage<MutablePair<?,CheckOrfStatus>> stor = new MemoryIntervalTreeStorage(MutablePair.class);
		stor.fill(EI.wrap(checkOrfs).map(c->new ImmutableReferenceGenomicRegion<>(c.getReference(), c.getRegion(), new MutablePair<>(c.getData(),CheckOrfStatus.NoOrf))));
		this.checkOrfs.put(name,stor);
	}
	
	public void addCheckAnnotation() {
		MemoryIntervalTreeStorage<MutablePair<?,CheckOrfStatus>> stor = new MemoryIntervalTreeStorage(MutablePair.class);
		stor.fill(allTranscripts.ei().filter(c->c.getData().isCoding()).map(c->
			new ImmutableReferenceGenomicRegion<>(
					c.getReference(), 
					c.getData().getCds(c).getRegion(), 
					new MutablePair<>(c.getData().getTranscriptId(),CheckOrfStatus.NoOrf)))
				);
		this.checkOrfs.put("Annotation",stor);
	}

	public double getMinActivity() {
		return minActivity;
	}
	
	public void addTranscripts(Iterator<? extends ReferenceGenomicRegion<Transcript>> transcripts) {
		allTranscripts.fill(transcripts);
		tindex = allTranscripts.ei().index(r->r.getData().getTranscriptId());
	}
	
	public void setCorrectedPvalue(MutableReferenceGenomicRegion<PriceOrf> orf, double pval) {
		orf.getData().combinedP = pval;
	}
	
	public double getTestThreshold() {
		return testThreshold;
	}
	
	public void setRemoveAnno(boolean removeAnno) {
		this.removeAnno = removeAnno;
	}
	
	public void setAllowNovelTranscripts(boolean allowNovelTranscripts) {
		this.allowNovelTranscripts = allowNovelTranscripts;
	}
	
	public void addSpliceJunctions(ExtendedIterator<? extends ReferenceGenomicRegion<?>> readsOrTranscr) {
		readsOrTranscr.unfold(r->r.getRegion().introns().map(i->new ImmutableReferenceGenomicRegion<Void>(r.getReference(), i))).forEachRemaining(i->spliceJunctions.add(i));;
	}

	
	public void setStartCodonPredictor(StartCodonScorePredictor predictor) {
		this.startPredictor = predictor;
	}
	
	public void setNoiseModel(NoiseModel noiseModel) {
		this.noiseModel = noiseModel;
	}
	
	public NoiseModel getNoiseModel() {
		return noiseModel;
	}

	
	
	public ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codonInference(
			CodonInference inference, int index, ReferenceSequence ref, int start, int end) { 
		
		if (log.isLoggable(Level.FINE)) log.fine("Computing chunk "+index+" "+ref+":"+start+"-"+end);
		
		MutableMonad<ToDoubleFunction<Collection<Codon>>> gofComp = new MutableMonad<ToDoubleFunction<Collection<Codon>>>();
		ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons = inference.inferCodons(reads, ref, start, end, 0, gofComp);
		// this is in induced coordinate system of ref:start-end !
		
		codons.getData().removeIf(c->c.getTotalActivity()<minActivity);
		if (codons.getData().isEmpty()) return null;
		
		return codons;
	}
	
	public SingleNoiseModel computeNoise(ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons) {
		codons.getData().removeIf(c->c.getTotalActivity()<minActivity);
		SingleNoiseModel rre = allTranscripts.ei(codons)
				.filter(t->SequenceUtils.checkCompleteCodingTranscript(genomic, t))
				.map(t->{
					if (!codons.getRegion().contains(t.getData().getCds(t).getRegion()))
						System.out.println(codons.toLocationString()+" "+t);
					GenomicRegion cds = codons.induce(t.getData().getCds(t),codonChr.getName()).getRegion();
					if (cds.getTotalLength()<300) return null;
					
					
					double[][] re = new double[3][(cds.getTotalLength()/3-1)];
					for (Codon cod : codons.getData()) 
						if (cds.contains(cod.map(0))) {
							int start = cds.induce(cod.map(0));
							if (start/3<re[0].length)
								re[start%3][start/3]+=cod.getTotalActivity();
						}
					double total = ArrayUtils.sum(re[0]);
					
					double mean = NumericArray.wrap(re[0]).evaluate(NumericArrayFunction.GeometricMeanRemoveNonPositive);
					if (mean==0) 
						return null;
					
					double startMean = NumericArray.wrap(re[0],0,abortiveLength).evaluate(NumericArrayFunction.SinhMean);
					
					double start = re[0][0];
					double beforeStop = re[0][re[0].length-1];
					
					double[][] re2 = new double[][] {
						ArrayUtils.slice(re[0], re[0].length/2,re[0].length),
						ArrayUtils.slice(re[1], re[1].length/2,re[1].length),
						ArrayUtils.slice(re[2], re[2].length/2,re[2].length)
					};
					for (int i=0; i<re2[0].length; i++) {
						re2[0][i]/=mean;
						re2[1][i]/=mean;
						re2[2][i]/=mean;
					}
					
					double codFrac = NumericArray.wrap(re2[0]).evaluate(NumericArrayFunction.count(d->d>=minActivity))/re2[0].length;
					if (codFrac<minCodonFraction) return null;
					
					
					Arrays.sort(re2[0]);
					Arrays.sort(re2[1]);
					Arrays.sort(re2[2]);
					
					return new SingleNoiseModel(t,total, mean, startMean, start, beforeStop, re2[0], re2[1], re2[2]);
				})
				.removeNulls()
				.sort((a,b)->Double.compare(b.getTotalFrame0(), a.getTotalFrame0()))
				.first();
		return rre;
	}

	
		
	public ArrayList<ImmutableReferenceGenomicRegion<PriceOrf>> findAnnotated(boolean firstOnly, ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons) {
		AtomicInteger id = new AtomicInteger();
		int numcond = codons.getData().iterator().next().activity.length;
		codons.getData().removeIf(c->c.getTotalActivity()<minActivity);
		
		ArrayList<ImmutableReferenceGenomicRegion<PriceOrf>> re2 = new ArrayList<>();
		
		allTranscripts.ei(codons)
				.filter(t->SequenceUtils.checkCompleteCodingTranscript(genomic, t))
				.map(t->{
					GenomicRegion cds = codons.induce(t.getData().getCds(t),codonChr.getName()).getRegion();
					GenomicRegion utr = codons.induce(t.getData().get5Utr(t),codonChr.getName()).getRegion();
					
					String sequence = genomic.getSequence(t.getData().get5Utr(t)).toString();
					AhoCorasickResult<String> lastInframeStop = stops.iterateAhoCorasick(sequence).filter(hit->(sequence.length()-hit.getStart())%3==0).last();
					int upstream = lastInframeStop==null?(utr.getTotalLength()/3)*3:(utr.getTotalLength()-lastInframeStop.getEnd());
					
					int pos = utr.getTotalLength()-upstream;
					while (pos<0) pos+=3;
					ArrayGenomicRegion ups = utr.map(new ArrayGenomicRegion(pos,utr.getTotalLength()));
					ArrayGenomicRegion ucds = ups.union(cds);
					
					double[][] profile = new double[numcond][ucds.getTotalLength()/3-1];
					for (Codon cod : codons.getData()) 
						if (ucds.containsUnspliced(cod)) {
							int start = ucds.induce(cod).getStart();
							if (start%3==0 && start/3<ucds.getTotalLength()/3-1) {
								for (int c=0; c<numcond; c++) {
									profile[c][start/3]+=cod.activity[c];
								}
							}
						}
					
					PriceOrf re = new PriceOrf(t.getData().getTranscriptId(), id.getAndIncrement(), profile, 1);
					re.predictedStartAminoAcid = ups.getTotalLength()/3;
					re.alternativeStartAminoAcids = new int[]{re.predictedStartAminoAcid};
					
					return new ImmutableReferenceGenomicRegion<>(codons.getReference(), codons.map(ucds),re);
				})
				.sort((a,b)->{
					return Double.compare(b.getData().getTotalActivityFromPredicted(), a.getData().getTotalActivityFromPredicted());
				})
				.forEachRemaining(rgr->{
					if (firstOnly && re2.size()>0) return;
					for (ImmutableReferenceGenomicRegion<PriceOrf> t : re2)
						if (rgr.getRegion().intersects(t.getRegion()))
							return;
					re2.add(rgr);
				});
		return re2;
	}
	
	
	
	public MemoryIntervalTreeStorage<PriceOrf> inferOrfs(ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons) {
		
		
		MemoryIntervalTreeStorage<PriceOrf> re = new MemoryIntervalTreeStorage<>(PriceOrf.class);

		if (log.isLoggable(Level.FINE)) log.fine("Infer ORFs");
		codons.getData().removeIf(c->c.getTotalActivity()<minActivity);
		if (codons.getData().isEmpty()) return re;
		
		// Step 1: determine transcripts
		MemoryIntervalTreeStorage<Transcript> transcripts = new MemoryIntervalTreeStorage<>(Transcript.class);
		transcripts.fill(allTranscripts.ei(codons).filter(t->codons.contains(t)));
		
		String gene = getGene(transcripts);
		
		if (allowNovelTranscripts)
			discoverTranscripts(gene,transcripts, codons);
		
		computeSetCover(transcripts, reads.ei(codons).set(), codons);

		// Step 2: Find all ORF candidates
		LinkedList<ImmutableReferenceGenomicRegion<PriceOrf>> candidates = findCandidates(transcripts,codons);
		if (candidates.size()==0) return re;
		
		
		checkOrfs(codons,EI.wrap(candidates),CheckOrfStatus.NoStart);
		
		
		// Step 3: Find potential start codons
		Iterator<ImmutableReferenceGenomicRegion<PriceOrf>> it = candidates.iterator();
		while (it.hasNext()) {
			ImmutableReferenceGenomicRegion<PriceOrf> orf = it.next();
			String orfseq = genomic.getSequence(codons.map(orf)).toString();
			if (!predictStart(codons,orf,orfseq)) {
				it.remove();
			}
		}
		if (candidates.size()==0) return re;
		
		
		// reindex
		int index = 0;
		for (ImmutableReferenceGenomicRegion<PriceOrf> orf : candidates) {
			if (log.isLoggable(Level.FINE)) log.fine("Index: "+orf.getData().getOrfid()+"->"+index);
			orf.getData().orfid = index++;
			if (log.isLoggable(Level.FINE)) log.fine("Remaining: "+codons.map(orf));
		}
		// sort
		Collections.sort(candidates,(a,b)->Double.compare(b.getData().getTotalActivity(),a.getData().getTotalActivity()));
				
		// Step 4: inframe test
		inFrameTest(transcripts, codons, candidates);
		if (candidates.size()==0) return re;
		TreeSet<ImmutableReferenceGenomicRegion<?>> pres = new TreeSet<>();
		candidates = EI.wrap(candidates).map(r->r.getData().restrictToLongestStart(r)).filter(r->pres.add(r)).toCollection(new LinkedList<>());
		
		// Step 5: deconvolution
		deconvolve(codons, candidates);
		
		// Step 6: annotate
		annotate(gene, codons, candidates);
		
		
		// reindex
		index = 0;
		for (ImmutableReferenceGenomicRegion<PriceOrf> orf : candidates) {
			if (log.isLoggable(Level.FINE)) log.fine("Index: "+orf.getData().getOrfid()+"->"+index);
			orf.getData().orfid = index++;
		}

				
		// Step 7: map to genome and return
		for (ImmutableReferenceGenomicRegion<PriceOrf> orf : candidates) {
			ImmutableReferenceGenomicRegion<PriceOrf> r = codons.map(orf);
			if (log.isLoggable(Level.FINE)) log.fine("Return: "+r);
			re.add(r);
		}
		
		
		return re;
	}

	private String getGene(MemoryIntervalTreeStorage<Transcript> transcripts) {
		String first = transcripts.ei().map(r->r.getData().getGeneId()).first();
		String last = transcripts.ei().map(r->r.getData().getGeneId()).last();
		String gene;
		if (first==null)
			gene = "NOVEL_"+StringUtils.padLeft(""+orphanCounter.getAndIncrement(), 5,'0');
		else if (first.equals(last))
			gene = first;
		else
			gene = first+"_"+last;
		return gene;
	}

	public void setDetected(MutableReferenceGenomicRegion<PriceOrf> o) {
		checkOrfs(null,EI.wrap(o),CheckOrfStatus.Detected);
	}

	public Collection<String> getCheckOrfNames() {
		return checkOrfs.keySet();
	}
	
	public ExtendedIterator<ImmutableReferenceGenomicRegion<MutablePair<?,CheckOrfStatus>>> iterateChecked(String name) {
		return checkOrfs.get(name).ei();
	}
	
	private void checkOrfs(ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons, ExtendedIterator<? extends ReferenceGenomicRegion<PriceOrf>> candidates, CheckOrfStatus status) {
		if (!this.checkOrfs.isEmpty())
			for (ReferenceGenomicRegion<PriceOrf> cand : candidates.loop()) 
				for (MemoryIntervalTreeStorage<MutablePair<?,CheckOrfStatus>> checkOrfs : this.checkOrfs.values()) {
					ReferenceGenomicRegion<PriceOrf> mapped = codons==null?cand:codons.map(cand);
					checkOrfs.ei(mapped).filter(co->isSameStop(co,mapped)).forEachRemaining(r->r.getData().Item2=status);
				}
	}

	private boolean isSameStop(ReferenceGenomicRegion<?> a, ReferenceGenomicRegion<?> b) {
		return a.map(a.getRegion().getTotalLength()-1)==b.map(b.getRegion().getTotalLength()-1);
	}

	public ExtendedIterator<ImmutableReferenceGenomicRegion<PriceOrf>> redistributeCodons(ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons, Iterator<ImmutableReferenceGenomicRegion<PriceOrf>> orfs) {
		
		HashMap<PriceOrf,Double> oldTotal = new HashMap<>();
		
		MemoryIntervalTreeStorage<PriceOrf> orfStore = new MemoryIntervalTreeStorage<>(PriceOrf.class);
		while (orfs.hasNext()) {
			ImmutableReferenceGenomicRegion<PriceOrf> o = orfs.next();
			if (codons.contains(o)) {
				o = codons.induce(o, codonChr.getName());
				orfStore.add(o);
				oldTotal.put(o.getData(), o.getData().getTotalActivityFromPredicted());
				o.getData().clearProfile();
			}
		}
		
		MutableReferenceGenomicRegion<Void> searcher = new MutableReferenceGenomicRegion<Void>().setReference(codonChr);
		for (Codon c : codons.getData()) {
			ArrayList<ImmutableReferenceGenomicRegion<PriceOrf>> corfs = orfStore.ei(searcher.setRegion(c))
						.filter(o->o.getRegion().containsUnspliced(c) && o.getRegion().induce(c).getStart()%3==0 && c.getEnd()<o.getRegion().getEnd())
						.list();
			
			double sum = EI.wrap(corfs).mapToDouble(r->r.getData().deconvolvedFraction).sum();
			for (ImmutableReferenceGenomicRegion<PriceOrf> o : corfs) {
				int p = o.getRegion().induce(c.getStart())/3;
				for (int co=0; co<c.activity.length; co++) 
					o.getData().codonProfiles[co][p]+=c.activity[co]*o.getData().deconvolvedFraction/sum;
			}
		}
		
		for (PriceOrf o : oldTotal.keySet()) {
			o.deconvolvedFraction = o.getTotalActivityFromPredicted()/oldTotal.get(o);
		}
		
		return orfStore.ei().map(o->codons.map(o));
	}
	
	
	
	
	private void discoverTranscripts(String gene, MemoryIntervalTreeStorage<Transcript> transcripts, ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codonsRegion) {
		
		String sequence = genomic.getSequence(codonsRegion).toString();
		
		SpliceGraph sg = new SpliceGraph(0, codonsRegion.getRegion().getTotalLength());

		reads.iterateIntersectingMutableReferenceGenomicRegions(codonsRegion.getReference(),codonsRegion.getRegion().getStart(),codonsRegion.getRegion().getEnd()).forEachRemaining(read->sg.addIntrons(codonsRegion.induce(read.getRegion())));
		allTranscripts.iterateIntersectingMutableReferenceGenomicRegions(codonsRegion.getReference(),codonsRegion.getRegion().getStart(),codonsRegion.getRegion().getEnd()).forEachRemaining(tr->{
			sg.addIntrons(codonsRegion.induce(tr.getRegion()));
		});
		this.spliceJunctions.ei(codonsRegion)
			.filter(r->codonsRegion.contains(r))
			.map(r->codonsRegion.getRegion().induce(r.getRegion()))
			.forEachRemaining(i->sg.addIntron(i.getStart(),i.getEnd()));
		
		SimpleDirectedGraph<Codon> fg = new SimpleDirectedGraph<Codon>("Codongraph");
		
		LeftMostInFrameAndClearList buff = new LeftMostInFrameAndClearList();

		IntervalTreeSet<Codon> codons = new IntervalTreeSet<>(codonChr);
		codons.addAll(codonsRegion.getData());
		
		// add stop codons for easy orf inference
		HashSet<Codon> stopCodons = new HashSet<Codon>();
		stops.iterateAhoCorasick(sequence).map(r->new Codon(new ArrayGenomicRegion(r.getStart(),r.getEnd()), r.getValue())).toCollection(stopCodons);

		for (Intron intr : sg.iterateIntrons().loop()) {
			ArrayGenomicRegion reg = new ArrayGenomicRegion(intr.getStart()-2, intr.getStart(), intr.getEnd(), intr.getEnd()+1);
			String cod = stops.get(SequenceUtils.extractSequence(reg, sequence));
			if (cod!=null)
				stopCodons.add(new Codon(reg,cod));
			
			reg = new ArrayGenomicRegion(intr.getStart()-1, intr.getStart(), intr.getEnd(), intr.getEnd()+2);
			cod = stops.get(SequenceUtils.extractSequence(reg, sequence));
			if (cod!=null)
				stopCodons.add(new Codon(reg,cod));
		}
		stopCodons.removeAll(codons);
		codons.addAll(stopCodons);
		
		HashSet<Codon> usedForAnno = new HashSet<Codon>();
		
		MutableReferenceGenomicRegion<?> searcher = new MutableReferenceGenomicRegion<>().setReference(codonChr);
		for (Codon c : codons) {
			if (transcripts.ei(searcher.setRegion(c)).filter(t->t.getRegion().containsUnspliced(c)).count()>0)
				usedForAnno.add(c);
		}
		

		// as edges only are represented in the splice graph, singleton codons are discarded (which does make sense anyway)
		for (Codon c : codons) {
			if (!stops.containsKey(SequenceUtils.extractSequence(c, sequence))) {
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
	
		
		

		IntervalTree<GenomicRegion, Transcript> novels = new IntervalTree<GenomicRegion, Transcript>(codonChr);
		
		int count = 0;
		for (SimpleDirectedGraph<Codon> g : fg.getWeaklyConnectedComponents()) {
			if (EI.wrap(g.getSources()).mapToDouble(c->c.getTotalActivity()).sum()==0) 
				continue;
			
			// iterate longest paths in g
			LinkedList<Codon> topo = g.getTopologicalOrder();
			HashSet<Codon> remInTopo = new HashSet<Codon>(topo);
			remInTopo.removeIf(c->!stopCodons.contains(c) && !usedForAnno.contains(c));
			HashSet<Codon> removed = new HashSet<Codon>(remInTopo);
			
	//		double maxPathScore = 0;
			
			while (removed.size()<topo.size()) {
				HashMap<Codon,MutablePair<GenomicRegion, Double>> longestPrefixes = new HashMap<Codon, MutablePair<GenomicRegion,Double>>();
				for (Codon c : topo)
					longestPrefixes.put(c, new MutablePair<GenomicRegion, Double>(c, removed.contains(c)?0:(c.getTotalActivity())));
	
				Codon longestEnd = null;
				HashMap<Codon,Codon> backtracking = new HashMap<Codon,Codon>();
	
				for (Codon c : topo) {
	//				if (codonsRegion.map(c).getStart()==100_466_118)
	//					System.out.println(c);
	//				
	//				if (codonsRegion.map(c).getStart()==100_465_842)
	//					System.out.println(c);
					
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
	
	//			System.out.println(codonsRegion.map(longestPrefixes.get(longestEnd).Item1));
				
				if (uniqueActivity>filterMinUniqueReads) {
					Collections.reverse(orfCodons);
	
					MutablePair<GenomicRegion, Double> triple = longestPrefixes.get(longestEnd);
					ArrayGenomicRegion region = triple.Item1.toArrayGenomicRegion();
					String lastCodon = SequenceUtils.extractSequence(region.map(new ArrayGenomicRegion(region.getTotalLength()-3, region.getTotalLength())), sequence);
					
					if (stops.containsKey(lastCodon)) {
						
						novels.add(region);
					}
					
				}
				
	//			maxPathScore = Math.max(maxPathScore,totalActivity);
			}
		}
		
		MemoryIntervalTreeStorage<Transcript> origTrans = new MemoryIntervalTreeStorage<>(Transcript.class);
		origTrans.fill(transcripts);
		
		for (GenomicRegion trans : novels.groupIterator().unfold(r->simplifyTranscripts(codonsRegion, origTrans,r.keySet()).iterator()).list()) {
			ImmutableReferenceGenomicRegion<Transcript> rgr = codonsRegion.map(new ImmutableReferenceGenomicRegion<>(codonChr, trans, new Transcript(gene,gene+"_NOVEL"+StringUtils.padLeft(""+count++, 5,'0'),-1,-1)));
			for (ImmutableReferenceGenomicRegion<Transcript> t : origTrans.ei(rgr).filter(t->t.getRegion().isIntronConsistent(rgr.getRegion()) && t.getRegion().intersects(rgr.getRegion())).loop()) {
				ArrayGenomicRegion rr = rgr.getRegion().union(t.getRegion());
				
				ImmutableReferenceGenomicRegion<Transcript> rgr2 = new ImmutableReferenceGenomicRegion<>(rgr.getReference(), rr,new Transcript(t.getData().getGeneId(),t.getData().getTranscriptId()+"_NOVEL"+StringUtils.padLeft(""+(count-1), 5,'0'),t.getData().getCodingStart(),t.getData().getCodingEnd()));
				transcripts.add(rgr2);
				if (log.isLoggable(Level.FINE)) log.fine("Found novel transcript: "+rgr2);
			}
			transcripts.add(rgr);
			if (log.isLoggable(Level.FINE)) log.fine("Found novel transcript: "+rgr);
			
		}
		
	}
	private LinkedList<GenomicRegion> simplifyTranscripts(ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codonsRegion, MemoryIntervalTreeStorage<Transcript> transcripts, Collection<GenomicRegion> regions) {
		
		LinkedList<GenomicRegion> ll = new LinkedList<>(regions);
		Collections.sort(ll,(x,y)->x.removeIntrons().compareTo(y.removeIntrons()));
		
		ListIterator<GenomicRegion> lit = ll.listIterator();
		while (lit.hasNext()) {
			GenomicRegion n = lit.next();
			if (lit.hasNext()) {
				GenomicRegion n2 = lit.next();
				if (n.isIntronConsistent(n2) && n.intersects(n2)) {
					lit.remove();
					lit.previous();
					lit.set(n.union(n2));
				}
			}
		}
		return ll;
	}
	public boolean isAnnotatedEnd(ReferenceGenomicRegion<?> genomicCoord, boolean includesStop) {
		return allTranscripts.ei(genomicCoord)
			.filter(t->SequenceUtils.checkCompleteCodingTranscript(genomic, t))
			.map(t->t.getData().getCds(t))
			.filter(t->t.getRegion().isIntronConsistent(genomicCoord.getRegion()))
			.filter(t->genomicCoord.map(genomicCoord.getRegion().getTotalLength()-1)==t.map(t.getRegion().getTotalLength()-(includesStop?1:4)))
			.count()>0;
		
	}
	private static boolean containsInframeStop(String s) {
		for (int i=0; i<s.length(); i+=3)
			if (SequenceUtils.translate(StringUtils.saveSubstring(s, i, i+3, 'N')).equals(SequenceUtils.STOP_CODON))
				return true;
		return false;
	}
	private GenomicRegion extendFullPath(GenomicRegion path, Codon from, Codon to,
			Intron intron) {

		ArrayGenomicRegion between = from.getEnd()<=to.getStart()?new ArrayGenomicRegion(from.getEnd(),to.getStart()):new ArrayGenomicRegion(to.getEnd(),from.getStart());
		if (intron!=null) 
			between = between.subtract(intron.asRegion());

		return path.union(between).union(to);
	}
	
	
	/**
	 * transcripts are in codons space
	 * @param transcripts
	 * @param reads
	 * @param codons
	 */
	private void computeSetCover(MemoryIntervalTreeStorage<Transcript> transcripts, HashSet<ImmutableReferenceGenomicRegion<AlignedReadsData>> reads, ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons) {
		MutableReferenceGenomicRegion<Void> searcher = new MutableReferenceGenomicRegion<Void>().setReference(codons.getReference());
		
		if (log.isLoggable(Level.FINE)) log.fine("Set cover");
		
		HashSet<ImmutableReferenceGenomicRegion<AlignedReadsData>> remainingReads = new HashSet<>(reads);
		HashMap<ImmutableReferenceGenomicRegion<Transcript>,HashSet<ImmutableReferenceGenomicRegion<AlignedReadsData>>> trToReads = new HashMap<>();
		for (ImmutableReferenceGenomicRegion<AlignedReadsData> r : reads) {
			for (ImmutableReferenceGenomicRegion<Transcript> t : transcripts
								.ei(searcher.setRegion(r.getRegion()))
								.filter(t->t.getRegion().containsUnspliced(r.getRegion()))
								.loop()) {
				trToReads.computeIfAbsent(t, x->new HashSet<ImmutableReferenceGenomicRegion<AlignedReadsData>>()).add(r);
			}
		}
		
		HashSet<Codon> remainingCodons = new HashSet<>(codons.getData());
		HashMap<ImmutableReferenceGenomicRegion<Transcript>,HashSet<Codon>> trToCodons = new HashMap<>();
		for (Codon c : codons.getData()) {
			GenomicRegion mapped = codons.map(c);
			for (ImmutableReferenceGenomicRegion<Transcript> t : transcripts
								.ei(searcher.setRegion(mapped))
								.filter(t->{
									return t.getRegion().containsUnspliced(mapped);
								})
								.loop()) {
				trToCodons.computeIfAbsent(t, x->new HashSet<Codon>()).add(c);
			}
		}
		trToReads.keySet().retainAll(trToCodons.keySet());
		trToCodons.keySet().retainAll(trToReads.keySet());
		
		ArrayList<ImmutableReferenceGenomicRegion<Transcript>> keep = new ArrayList<>();
		
		if (log.isLoggable(Level.FINE)) log.fine("All transcripts:");
		transcripts.ei().map(t->t.getData().getTranscriptId()+" "+t).log(log,Level.FINE);
		ToDoubleFunction<ImmutableReferenceGenomicRegion<AlignedReadsData>> rc = r->r.getData().getTotalCountOverall(ReadCountMode.Weight);
		
		int total = (int) EI.wrap(remainingCodons).count();
		while (!trToReads.isEmpty()) {
			
			// cds reads remaining, all reads remaining, cds reads, all reads, codons, cds len, all len
			HashMap<ImmutableReferenceGenomicRegion<Transcript>,double[]> data = new HashMap<>();

			Iterator<ImmutableReferenceGenomicRegion<Transcript>> it = trToReads.keySet().iterator();
			while(it.hasNext()) {
				ImmutableReferenceGenomicRegion<Transcript> t= it.next();
				GenomicRegion cds = SequenceUtils.checkCompleteCodingTranscript(genomic, t)?t.getData().getCds(t).getRegion():null;
				
				ArrayList<ImmutableReferenceGenomicRegion<AlignedReadsData>> remR = EI.wrap(trToReads.get(t)).filter(c->remainingReads.contains(c)).list();
				ArrayList<Codon> remC = EI.wrap(trToCodons.get(t)).filter(c->remainingCodons.contains(c)).list();
				
				double allReadsRemaining = EI.wrap(remR).mapToDouble(rc).sum();
				int allCodonsRemaining = remC.size();
				 
				if (allReadsRemaining<filterMinUniqueReads) {
					it.remove();
					if (log.isLoggable(Level.FINE)) log.fine("removing: "+t.getData().getTranscriptId()+" "+allCodonsRemaining+" "+allReadsRemaining+" left: "+trToReads.size());
				}
				else {
					
					ArrayList<ImmutableReferenceGenomicRegion<AlignedReadsData>> allR = EI.wrap(trToReads.get(t)).list();
					double cdsReadsRemaining = cds==null?0:EI.wrap(remR).filter(r->cds.intersects(r.getRegion())).mapToDouble(rc).sum();
					double allReads = EI.wrap(allR).mapToDouble(rc).sum();
					double cdsReads = cds==null?0:EI.wrap(allR).filter(r->cds.intersects(r.getRegion())).mapToDouble(rc).sum();
					
					data.put(t, new double[] {cdsReadsRemaining,allReadsRemaining,cdsReads,allReads,allCodonsRemaining, cds==null?0:cds.getTotalLength(), t.getRegion().getTotalLength()});
					if (log.isLoggable(Level.FINE)) log.fine("considering: "+t.getData().getTranscriptId()+" "+NumericArray.wrap(data.get(t)).formatArray(0,","));
				}
			}
			
			if (!data.isEmpty()) {
				ArrayList<ImmutableReferenceGenomicRegion<Transcript>> arr = new ArrayList<>(data.keySet());
				Collections.sort(arr,(a,b)->{
					int re = 0;
					double[] da = data.get(a);
					double[] db = data.get(b);
					
					for (int i=0; re==0 && i<da.length; i++)
						re= Double.compare(db[i], da[i]);
					
					return re;
				});
				
				ImmutableReferenceGenomicRegion<Transcript> maxt = arr.get(0);
				keep.add(maxt);
				remainingReads.removeAll(trToReads.get(maxt));
				remainingCodons.removeAll(trToCodons.get(maxt));
				trToReads.remove(maxt);
				
				if (log.isLoggable(Level.FINE)) log.fine("keep: "+maxt.getData().getTranscriptId()+" explained_cds="+data.get(maxt)[0]+" explained="+data.get(maxt)[1]+" total="+total);
			}
		}
		
		transcripts.clear();
		transcripts.fill(EI.wrap(keep).map(t->codons.induce(t,codonChr.getName())));
		
		if (log.isLoggable(Level.FINE)) {
			log.fine("Kept transcripts:");
			transcripts.ei().map(t->t.getData().getTranscriptId()+" "+t.toLocationString()).log(log,Level.FINE);
		}
		
	}

	/**
	 * The returned ORFs are in the coordinate system of codons
	 * @param codons 
	 * @param transcriptId
	 * @param profile
	 * @param sequence
	 * @return
	 */
	private LinkedList<ImmutableReferenceGenomicRegion<PriceOrf>> findCandidates(MemoryIntervalTreeStorage<Transcript> transcripts, 
			ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons) {
		
		
		//Identify ORF candidates (at least (minCodons) codons, at least (minCodonFraction) of the codons covered), end at stop, no in-frame stop
		
		int numcond = codons.getData().iterator().next().activity.length;
		
		class PreOrf {
			double[] range;
			double[] present;
			double[][] codons;
			String tid;
			public PreOrf(int l, String tid) {
				super();
				this.range = new double[l];
				this.present = new double[l];
				this.codons = new double[numcond][l];
				this.tid = tid;
			}
			
		}
		
		
		IntervalTree<GenomicRegion,PreOrf> pre = new IntervalTree<>(codonChr);
		// identify all stop codon defined orfs on any transcript
		for (ImmutableReferenceGenomicRegion<Transcript> trans : transcripts.ei().loop()) {
			
			String sequence = genomic.getSequence(codons.map(trans)).toString();
			
			for (int f=0; f<3; f++) {
				int uf = f;
				ArrayList<AhoCorasickResult<String>> stopHits = stops.iterateAhoCorasick(sequence).filter(hit->hit.getStart()%3==uf).list();
				
				int prev = f;
				for (AhoCorasickResult<String> hit : stopHits) {
					
					GenomicRegion reg = new ArrayGenomicRegion(prev,hit.getEnd());
					reg = trans.map(reg);
					
					int l = (hit.getEnd()-3-prev)/3;
					pre.put(reg,new PreOrf(l, trans.getData().getTranscriptId()));
					
					
					
					prev = hit.getEnd();
				}
			}
		
		}
		
		
		// now map the codons
		for (Codon c : codons.getData()) {
			for (Map.Entry<GenomicRegion,PreOrf> r : EI.wrap(pre.iterateIntervalsIntersecting(c, orf->orf.containsUnspliced(c) && orf.induce(c.getStart())%3==0)).loop()) {
				int s = r.getKey().induce(c.getStart())/3;
				if (s<r.getValue().range.length) {
					r.getValue().range[s]+=c.getTotalActivity();
					r.getValue().present[s]=r.getValue().range[s]>=minActivity?1:0;
					for (int co=0; co<numcond; co++)
						r.getValue().codons[co][s]+=c.getActivity()[co];
				}
			}
		}
		
		for (PreOrf v : pre.values()) {
			ArrayUtils.cumSumInPlace(v.range, -1);
			ArrayUtils.cumSumInPlace(v.present, -1);
		}

		
		IntervalTree<GenomicRegion,PriceOrf> re = new IntervalTree<>(codonChr);
		
		for (Map.Entry<GenomicRegion,PreOrf> r : pre.entrySet()) {
			PreOrf o = r.getValue();
			if (log.isLoggable(Level.FINE)) log.fine("Checking preORF: "+codons.map(r.getKey()));
			// find the longest region that ends at hit, start>=prev, has minReads and minCodonFraction
			for (int i=0; i<o.range.length; i++) {
				double decum = o.range[i]-(i<o.range.length-1?o.range[i+1]:0);
				if (decum>=minActivity && o.range[i]>=minReads && (o.present[i]>=minCodonFraction*Math.min(100, o.present.length-i))) {
					
					double[][] slice = new double[o.codons.length][o.codons[0].length-i];
					for (int co=0; co<slice.length; co++)
						slice[co] = ArrayUtils.slice(o.codons[co], i, o.codons[co].length);
					// found it!
					ArrayGenomicRegion reg = r.getKey().map(new ArrayGenomicRegion(i*3,r.getKey().getTotalLength()));
					PriceOrf orf = new PriceOrf("",re.size(),slice,-1);
					orf.transcriptId = o.tid;
					
					if (re.put(reg,orf)==null) {
						if (log.isLoggable(Level.FINE)) log.fine("Found: "+codons.getReference()+":"+codons.map(reg)+" "+orf);
					}
					
					break;
				}
			}
			
		}
		
		
		return re.ei().toCollection(new LinkedList<ImmutableReferenceGenomicRegion<PriceOrf>>());
	}


	
	private boolean predictStart(ImmutableReferenceGenomicRegion<?> ref, ImmutableReferenceGenomicRegion<PriceOrf> orf, String sequence) {
		
		if (log.isLoggable(Level.FINE)) log.fine("Predict starts for: "+ref.map(orf));
		
		PriceOrf price = orf.getData();
		double[] score = computeStartScores(price,sequence,true);
		
		price.predictedStartAminoAcid = ArrayUtils.argmax(score);
		boolean annot = false;
		if (score[price.predictedStartAminoAcid]<minOverallStartScore) {
			
			if (!removeAnno && isAnnotatedEnd(ref.map(orf),true)) {
				score = computeStartScores(price,sequence,false);
				price.predictedStartAminoAcid = ArrayUtils.argmax(score);
				annot = true;
			} else {
				if (log.isLoggable(Level.FINE)) log.fine("No start found");
				return false;
			}
		}
		
		IntArrayList starts = new IntArrayList();
		for (int start = 0; start<score.length; start++) {
			if (score[start]>=minOverallStartScore || (annot && start==price.predictedStartAminoAcid)) {  //minStartScoreFraction*score[price.predictedStartAminoAcid]) {
				if (log.isLoggable(Level.FINE)) log.fine("Found start: "+start+" codon="+sequence.substring(start*3,start*3+3)+" start="+price.startScores[start]+" range="+price.startRangeScores[start]+" total="+score[start]);
				starts.add(start);
			}
		}
		price.alternativeStartAminoAcids = starts.toIntArray();
		return true;
	}
	public double[] computeStartScores(PriceOrf price,  String sequence, boolean ext) {
		startPredictor.predict(price);
		
		double[] cumu = new double[price.codonProfiles[0].length];
		for (int c=0; c<price.codonProfiles.length; c++)
			ArrayUtils.add(cumu,price.codonProfiles[c]);
		ArrayUtils.cumSumInPlace(cumu, -1);
		ArrayUtils.mult(cumu, 1/cumu[0]);
		
		double maxwo = 0;
		double[] score = new double[price.startScores.length];
		for (int i=0; i<score.length; i++)
			maxwo = Math.max(maxwo, price.startRangeScores[i]*price.startScores[i]);
		
//		int best = 0;
		for (int i=0; i<score.length; i++) {
			double f = sequence==null?1:(startCandidates.contains(sequence.substring(i*3,i*3+3))?1:0);
			double f2 = orfFractionScore(cumu[i]); //cumu[i]>=minOrfFraction?1:0;
			if (ext)
				score[i]=f*f2*price.startRangeScores[i]*price.startScores[i]/maxwo;
			else
				score[i]=f*f2*price.startScores[i];
//			if (f==1 && price.startScores[i]>price.startScores[best])
//				best = i;
		}
//		price.bestStartCodonStart = price.startScores[best];
//		DoubleRanking rank = new DoubleRanking(price.startScores).sort(false);
//		price.bestStartCodonStartRank = rank.getCurrentRank(best)/(double)price.startScores.length;
//		rank.restore();
		
		return score;
	}
	private double orfFractionScore(double x) {
		double sig = 1/(1+Math.exp(-20*(x-0.7)));
		return (sig*(1-minOverallStartScore*2))+2*minOverallStartScore;
	}

	
	
	
	private void inFrameTest(MemoryIntervalTreeStorage<Transcript> transcripts, ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons, List<ImmutableReferenceGenomicRegion<PriceOrf>> candidates) {
		
		if (log.isLoggable(Level.FINE)) {
			log.fine("Candidate order");
			for (ImmutableReferenceGenomicRegion<PriceOrf> cand : candidates) {
				log.fine("SMean="+cand.getData().getGeomMean(cand.getData().getPredictedStartAminoAcid())
						+" len="+cand.getData().getOrfAaLength(cand.getData().getPredictedStartAminoAcid())
						+" prod="+cand.getData().getGeomMean(cand.getData().getPredictedStartAminoAcid())
						*cand.getData().getOrfAaLength(cand.getData().getPredictedStartAminoAcid())+"\t"+codons.map(cand.getData().getStartStop(cand, false)));
			}
		}
	
		IntervalTree<GenomicRegion,PriceOrf> covered = new IntervalTree<>(codonChr);
		
		IntervalTree<GenomicRegion,StartSet[]> map = new IntervalTree<>(codonChr);
		Iterator<ImmutableReferenceGenomicRegion<PriceOrf>> cit = candidates.iterator();
		while (cit.hasNext()) {
			
			ImmutableReferenceGenomicRegion<PriceOrf> cand  = cit.next();
			cand.getData().combinedP = 1;
//			if (cand.getData() instanceof PriceOrfWithPep)
//				System.out.println();
			

			LinkedList<StartSet> startCands = new LinkedList<>();
			
			for (int startIndex=0; startIndex<cand.getData().getNumAlternativeStartCodons(); startIndex++) {
			
				StartSet sc = inFrameTestCandidate(cand,startIndex, covered, transcripts, codons);
				if (sc!=null)
					startCands.add(sc);
			}
			
			Consumer<StartSet> checker = start ->{
				if (start.ipval0<testThreshold)
					checkOrfs(codons,EI.wrap(cand),CheckOrfStatus.ManyGaps);
				else if (start.ipvala<testThreshold)
					checkOrfs(codons,EI.wrap(cand),CheckOrfStatus.Abortive);
				else
					checkOrfs(codons,EI.wrap(cand),start.test);
			};
			
			if (startCands.size()>0 && checkNterm(cand, startCands, checker)) {
				
//				checkAbortive(cand, covered, codons);
				
//				if (cand.getData().combinedP<testThreshold && cand.getData().expP>testThreshold && cand.getData().abortiveP>testThreshold)
//					covered.put(cand.getRegion().map(new ArrayGenomicRegion(0,cand.getRegion().getTotalLength()-3)), cand.getData());
				if (cand.getData().combinedP<testThreshold)
					covered.put(cand.getData().getStartStop(cand, 0, false).getRegion(),cand.getData());

				map.put(cand.getRegion(), startCands.toArray(new StartSet[0]));
				
				if (log.isLoggable(Level.FINE)) log.fine("Best pvalue: "+cand.getData().combinedP);
				if (log.isLoggable(Level.FINE)) log.fine("Covered now: "+EI.wrap(covered.keySet()).concat(", "));
			}
			else {
				if (log.isLoggable(Level.FINE)) log.fine("No ORF");
				cit.remove();
			}
			
		}
		
//		Collections.reverse(candidates);
//		
//		cit = candidates.iterator();
//		while (cit.hasNext()) {
//			ImmutableReferenceGenomicRegion<PriceOrf> cand  = cit.next();
//			
//			covered.remove(cand.getData().getStartStop(cand, 0, false).getRegion());
//			
//			DoubleArrayList pvals = new DoubleArrayList();
//			IntArrayList start = new IntArrayList();
//			for (int startIndex=0; startIndex<cand.getData().getNumAlternativeStartCodons(); startIndex++) {
//				
//				boolean passes = outOfFrameTestCandidate(cand,startIndex, covered, transcripts, codons);
//				if (passes) {
//					start.add(cand.getData().alternativeStartAminoAcids[startIndex]);
//					pvals.add(map.get(cand.getRegion())[startIndex].pval);
//				}
//			}
//			
//			if (start.size()>0) {
//				
//				int best = ArrayUtils.argmin(pvals.toDoubleArray());
//				cand.getData().combinedP = pvals.getDouble(best);
//				cand.getData().predictedStartAminoAcid = start.getInt(best);
//				
//				cand.getData().alternativeStartAminoAcids = start.toIntArray();
//				if (log.isLoggable(Level.FINE)) log.fine("Kept starts: "+StringUtils.toString(cand.getData().alternativeStartAminoAcids));
//				
//				if (cand.getData().combinedP<testThreshold)
//					covered.put(cand.getData().getStartStop(cand, 0, false).getRegion(),cand.getData());
//			}
//			else {
//				
//				if (log.isLoggable(Level.FINE)) log.fine("No Start remaining");
//				cit.remove();
//				if (log.isLoggable(Level.FINE)) log.fine("Covered now: "+EI.wrap(covered.keySet()).concat(", "));
//			}
//
//		}
//		
//		Collections.reverse(candidates);
		
	}
	
	
	

	private StartSet inFrameTestCandidate(ImmutableReferenceGenomicRegion<PriceOrf> cand, int startIndex, IntervalTree<GenomicRegion,PriceOrf> covered, MemoryIntervalTreeStorage<Transcript> transcripts, ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons) {
		int startPos = cand.getData().alternativeStartAminoAcids[startIndex];
//		if (cand.getData().getOrfAaLength(startPos)<=abortiveLength)
//			return null;
		// check for signs of active elongation!!
		
//		startPos+=abortiveLength;
		// does not work, power is dramatically reduced
		
//		Logger log = this.log;
//		if (codons.map(cand.getData().getStartStop(cand, startIndex, false)).toLocationString().endsWith("-17645")) {
//			log = Logger.getLogger("special");
//			log.setLevel(Level.FINE);
//			EI.wrap(covered).log(log, Level.FINE);
//		}
		
		ImmutableReferenceGenomicRegion<PriceOrf> startstop = cand.getData().getPositionToStop(cand, startPos, false);

		if (log.isLoggable(Level.FINE)) log.fine("Compute tests for: "+codons.map(cand.getData().getStartStop(cand, startIndex, false))+" ORF: "+codons.map(cand));

//		double[] totalActivity = cand.getData().getTotalActivities(0);
		GenomicRegion largest = cand.getData().getStartStop(cand, 0, false).getRegion();

		
		double smean = cand.getData().getGeomMean(startPos);
		double cut = smean*testFactor;
		if (log.isLoggable(Level.FINE)) log.fine("Sinh mean="+smean+" cutoff="+cut);
		
//		if (totalActivity[startPos]<cut|| totalActivity[totalActivity.length-1]<cut) {
//			if (log.isLoggable(Level.FINE)) log.fine("Start: "+totalActivity[startPos]+" Before stop: "+totalActivity[totalActivity.length-1]);
//			return null;
//		}
		
		
		ToDoubleBiFunction<Integer,GenomicRegion> findBestOverlapper = getOverlapperFunction(covered, cut);
		
		
		double[] outsidePrior = new double[]{1,1};
		double[] inside = getBioNoise(codons, startstop.getRegion(), false, findBestOverlapper, cut);
		double[] maxOutside = null;
		MutableInteger which = new MutableInteger();
		CheckOrfStatus testUsed = CheckOrfStatus.Downstream;
		
		for (ImmutableReferenceGenomicRegion<Transcript> t : transcripts.ei(startstop).filter(t->t.getRegion().containsUnspliced(startstop.getRegion())).loop()) {
			ArrayGenomicRegion upstream = t.getRegion().map(new ArrayGenomicRegion(0,t.getRegion().induce(startstop.getRegion().getStart())));
			ArrayGenomicRegion ext = upstream.intersect(largest);
			upstream = upstream.subtract(largest);
			ArrayGenomicRegion downstream = t.getRegion().map(new ArrayGenomicRegion(t.getRegion().induce(startstop.getRegion()).getEnd(),t.getRegion().getTotalLength()));
			
			
			int lencut = (int) (startstop.getRegion().getTotalLength());
			if (upstream.getTotalLength()>lencut)
				upstream = upstream.map(new ArrayGenomicRegion(upstream.getTotalLength()-lencut,upstream.getTotalLength()));
			if (ext.getTotalLength()>lencut)
				ext = ext.map(new ArrayGenomicRegion(ext.getTotalLength()-lencut,ext.getTotalLength()));
			if (downstream.getTotalLength()>lencut)
				downstream = downstream.map(new ArrayGenomicRegion(0,lencut));
//			ArrayGenomicRegion merged = downstream.union(upstream);
			
			if (log.isLoggable(Level.FINE)) log.fine("Downstream: "+codons.getReference()+":"+codons.map(downstream));
			maxOutside = maxPair(maxOutside, getBioNoise(codons,downstream,true,findBestOverlapper,cut), outsidePrior, which);
			if (which.N==1)
				testUsed = CheckOrfStatus.Downstream;
			if (ext.getTotalLength()>5) {
				if (log.isLoggable(Level.FINE)) log.fine("Extension: "+codons.getReference()+":"+codons.map(ext));
				maxOutside = maxPair(maxOutside, getBioNoise(codons,ext,false,findBestOverlapper,cut), outsidePrior, which);
				if (which.N==1)
					testUsed = CheckOrfStatus.Upstream;
			}
			if (log.isLoggable(Level.FINE)) log.fine("Upstream: "+codons.getReference()+":"+codons.map(upstream));
			maxOutside = maxPair(maxOutside, getBioNoise(codons,upstream,true,findBestOverlapper,cut), outsidePrior, which);
			if (which.N==1)
				testUsed = CheckOrfStatus.Upstream;
				
//			double[] toutside = getBioNoise(codons,merged,true,findBestOverlapper,cut);
//			double prob = toutside[0]/(toutside[0]+toutside[1]);
//			if (log.isLoggable(Level.FINE)) log.fine("Prob for "+codons.getReference()+":"+codons.map(merged)+" "+prob);
			
//			if (!Double.isNaN(prob) && (maxOutside==null || prob>maxOutside[0]/(maxOutside[0]+maxOutside[1])))
//				maxOutside = toutside;
		}

		if (maxOutside==null) maxOutside=new double[]{0,0};
		
//		ArrayUtils.mult(maxOutside, ArrayUtils.sum(inside)/ArrayUtils.sum(maxOutside));
		if (log.isLoggable(Level.FINE)) log.fine("Prior="+StringUtils.toString(outsidePrior));
		if (log.isLoggable(Level.FINE)) log.fine("Inside="+StringUtils.toString(inside));
		if (log.isLoggable(Level.FINE)) log.fine("Outside="+StringUtils.toString(maxOutside));
		
//		double outsideProb = Beta.quantile(0.75, 1+outsidePrior[0]+inside[0]+maxOutside[0], 1+outsidePrior[1]+inside[1]+maxOutside[1], true, false);
		double outsideProb = (outsidePrior[0]+inside[0]+maxOutside[0])/(outsidePrior[0]+inside[0]+maxOutside[0]+outsidePrior[1]+inside[1]+maxOutside[1]);
		
		double[] use = null;
		if (maxOutside[0]/(maxOutside[0]+maxOutside[1])>inside[0]/(inside[0]+inside[1])) {
			use = maxOutside;
		} else {
			use = inside;
			testUsed = CheckOrfStatus.OffFrame;
		}
//		double[] use = maxPair(inside, maxOutside, outsidePrior);
		
		outsideProb = (outsidePrior[0]+use[0])/(outsidePrior[0]+use[0]+outsidePrior[1]+use[1]);
//		outsideProb = Beta.quantile(0.75, 1+outsidePrior[0]+use[0], 1+outsidePrior[1]+use[1], true, false);
		
		if (log.isLoggable(Level.FINE)) log.fine("Outside prob="+outsideProb);
		
		DoubleArrayList pp0 = new DoubleArrayList();
//		DoubleArrayList pp10 = new DoubleArrayList();
//		DoubleArrayList pp11 = new DoubleArrayList();
//		DoubleArrayList pp12 = new DoubleArrayList();
		double p0 = noiseModel.getProbability0(smean, cut/smean,0.05);
//		double p1 = noiseModel.getProbability1(smean, cut/smean);
//		double p2 = noiseModel.getProbability2(smean, cut/smean);
		
		BitVector obs = getObserved(codons, startstop.getRegion(), cut);
		int k = 0;
		for (int aapos=0; aapos<cand.getData().getOrfAaLength(startPos); aapos++) {
			int profilePos = aapos+startPos;
			int codonsPos = cand.map(profilePos*3);

			double p = findBestOverlapper.applyAsDouble(codonsPos,startstop.getRegion());
			if (p<1) {
				pp0.add(1-(1-p)*(1-outsideProb));
//				pp10.add(1-(1-p)*(1-p0));
				if (obs.getQuick(aapos*3+1))
					k++;
			}
			
			
//			pp11.add(1-(1-findBestOverlapper.applyAsDouble(cand.map(profilePos*3+1),startstop.getRegion()))*(1-p1));
//			pp12.add(1-(1-findBestOverlapper.applyAsDouble(cand.map(profilePos*3-1),startstop.getRegion()))*(1-p2));
			
		}
		
//		int[] kk = new int[3];
//		for (int i=0; i<obs.size(); i++) {
//			if (obs.getQuick(i))
//				kk[(i+2)%3]++;
//		}
		// be careful, if the overlapper prob=1 (inframe overlapping), then do not count (as the p=1 is not added)
		
		
		// compute abortive pval
		double ipvala = 1;
		if (cand.getData().getOrfAaLength(startPos)>abortiveLength) {
			double[] act = cand.getData().getTotalActivities(startPos);
			for (int i=0; i<act.length; i++) {
				int profilePos = i+startPos;
				int codonsPos = cand.map(profilePos*3);
				double p = getOverlapperFunction(covered, act[i]).applyAsDouble(codonsPos, startstop.getRegion());
				act[i] *= 1-p;
			}
			
			NumericArray ppa = NumericArray.wrap(act);
			double startmean = ppa.slice(0,abortiveLength).evaluate(NumericArrayFunction.SinhMean);
			double downmean = ppa.slice(abortiveLength).evaluate(NumericArrayFunction.SinhMean);
			
			if (log.isLoggable(Level.FINE)) log.fine("start mean="+startmean+" down mean="+downmean);
			
			double std = Math.log((downmean)/Math.max(0,startmean))/Math.log(2);
			ipvala = noiseModel.getEmpiricalDownToStartDistribution().applyAsDouble(std);
		}
		
		if (log.isLoggable(Level.FINE)) log.fine("pp0="+EI.wrap(pp0).add(new Counter<Double>()).toSingleLine());
//		if (log.isLoggable(Level.FINE)) log.fine("pp10="+EI.wrap(pp10).add(new Counter<Double>()).toSingleLine());
//		if (log.isLoggable(Level.FINE)) log.fine("pp11="+EI.wrap(pp11).add(new Counter()).toSingleLine());
//		if (log.isLoggable(Level.FINE)) log.fine("pp12="+EI.wrap(pp12).add(new Counter()).toSingleLine());
//		if (log.isLoggable(Level.FINE)) log.fine("kk="+StringUtils.toString(kk));
		if (log.isLoggable(Level.FINE)) log.fine("kk="+StringUtils.toString(k));
		double exp0 = ArrayUtils.sum(pp0.toDoubleArray());
//		double exp1 = ArrayUtils.sum(pp10.toDoubleArray());
		if (log.isLoggable(Level.FINE)) log.fine("exp0="+exp0);
//		if (log.isLoggable(Level.FINE)) log.fine("exp10="+exp1);
//		if (log.isLoggable(Level.FINE)) log.fine("exp11="+ArrayUtils.sum(pp11.toDoubleArray()));
//		if (log.isLoggable(Level.FINE)) log.fine("exp12="+ArrayUtils.sum(pp12.toDoubleArray()));
		
		int min = (int) ((Math.floor(p0*pp0.size())-3)*0.975);
		double[] activities = getActivities(codons, cand.getData().getPositionToStop(cand, startPos, true).getRegion());
		
		if (log.isLoggable(Level.FINE)) log.fine("l="+pp0.size()+" min="+min);
		if (log.isLoggable(Level.FINE)) log.fine("act(start)="+activities[1]);
		if (log.isLoggable(Level.FINE)) log.fine("act(beforestop)="+activities[activities.length-5]);
		if (log.isLoggable(Level.FINE)) log.fine("act(stop)="+activities[activities.length-2]);
		
		
		
		double pval = pp0.size()==0?1:new PoissonBinomial(pp0.toDoubleArray()).cumulative(k-1, false, false);
		pval = Math.max(pval, 0);
		
		
//		double ipval0 = pp10.size()==0?1:new PoissonBinomial(pp10.toDoubleArray()).cumulative(k, true, false);
//		ipval0 = Math.max(ipval0, 0);
		double ipval0 = k>=min?1:0;
//		if (pval>1E-3 && (activities[1]<minActivity || activities[activities.length-5]<minActivity))
//			ipval0 = 0;
		
//		double ipval1 = pp11.size()==0?1:new PoissonBinomial(pp11.toDoubleArray()).cumulative(kk[1]-1, false, false);
//		ipval1 = Math.max(ipval1, 0);
//		double ipval2 = pp12.size()==0?1:new PoissonBinomial(pp12.toDoubleArray()).cumulative(kk[2]-1, false, false);
//		ipval2 = Math.max(ipval2, 0);
		// this is not reasonable, as overlapping weaker orfs are not resolved yet, and these may increase the off-frame signal to an extent
		// at which these pvalues get significant

		
		if (log.isLoggable(Level.FINE)) log.fine("P value: "+pval);
		if (log.isLoggable(Level.FINE)) log.fine("exp P value: "+ipval0);
		if (log.isLoggable(Level.FINE)) log.fine("abortive P value: "+ipvala);
		
		if (!removeAnno && isAnnotatedEnd(codons.map(startstop),false)) {
			ipval0 = ipvala = 1;
			if (log.isLoggable(Level.FINE)) log.fine("not filtering");
		}
//		if (ipval0<testThreshold || ipvala<testThreshold) // || ipval1<testThreshold || ipval2<testThreshold)
//			pval = 1;
//		if (log.isLoggable(Level.FINE)) log.fine("final P value: "+pval);
		
		
		
		return new StartSet(startstop.getRegion(),cand.getData().alternativeStartAminoAcids[startIndex],pval,ipval0,ipvala,testUsed);
	}
	
	
	
	
	private double[] maxPair(double[] a, double[] b, double[] prior, MutableInteger which) {
		
		if (a==null) {
			which.N = 1;
			return b;
		}
		double pa = (a[0]+prior[0])/(a[0]+prior[0]+a[1]+prior[1]);
		double pb = (b[0]+prior[0])/(b[0]+prior[0]+b[1]+prior[1]);
		if (pa>pb) {
			which.N = 0;
			return a;
		}
		which.N = 1;
		return b;
	}
	private ToDoubleBiFunction<Integer,GenomicRegion> getOverlapperFunction(IntervalTree<GenomicRegion, PriceOrf> covered, double cut) {
		return (p,r)->{
			double f0 = 0;
			double f1 = 0;
			double f2 = 0;
			for (GenomicRegion c : covered.keys(p, p).filter(c->c.contains(p)).loop()) {
				int f = c.induce(p)%3;
				
				GenomicRegion inter = c.intersect(r.extendBack(1).extendFront(1));
			
				if (inter.isEmpty())
					continue; // can only happen in one situation: for pp11 or pp12 when directly after or before the actual is a split, respectively

				int start = c.induce(inter.getStart());
				int end = c.induce(inter.getStop())+1;
				start = start/3;
				end = (end+2)/3;
				
				double osm = Math.max(minActivity, covered.get(c).getGeomMean(start,end));
				if (f==0) f0 = 1;//Math.max(f0, noiseModel.getProbability0(osm, cut/osm));
				else if (f==1) f1 = Math.max(f1, noiseModel.getProbability1(osm, cut/osm));
				else f2 = Math.max(f2, noiseModel.getProbability2(osm, cut/osm));
				
			}
			return 1-(1-f0)*(1-f1)*(1-f2);
		};
	}

	
	private double[] getBioNoise(
			ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons,
			GenomicRegion reg,
			boolean considerInframe,
			ToDoubleBiFunction<Integer,GenomicRegion> exisiting, // prob that already there
			double cutoff) {
		
		// first element: codon left of start codon!
		BitVector obs = getObserved(codons,reg,cutoff);
		
		
		HashMap<Double,int[]> counter = new HashMap<>();
//		int k = 0;
		DoubleArrayList ppc = new DoubleArrayList();
		for (int i=0; i<obs.size(); i++) {
			if (considerInframe || i%3!=1) {
				double p = exisiting.applyAsDouble(reg.mapMaybeOutside(i-1),reg);
				if (p<1) {
//					if (obs.getQuick(i)) 
//						k++;
					ppc.add(p);
				}
				counter.computeIfAbsent(p, x->new int[2])[obs.getQuick(i)?0:1]++;
			}
		}
		double k=0;
		for (Double p : counter.keySet()) {
			if (p<1){
				int[] c=counter.get(p);
				k+=Math.max(c[0], p*(c[0]+c[1]));
			}
		}
		
		double[] pp = ppc.toDoubleArray();
		double[] re = new double[2];
//		re[0] = k-ArrayUtils.sum(pp);
//		re[1] = pp.length-k;
		
		if (ArrayUtils.max(pp)>0) {
			PoissonBinomial poibin = new PoissonBinomial(pp);
			int n = pp.length;
			for (int i=0; i<pp.length; i++) {
				double p = poibin.density(i, false);
				double missing = Math.max(0, k-i);
				double present = Math.max(0, n-i);
				re[0]+=p*missing;
				re[1]+=p*present;
			}
			re[1] = re[1]-re[0];
		} else {
			re[0] = k;
			re[1] = pp.length-k;
		}
		
		if (log.isLoggable(Level.FINE)) log.fine("Considering: "+StringUtils.toString(re));
		return re;
	}

	private BitVector getObserved(ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons, GenomicRegion reg,
			double cutoff) {
		
		BitVector obs = new BitVector(reg.getTotalLength());
		if (reg.isEmpty()) return obs;
		codons.getData().forEachIntervalIntersecting(reg.getStart(), reg.getStop(), c->{
			if (reg.isIntronConsistent(c) && reg.contains(c.map(1))) {
				obs.putQuick(reg.induce(c.map(1)), c.getTotalActivity()>cutoff);
			}
		});
		return obs;
	}
	
	private double[] getActivities(ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons, GenomicRegion reg) {
		
		double[] re = new double[reg.getTotalLength()];
		if (reg.isEmpty()) return re;
		codons.getData().forEachIntervalIntersecting(reg.getStart(), reg.getStop(), c->{
			if (reg.isIntronConsistent(c) && reg.contains(c.map(1))) {
				re[reg.induce(c.map(1))]+=c.getTotalActivity();
			}
		});
		return re;
	}
	
	private boolean checkNterm(ImmutableReferenceGenomicRegion<PriceOrf> cand, LinkedList<StartSet> startCands,Consumer<StartSet> checker) {
		boolean hasValid = EI.wrap(startCands).filter(ss->ss.ipval0>testThreshold && ss.ipvala>testThreshold).count()>0;
		
		Iterator<StartSet> it = startCands.descendingIterator();
		while (it.hasNext()) {
			StartSet start = it.next();
			if (hasValid && (start.ipval0<testThreshold || start.ipvala<testThreshold)) {
				it.remove();
			}
		}
		
		// remove all starts, where the extension has significantly fewer codons over the threshold
		
		double[] totalActivity = cand.getData().getTotalActivities(0);
		it = startCands.descendingIterator();
		
		StartSet last = it.next();
		double smean = NumericArray.wrap(totalActivity)
				.slice(last.start,totalActivity.length)
				.evaluate(NumericArrayFunction.SinhMean);
		
		int lastSuccessS = last.start;
		while (it.hasNext()) {
			StartSet start = it.next();
			
			double smeanext = NumericArray.wrap(totalActivity)
					.slice(start.start, lastSuccessS)
					.evaluate(NumericArrayFunction.SinhMean);
			
			if (log.isLoggable(Level.FINE)) log.fine("Checking extension coverage: "+start.start+" main smean="+smean+" smean="+smeanext);
			
			double fold = smeanext/smean;
			if (fold<ntermMinFold) {
				it.remove();
				if (log.isLoggable(Level.FINE)) log.fine("Remove start: "+start.start+" fold="+fold);
			} else
				lastSuccessS = start.start;
		}
		
		double bestPval = EI.wrap(startCands).mapToDouble(s->s.pval).min();
		
		Collections.sort(startCands,(a,b)->Integer.compare(a.start, b.start));
		
		cand.getData().alternativeStartAminoAcids = EI.wrap(startCands).mapToInt(s->s.start).toIntArray();
		StartSet first = EI.wrap(startCands).filter(s->s.pval==bestPval).first();
		cand.getData().predictedStartAminoAcid = first.start;
		cand.getData().combinedP = first.pval;
		checker.accept(first);
		return hasValid;
	}


//	private void checkAbortive(ImmutableReferenceGenomicRegion<PriceOrf> cand, IntervalTree<GenomicRegion,PriceOrf> covered, ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons) {
//		
//		
//		int startPos = cand.getData().getPredictedStartAminoAcid();
//		
//		if (cand.getData().getOrfAaLength(startPos)<abortiveLength+1) {
////			cand.getData().abortivePvalue = 1;
//		} else {
//		
//			if (log.isLoggable(Level.FINE)) log.fine("Checking for abortive translation");
//			
////			double startMean = NumericArray.wrap(re[0],0,abortiveLength).evaluate(NumericArrayFunction.SinhMean);
//			
//			double smean = NumericArray.wrap(cand.getData().getTotalActivities(startPos,startPos+abortiveLength)).evaluate(NumericArrayFunction.SinhMean);
//			double cut = smean*testFactor;
//			double smean2 = NumericArray.wrap(cand.getData().getTotalActivities(startPos+abortiveLength)).evaluate(NumericArrayFunction.SinhMean);
//			
//			if (log.isLoggable(Level.FINE)) log.fine("Sinh mean="+smean+" cutoff="+cut+" down="+smean2);
//			
//			
//			ToDoubleBiFunction<Integer,GenomicRegion> findBestOverlapper = getOverlapperFunction(covered, cut);
//			
//			
//			ImmutableReferenceGenomicRegion<PriceOrf> startstop = cand.getData().getPositionToStop(cand, startPos, false);
//			DoubleArrayList pp10 = new DoubleArrayList();
//			double p0 = noiseModel.getAbortiveStartLevelProbability(smean, cut/smean);
//			
//			for (int aapos=abortiveLength ; aapos<cand.getData().getOrfAaLength(startPos); aapos++) {
//				int profilePos = aapos+startPos;
//				int codonsPos = cand.map(profilePos*3);
//	
//				double p = findBestOverlapper.applyAsDouble(codonsPos,startstop.getRegion());
//				pp10.add(1-(1-p)*(1-p0));
//				
//	//			pp11.add(1-(1-findBestOverlapper.applyAsDouble(cand.map(profilePos*3+1),startstop.getRegion()))*(1-p1));
//	//			pp12.add(1-(1-findBestOverlapper.applyAsDouble(cand.map(profilePos*3-1),startstop.getRegion()))*(1-p2));
//				
//			}
//			
//			BitVector obs = getObserved(codons, startstop.getRegion().map(new ArrayGenomicRegion(abortiveLength*3,startstop.getRegion().getTotalLength())), cut);
//			int k = 0;
//			for (int i=1; i<obs.size(); i+=3) 
//				if (obs.getQuick(i))
//					k++;
//			
//			if (log.isLoggable(Level.FINE)) log.fine("pp10="+EI.wrap(pp10).add(new Counter<Double>()).toSingleLine());
//			if (log.isLoggable(Level.FINE)) log.fine("k="+k);
//			
//			double ipval0 = pp10.size()==0?1:new PoissonBinomial(pp10.toDoubleArray()).cumulative(k, true, false);
//			
//			if (log.isLoggable(Level.FINE)) log.fine("p="+ipval0);
//			
////			cand.getData().abortivePvalue = ipval0;
//		}
//		
//	}
	
	
	@SuppressWarnings("unchecked")
	private void deconvolve(ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons, List<ImmutableReferenceGenomicRegion<PriceOrf>> orfs) {
		
		if (log.isLoggable(Level.FINE)) log.fine("Deconvolution");
		
		HashMap<PriceOrf,Double> re = new HashMap<>();
		
		MemoryIntervalTreeStorage<PriceOrf> orfStore = new MemoryIntervalTreeStorage<>(PriceOrf.class);
		orfStore.fill(EI.wrap(orfs).filter(r->r.getData().combinedP<testThreshold));
		if (orfStore.size()==0) return;
		
		MutableReferenceGenomicRegion<Void> searcher = new MutableReferenceGenomicRegion<Void>().setReference(codonChr);
		
		HashMap<Codon,HashSet<ImmutableReferenceGenomicRegion<PriceOrf>>> cod2Eq = new HashMap<>();
		for (Codon c : codons.getData()) {
			HashSet<ImmutableReferenceGenomicRegion<PriceOrf>> eq = orfStore
					.ei(searcher.setRegion(c))
					.filter(t->t.getData().getStartStop(t,false).getRegion().containsUnspliced(c) && t.getData().getStartStop(t,false).getRegion().induce(c).getStart()%3==0)
					.toCollection(new HashSet<>());
			if (!eq.isEmpty())
				cod2Eq.put(c, eq);
		}
		
		// now equivalence classes: gives you all codons that are consistent with a specific combination of orfs
		HashMap<HashSet<ImmutableReferenceGenomicRegion<PriceOrf>>,HashSet<Codon>> equi = new HashMap<>();
		for (Codon c : cod2Eq.keySet()) {
			equi.computeIfAbsent(cod2Eq.get(c), x->new HashSet<>()).add(c);
		}
		if (equi.size()==0) 
			throw new RuntimeException();
		
		if (equi.size()>1 || equi.keySet().iterator().next().size()>1) {
			// only in this case, deconvolution is necessary!
			ImmutableReferenceGenomicRegion<PriceOrf>[][] E = new ImmutableReferenceGenomicRegion[equi.size()][];
			HashSet<Codon>[] codonsPer = new HashSet[E.length];
			int ind = 0;
			for (HashSet<ImmutableReferenceGenomicRegion<PriceOrf>> e : equi.keySet()) {
				if (e.size()>1 && log.isLoggable(Level.FINE)) log.fine("Overlap group: "+EI.wrap(e).map(r->r.getData().getOrfid()).concat(", "));
				codonsPer[ind] = equi.get(e);
				E[ind++] = e.toArray(new ImmutableReferenceGenomicRegion[0]);
			}
			
			
			double[] alpha = new double[E.length];
			for (int i=0; i<alpha.length; i++) {
				for (Codon codon : codonsPer[i])
					alpha[i] += codon.getTotalActivity();
			}
			
			
			new EquivalenceClassCountEM<ImmutableReferenceGenomicRegion<PriceOrf>>(E, alpha, t->t.getData().getOrfAaLength())
				.compute(miniter, maxiter, (t,pi)->re.put(t.getData(), pi/t.getData().getOrfAaLength()));
		}
		else {
			// equi.size==1 && element size ==1
			ImmutableReferenceGenomicRegion<PriceOrf> unique = equi.keySet().iterator().next().iterator().next();
			re.put(unique.getData(), 1.0);
		}
		
		
		double sum = 0;
		for (Double v : re.values())
			sum+=v;
		for (PriceOrf t : re.keySet()) {
			re.put(t, re.get(t)/sum);
			t.deconvolvedFraction = re.get(t);
			
			if (log.isLoggable(Level.FINE)) log.fine(t+" theta="+re.get(t));
		}
		
	}
	
	private void annotate(String gene,ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons, LinkedList<ImmutableReferenceGenomicRegion<PriceOrf>> candidates) {
		
		for (ImmutableReferenceGenomicRegion<PriceOrf> orf : candidates) {
			String orfseq = genomic.getSequence(codons.map(orf)).toString();
			orf.getData().startCodon = orfseq.substring(orf.getData().getPredictedStartAminoAcid()*3,orf.getData().getPredictedStartAminoAcid()*3+3);
			
			ImmutableReferenceGenomicRegion<Transcript> tr = tindex.get(orf.getData().transcriptId);
			orf.getData().type = tr==null?PriceOrfType.orphan:PriceOrfType.annotate(genomic, tr, codons.map(orf.getData().getStartStop(orf, true)));
			orf.getData().geneId = gene;
		}
		
	}
	
	private class StartSet {
		GenomicRegion startstop;
		int start;
		double pval;
		double ipval0;
		double ipvala;
		CheckOrfStatus test;
		public StartSet(GenomicRegion startstop, int start, double pval, double ipval0, double ipvala,CheckOrfStatus test) {
			this.startstop = startstop;
			this.start = start;
			this.pval = pval;
			this.ipval0 = ipval0;
			this.ipvala = ipvala;
			this.test = test;
		}
		@SuppressWarnings("unused")
		public String toString(ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons) {
			return codons.map(startstop)+" "+pval;
		}
	}


	

	public static class CodonProfile implements BinarySerializable {
		private double theta;
		private double[] sum;
		// [condition][position]
		private double[][] percond;
		private String transcript;
		
		public CodonProfile() {
		}
		

		public CodonProfile(double theta, int nucLen, int numCond, String transcript) {
			this.theta = theta;
			sum = new double[nucLen-1];
			percond = new double[numCond][nucLen-1];
			this.transcript = transcript;
		}

		public void clear() {
			Arrays.fill(sum, 0);
			for (int i=0; i<percond.length; i++)
				Arrays.fill(percond[i], 0);
		}

		public double[][] getActivities(ArrayGenomicRegion reg) {
			double[][] re = new double[percond.length][reg.getTotalLength()/3];
			for (int i=0; i<re.length; i++)
				for (int p=0; p<re[i].length; p++)
					re[i][p] = percond[i][reg.map(p*3)];
			return re;
		}

		public double getTotal() {
			return ArrayUtils.sum(sum);
		}
		
		public String getTranscript() {
			return transcript;
		}

		public void set(double prop, int pos, Codon c) {
			sum[pos]+=prop*c.getTotalActivity();
			for (int i=0; i<c.activity.length; i++)
				percond[i][pos]+=prop*c.activity[i];
		}

		@Override
		public void serialize(BinaryWriter out) throws IOException {
			out.putString(transcript);
			out.putDouble(theta);
			out.putCInt(percond.length);
			if (dense()) {
				out.putByte(1);
				for (int i=0; i<percond.length; i++) 
					FileUtils.writeDoubleArray(out, percond[i]);
			} else {
				out.putByte(0);
				out.putCInt(percond[0].length);
				for (int c=0; c<percond.length; c++) {
					for (int p=0; p<percond[c].length; p++) {
						if (percond[c][p]!=0) {
							out.putCInt(c);
							out.putCInt(p);
							out.putDouble(percond[c][p]);
						}
					}
				}
				out.putCInt(percond.length);
			}
		}

		private boolean dense() {
			int count = 0;
			for (int c=0; c<percond.length; c++) {
				for (int p=0; p<percond[c].length; p++) {
					if (percond[c][p]!=0)
						count++;
				}
			}
			return count*2>percond.length*percond[0].length;
		}

		@Override
		public void deserialize(BinaryReader in) throws IOException {
			transcript = in.getString();
			theta = in.getDouble();
			percond = new double[in.getCInt()][];
			if (in.getByte()==1) {
				for (int i=0; i<percond.length; i++)
					percond[i] = FileUtils.readDoubleArray(in);
			} else {
				int len = in.getCInt();
				for (int c=0; c<percond.length; c++) 
					percond[c]=new double[len];
				for (;;) {
					int c = in.getCInt();
					if (c==percond.length) break;
					int p = in.getCInt();
					percond[c][p] = in.getDouble();
				}
			}
			
			
			
			sum = new double[percond[0].length];
			for (int p=0; p<sum.length; p++)
				for (int i=0; i<percond.length; i++)
					sum[p] += percond[i][p];
		}
		
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

	public String[] getConditions() {
		int numCond = reads.getRandomRecord().getNumConditions();
		String[] conditions = new String[numCond];
		if (reads.getMetaData().isNull()) {
			for (int c=0; c<conditions.length; c++)
				conditions[c] = c+"";
		} else {
			for (int c=0; c<conditions.length; c++) {
				conditions[c] = reads.getMetaData().getEntry("conditions").getEntry(c).getEntry("name").asString();
				if ("null".equals(conditions[c])) conditions[c] = c+"";
			}
		}
		return conditions;
	}

	public GenomicRegionStorage<AlignedReadsData> getReads() {
		return reads;
	}

}
