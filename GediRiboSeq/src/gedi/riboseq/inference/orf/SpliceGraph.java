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
package gedi.riboseq.inference.orf;

import gedi.core.region.GenomicRegion;
import gedi.util.datastructure.tree.redblacktree.IntervalTreeSet;
import gedi.util.datastructure.tree.redblacktree.SimpleInterval;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;

import java.util.function.Consumer;

public class SpliceGraph {

	
	private int start;
	private int end;
	
	
	private IntervalTreeSet<Intron> introns = new IntervalTreeSet<Intron>(null);
	
	public SpliceGraph(int start, int end) {
		this.start = start;
		this.end = end;
	}
	
	public void addIntron(int start, int end) {
		if (start>this.start && end<this.end)
			introns.add(new Intron(start, end-1));
	}
	
	public void addIntrons(GenomicRegion region) {
		for (int i=1; i<region.getNumParts(); i++)
			addIntron(region.getEnd(i-1), region.getStart(i));
	}
	

	public boolean contains(int start, int end) {
		return introns.contains(new Intron(start, end-1));
	}
	
	public ExtendedIterator<Intron> iterateIntrons() {
		return EI.wrap(introns);
	}
	

	public void forEachIntronStartingBetween(int start, int end, Consumer<Intron> consumer) {
		introns.forEachIntervalIntersecting(start, end-1, intron->{
			if (intron.getStart()>=start && intron.getStart()<end)
				consumer.accept(intron);
		});
		
	}
	
	
	public void forEachIntronEndingBetween(int start, int end, Consumer<Intron> consumer) {
		introns.forEachIntervalIntersecting(start, end-1, intron->{
			if (intron.getEnd()>=start && intron.getEnd()<end)
				consumer.accept(intron);
		});
		
	}
	
	
	
	
	public static class Intron extends SimpleInterval {

		public Intron(int start, int stop) {
			super(start, stop);
		}

		
	}





}
