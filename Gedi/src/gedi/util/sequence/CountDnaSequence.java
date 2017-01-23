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
