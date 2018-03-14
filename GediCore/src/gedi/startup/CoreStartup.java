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
package gedi.startup;

import java.lang.reflect.Modifier;

import gedi.app.Startup;
import gedi.app.classpath.ClassPath;
import gedi.app.classpath.ClassPathCache;
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericLoader;
import gedi.core.data.table.CsvTableLoader;
import gedi.core.region.GenomicRegionStorageCapabilities;
import gedi.core.region.GenomicRegionStorageExtensionPoint;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.core.workspace.loader.WorkspaceItemLoaderExtensionPoint;
import gedi.util.oml.OmlLoader;
import gedi.util.ReflectionUtils;
import gedi.util.io.text.jhp.TemplateGenerator;
import gedi.util.io.text.jhp.display.DisplayTemplateGeneratorExtensionPoint;
import gedi.util.io.text.tsv.formats.BedFileLoader;
import gedi.util.io.text.tsv.formats.GediViewFileLoader;
import gedi.util.io.text.tsv.formats.LocationsFileLoader;
import gedi.util.job.pipeline.ClusterPipelineRunner;
import gedi.util.job.pipeline.ParallelPipelineRunner;
import gedi.util.job.pipeline.PipelineRunnerExtensionPoint;
import gedi.util.job.pipeline.SerialPipelineRunner;

public class CoreStartup implements Startup {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void accept(ClassPath t) {
		
		for (String ext : CsvTableLoader.extensions)
			WorkspaceItemLoaderExtensionPoint.getInstance().addExtension(CsvTableLoader.class,ext);
		
		for (String ext : LocationsFileLoader.extensions)
			WorkspaceItemLoaderExtensionPoint.getInstance().addExtension(LocationsFileLoader.class,ext);
		
		for (String ext : BedFileLoader.extensions)
			WorkspaceItemLoaderExtensionPoint.getInstance().addExtension(BedFileLoader.class,ext);
		
		for (String ext : GediViewFileLoader.extensions)
			WorkspaceItemLoaderExtensionPoint.getInstance().addExtension(GediViewFileLoader.class,ext);
		
		for (String ext : DiskGenomicNumericLoader.extensions)
			WorkspaceItemLoaderExtensionPoint.getInstance().addExtension(DiskGenomicNumericLoader.class,ext);
		
		
		GenomicRegionStorageExtensionPoint.getInstance().addExtension(MemoryIntervalTreeStorage.class,
				GenomicRegionStorageCapabilities.Memory,
				GenomicRegionStorageCapabilities.Add,
				GenomicRegionStorageCapabilities.Fill
				);
		
		PipelineRunnerExtensionPoint.getInstance().addExtension(SerialPipelineRunner.class, SerialPipelineRunner.name);
		PipelineRunnerExtensionPoint.getInstance().addExtension(ParallelPipelineRunner.class, ParallelPipelineRunner.name);
		PipelineRunnerExtensionPoint.getInstance().addExtension(ClusterPipelineRunner.class, ClusterPipelineRunner.name);
		
		
		
		WorkspaceItemLoaderExtensionPoint.getInstance().addExtension(OmlLoader.class,"oml");
		WorkspaceItemLoaderExtensionPoint.getInstance().addExtension(OmlLoader.class,"oml.jhp");
		
		
		for (String cls : ClassPathCache.getInstance().getClassesOfPackage("gedi.util.io.text.jhp.display")) {
			try {
				Class c = (Class)Class.forName("gedi.util.io.text.jhp.display."+cls);
				if (TemplateGenerator.class.isAssignableFrom(c) 
						&& !c.isInterface() 
						&& !Modifier.isAbstract(c.getModifiers())
						&& ReflectionUtils.hasStatic(c, "cls"))
					DisplayTemplateGeneratorExtensionPoint.getInstance().addExtension((Class)c, (Class) ReflectionUtils.getStatic(c, "cls"));
			} catch (Exception e) {
				throw new RuntimeException("Could not load class "+cls,e);
			}
		}
	}
	
	
}
