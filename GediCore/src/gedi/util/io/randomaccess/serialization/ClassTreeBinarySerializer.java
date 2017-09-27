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

import gedi.util.ArrayUtils;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.mutable.MutableInteger;
import gedi.util.orm.ClassTree;
import gedi.util.orm.Orm;

import java.io.IOException;

public class ClassTreeBinarySerializer<T> extends AbstractBinarySerializer<T> {

	private ClassTree<T> classTree;
	private boolean compress = false;

	public ClassTreeBinarySerializer(T proto) {
		super((Class)proto.getClass(), true);
		classTree = new ClassTree<T>(proto);
	}
	
	public void setCompress(boolean compress) {
		this.compress = compress;
	}
	
	public boolean isCompress() {
		return compress;
	}


	@Override
	public void serialize(BinaryWriter out, T object) throws IOException {
		
		ClassTreeContext con = out.getContext().get(ClassTreeContext.class);
		
		con.buffer = classTree.toBuffer(object, con.buffer, con.length);
		byte[] ob = con.buffer;
		out.putCInt(con.length.N);
		if (compress) {
			while (con.cbuffer.length<ArrayUtils.getSaveCompressedSize(con.length.N)) con.cbuffer = new byte[con.cbuffer.length*2];
			con.length.N = ArrayUtils.compress(con.buffer, 0, con.length.N, con.cbuffer, 0);
			out.putCInt(con.length.N);
			ob=  con.cbuffer;
		}

		out.put(ob, 0, con.length.N);
		
	}

	@Override
	public T deserialize(BinaryReader in) throws IOException {
		
		ClassTreeContext con = in.getContext().get(ClassTreeContext.class);
		
		int size = in.getCInt();
		int rsize = size;
		if (compress) rsize = in.getCInt();
		if (con.buffer==null || con.buffer.length<rsize) con.buffer = new byte[rsize];
		
		in.get(con.buffer,0,rsize); // must always be large enough (has been used to write)
		byte[] ib = con.buffer;
		if (compress) {
			if (con.cbuffer==null || con.cbuffer.length<size) con.cbuffer = new byte[size];
			ArrayUtils.decompress(con.buffer, 0, con.cbuffer, 0, size);
			ib = con.cbuffer;
		}
		return (T)classTree.fromBuffer(ib);
	}
	
	
	@Override
	public void serializeConfig(BinaryWriter out) throws IOException {
		classTree.serialize(out);
		out.put(compress?(byte)1:(byte)0);
	}
	
	@Override
	public void deserializeConfig(BinaryReader in) throws IOException {
		classTree = Orm.create(ClassTree.class);
		classTree.deserialize(in);
		compress = in.get()==(byte)1;
		super.type = classTree.getType();
	}
	
	private static class ClassTreeContext {
		private byte[] buffer;
		private byte[] cbuffer;
		private MutableInteger length = new MutableInteger();
	}
	
	
}
