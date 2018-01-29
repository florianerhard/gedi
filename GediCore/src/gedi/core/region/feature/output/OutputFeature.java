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

package gedi.core.region.feature.output;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import cern.colt.bitvector.BitVector;
import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.features.AbstractFeature;
import gedi.core.region.feature.special.UnfoldGenomicRegionStatistics;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.mutable.MutableTuple;

public abstract class OutputFeature extends AbstractFeature<Void> {

	
	protected MutableTuple key;
	protected BitVector hidden;
	
	
	@Override
	protected void copyProperties(AbstractFeature<Void> from) {
		super.copyProperties(from);
		Class[] types = new Class[inputs.length];
		Arrays.fill(types, Set.class);
		key = new MutableTuple(types);
		hidden = ((OutputFeature)from).hidden;
	}
	

	@Override
	public void setInputNames(String[] inputs) {
		super.setInputNames(inputs);
		Class[] types = new Class[inputs.length];
		Arrays.fill(types, Set.class);
		key = new MutableTuple(types);
		if (hidden!=null)
			hidden.setSize(inputs.length);
		else
			hidden = new BitVector(inputs.length);
	}
	
	@Override
	public <I> void setInput(int index, Set<I> input) {
		super.setInput(index, input);
		if (!hidden.getQuick(index))
			key.setQuick(index, input);
		else
			key.setQuick(index, Collections.emptySet());
	}

	public void hideInput(int index) {
		if (this.hidden==null)
			this.hidden = new BitVector(index+1);
		else if (this.hidden.size()<=index)
			this.hidden.setSize(index+1);
		this.hidden.putQuick(index, true);
	}
	

	protected boolean mustUnfold(MutableTuple key) {
		for (int i=0; i<key.size(); i++) {
			Set<?> e = key.get(i);
			if (e.size()==1 && e.iterator().next() instanceof UnfoldGenomicRegionStatistics)
				return true;
		}
		return false;
	}
	
	protected ExtendedIterator<MutableTuple> unfold(MutableTuple key) {
		IntArrayList ind = new IntArrayList();
		ArrayList<Iterator<Object>> unf = new ArrayList<>();
		for (int i=0; i<key.size(); i++) {
			Set<?> e = key.get(i);
			Iterator<?> it = e.iterator();
			int n = 0;
			if (it.hasNext()) {
				Object o = it.next();
				if (o instanceof UnfoldGenomicRegionStatistics) {
					UnfoldGenomicRegionStatistics unfolder = (UnfoldGenomicRegionStatistics)o;
					unf.add(unfolder.iterator());
					ind.add(i);
					if (it.hasNext() || n!=0)
						throw new RuntimeException("If a feature wants to return an UnfoldGenomicRegionStatistics, it must be the only result value!");
				}
				n++;
			}
		}
		if (unf.isEmpty()) return EI.singleton(key);
		
		MutableTuple re = key.clone();
		for (int i:ind.toIntArray())
			re.set(i, new HashSet());

		return EI.wrap(unf.get(0)).map(e0->{
			
			Set s = re.get(ind.getInt(0));
			s.clear();
			s.add(e0);
			
			for (int i=1; i<ind.size(); i++) {
				s = re.get(ind.getInt(i));
				s.clear();
				if (!unf.get(i).hasNext())
					throw new RuntimeException("All UnfoldGenomicRegionStatistics must return the same number of entries!");
				s.add(unf.get(i).next());
			}
			return re;
		}).endAction(()->{
			for (int i=1; i<ind.size(); i++) 
				if (unf.get(i).hasNext())
					throw new RuntimeException("All UnfoldGenomicRegionStatistics must return the same number of entries!");
		});
	}
	
}
