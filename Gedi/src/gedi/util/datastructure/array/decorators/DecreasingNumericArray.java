package gedi.util.datastructure.array.decorators;


import gedi.util.datastructure.array.NumericArray;

public class DecreasingNumericArray extends DecoratedNumericArray {
	
	
	public DecreasingNumericArray(NumericArray parent) {
		super(parent);
	}
	
	@Override
	public void setInfimum(int index) {
		parent.setSupremum(index);
	}
	
	@Override
	public void setSupremum(int index) {
		parent.setInfimum(index);
	}


	@Override
	public int compare(int index1, int index2) {
		return -parent.compare(index1, index2);
	}

	@Override
	public int compare(int index1, NumericArray a2, int index2) {
		return -parent.compare(index1, a2, index2);
	}

	@Override
	public int compareInCum(int index1, int index2) {
		return -parent.compareInCum(index1, index2);
	}

}
