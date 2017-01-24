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

package gedi.core.processing;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.region.MutableReferenceGenomicRegion;

public interface GenomicRegionProcessor {

	default void begin(ProcessorContext context) throws Exception {}
	default void beginRegion(MutableReferenceGenomicRegion<?> region, ProcessorContext context) throws Exception {}
	default void read(MutableReferenceGenomicRegion<?> region, MutableReferenceGenomicRegion<AlignedReadsData> read, ProcessorContext context) throws Exception {}
	default void value(MutableReferenceGenomicRegion<?> region, int position,double[] values, ProcessorContext context) throws Exception {}
	default void endRegion(MutableReferenceGenomicRegion<?> region, ProcessorContext context) throws Exception {}
	default void end(ProcessorContext context) throws Exception {}

}
