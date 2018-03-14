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
package gedi.util.sequence;


import java.io.IOException;

import gedi.util.FileUtils;
import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import cern.colt.bitvector.BitVector;


public class CountDnaSequence extends DnaSequence implements BinarySerializable {


	private int count;

	public CountDnaSequence(CharSequence sequence, int count) {
		super(sequence);
		this.count = count;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + count;
		return result;
	}
	
	public int getCount() {
		return count;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		CountDnaSequence other = (CountDnaSequence) obj;
		if (count != other.count)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return super.toString()+"\t"+count;
	}

	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putInt(count);
		FileUtils.writeBitVector(this,out);
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		count = in.getInt();
		FileUtils.readBitVector(this,in);
	}
	
	
}
