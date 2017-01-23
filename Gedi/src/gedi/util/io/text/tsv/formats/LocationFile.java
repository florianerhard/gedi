package gedi.util.io.text.tsv.formats;

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;

import java.io.IOException;

public class LocationFile extends MemoryIntervalTreeStorage<NameAnnotation> {

	public LocationFile(String file) throws IOException {
		super(NameAnnotation.class);
		new LocationFileReader(file).readIntoMemoryTakeFirst(this);
	}
	
}
