package gedi.util.datastructure.array.computed;

import java.io.IOException;
import java.util.function.IntUnaryOperator;

import gedi.util.datastructure.array.IntegerArray;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.io.randomaccess.BinaryReader;

public class ComputedIntegerArray extends IntegerArray {

	private IntUnaryOperator function;
	private int length;
	
	public ComputedIntegerArray(IntUnaryOperator function, int length) {
		this.function = function;
		this.length = length;
	}

	@Override
	public void setInt(int index, int value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public int length() {
		return length;
	}

	@Override
	public int getInt(int index) {
		return function.applyAsInt(index);
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public NumericArray clear() {
		throw new UnsupportedOperationException();
	}
	
}
