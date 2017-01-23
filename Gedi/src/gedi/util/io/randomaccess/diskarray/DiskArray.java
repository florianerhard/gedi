package gedi.util.io.randomaccess.diskarray;

import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileView;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Comparator;
import java.util.function.Supplier;

public abstract class DiskArray<T extends BinarySerializable> implements Iterable<T> {

	protected RandomAccessFile file;
	protected PageFileView data;
	protected Supplier<T> supplier;
	
	public DiskArray(String path) throws IOException {
		this(path,null);
	}
	
	public DiskArray(String path, Supplier<T> supplier) throws IOException {
		file = new RandomAccessFile(new File(path),"r");
		data = createData(path);
		this.supplier = supplier;
	}

	protected abstract PageFileView createData(String path) throws IOException;
	public abstract long indexToOffset(long index) throws IOException;
	public abstract long length();

	public void close() throws IOException {
		file.close();
	}
	
	public Supplier<T> getSupplier() {
		return supplier;
	}
	
	public DiskArray<T> setSupplier(Supplier<T> supplier) {
		this.supplier = supplier;
		return this;
	}

	public T get(T proto, long index) throws IOException {
		long offset = indexToOffset(index);
		data.position(offset-data.getStart());
		proto.deserialize(data);
		return proto;
	}
	
	public T get(long index) throws IOException {
		return get(getSupplier().get(),index);
	}
	
	public T get(Supplier<T> supp, long index) throws IOException {
		return get(supp.get(),index);
	}
	
	public T[] load(Class<T> cls) {
		if (length()>Integer.MAX_VALUE) throw new RuntimeException("Cannot load to memory, too many entries!");
		return iterator().toArray(cls);
	}
	
	public ExtendedIterator<T> iterator() {
		return new ExtendedIterator<T>() {

			long index = 0;
			@Override
			public boolean hasNext() {
				return index<length();
			}

			@Override
			public T next() {
				try {
					return get(index++);
				} catch (IOException e) {
					throw new RuntimeException("Cannot iterate disk array elements!",e);
				}
			}
			
		};
	}
	

}
