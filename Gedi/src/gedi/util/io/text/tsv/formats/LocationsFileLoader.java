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

package gedi.util.io.text.tsv.formats;

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.core.workspace.loader.WorkspaceItemLoader;

import java.io.IOException;
import java.nio.file.Path;

public class LocationsFileLoader implements WorkspaceItemLoader<MemoryIntervalTreeStorage<NameAnnotation>> {

	public static final String[] extensions = new String[]{"locations"};
	
	@Override
	public String[] getExtensions() {
		return extensions;
	}

	@Override
	public MemoryIntervalTreeStorage<NameAnnotation> load(Path path)
			throws IOException {
		return new LocationFileReader(path.toString()).readIntoMemoryThrowOnNonUnique();
	}

	@Override
	public Class<MemoryIntervalTreeStorage<NameAnnotation>> getItemClass() {
		return (Class)MemoryIntervalTreeStorage.class;
	}

	@Override
	public boolean hasOptions() {
		return false;
	}

	@Override
	public void updateOptions(Path path) {
	}

}
