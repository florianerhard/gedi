package gedi.centeredDiskIntervalTree;

import java.io.IOException;
import java.nio.file.Path;

import gedi.core.region.GenomicRegionStorage;
import gedi.core.workspace.loader.WorkspaceItemLoader;

public class CenteredDiskIntervalTreeStorageLoader implements WorkspaceItemLoader<GenomicRegionStorage> {

	private static String[] extensions = {"cit"};
	
	@Override
	public String[] getExtensions() {
		return extensions;
	}

	@Override
	public GenomicRegionStorage load(Path path) throws IOException {
		return new CenteredDiskIntervalTreeStorage(path.toString());
	}

	@Override
	public Class<GenomicRegionStorage> getItemClass() {
		return GenomicRegionStorage.class;
	}

	@Override
	public boolean hasOptions() {
		return false;
	}

	@Override
	public void updateOptions(Path path) {
		
	}

}
