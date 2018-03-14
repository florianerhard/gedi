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
package gedi.util.io.randomaccess.serialization;

import gedi.util.ReflectionUtils;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.orm.Orm;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.function.Supplier;

import cern.colt.Arrays;

/**
 * Elements cannot be null!
 * @author erhard
 *
 * @param <T>
 */
public class BinarySerializableArray<T extends BinarySerializable> implements BinarySerializable {

	protected T[] a;
	private Class<T> cls;
	private Supplier<T> supplier;
	
	public BinarySerializableArray(Class<T> cls) {
		this(cls,()->Orm.create(cls));
	}
	
	public BinarySerializableArray(Class<T> cls, Supplier<T> supplier) {
		this.cls = cls;
		this.supplier = supplier;
	}
	
	public T[] getArray() {
		return a;
	}
	
	public void setArray(T[] a) {
		this.a = a;
	}

	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putCInt(a.length);
		for (int i=0; i<a.length; i++)
			a[i].serialize(out);
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		a = (T[]) Array.newInstance(cls, in.getCInt());
		for (int i=0; i<a.length; i++) {
			a[i] = supplier.get();
			a[i].deserialize(in);
		}
	}
	@Override
	public String toString() {
		return Arrays.toString(a);
	}
	
	
}
