package gedi.startup;

import gedi.app.Startup;
import gedi.app.classpath.ClassPath;
import gedi.core.processing.GenomicRegionProcessorExtensionPoint;
import gedi.lfc.LfcAlignedReadsProcessor;

public class GediLfcStartup implements Startup {

	@Override
	public void accept(ClassPath t) {
		GenomicRegionProcessorExtensionPoint.getInstance().addExtension(LfcAlignedReadsProcessor.class,"LFC");
	}

}
