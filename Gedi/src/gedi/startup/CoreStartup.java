package gedi.startup;

import gedi.app.Startup;
import gedi.app.classpath.ClassPath;
import gedi.core.data.table.CsvTableLoader;
import gedi.core.region.GenomicRegionStorageCapabilities;
import gedi.core.region.GenomicRegionStorageExtensionPoint;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.core.workspace.loader.WorkspaceItemLoaderExtensionPoint;
import gedi.util.io.text.tsv.formats.LocationsFileLoader;
import gedi.util.job.pipeline.ClusterPipelineRunner;
import gedi.util.job.pipeline.ParallelPipelineRunner;
import gedi.util.job.pipeline.PipelineRunner;
import gedi.util.job.pipeline.PipelineRunnerExtensionPoint;
import gedi.util.job.pipeline.SerialPipelineRunner;

public class CoreStartup implements Startup {

	@Override
	public void accept(ClassPath t) {
		
		for (String ext : CsvTableLoader.extensions)
			WorkspaceItemLoaderExtensionPoint.getInstance().addExtension(CsvTableLoader.class,ext);
		
		for (String ext : LocationsFileLoader.extensions)
			WorkspaceItemLoaderExtensionPoint.getInstance().addExtension(LocationsFileLoader.class,ext);
		
		
		
		GenomicRegionStorageExtensionPoint.getInstance().addExtension(MemoryIntervalTreeStorage.class,
				GenomicRegionStorageCapabilities.Memory,
				GenomicRegionStorageCapabilities.Add,
				GenomicRegionStorageCapabilities.Fill
				);
		
		PipelineRunnerExtensionPoint.getInstance().addExtension(SerialPipelineRunner.class, SerialPipelineRunner.name);
		PipelineRunnerExtensionPoint.getInstance().addExtension(ParallelPipelineRunner.class, ParallelPipelineRunner.name);
		PipelineRunnerExtensionPoint.getInstance().addExtension(ClusterPipelineRunner.class, ClusterPipelineRunner.name);
		
		
	}
	
	
}
