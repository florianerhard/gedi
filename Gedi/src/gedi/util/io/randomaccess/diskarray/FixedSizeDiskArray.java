package gedi.util.io.randomaccess.diskarray;

import gedi.util.io.randomaccess.FixedSizeBinarySerializable;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileView;

import java.io.IOException;
import java.util.Comparator;
import java.util.function.Supplier;

public class FixedSizeDiskArray<T extends FixedSizeBinarySerializable> extends DiskArray<T> {

	private long len;
	private int fixedSize = -1;
	
	
	public FixedSizeDiskArray(String path) throws IOException {
		this(path,null);
	}
	
	public FixedSizeDiskArray(String path, Supplier<T> supplier) throws IOException {
		super(path,supplier);
	}

	public long indexToOffset(long index) throws IOException {
		if (fixedSize==-1) throw new RuntimeException("Size unclear!");
		return index*fixedSize;
	}

	public long length() {
		return len;
	}
	
	@Override
	public T get(T proto, long index) throws IOException {
		if (fixedSize==-1) {
			fixedSize = proto.getFixedSize();
			len = data.size()/fixedSize;
		}
		return super.get(proto, index);
	}

	@Override
	protected PageFileView createData(String path) throws IOException {
		return new PageFileView(new PageFile(path));
	}


}
