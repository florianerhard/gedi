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
import java.util.Arrays;

public class DiskByteArray extends ByteArray {
	
	private long offset;
	private int length;
	private BinaryReader reader;
	private BinaryWriter writer;
	
	public DiskByteArray() {
	}
	
	public DiskByteArray(BinaryReader in, BinaryWriter out, long offset, int length) {
		this.reader = in;
		this.length = length;
		this.offset = offset;
		this.writer = out;
	}
	
	
	
	@Override
	public void deserialize(BinaryReader in) throws IOException {
		length = in.getInt();
		offset = in.position();
		this.reader = in;
		this.writer = in instanceof BinaryWriter?(BinaryWriter)in:null;
		in.position(offset+length*getType().getBytes());
	}
	
	@Override
	public void setByte(int index, byte value) {
		try {
			writer.put(offset+index*getType().getBytes(),value);
		} catch (IOException e) {
			throw new RuntimeException("Cannot set byte value in reader/writer!",e);
		}
	}
	
	@Override
	public NumericArray clear() {
		for (int i=0; i<length(); i++)
			setByte(i, (byte)0);
		return this;
	}

	
	@Override
	public boolean isReadOnly() {
		return writer==null;
	}

	@Override
	public int length() {
		return length;
	}

	@Override
	public byte getByte(int index) {
		try {
			return reader.get(offset+index*getType().getBytes());
		} catch (IOException e) {
			throw new RuntimeException("Cannot get byte value from reader/writer!",e);
		}
	}
	
	

}
