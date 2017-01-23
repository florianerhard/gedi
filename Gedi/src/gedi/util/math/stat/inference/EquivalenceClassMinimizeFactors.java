package gedi.util.math.stat.inference;

import gedi.util.ArrayUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.datastructure.unionFind.IntUnionFind;
import gedi.util.math.optim.LBFGS;
import gedi.util.math.optim.LBFGS.Result;
import gedi.util.math.stat.RandomNumbers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.BiConsumer;


/**
 * Minimizes the factors to make the coverages of given equivalence classes add up. More precisely, let c_1...c_k be the unknown coverages of 
 * k transcripts. For an equivalence class e, the observed coverage should be v_e = sum_{i \in e} c_i under the assumption of uniform read coverage.
 * When this assumption is not met, v_e = f_e * sum_{i \in e} c_i for a small log(f_e)^2.
 * 
 * The c_j can then be taken to weigh the total count in an equivalence class.
 * 
 * The minimization is done using {@link LBFGS} for the function f(c_1,...,c_k) = \sum_e log^2 (v_e/(\sum_{i \in e} c_i))
 * with partial derivatives df/dc_i = \sum_e (i \in e) -2 * log (v_e/(\sum_{i \in e} c_i) / \sum_{i \in e} c_i
 * 
 * @author erhard
 *
 */
public class EquivalenceClassMinimizeFactors<O> {

	private O[][] E;
	private boolean normalize = true;
	
	private HashMap<O,Integer> o2Index;
	private HashSet<O> all;
	private double[] v;
	private RandomNumbers rnd = RandomNumbers.getGlobal();
	
	public EquivalenceClassMinimizeFactors(O[][] E,
			double[] v) {
		all = new HashSet<O>();
		for (O[] e : E) 
			for (O o : e)
				all.add(o);
		
		this.E = ArrayUtils.restrict(E, i->v[i]>0);
		o2Index = new HashMap<O, Integer>();
		for (O[] e : this.E) 
			for (O o : e)
				o2Index.computeIfAbsent(o, x->o2Index.size());
		this.v = ArrayUtils.restrict(v, i->v[i]>0);
				
	}
	
	public EquivalenceClassMinimizeFactors<O> setNormalize(boolean normalize) {
		this.normalize = normalize;
		return this;
	}

	/**
	 * Returns true if there was no error during optimization
	 * @param coverageSetter
	 * @return
	 */
	public boolean compute(BiConsumer<O,Double> coverageSetter) {
		double[] c= new double[o2Index.size()];
		
		// determine independent components first
		IntUnionFind uf = new IntUnionFind(E.length);
		for (int i=1; i<E.length; i++) {
			for (int j=0; j<i; j++) {
				if (intersects(j,i))
					uf.union(i, j);
			}
		}
		
		IntArrayList[] independentGroups = uf.getGroups();
		boolean success = true;
		
		if (independentGroups.length>1) {
			
			for (IntArrayList ic : independentGroups) {
				O[][] Eic = ArrayUtils.restrict(E, ind->ic.contains(ind));
				double[] vic = ArrayUtils.restrict(v, ind->ic.contains(ind));
				success &= new EquivalenceClassMinimizeFactors<O>(Eic, vic).setNormalize(false).compute((o,cov)->{c[o2Index.get(o)] = cov;});
			}
			
		} else if (E.length==1) {
			// no optimization necessary, just distribute evenly
			for (O o : E[0])
				c[o2Index.get(o)] = v[0]/E[0].length;
		}
		else {
			for (int i=0; i<c.length; i++)
				c[i] = rnd.getUnif();
			
			Result res = LBFGS.lbfgs(c, this::lbfgsfun);
			success = !res.status.isError();
		}
		
//		double[] re = new double[o2Index.size()];
//		for (int e=0; e<E.length; e++) {
//			double csum = EI.wrap(E[e]).mapToDouble(i->c[o2Index.get(i)]).sum();
//			for (O i : E[e]) {
//				re[o2Index.get(i)] += v[e]*c[o2Index.get(i)]/csum;
//			}
//		}		
		
//		for (O o : o2Index.keySet())
//			coverageSetter.accept(o,re[o2Index.get(o)]);
		
		if (normalize)
			ArrayUtils.normalize(c);
		
		for (O o : all)
			coverageSetter.accept(o,o2Index.get(o)!=null?c[o2Index.get(o)]:0);
		
		return success;
	}
	
	
	private boolean intersects(int j, int i) {
		HashSet<O> s = new HashSet<O>(Arrays.asList(E[i]));
		s.retainAll(Arrays.asList(E[j]));
		return !s.isEmpty();
	}

	private double lbfgsfun(double[] c, double[] g, int n, double step) {
		if (ArrayUtils.min(c)<0) return Double.POSITIVE_INFINITY;
		double[] csum = new double[E.length];
		for (int e=0; e<E.length; e++) {
			for (O i : E[e])
				csum[e] += c[o2Index.get(i)];
		}
		
		double[] logs = new double[E.length];
		for (int e=0; e<logs.length; e++) {
			logs[e] = Math.log(v[e]/csum[e]);
		}
		
		Arrays.fill(g, 0);
		double re = 0;
		for (int e=0; e<E.length; e++) {
			re+=logs[e]*logs[e];
			double a = -2*logs[e]/csum[e];
			for (O i : E[e])
				g[o2Index.get(i)] += a;
		}
//		System.out.println(Arrays.toString(c)+"\t"+re);
		return re;
	}
	
	
}
