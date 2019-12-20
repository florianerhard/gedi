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
package gedi.util.datastructure.tree.redblacktree;

import java.io.IOException;
import java.util.Iterator;

import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionPart;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

public interface Interval {
	
	int getStart();
	int getStop();

	
	default int getEnd() {
		return getStop()+1;
	}
	
	default int length() {
		return getEnd()-getStart();
	}
	
	
	default GenomicRegion asRegion() {
		return new GenomicRegion() {
			@Override
			public int getNumParts() {
				return 1;
			}

			@Override
			public int getStart(int part) {
				return Interval.this.getStart();
			}

			@Override
			public int getEnd(int part) {
				return Interval.this.getEnd();
			}
			@Override
			public String toString() {
				return toRegionString();
			}
			
			@Override
			public boolean equals(Object obj) {
				return equals2(obj);
			}
			
			@Override
			public int hashCode() {
				return hashCode2();
			}
		};
	}
}
