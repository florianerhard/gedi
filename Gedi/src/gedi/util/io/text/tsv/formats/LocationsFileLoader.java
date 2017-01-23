package gedi.util.io.text.tsv.formats;

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.core.workspace.loader.WorkspaceItemLoader;

import java.io.IOException;
import java.nio.file.Path;

public class LocationsFileLoader implements WorkspaceItemLoader<MemoryIntervalTreeStorage<NameAnnotation>> {

	public static final String[] extensions = new String[]{"locations"};
	
	@Override
	public String[] getExtensions() {
		return extensions;
	}

	@Override
	public MemoryIntervalTreeStorage<NameAnnotation> load(Path path)
			throws IOException {
		return new LocationFileReader(path.toString()).readIntoMemoryThrowOnNonUnique();
	}

	@Override
	public Class<MemoryIntervalTreeStorage<NameAnnotation>> getItemClass() {
		return (Class)MemoryIntervalTreeStorage.class;
	}

	@Override
	public boolean hasOptions() {
		return false;
	}

	@Override
	public void updateOptions(Path path) {
	}

}
