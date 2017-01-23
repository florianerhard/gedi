package gedi.core.region.feature.index;

import java.io.IOException;
import java.util.Set;

import gedi.core.data.numeric.diskrmq.DiskGenomicNumericBuilder;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.features.AbstractFeature;

public class WriteCoverageRmq extends AbstractFeature<Void> {

	private String file;
	
	public WriteCoverageRmq(String file) {
		this.file = file;
	}

	@Override
	public GenomicRegionFeature<Void> copy() {
		WriteCoverageRmq re = new WriteCoverageRmq(file);
		re.copyProperties(this);
		return this;
	}

	private DiskGenomicNumericBuilder bui;
	
	@Override
	public void begin() {
		if (program.getThreads()>1) throw new RuntimeException("Can only be run with 1 thread!");
		try {
			bui = new DiskGenomicNumericBuilder(file);
		} catch (IOException e) {
			throw new RuntimeException("Cannot write file!",e);
		}
	}
	
	@Override
	protected void accept_internal(Set<Void> t) {
		bui.addCoverageEx((MutableReferenceGenomicRegion) referenceRegion);
	}
	
	@Override
	public void end() {
		try {
			bui.build(true);
		} catch (IOException e) {
			throw new RuntimeException("Cannot write file!",e);
		}
	}

}
