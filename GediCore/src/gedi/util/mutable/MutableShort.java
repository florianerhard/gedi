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

public class MutableShort extends Number implements Comparable<MutableShort>, FixedSizeBinarySerializable, Mutable {
	public short N;
	public MutableShort() {
	}
	public MutableShort(short N) {
		this.N = N;
	}
	public MutableShort set(short N) {
		this.N = N;
		return this;
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
		return N;
	}
	@Override
	public int intValue() {
		return N;
	}
	@Override
	public long longValue() {
		return N;
	}
	@Override
	public int compareTo(MutableShort o) {
		return Short.compare(N,o.N);
	}
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MutableShort))
			return false;
		MutableShort n = (MutableShort) obj;
		return N==n.N;
	}
	@Override
	public int hashCode() {
		return Short.hashCode(N);
	}
	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putShort(N);
	}
	@Override
	public void deserialize(BinaryReader in) throws IOException {
		N = in.getShort();
	}
	@Override
	public int getFixedSize() {
		return Short.BYTES;
	}
	@Override
	public int size() {
		return 1;
	}
	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(int index) {
		if (index==0) return (T)new Short(N);
		throw new IndexOutOfBoundsException();
	}
	@SuppressWarnings("unchecked")
	@Override
	public <T> T set(int index, T o) {
		if (index==0) {
			T re = (T)new Short(N);
			N = (Short)o;
			return re;
		}
		throw new IndexOutOfBoundsException();
	}
}
