/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */

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
