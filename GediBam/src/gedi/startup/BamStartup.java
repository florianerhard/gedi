package gedi.startup;

import gedi.app.Startup;
import gedi.app.classpath.ClassPath;
import gedi.core.region.GenomicRegionStorageCapabilities;
import gedi.core.region.GenomicRegionStorageExtensionPoint;
import gedi.core.workspace.loader.WorkspaceItemLoaderExtensionPoint;
import gedi.region.bam.BamGenomicRegionStorage;
import gedi.region.bam.BamGenomicRegionStorageLoader;

public class BamStartup implements Startup {

	@Override
	public void accept(ClassPath t) {
		for (String k : BamGenomicRegionStorageLoader.extensions)
			WorkspaceItemLoaderExtensionPoint.getInstance().addExtension(BamGenomicRegionStorageLoader.class,k);
		
		
		GenomicRegionStorageExtensionPoint.getInstance().addExtension(BamGenomicRegionStorage.class,
				GenomicRegionStorageCapabilities.Disk
				);
		
	}

}
