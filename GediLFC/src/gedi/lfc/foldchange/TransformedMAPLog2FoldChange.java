package gedi.lfc.foldchange;

import gedi.lfc.Log2FoldChange;

public class TransformedMAPLog2FoldChange implements Log2FoldChange{
	
	private double priorA = 1;
	private double priorB = 1;
	
	/**
	 * These are the prior parameters, i.e. for no pseudocounts, use 1!
	 * @param priorA
	 * @param priorB
	 */
	public TransformedMAPLog2FoldChange(double priorA, double priorB) {
		this.priorA = priorA;
		this.priorB = priorB;
	}


	@Override
	public double computeFoldChange(double a, double b) {
		return (Math.log(a+priorA-1)-Math.log(b+priorB-1))/Math.log(2);
	}

}
