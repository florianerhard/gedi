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
