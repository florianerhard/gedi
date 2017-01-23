package gedi.util.io.randomaccess.diskarray;

import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileView;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * Format: n longs for pointers to data region, then dataregion
 * @author erhard
 *
 */
public class VariableSizeDiskArray<T extends BinarySerializable> extends DiskArray<T> {


	private long len;
	private PageFileView index;
	
	public VariableSizeDiskArray(String path) throws IOException {
		this(path,null);
	}
	
	public VariableSizeDiskArray(String path, Supplier<T> supplier) throws IOException {
		super(path,supplier);
		file = new RandomAccessFile(new File(path),"r");
		this.supplier = supplier;
	}


	public long indexToOffset(long index) throws IOException {
		return this.index.getLong(index*Long.BYTES);
	}

	public long length() {
		return len/Long.BYTES;
	}

	@Override
	protected PageFileView createData(String path) throws IOException {
		len = file.readLong();
		index = new PageFileView(new PageFile(path),0,len);
		return new PageFileView(new PageFile(path),len,file.length());
	}

	public <C extends Collection<? super T>> C getCollection(long index, C re) throws IOException {
		long nextOffset;
		if (index==length()-1) nextOffset = this.data.getEnd();
		else nextOffset = indexToOffset(index+1);
		
		long offset = indexToOffset(index);
		data.position(offset-data.getStart());
		while (data.position()<nextOffset-data.getStart()) {
			T r = supplier.get();
			r.deserialize(data);
			re.add(r);
		}
		
		return re;
	}
	
}
