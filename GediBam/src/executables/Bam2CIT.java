package executables;

import gedi.app.Gedi;
import gedi.app.extension.ExtensionContext;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.GenomicRegionStorageCapabilities;
import gedi.core.region.GenomicRegionStorageExtensionPoint;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.region.bam.BamGenomicRegionStorage;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Bam2CIT {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException {
		if (args.length<2) {
			usage();
			System.exit(1);
		}
		
		boolean progress = false;
		boolean keepIds = false;
		String out = null;
		
		for (int i=0; i<args.length; i++) {
			if (args[i].equals("-p"))
				progress = true;
			else if (args[i].equals("-id"))
				keepIds = true;
			else {
				out = args[i++];
				args = Arrays.copyOfRange(args, i, args.length);
				i = args.length;
			}
		}
		if (out==null) {
			usage();
			System.exit(1);
		}
		
		EI.wrap(args).map(File::new).throwArg(File::exists,"File %s does not exist!");
		
		BamGenomicRegionStorage storage = new BamGenomicRegionStorage(args);
		storage.setKeepReadNames(keepIds);
		ExtendedIterator<ImmutableReferenceGenomicRegion<AlignedReadsData>> it = storage.ei();
		if (progress) it = it.progress();
		
		Gedi.startup(false);
		@SuppressWarnings("rawtypes")
		GenomicRegionStorage outStorage = GenomicRegionStorageExtensionPoint.getInstance().get(new ExtensionContext().add(String.class, out).add(Class.class, DefaultAlignedReadsData.class), GenomicRegionStorageCapabilities.Disk, GenomicRegionStorageCapabilities.Fill);
		outStorage.fill(it);
		
		
	}

	private static void usage() {
		System.out.println("Bam2CIT [-p] <output> <file1> <file2> ... \n\n -p shows progress");
	}
	
}
