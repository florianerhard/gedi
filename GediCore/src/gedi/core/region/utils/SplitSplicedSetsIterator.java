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

package gedi.core.region.utils;

import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionPart;
import gedi.util.FunctorUtils;
import gedi.util.datastructure.tree.redblacktree.IntervalTreeSet;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Stack;

public class SplitSplicedSetsIterator implements Iterator<Collection<GenomicRegion>> {

	private Iterator<Collection<GenomicRegion>> it;
	private Iterator<Collection<GenomicRegion>> splited;
	
	public SplitSplicedSetsIterator(Iterator<Collection<GenomicRegion>> it) {
		this.it = it;
	}
	
	@Override
	public boolean hasNext() {
		checkNext();
		return splited!=null && splited.hasNext();
	}

	@Override
	public Collection<GenomicRegion> next() {
		checkNext();
		return splited.next();
	}

	private void checkNext() {
		while ((splited==null || !splited.hasNext()) && it.hasNext()) {
			
			HashSet<GenomicRegion> tosplit = new HashSet<GenomicRegion>(it.next());
			if (tosplit.size()==1) {
				splited = FunctorUtils.singletonIterator(tosplit);
				return;
			}
			boolean multi = false;
			for (GenomicRegion r : tosplit)
				if (r.getNumParts()>1) {
					multi = true;
					break;
				}
			
			if (!multi) {
				splited = FunctorUtils.singletonIterator(tosplit);
				return;
			}
			
			
			IntervalTreeSet<GenomicRegionPart> partTree = new IntervalTreeSet<GenomicRegionPart>(null);
			for (GenomicRegion r : tosplit)
				for (int i=0; i<r.getNumParts(); i++)
					partTree.add(r.getPart(i));

			LinkedList<Collection<GenomicRegion>> re = new LinkedList<>(); 
			
			while (!tosplit.isEmpty()) {
				Stack<GenomicRegion> dfs = new Stack<>();
				PartToUniqueGenomicRegionTransformer forwarder = new PartToUniqueGenomicRegionTransformer(dfs);
				forwarder.add(tosplit.iterator().next().getPart(0));
				
				while (!dfs.isEmpty()) {
					GenomicRegion n = dfs.pop();
					for (int i=0; i<n.getNumParts(); i++)
						partTree.getIntervalsIntersecting(n.getStart(i), n.getEnd(i)-1, forwarder);	
				}
				re.add(forwarder.getObserved());
				tosplit.removeAll(forwarder.getObserved());
			}
			
			splited = re.iterator();
		}
	}
	

}
