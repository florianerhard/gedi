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
