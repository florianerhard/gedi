package gedi.lfc.foldchange;

import gedi.lfc.Log2FoldChange;

import org.apache.commons.math3.special.Gamma;

public class PosteriorMeanLog2FoldChange implements Log2FoldChange{
	
	private double priorA = 1;
	private double priorB = 1;
	
	/**
	 * These are the prior parameters, i.e. for no pseudocounts, use 1!
	 * @param priorA
	 * @param priorB
	 */
	public PosteriorMeanLog2FoldChange(double priorA, double priorB) {
		this.priorA = priorA;
		this.priorB = priorB;
	}


	@Override
	public double computeFoldChange(double a, double b) {
		if (Double.isNaN(a) || Double.isNaN(b)) return Double.NaN;
		return (Gamma.digamma(a+priorA)-Gamma.digamma(b+priorB))/Math.log(2);
	}

}
