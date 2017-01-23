package gedi.util.datastructure.array.decorators;


import gedi.util.datastructure.array.NumericArray;

public class CumulatedNumericArray extends DecoratedNumericArray {
	
	
	public CumulatedNumericArray(NumericArray parent) {
		super(parent);
	}


	@Override
	public int compare(int index1, int index2) {
		return parent.compareInCum(index1, index2);
	}

	@Override
	public int compare(int index1, NumericArray a2, int index2) {
		throw new RuntimeException("Not possible!");
	}

	@Override
	public int compareInCum(int index1, int index2) {
		throw new RuntimeException("Not possible!");
	}

}
