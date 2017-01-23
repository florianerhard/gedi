package gedi.lfc.foldchange;

import gedi.lfc.Log2FoldChange;

public class MLLog2FoldChange implements Log2FoldChange{

	@Override
	public double computeFoldChange(double a, double b) {
		return (Math.log(a+1)-Math.log(b+1))/Math.log(2);
	}

}
