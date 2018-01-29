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

package gedi.core.data.numeric.diskrmq;

import java.io.IOException;
import java.nio.file.Path;

import gedi.core.workspace.loader.WorkspaceItemLoader;

public class DiskGenomicNumericLoader implements WorkspaceItemLoader<DiskGenomicNumericProvider,Void> {

	public static String[] extensions = {"rmq"};
	
	@Override
	public String[] getExtensions() {
		return extensions;
	}

	@Override
	public DiskGenomicNumericProvider load(Path path) throws IOException {
		return new DiskGenomicNumericProvider(path.toString());
	}

	@Override
	public Class<DiskGenomicNumericProvider> getItemClass() {
		return DiskGenomicNumericProvider.class;
	}
	
	@Override
	public Void preload(Path path) throws IOException {
		return null;
	}

	@Override
	public boolean hasOptions() {
		return false;
	}

	@Override
	public void updateOptions(Path path) {
		
	}

}
