package gedi.region.bam;

import gedi.core.region.GenomicRegionStorage;
import gedi.core.workspace.loader.WorkspaceItemLoader;
import gedi.util.io.text.LineOrientedFile;

import java.io.IOException;
import java.nio.file.Path;

public class BamGenomicRegionStorageLoader implements WorkspaceItemLoader<GenomicRegionStorage> {

	public static String[] extensions = {"bam","bamlist"};
	
	@Override
	public String[] getExtensions() {
		return extensions;
	}

	@Override
	public GenomicRegionStorage load(Path path) throws IOException {
		String p = path.toString();
		if (p.endsWith(".bam"))
			return new BamGenomicRegionStorage(p);
		else if (p.endsWith(".bamlist")) 
			return new BamGenomicRegionStorage(new LineOrientedFile(p).readAllLines("#"));
		throw new RuntimeException("Unknown file type: "+p);
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
