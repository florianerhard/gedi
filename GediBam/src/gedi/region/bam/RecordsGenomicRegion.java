/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */

package gedi.region.bam;

import gedi.core.region.ArrayGenomicRegion;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import htsjdk.samtools.SAMRecord;

import java.util.ArrayList;

public class RecordsGenomicRegion extends ArrayGenomicRegion {

	private SAMRecord[][] records;
	private ArrayList<SAMRecord> build = new ArrayList<SAMRecord>();
	
	public RecordsGenomicRegion(IntArrayList coords, SAMRecord r) {
		super(coords);
		build.add(r);
	}

	public RecordsGenomicRegion merge(RecordsGenomicRegion b) {
		if (compareTo(b)!=0) throw new RuntimeException("Cannot merge distinct regions!");
		build.addAll(b.build);
		return this;
	}
	
	public RecordsGenomicRegion join(RecordsGenomicRegion[] a) {
		records = new SAMRecord[a.length][];
		for (int i=0; i<a.length; i++)
			if (a[i]!=null) {
				if (compareTo(a[i])!=0) throw new RuntimeException("Cannot join distinct regions!");
				records[i] = a[i].build.toArray(new SAMRecord[0]);
			} else
				records[i] = new SAMRecord[0];
		return this;
	}

	public SAMRecord[][] getRecords() {
		return records;
	}
	
}
