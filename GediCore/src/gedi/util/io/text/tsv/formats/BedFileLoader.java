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
import gedi.core.data.annotation.ScoreNameAnnotation;
import gedi.core.region.GenomicRegionStoragePreload;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.core.workspace.loader.WorkspaceItemLoader;
import gedi.util.dynamic.DynamicObject;

import java.io.IOException;
import java.nio.file.Path;

public class BedFileLoader implements WorkspaceItemLoader<MemoryIntervalTreeStorage<ScoreNameAnnotation>,GenomicRegionStoragePreload> {

	public static final String[] extensions = new String[]{"bed","bed.gz"};
	
	@Override
	public String[] getExtensions() {
		return extensions;
	}

	@Override
	public MemoryIntervalTreeStorage<ScoreNameAnnotation> load(Path path)
			throws IOException {
		return new ScoreNameBedFileReader(path.toString()).readIntoMemoryThrowOnNonUnique();
	}
	
	@Override
	public GenomicRegionStoragePreload preload(Path path) throws IOException {
		return new GenomicRegionStoragePreload(ScoreNameAnnotation.class, new ScoreNameAnnotation("", 0),DynamicObject.getEmpty());
	}


	@Override
	public Class<MemoryIntervalTreeStorage<ScoreNameAnnotation>> getItemClass() {
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
