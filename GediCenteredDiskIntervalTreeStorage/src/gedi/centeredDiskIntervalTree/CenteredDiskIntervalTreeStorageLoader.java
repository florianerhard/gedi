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

package gedi.centeredDiskIntervalTree;

import java.io.IOException;
import java.nio.file.Path;

import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.GenomicRegionStoragePreload;
import gedi.core.workspace.loader.WorkspaceItemLoader;
import gedi.util.dynamic.DynamicObject;

public class CenteredDiskIntervalTreeStorageLoader implements WorkspaceItemLoader<CenteredDiskIntervalTreeStorage,GenomicRegionStoragePreload> {

	private static String[] extensions = {"cit"};
	
	@Override
	public String[] getExtensions() {
		return extensions;
	}

	@Override
	public CenteredDiskIntervalTreeStorage<?> load(Path path) throws IOException {
		return new CenteredDiskIntervalTreeStorage(path.toString());
	}

	@Override
	public Class<CenteredDiskIntervalTreeStorage> getItemClass() {
		return CenteredDiskIntervalTreeStorage.class;
	}
	
	@Override
	public GenomicRegionStoragePreload preload(Path path) throws IOException {
		CenteredDiskIntervalTreeStorage cit = new CenteredDiskIntervalTreeStorage(path.toString());
		Class<?> cls = cit.getType();
		Object rec = cit.getRandomRecord();
		DynamicObject meta = cit.getMetaData();
		cit.close();
		return new GenomicRegionStoragePreload(cls, rec, meta);
	}

	@Override
	public boolean hasOptions() {
		return false;
	}

	@Override
	public void updateOptions(Path path) {
		
	}

}
