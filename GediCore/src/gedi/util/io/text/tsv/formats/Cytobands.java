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
package gedi.util.io.text.tsv.formats;

import java.io.IOException;
import java.util.Iterator;
import java.util.Spliterators;

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.data.annotation.ScoreNameAnnotation;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;

public class Cytobands extends MemoryIntervalTreeStorage<ScoreNameAnnotation> {

	public Cytobands(String file) throws IOException {
		super(ScoreNameAnnotation.class);
		new ScoreNameBedFileReader(file).readIntoMemoryThrowOnNonUnique(this);
		
		for (ReferenceSequence ref : getReferenceSequences()) {
			boolean black = true;
			Iterator<MutableReferenceGenomicRegion<ScoreNameAnnotation>> it = Spliterators.iterator(iterateMutableReferenceGenomicRegions(ref));
			while (it.hasNext()) {
				MutableReferenceGenomicRegion<ScoreNameAnnotation> n = it.next();
				n.getData().setScore(black?1:0);
				black = !black;
			}
		
		}
		
	}
	
	@Override
	public int getLength(String name) {
		int re = super.getLength(name);
		if (re!=-1) return -re;
		return re;
	}
	
}
