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
package gedi.core.processing.old;

import gedi.core.region.GenomicRegion;
import gedi.core.region.MissingInformationIntronInformation;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.datastructure.tree.redblacktree.SimpleInterval;
import gedi.util.functions.TriPredicate;

import java.util.Map.Entry;
import java.util.Spliterator;
import java.util.function.BiPredicate;

public enum OverlapMode implements TriPredicate<GenomicRegion, IntervalTree<SimpleInterval,String>, GenomicRegion> {
	ContainedUnspliced {

			@Override
			public boolean test(GenomicRegion reference, IntervalTree<SimpleInterval,String> exons, GenomicRegion test) {
				if (test instanceof MissingInformationIntronInformation) {
					if (((MissingInformationIntronInformation)test).isPartMissing())
						return false;
					for (GenomicRegion t : ((MissingInformationIntronInformation)test).getInformationGenomicRegions())
						if (!reference.containsUnspliced(t))
							return false;
					return true;
				} else {
					return reference.containsUnspliced(test);
				}
				
			}
			 
		 }, 
	 ExonConsistent {
		 
			private boolean testPart(IntervalTree<SimpleInterval,String> exons, GenomicRegion test) {
				for (int p=0; p<test.getNumParts(); p++) {
					boolean testStart = p>0;
					boolean testStop = p<test.getNumParts()-1;
					
					int ss = test.getStart(p);
					int st = test.getStop(p);
					if (testStart && !exons.iterateIntervalsIntersecting(ss,ss, si->si.getStart()==ss).tryAdvance(s->{})) 
						return false;
					if (testStop && !exons.iterateIntervalsIntersecting(st, st, si->si.getStop()==st).tryAdvance(s->{})) 
						return false;
				}
				return true;
			}
			@Override
			public boolean test(GenomicRegion reference, IntervalTree<SimpleInterval,String> exons, GenomicRegion testMi) {
				if (exons==null) return reference.containsUnspliced(testMi);
				
				
				if (!reference.contains(testMi)) return false;
				
				if (testMi instanceof MissingInformationIntronInformation) {
					if (((MissingInformationIntronInformation)testMi).isPartMissing())
						return false;
					for (GenomicRegion test : ((MissingInformationIntronInformation)testMi).getInformationGenomicRegions())
						if (!testPart(exons,test))
							return false;
					return true;
				} else {
					return testPart(exons,testMi);
				}
				
			}
			 
		 }, 
		 Contained {

			@Override
			public boolean test(GenomicRegion reference, IntervalTree<SimpleInterval,String> exons, GenomicRegion test) {
				if (test instanceof MissingInformationIntronInformation && ((MissingInformationIntronInformation)test).isPartMissing())
					return false;
				return reference.contains(test);
			}
			 
		 },
	 Intersected {

			@Override
			public boolean test(GenomicRegion reference, IntervalTree<SimpleInterval,String> exons, GenomicRegion test) {
				return reference.intersects(test);
			}
			 
		 },
	 Always {

		@Override
		public boolean test(GenomicRegion reference, IntervalTree<SimpleInterval,String> exons, GenomicRegion test) {
			return true;
		}
		 
	 }, 
	 ;


	 public abstract boolean test(GenomicRegion reference, IntervalTree<SimpleInterval,String> exons, GenomicRegion test);
	 
	 
}