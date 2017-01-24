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

package gedi.core.region;

import gedi.app.extension.CapabilitiesExtensionPoint;
import gedi.app.extension.DefaultExtensionPoint;
import gedi.app.extension.ExtensionContext;
import gedi.app.extension.ExtensionPoint;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.FileUtils;
import gedi.util.StringUtils;
import gedi.util.mutable.MutableLong;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.compress.compressors.FileNameUtil;

public class GenomicRegionStorageExtensionPoint extends CapabilitiesExtensionPoint<GenomicRegionStorageCapabilities,GenomicRegionStorage> {

	protected GenomicRegionStorageExtensionPoint() {
		super(GenomicRegionStorage.class);
	}


	private static final Logger log = Logger.getLogger( GenomicRegionStorageExtensionPoint.class.getName() );

	private static GenomicRegionStorageExtensionPoint instance;

	public static GenomicRegionStorageExtensionPoint getInstance() {
		if (instance==null) 
			instance = new GenomicRegionStorageExtensionPoint();
		return instance;
	}

	public <T> GenomicRegionStorage<T> get(Class<T> cls, String path,
			GenomicRegionStorageCapabilities... caps) {
		return get(new ExtensionContext().add(Class.class, cls).add(String.class, path),caps);
	}

	public <T> GenomicRegionStorage<T> get(String path,
			GenomicRegionStorageCapabilities... caps) {
		return get(new ExtensionContext().add(String.class, path),caps);
	}


}
