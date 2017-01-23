package gedi.util.io.randomaccess.diskarray;

import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

import java.io.IOException;


public class FixedSizeDiskArrayBuilder<T extends BinarySerializable> {

	
	private PageFileWriter file;
	
	public FixedSizeDiskArrayBuilder(String path) throws IOException {
		file = new PageFileWriter(path);
	}
	
	
	public FixedSizeDiskArrayBuilder<T> add(T data) throws IOException {
		data.serialize(file);
		return this;
	}
	
	
	public void finish() throws IOException {
		file.close();
	}
	
	
}
