package gedi.core.processing;

import gedi.app.extension.DefaultExtensionPoint;

import java.util.logging.Logger;

public class GenomicRegionProcessorExtensionPoint extends DefaultExtensionPoint<String, GenomicRegionProcessor> {


	private static GenomicRegionProcessorExtensionPoint instance;

	public static GenomicRegionProcessorExtensionPoint getInstance() {
		if (instance==null) 
			instance = new GenomicRegionProcessorExtensionPoint(GenomicRegionProcessor.class);
		return instance;
	}

	protected GenomicRegionProcessorExtensionPoint(
			Class<GenomicRegionProcessor> cls) {
		super(cls);
	}



}
