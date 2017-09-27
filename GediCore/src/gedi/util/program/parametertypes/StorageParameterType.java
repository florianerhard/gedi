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

package gedi.util.program.parametertypes;


import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.workspace.loader.WorkspaceItemLoaderExtensionPoint;

public class StorageParameterType<T> implements GediParameterType<GenomicRegionStorage<T>> {

	@Override
	public GenomicRegionStorage<T> parse(String s) {
		try {
			Path p = Paths.get(s);
			return (GenomicRegionStorage<T>) WorkspaceItemLoaderExtensionPoint.getInstance().get(p).load(p);
		} catch (Exception e) {
			throw new RuntimeException("Could not load storage!",e);
		}
	}

	@Override
	public Class<GenomicRegionStorage<T>> getType() {
		return (Class)GenomicRegionStorage.class;
	}
	

}
