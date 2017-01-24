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

import gedi.util.FileUtils;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;


public class VariableSizeDiskArrayBuilder<T extends BinarySerializable> {

	
	private PageFileWriter data;
	private PageFileWriter index;
	private long currentLength=0;
	private String path;
	
	
	public VariableSizeDiskArrayBuilder(String path) throws IOException {
		this.path = path;
		data = new PageFileWriter(File.createTempFile(FileUtils.getFullNameWithoutExtension(new File(path)), ".data").getAbsolutePath());
		index = new PageFileWriter(File.createTempFile(FileUtils.getFullNameWithoutExtension(new File(path)), ".index").getAbsolutePath());
	}
	
	
	public VariableSizeDiskArrayBuilder<T> add(T data) throws IOException {
		currentLength++;
		index.putLong(this.data.position());
		data.serialize(this.data);
		return this;
	}
	
	public long getCurrentLength() {
		return currentLength;
	}
	
	public VariableSizeDiskArrayBuilder<T> add(Collection<T> data) throws IOException {
		currentLength++;
		index.putLong(this.data.position());
		for (T d : data)
			d.serialize(this.data);
		return this;
	}
	
	
	public void finish() throws IOException {
		data.close();
		index.close();
		
		PageFileWriter file = new PageFileWriter(path);
		
		long offset = currentLength*Long.BYTES;
		PageFile index = new PageFile(this.index.getPath());
		for (int i=0; i<currentLength; i++) {
			file.putLong(index.getLong()+offset);
		}
		index.close();
		
		PageFile data = new PageFile(this.data.getPath());
		while (!data.eof()) 
			file.put(data.get());
		data.close();
		file.close();
		
		new File(data.getPath()).delete();
		new File(index.getPath()).delete();
	}
	
	
}
