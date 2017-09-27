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

package gedi.util.datastructure.array;

import gedi.util.io.randomaccess.BinaryReader;

import java.io.IOException;
import java.util.Arrays;

public class MemoryIntegerArray extends IntegerArray {
	
	private int[] a;
	
	public MemoryIntegerArray() {
	}
	
	public MemoryIntegerArray(int[] wrap) {
		this.a = wrap;
	}
	public MemoryIntegerArray(int length) {
		if (length>=0) a = new int[length];
	}

	@Override
	public void setInt(int index, int value) {
		a[index] = value;
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Override
	public int length() {
		return a.length;
	}
	@Override
	public NumericArray clear() {
		Arrays.fill(a, 0);
		return this;
	}

	@Override
	public int getInt(int index) {
		return a[index];
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		int len = in.getInt();
		if (a==null || a.length!=len)
			a = new int[len];
		for (int i=0; i<a.length; i++)
			a[i] = in.getInt();
	}
	
	
	@Override
	public int compare(int index1, NumericArray a2, int index2) {
		return Integer.compare(a[index1], a2.getInt(index2));
	}

	@Override
	public int compareInCum(int index1, int index2) {
		int b1 = index1==0?a[index1]:(a[index1]-a[index1-1]);
		int b2 = index2==0?a[index2]:(a[index2]-a[index2-1]);
		return Integer.compare(b1,b2);
	}
	
	@Override
	public int compare(int index1, int index2) {
		return Integer.compare(a[index1], a[index2]);
	}
	
	@Override
	public NumericArray copy(int from, int to) {
		a[to] = a[from];
		return this;
	}
	
	@Override
	public NumericArray sort(int from, int to) {
		Arrays.sort(a, from, to);
		return this;
	}
	
	@Override
	public NumericArray reverse(int from, int to) {
		to--;
        while (from< to) {
        	int tmp = a[from];
    		a[from++] = a[to];
    		a[to++] = tmp;
        }
        return this;
	}
	

	@Override
	public NumericArray cumSum(int from, int to) {
		for (int i=from+1; i<to; i++)
			a[i] += a[i-1];
		return this;
	}

	@Override
	public NumericArray deCumSum(int from, int to) {
		for (int i=to-1; i>from; i--)
			a[i] -= a[i-1];
		return this;
	}

	@Override
	public NumericArray switchElements(int i, int j) {
		int tmp = a[i];
		a[i] = a[j];
		a[j] = tmp;
		return this;
	}

	@Override
	public NumericArray copy(NumericArray fromArray, int fromIndex, int to) {
		a[to]=fromArray.getInt(fromIndex);
		return this;
	}

	@Override
	public NumericArray copyRange(int start, NumericArray dest, int destOffset,
			int len) {
		if (dest instanceof MemoryIntegerArray)
			System.arraycopy(a, start, ((MemoryIntegerArray)dest).a, destOffset, len);
		else
			return super.copyRange(start, dest, destOffset, len);
		return this;
	}

}
