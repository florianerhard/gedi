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
package gedi.bam.tools;

import gedi.core.reference.Chromosome;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.util.ArrayUtils;
import gedi.util.FunctorUtils;
import gedi.util.io.randomaccess.arrays.AmbiguityGenomicRegionArray;
import gedi.util.io.randomaccess.arrays.GenomicRegionArray;
import gedi.util.io.randomaccess.diskarray.DiskArray;
import gedi.util.io.randomaccess.diskarray.VariableSizeDiskArrayBuilder;
import gedi.util.io.randomaccess.serialization.BinarySerializableSerializer;
import gedi.util.mutable.MutableInteger;
import gedi.util.sequence.MismatchString;

import java.io.IOException;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;

public class SamIndexAmbiguousReads implements UnaryOperator<Iterator<SAMRecord>> {

	private VariableSizeDiskArrayBuilder<AmbiguityGenomicRegionArray> builder;
	private int minLength;
	
	
	public SamIndexAmbiguousReads(String path) throws IOException {
		this(path,18);
	}
	public SamIndexAmbiguousReads(String path, int minLength) throws IOException {
		builder = new VariableSizeDiskArrayBuilder<AmbiguityGenomicRegionArray>(path);
		this.minLength = minLength;
	}
	
	public Iterator<SAMRecord> apply(Iterator<SAMRecord> it) {
		Iterator<SAMRecord[]> ait = FunctorUtils.multiplexIterator(it, new SamRecordNameComparator(), SAMRecord.class);
		Iterator<SAMRecord[]> sit = FunctorUtils.mappedIterator(ait,a->{
			TreeSet<MutableReferenceGenomicRegion<Float>> set = new TreeSet<MutableReferenceGenomicRegion<Float>>();
			for (SAMRecord s : a) {
				if (s.getReadPairedFlag()) throw new RuntimeException("Not implemented for paired-end data!");
				set.add(BamUtils.getReferenceGenomicRegion(s, 0f));
			}
			
			
			if (set.size()>1 && set.iterator().next().getRegion().getTotalLength()>=minLength) {
				for (MutableReferenceGenomicRegion<Float> r : set) {
					r.setData(1f/set.size());
				}
				try {
					builder.add(new AmbiguityGenomicRegionArray(set.toArray(new MutableReferenceGenomicRegion[0])));
				} catch (Exception e) {
					throw new RuntimeException("Could not write ambiguity index!",e);
				}
			}
			
			return a;
		});
	
		return FunctorUtils.demultiplexIterator(sit, a->FunctorUtils.arrayIterator(a)).hasNextAction((hasNext)->{
			if (!hasNext)
				try {
					builder.finish();
				} catch (Exception e) {
					throw new RuntimeException("Could not write ambiguity index!",e);
				}
			return hasNext;
		});
	}
	
	
}