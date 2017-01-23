package gedi.util.datastructure.array;

import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;

import java.io.IOException;

public class DiskShortArray extends ShortArray {
	
	private long offset;
	private int length;
	private BinaryReader reader;
	private BinaryWriter writer;
	
	public DiskShortArray() {
	}
	
	public DiskShortArray(BinaryReader in, BinaryWriter out, long offset, int length) {
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
	public void setShort(int index, short value) {
		try {
			writer.putShort(offset+index*getType().getBytes(),value);
		} catch (IOException e) {
			throw new RuntimeException("Cannot set double value in reader/writer!",e);
		}
	}

	@Override
	public boolean isReadOnly() {
		return writer==null;
	}
	

	@Override
	public NumericArray clear() {
		for (int i=0; i<length(); i++)
			setShort(i, (short)0);
		return this;
	}

	@Override
	public int length() {
		return length;
	}

	@Override
	public short getShort(int index) {
		try {
			return reader.getShort(offset+index*getType().getBytes());
		} catch (IOException e) {
			throw new RuntimeException("Cannot get double value from reader/writer!",e);
		}
	}
	
	

}
