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

package gedi.virology.hsv1annotation.kinetics.provider;

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.mutable.MutableInteger;

import java.util.ArrayList;

public class TtsProvider implements KineticRegionProvider {

	private MemoryIntervalTreeStorage<NameAnnotation> mrna;

	private int maxUpstream = 500;

	public TtsProvider(MemoryIntervalTreeStorage<NameAnnotation> mrna) {
		this.mrna = mrna;
	}

	@Override
	public String getType() {
		return "TTS";
	}


	@Override
	public ExtendedIterator<ImmutableReferenceGenomicRegion<NameAnnotation>> ei(
			ReferenceGenomicRegion<?> region) {

		MutableInteger n = new MutableInteger(1);
		return mrna.ei(region)
				.map(mr->getUpstream(mr))
				.map(down->restrict(down))
				.sort()
				.unique(true)
				.map(r->new ImmutableReferenceGenomicRegion<NameAnnotation>(r.getReference(), r.getRegion(), new NameAnnotation("TTS "+(n.N++))));
	}

	public ReferenceGenomicRegion<Void> getUpstream(ReferenceGenomicRegion<?> r) {
		GenomicRegion reg = r.getRegion();
		return new ImmutableReferenceGenomicRegion<Void>(r.getReference(), r.getReference().getStrand().equals(Strand.Plus)?new ArrayGenomicRegion(reg.getEnd()-maxUpstream,reg.getEnd()):new ArrayGenomicRegion(reg.getStart(),reg.getStart()+maxUpstream));
	}

	public <T> ReferenceGenomicRegion<T> restrict(ReferenceGenomicRegion<T> r) {
		ArrayList<GenomicRegion> other = mrna.ei(r).map(m->m.getRegion()).list();
		if (other.size()==0) return r;

		if (r.getReference().getStrand().equals(Strand.Plus)){
			int first = EI.wrap(other).mapToInt(o->o.getEnd()).filterInt(i->r.getRegion().contains(i)).max();
			if (first==Integer.MIN_VALUE)
				return r;
			return r.toMutable().transformRegion(reg->reg.subtract(new ArrayGenomicRegion(first,reg.getEnd())));
		}

		int first = EI.wrap(other).mapToInt(o->o.getStart()).filterInt(i->r.getRegion().contains(i)).min();
		if (first==Integer.MAX_VALUE)
			return r;
		return r.toMutable().transformRegion(reg->reg.subtract(new ArrayGenomicRegion(reg.getStart(),first)));
	}


}
