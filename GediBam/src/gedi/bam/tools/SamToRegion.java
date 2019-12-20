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

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.AlignedReadsDataFactory;
import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.reference.Chromosome;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.region.bam.FactoryGenomicRegion;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import htsjdk.samtools.SAMFileReader;
import htsjdk.samtools.SAMRecord;

import java.io.File;
import java.util.function.BiFunction;
import java.util.function.Function;

public class SamToRegion<O> implements Function<SAMRecord,ReferenceGenomicRegion<O>>{

	
	public static ExtendedIterator<ReferenceGenomicRegion<AlignedReadsData>> iterate(String file) {
		return iterate(file, (ard,r)->ard);
	}

	public static <O> ExtendedIterator<ReferenceGenomicRegion<O>> iterate(String file, BiFunction<DefaultAlignedReadsData, SAMRecord, O> dataMapper) {
		return EI.wrap(new SAMFileReader(new File(file)).iterator()).map(new SamToRegion<O>(dataMapper));
	}

	
	
	private int[] c = {1};
	private BiFunction<DefaultAlignedReadsData, SAMRecord, O> data;
	
	
	public SamToRegion(BiFunction<DefaultAlignedReadsData, SAMRecord, O> data) {
		this.data = data;
	}

	@Override
	public ReferenceGenomicRegion<O> apply(SAMRecord r) {
		if (r.getReadUnmappedFlag()) {
			DefaultAlignedReadsData ard = new AlignedReadsDataFactory(1).start().newDistinctSequence().setCount(new int[]{1}).setId(Integer.parseInt(r.getReadName())).create();
			O d = data.apply(ard, r);
			return new ImmutableReferenceGenomicRegion<O>(Chromosome.UNMAPPED, new ArrayGenomicRegion(0,r.getReadLength()),d);
		}
		FactoryGenomicRegion region = BamUtils.getFactoryGenomicRegion(r, c, false, true, null);
		region.add(r,0);
		DefaultAlignedReadsData ard = region.create();
		O d = data.apply(ard, r);
		return new ImmutableReferenceGenomicRegion<O>(BamUtils.getReference(r), new ArrayGenomicRegion(region), d);
	}
	
}
