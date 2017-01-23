package gedi.startup;

import gedi.app.Startup;
import gedi.app.classpath.ClassPath;
import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorageLoader;
import gedi.core.data.table.CsvTableLoader;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.GenomicRegionStorageCapabilities;
import gedi.core.region.GenomicRegionStorageExtensionPoint;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.core.workspace.loader.WorkspaceItemLoaderExtensionPoint;

public class CitStartup implements Startup {

	@Override
	public void accept(ClassPath t) {
		
		WorkspaceItemLoaderExtensionPoint.getInstance().addExtension(CenteredDiskIntervalTreeStorageLoader.class,"cit");
		
		
		GenomicRegionStorageExtensionPoint.getInstance().addExtension(CenteredDiskIntervalTreeStorage.class,
				GenomicRegionStorageCapabilities.Disk,
				GenomicRegionStorageCapabilities.Fill
				);
		
	}
	
	
}
