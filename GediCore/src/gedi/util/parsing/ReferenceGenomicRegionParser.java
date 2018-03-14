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
package gedi.util.parsing;

import java.util.function.Supplier;

import gedi.core.reference.Chromosome;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;

public class ReferenceGenomicRegionParser<D> implements Parser<MutableReferenceGenomicRegion<D>> {
	
	public Supplier<D> factory;
	
	public ReferenceGenomicRegionParser() {
	}
	
	public ReferenceGenomicRegionParser(Supplier<D> factory) {
		this.factory = factory;
	}

	@Override
	public MutableReferenceGenomicRegion apply(String s) {
		int col = s.indexOf(':');
		if (col==-1) throw new IllegalArgumentException("No : found in "+s);
		Chromosome chr = Chromosome.obtain(s.substring(0,col));
		GenomicRegion reg = GenomicRegion.parse(s.substring(col+1));
		if (reg==null) return null;
		return new MutableReferenceGenomicRegion().set(chr, reg,factory==null?null:factory.get());
	}

	@Override
	public Class<MutableReferenceGenomicRegion<D>> getParsedType() {
		return (Class)MutableReferenceGenomicRegion.class;
	}
}