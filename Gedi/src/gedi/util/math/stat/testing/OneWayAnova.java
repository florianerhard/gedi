package gedi.util.math.stat.testing;

import gedi.util.ArrayUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;

import java.util.ArrayList;

import org.apache.commons.math3.distribution.FDistribution;
import org.apache.commons.math3.stat.descriptive.UnivariateStatistic;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.summary.SumOfSquares;

/**
 * Not thread safe!
 * @author erhard
 *
 */
public class OneWayAnova {

	private OneWayAnovaData values;
	
	private double[] working = new double[0];
	private double[] centrals = new double[0];
	private int[] num = new int[0];
	private double varFactor;
	private double varTotal;
	private double varErr;
	
	private UnivariateStatistic central = new Mean();
	private UnivariateStatistic var = new SumOfSquares();
	
	private double centralTotal;
	
	public void setValues(OneWayAnovaData values) {
		this.values = values;
	}
	
	public void setCentralStatistic(UnivariateStatistic central) {
		this.central = central;
	}
	
	public void setSumOfSquaresStatistic(UnivariateStatistic var) {
		this.var = var;
	}
	
	public void compute() {
		ensureSize();
		int n = values.getNumObservations();
		
		for (int i=0; i<values.getNumGroups(); i++)
			num[i]=0;
		for (int i=0; i<n; i++) {
			working[i] = values.getObservation(i);
			num[values.getGroup(i)]++;
		}
		
		centralTotal = central.evaluate(working,0,n);
		varTotal = var.evaluate(working,0,n);
		
		ArrayUtils.cumSumInPlace(num, +1);
		for (int i=0; i<n; i++) 
			working[--num[values.getGroup(i)]] = values.getObservation(i);
		for (int i=0; i<n; i++) 
			num[values.getGroup(i)]++;
		
		varErr=0;
		for (int g=0; g<values.getNumGroups(); g++) {
			int s = g==0?0:num[g-1];
			centrals[g] = central.evaluate(working,s,num[g]-s);
			varErr+=var.evaluate(working,s,num[g]-s);
			for (int i=s; i<num[g]; i++)
				working[i] = centrals[g];
		}
		
		varFactor = var.evaluate(working,0,n);
	}
	
	public double getCentralTotal() {
		return centralTotal;
	}
	
	public double getCentral(int group) {
		return centrals[group];
	}
	
	public double getFactorMeanSquare() {
		return varFactor/getFactorDof();
	}
	
	public double getErrorMeanSquare() {
		return varErr/getErrorDof();
	}
	
	public double getFStatistic() {
		return getFactorMeanSquare()/getErrorMeanSquare();
	}
	
	public double getPvalue() {
		FDistribution fdist = new FDistribution(getFactorDof(), getErrorDof());
		if (getFactorDof()<1 || getErrorDof()<1) return Double.NaN;
		return 1-fdist.cumulativeProbability(getFStatistic());
	}
	
	private void ensureSize() {
		if (centrals.length<=values.getNumGroups()) {
			int nm = 1;
			for (;nm<=values.getNumGroups(); nm<<=1);
			centrals = new double[nm];
			num = new int[nm];
		}
		if (working.length<=values.getNumObservations()) {
			int nm = 1;
			for (;nm<=values.getNumObservations(); nm<<=1);
			working = new double[nm];
		}
	}
	
	public double getVarTotal() {
		return varTotal;
	}
	
	public double getVarErr() {
		return varErr;
	}
	
	public double getVarFactor() {
		return varFactor;
	}

	public double getEtaSq() {
		return varFactor/varTotal;
	}
	
	public int getTotalDof() {
		return values.getNumObservations()-1;
	}
	
	public int getFactorDof() {
		return values.getNumGroups()-1;
	}
	
	public int getErrorDof() {
		return values.getNumObservations()-values.getNumGroups();
	}
	
	
	public static interface OneWayAnovaData {
		int getNumObservations();
		int getNumGroups();
		double getObservation(int index);
		int getGroup(int index);
	}
	
	public static class DefaultRobustOneWayAnovaData implements OneWayAnovaData {

		private IntArrayList sizes = new IntArrayList();
		private ArrayList<double[]> list = new ArrayList<double[]>();
		
		public void add(double[] group) {
			sizes.add((sizes.size()==0?0:sizes.getLastInt())+group.length);
			list.add(group);
		}
		
		@Override
		public int getNumObservations() {
			return sizes.getLastInt();
		}

		@Override
		public int getNumGroups() {
			return sizes.size();
		}

		@Override
		public double getObservation(int index) {
			int g = getGroup(index);
			return list.get(g)[index-(g==0?0:sizes.getInt(g-1))];
		}

		@Override
		public int getGroup(int index) {
			int g = sizes.binarySearch(index);
			if (g>=0) g++;
			else g = -g-1;
			return g;
		}
		
		
	}
	
}
