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

package gedi.util.mutable;

import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.FixedSizeBinarySerializable;

import java.io.IOException;

public class MutableDouble extends Number implements FixedSizeBinarySerializable, Comparable<MutableDouble>, Mutable{
	
	
	public double N;
	
	public MutableDouble() {
		
	}
	
	public MutableDouble set(double N) {
		this.N = N;
		return this;
	}
	
	public MutableDouble(double N) {
		this.N = N;
	}
	@Override
	public String toString() {
		return N+"";
	}
	@Override
	public double doubleValue() {
		return N;
	}
	@Override
	public float floatValue() {
		return (float)N;
	}
	@Override
	public int intValue() {
		return (int)N;
	}
	@Override
	public long longValue() {
		return (long)N;
	}
	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putDouble(N);
	}
	@Override
	public void deserialize(BinaryReader in) throws IOException {
		N = in.getDouble();
	}
	@Override
	public int getFixedSize() {
		return Double.BYTES;
	}

	@Override
	public int compareTo(MutableDouble o) {
		return Double.compare(N, o.N);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MutableDouble))
			return false;
		MutableDouble n = (MutableDouble) obj;
		return N==n.N;
	}
	@Override
	public int hashCode() {
		return Double.hashCode(N);
	}
	
	@Override
	public int size() {
		return 1;
	}
	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(int index) {
		if (index==0) return (T)new Double(N);
		throw new IndexOutOfBoundsException();
	}
	@SuppressWarnings("unchecked")
	@Override
	public <T> T set(int index, T o) {
		if (index==0) {
			T re = (T)new Double(N);
			N = (Double)o;
			return re;
		}
		throw new IndexOutOfBoundsException();
	}
}
