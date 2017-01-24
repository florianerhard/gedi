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
