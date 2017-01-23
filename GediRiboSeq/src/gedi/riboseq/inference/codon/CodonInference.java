package gedi.riboseq.inference.codon;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.AlignedReadsDataFactory;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.sequence.SequenceProvider;
import gedi.riboseq.cleavage.RiboModel;
import gedi.riboseq.cleavage.SimpleCodonModel;
import gedi.util.FunctorUtils;
import gedi.util.datastructure.tree.redblacktree.IntervalTreeSet;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.mutable.MutableMonad;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

public class CodonInference {

	private RiboModel[] models;
	private double threshold = 1E-2;
	private int iters;
	private int maxIter = 1000;
	private double lastDifference;
	private SequenceProvider sequence;
	private double lambda = 1;
	private Predicate<ReferenceGenomicRegion<AlignedReadsData>> filter;
	
	
	public CodonInference(RiboModel[] models) {
		this.models = models;
	}
	public CodonInference(RiboModel[] models, SequenceProvider sequence) {
		this.models= models;
		this.sequence = sequence;
	}
	
	public CodonInference setFilter(
			Predicate<ReferenceGenomicRegion<AlignedReadsData>> filter) {
		this.filter = filter;
		return this;
	}
	
	
	public RiboModel[] getModels() {
		return models;
	}
	
	public void setMaxIter(int maxIter) {
		this.maxIter = maxIter;
	}
	
	public CodonInference setRegularization(double lambda) {
		this.lambda = lambda;
		return this;
	}
	
	public void setNeighborFactor(double neighborFactor) {
		this.neighborFactor = neighborFactor;
	}
	
	/**
	 * Infers codons and reports all that are contained, splice consistent and in-frame in/with part, in the 5' to 3' coordinate system of part
	 * 
	 * Part should be an ORF!
	 * @param reads
	 * @param part
	 * @return
	 */
	public ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> inferCodons(GenomicRegionStorage<AlignedReadsData> reads, ReferenceGenomicRegion<?> part) {
		Set<Codon> codons = inferCodons(()->Spliterators.iterator(reads.iterateIntersectingMutableReferenceGenomicRegions(part.getReference(), part.getRegion())),null);

		GenomicRegion reg = part.getRegion();
		IntervalTreeSet<Codon> set = new IntervalTreeSet<Codon>(part.getReference());
		for (Codon c : codons) {
			if (reg.containsUnspliced(c)) {
				ArrayGenomicRegion p = reg.induce(c);
				if (part.getReference().getStrand()==Strand.Minus)
					p = p.reverse(reg.getTotalLength());
				if (p.getStart()%3==0)
					set.add(new Codon(p, c.getActivity(), c.getTotalActivity(), c.getGoodness(), sequence==null?null:sequence.getSequence(part.getReference(), c).toString()));
			}
		}
		
		return new ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>>(part.getReference(), reg, set);
	}
	
	/**
	 * Infers codons and reports all that are contained in start-end, in the 5' to 3' coordinate system of ref
	 * 
	 * @param reads
	 * @param part
	 * @return
	 */
	public ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> inferCodons(GenomicRegionStorage<AlignedReadsData> reads, ReferenceSequence ref, int start, int end, MutableMonad<ToDoubleFunction<Collection<Codon>>> gofComp) {
		ArrayGenomicRegion reg = new ArrayGenomicRegion(start,end);
		Set<Codon> codons = inferCodons(()->Spliterators.iterator(reads.iterateIntersectingMutableReferenceGenomicRegions(ref, reg)),gofComp);

		IntervalTreeSet<Codon> set = new IntervalTreeSet<Codon>(ref);
		for (Codon c : codons) {
			if (reg.contains(c)) {
				ArrayGenomicRegion p = reg.induce(c);
				if (ref.getStrand()==Strand.Minus)
					p = p.reverse(reg.getTotalLength());
				set.add(new Codon(p, c.getActivity(), c.getTotalActivity(), c.getGoodness(), sequence==null?null:sequence.getSequence(ref, c)));
			}
		}
		
		return new ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>>(ref, reg, set);
	}
	
	
	/**
	 * Infers codons and reports all that are contained in start-end, in the 5' to 3' coordinate system of ref:(start-flanking)-(end+flanking)
	 * 
	 * @param reads
	 * @param part
	 * @return
	 */
	public ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> inferCodons(GenomicRegionStorage<AlignedReadsData> reads, ReferenceSequence ref, int start, int end, int flanking, MutableMonad<ToDoubleFunction<Collection<Codon>>> gofComp) {
		Set<Codon> codons = inferCodons(()->Spliterators.iterator(reads.iterateIntersectingMutableReferenceGenomicRegions(ref, new ArrayGenomicRegion(start,end))),gofComp);

		ArrayGenomicRegion reg = new ArrayGenomicRegion(Math.max(0, start-flanking),sequence==null?end+flanking:Math.min(end+flanking,sequence.getLength(ref.getName())));
		
		IntervalTreeSet<Codon> set = new IntervalTreeSet<Codon>(ref);
		for (Codon c : codons) {
			if (reg.contains(c)) {
				ArrayGenomicRegion p = reg.induce(c);
				if (ref.getStrand()==Strand.Minus)
					p = p.reverse(reg.getTotalLength());
				set.add(new Codon(p, c.getActivity(), c.getTotalActivity(), c.getGoodness(), sequence==null?null:sequence.getSequence(ref, c)));
			}
		}
		
		return new ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>>(ref, reg, set);
	}
	
	
	private double totalReads;
	private double neighborFactor = 10;
	
	public double getTotalReads() {
		return totalReads;
	}
	public Set<Codon> inferCodons(Supplier<Iterator<? extends ReferenceGenomicRegion<AlignedReadsData>>> reads, MutableMonad<ToDoubleFunction<Collection<Codon>>> gofComp) {
		if (models.length==1) 
			return inferCodons(models[0], -1, reads.get(), gofComp);
		
		ToDoubleFunction<Collection<Codon>>[] gofComps = new ToDoubleFunction[models.length];
		
		HashMap<Codon,Codon> unif = new HashMap<Codon, Codon>();
		for (int c=0; c<models.length; c++) {
			MutableMonad<ToDoubleFunction<Collection<Codon>>> mgof = new MutableMonad<ToDoubleFunction<Collection<Codon>>>();
			Set<Codon> tc = inferCodons(models[c], c, reads.get(), mgof);
			for (Codon cod : tc) {
				Codon ex = unif.get(cod);
				if (ex==null)
					unif.put(cod, cod);
				else {
					ex.goodness+=cod.goodness;
					ex.activity[c] = cod.activity[c];
					ex.totalActivity+=cod.activity[c];
				}
			}
			gofComps[c] = mgof.Item;
		}
		
		
		if (gofComp!=null)
			gofComp.Item = c->{
				double re = 0;
				for (int i=0; i<gofComps.length; i++)
					re+=gofComps[i].applyAsDouble(c);
				return re;
			};
		
		return unif.keySet();
	}
	
	private Set<Codon> inferCodons(RiboModel model, int condition, Iterator<? extends ReferenceGenomicRegion<AlignedReadsData>> reads, MutableMonad<ToDoubleFunction<Collection<Codon>>> gofComp) {
		if (filter!=null)
			reads = FunctorUtils.filteredIterator(reads, filter);
		
		if (model.getSimple()!=null) {
			return inferSimple(reads, model.getSimple());
		}
		
		
		ReadsXCodonMatrix m = new ReadsXCodonMatrix(model, condition);
		int readcount = m.addAll(reads);
		m.finishReads();
		totalReads = m.getTotal();
		
		if (readcount==0) return Collections.emptySet();
		
		int cond = m.checkConditions();
		if (cond==-2) // no reads at all
			return new HashSet<Codon>();
		
		if (cond<0) throw new RuntimeException("Inconsistent conditions!");
		
		iters = 0;
		do  {
			m.computeExpectedReadsPerCodon();
			m.computePriorReadProbabilities();
			m.computeExpectedCodonPerRead();
			iters++;
			lastDifference = m.computeExpectedCodons();
//			System.out.println(m.computeLogLikelihood());
		} while (lastDifference>threshold && iters<maxIter);
		
		if (lambda>=0) {
//			System.out.println("Before "+m.computeLogLikelihood());
			// regularization
			HashMap<Integer,ArrayList<Codon>> startMap = new HashMap<>();
			HashMap<Integer,ArrayList<Codon>> endMap = new HashMap<>();
			for (Codon c : m.getCodons()) {
				startMap.computeIfAbsent(c.getStart(), x->new ArrayList<>()).add(c);
				endMap.computeIfAbsent(c.getEnd(), x->new ArrayList<>()).add(c);
			}
			
			
			Codon[] codons = m.getCodons().toArray(new Codon[0]);
			Arrays.sort(codons,(a,b)->Double.compare(a.totalActivity, b.totalActivity));
//			double ll = m.computeLogLikelihood();
			m.copySlots(1, 2);
			HashSet<Codon> removed = new HashSet<Codon>();
			HashMap<Codon,ArrayList<Codon>> removeIfNeighborRemoved = new HashMap<Codon, ArrayList<Codon>>();
			for (Codon c : codons) {
				ArrayList<Codon> neia = startMap.get(c.getEnd());
				ArrayList<Codon> neib = endMap.get(c.getStart());
				boolean hasNeighbors = neia!=null && neib!=null &&  EI.wrap(neia).filter(n->!removed.contains(n) && n.totalActivity>threshold).count()>0 
						&& EI.wrap(neib).filter(n->!removed.contains(n) && n.totalActivity>threshold).count()>0;

				double delta = m.regularize3(c);
//				if (c.getStart()>=142747 && c.getStart()<142880 && c.getStart()%3==1)
//					System.out.println("overlap "+c+" "+delta);
//				if (c.getStart()<142747 && c.getStart()>142580 && c.getStart()%3!=0)
//					System.out.println("nooverl "+c+" "+delta);
				
				if (delta<(hasNeighbors?-lambda/neighborFactor :-lambda)){
					m.copySlotsReads(c,2, 1); //revert
				} else {
					if (hasNeighbors && delta<-lambda) { // i.e. there are two neigbors; if they are removed and there are no other neighbors, c must also be deleted
						ExtendedIterator<Codon> ita = EI.wrap(neia).filter(n->!removed.contains(n) && n.totalActivity>threshold);
						ExtendedIterator<Codon> itb = EI.wrap(neib).filter(n->!removed.contains(n) && n.totalActivity>threshold);
						Codon na = ita.next();
						Codon nb = itb.next();
						if (!ita.hasNext())
							removeIfNeighborRemoved.computeIfAbsent(na, x->new ArrayList<>()).add(c);
						if (!itb.hasNext())
							removeIfNeighborRemoved.computeIfAbsent(nb, x->new ArrayList<>()).add(c);
						
					}
					m.copySlotsReads(c,1, 2); //save it
					removed.add(c);
					m.computeExpectedCodons(c);
					
				}
			}
			
			for (Codon c : removeIfNeighborRemoved.keySet()) {
				if (removed.contains(c)) {
					for (Codon rc : removeIfNeighborRemoved.get(c)) {
						m.regularize3(rc);
						m.computeExpectedCodons(c);
					}
				}
			}
//			System.out.println("After "+m.computeLogLikelihood());
			m.computeExpectedCodons();
			m.removeZeroCodons();
			m.resetCodons();
			
			iters = 0;
			do  {
				m.computeExpectedReadsPerCodon();
				m.computePriorReadProbabilities();
				m.computeExpectedCodonPerRead();
				iters++;
				lastDifference = m.computeExpectedCodons();
	//			System.out.println(m.computeLogLikelihood());
			} while (lastDifference>threshold && iters<maxIter);
			// end regularization
//			System.out.println("Recalc "+m.computeLogLikelihood());
			
		}
		
		m.computeGoodnessOfFit();
		
		m.computeExpectedReadsPerCodon();
		m.computePriorReadProbabilities();
		m.copySlots(1,2); 
		
		// do the final inference step for each condition individually
		if (condition==-1) {
			for (int i=0; i<cond; i++) {
				m.computeExpectedCodonPerRead(i);
				m.computeExpectedCodons(i);
				m.copySlots(2, 1); // restore the prior read probabilities
			}
		} else {
			m.computeExpectedCodonPerRead(condition);
			m.computeExpectedCodons(condition);
			m.copySlots(2, 1); // restore the prior read probabilities
		}
		
//		System.out.println(iters+"\t"+lastDifference);
		
		if (gofComp!=null) {
			m.prepareGoodnessOfFit();
			gofComp.Item = coll->m.computeGoodnessOfFit(coll);
		}
		
		return m.getCodons();
	}
	
	
	
	public Set<Codon> inferSimple(
			Iterator<? extends ReferenceGenomicRegion<AlignedReadsData>> reads,
			SimpleCodonModel simple) {
		HashMap<Codon,Codon> unifier = new HashMap<Codon, Codon>();
		while (reads.hasNext()) {
			ImmutableReferenceGenomicRegion<AlignedReadsData> r = reads.next().toImmutable();
			for (int d=0; d<r.getData().getDistinctSequences(); d++) {
				int p = simple.getPosition(r, d);
				if (p>-1) {
					ArrayGenomicRegion cp = r.getReference().getStrand().equals(Strand.Plus)?
							new ArrayGenomicRegion(p,p+3)
							:new ArrayGenomicRegion(r.getRegion().getTotalLength()-p-3,r.getRegion().getTotalLength()-p);
					Codon cod = unifier.computeIfAbsent(new Codon(r.getRegion().map(cp),0), a->a);
					
					double c = r.getData().getTotalCountForDistinct(d, ReadCountMode.Weight);// getSumCount(d,true);
					cod.totalActivity+=c;
					cod.activity = r.getData().addCountsForDistinct(d, cod.activity, ReadCountMode.Weight);
				}
			}
		}
		return unifier.keySet();
		
	}
	
	
	public double getLastDifference() {
		return lastDifference;
	}
	
	public int getIterations() {
		return iters;
	}
	
	public static void main(String[] args) {
		
		RiboModel model = new RiboModel() {
			@Override
			public double getPosterior(boolean leading, int l, int p) {
				if (!leading && l==28) {
					if (p==11) return 0.1;
					if (p==12) return 0.7;
					if (p==13) return 0.2;
				}
				return 0;
			}
			@Override
			public int getObservedMaxLength() {
				return 28;
			}
		};
		model.setModel(false, 28, new double[] {0,0,0,0,0,0,0,0,0,0,0,1,1,1});
		
		
		CodonInference inf = new CodonInference(new RiboModel[]{model});
		System.out.println(inf.inferCodons(()->Arrays.asList(
				new MutableReferenceGenomicRegion<AlignedReadsData>().set(Chromosome.obtain("chr1+"), new ArrayGenomicRegion(1,29), new AlignedReadsDataFactory(1).start().newDistinctSequence().setCount(0, 2).create()),
				new MutableReferenceGenomicRegion<AlignedReadsData>().set(Chromosome.obtain("chr1+"), new ArrayGenomicRegion(2,30), new AlignedReadsDataFactory(1).start().newDistinctSequence().setCount(0, 7).create()),
				new MutableReferenceGenomicRegion<AlignedReadsData>().set(Chromosome.obtain("chr1+"), new ArrayGenomicRegion(3,31), new AlignedReadsDataFactory(1).start().newDistinctSequence().setCount(0, 1).create())
				).iterator(),null)
				);
		System.out.println(inf.getIterations());
		
	}
	public SequenceProvider getGenome() {
		return sequence;
	}
	
	
	
}
