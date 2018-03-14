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
import gedi.core.region.GenomicRegionStorage;
import gedi.riboseq.analysis.PriceAnalysis;
import gedi.riboseq.analysis.PriceAnalysisExtensionPoint;
import gedi.riboseq.visu.PriceOrfGenomicRegionStorageDisplayTemplateGenerator;
import gedi.util.ReflectionUtils;
import gedi.util.io.text.jhp.TemplateGenerator;
import gedi.util.io.text.jhp.display.DisplayTemplateGeneratorExtensionPoint;

public class PriceStartUp implements Startup {

	@Override
	public void accept(ClassPath t) {
		DisplayTemplateGeneratorExtensionPoint.getInstance().addExtension((Class)PriceOrfGenomicRegionStorageDisplayTemplateGenerator.class, (Class)PriceOrfGenomicRegionStorageDisplayTemplateGenerator.cls);

		
		for (String cls : ClassPathCache.getInstance().getClassesOfPackage("gedi.riboseq.analysis")) {
			try {
				Class c = (Class)Class.forName("gedi.riboseq.analysis."+cls);
				if (PriceAnalysis.class.isAssignableFrom(c) 
						&& !c.isInterface() 
						&& !Modifier.isAbstract(c.getModifiers())
						&& ReflectionUtils.hasStatic(c, "name"))
					PriceAnalysisExtensionPoint.getInstance().addExtension((Class)c, (String) ReflectionUtils.getStatic(c, "name"));
			} catch (Exception e) {
				throw new RuntimeException("Could not load class "+cls,e);
			}
		}
	}

	
	
	
}
