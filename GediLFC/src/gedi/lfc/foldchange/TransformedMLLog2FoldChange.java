package gedi.lfc.foldchange;

import gedi.lfc.Log2FoldChange;

public class TransformedMLLog2FoldChange implements Log2FoldChange{

	@Override
	public double computeFoldChange(double a, double b) {
		return (Math.log(a)-Math.log(b))/Math.log(2);
	}

}
