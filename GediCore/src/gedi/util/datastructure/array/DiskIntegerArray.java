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

public class DiskIntegerArray extends IntegerArray {
	
	private long offset;
	private int length;
	private BinaryReader reader;
	private BinaryWriter writer;
	
	
	public DiskIntegerArray() {
	}
	
	public DiskIntegerArray(BinaryReader in, BinaryWriter out, long offset, int length) {
		this.reader = in;
		this.length = length;
		this.offset = offset;
		this.writer = out;
	}
	
	
	@Override
	public void deserialize(BinaryReader in) throws IOException {
		deserialize(in, in.getInt());
	}

	/**
	 * Use this, if length already known and not in the reader
	 * @param in
	 * @param length
	 * @throws IOException
	 */
	public void deserialize(BinaryReader in, int length) throws IOException {
		this.length = length;
		offset = in.position();
		this.reader = in;
		this.writer = in instanceof BinaryWriter?(BinaryWriter)in:null;
		
		in.position(offset+length*getType().getBytes());
	}
	

	@Override
	public NumericArray clear() {
		for (int i=0; i<length(); i++)
			setInt(i, 0);
		return this;
	}
	
	@Override
	public void setInt(int index, int value) {
		try {
			writer.putInt(offset+index*getType().getBytes(),value);
		} catch (IOException e) {
			throw new RuntimeException("Cannot set int value in reader/writer!",e);
		}
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
	public int getInt(int index) {
		try {
			if (index<0 || index>=length) 
				throw new IndexOutOfBoundsException(index+"<0 or >="+length);
			return reader.getInt(offset+index*getType().getBytes());
		} catch (IOException e) {
			throw new RuntimeException("Cannot get int value from reader/writer!",e);
		}
	}
	
	

}
