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
import gedi.util.io.randomaccess.BinaryWriter;

import java.io.IOException;

public class SparseMemoryFloatArray extends MemoryFloatArray {
	
	
	public SparseMemoryFloatArray() {
	}
	
	public SparseMemoryFloatArray(float[] wrap) {
		super(wrap);
	}
	public SparseMemoryFloatArray(int length) {
		super(length);
	}

	
	@Override
	public void serialize(BinaryWriter out) throws IOException {
		int zeros = 0;
		for (int i=0; i<length(); i++)
			if (getFloat(i)==0)
				zeros++;
		if (4*length()>(length()-zeros)*5+1) {
			// sparse
			out.putInt(-length());
			out.putCInt(length()-zeros);
			for (int i=0; i<length(); i++)
				if (getFloat(i)!=0) {
					out.putCInt(i);
					out.putFloat(getFloat(i));
				}
		} else {
			out.putInt(length());
			for (int i=0; i<length(); i++)
				out.putFloat(getFloat(i));
		}
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		int len = in.getInt();
		if (len>=0) {
			if (a==null || a.length!=len)
				a = new float[len];
			for (int i=0; i<a.length; i++)
				a[i] = in.getFloat();
		} else {
			len = -len;
			if (a==null || a.length!=len)
				a = new float[len];
			int elems = in.getCInt();
			for (int i=0; i<elems; i++) {
				int ind = in.getCInt();
				a[ind] = in.getFloat();
			}
		}
	}
	
}
