package executables;

import gedi.app.Gedi;
import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericBuilder;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.AlignedReadsDataFactory;
import gedi.core.data.reads.filter.MaxMultiplicityFilter;
import gedi.core.reference.Chromosome;
import gedi.core.region.GenomicRegionPosition;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.GenomicRegionStorageExtensionPoint;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.Progress;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class CreateCoverageRMQ {

	
	public static void main(String[] args) throws IOException {
	
		if (args.length!=2 || !new File(args[0]).exists()) {
			System.err.println("CreateCoverageRMQ <cit> <rmq>");
			System.exit(1);
		}
		
		
		int maxMulti = 1;
		
		Gedi.startup();	

		
		Progress progress = new ConsoleProgress();
		
		DiskGenomicNumericBuilder build = new DiskGenomicNumericBuilder(args[1]);
		build.setReferenceSorted(true);
		
		CenteredDiskIntervalTreeStorage<AlignedReadsData> storage = new CenteredDiskIntervalTreeStorage<AlignedReadsData>(args[0]);
		progress.init();
		
		UnaryOperator<ReferenceGenomicRegion<AlignedReadsData>> filter = new MaxMultiplicityFilter(maxMulti);
		
		storage.iterateMutableReferenceGenomicRegions().forEachRemaining(mrgr->{
			progress.setDescription(mrgr.getReference()+":"+mrgr.getRegion()).incrementProgress();
			try {
				ReferenceGenomicRegion<AlignedReadsData> data = filter.apply(mrgr);
				if (data!=null)
					build.addCoverage(mrgr.getReference(),mrgr.getRegion(),data.getData());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}				
			
		});
		progress.setDescription("Building...");
		
		build.build(true);
		
		progress.finish();
	}
}
