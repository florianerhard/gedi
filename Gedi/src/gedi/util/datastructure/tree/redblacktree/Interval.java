package gedi.util.datastructure.tree.redblacktree;

import java.io.IOException;
import java.util.Iterator;

import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionPart;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

public interface Interval {
	
	int getStart();
	int getStop();

	
	default int getEnd() {
		return getStop()+1;
	}
	
	
	
	default GenomicRegion asRegion() {
		return new GenomicRegion() {
			@Override
			public int getNumParts() {
				return 1;
			}

			@Override
			public int getStart(int part) {
				return Interval.this.getStart();
			}

			@Override
			public int getEnd(int part) {
				return Interval.this.getEnd();
			}
			
		};
	}
}
