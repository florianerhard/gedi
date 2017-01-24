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

package gedi.region.bam;

import gedi.core.region.GenomicRegionStorage;
import gedi.core.workspace.loader.WorkspaceItemLoader;
import gedi.util.io.text.LineOrientedFile;

import java.io.IOException;
import java.nio.file.Path;

public class BamGenomicRegionStorageLoader implements WorkspaceItemLoader<GenomicRegionStorage> {

	public static String[] extensions = {"bam","bamlist"};
	
	@Override
	public String[] getExtensions() {
		return extensions;
	}

	@Override
	public GenomicRegionStorage load(Path path) throws IOException {
		String p = path.toString();
		if (p.endsWith(".bam"))
			return new BamGenomicRegionStorage(p);
		else if (p.endsWith(".bamlist")) 
			return new BamGenomicRegionStorage(new LineOrientedFile(p).readAllLines("#"));
		throw new RuntimeException("Unknown file type: "+p);
	}

	@Override
	public Class<GenomicRegionStorage> getItemClass() {
		return GenomicRegionStorage.class;
	}

	@Override
	public boolean hasOptions() {
		return false;
	}

	@Override
	public void updateOptions(Path path) {
		
	}

}
