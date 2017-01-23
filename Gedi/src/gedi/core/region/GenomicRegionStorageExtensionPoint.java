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
