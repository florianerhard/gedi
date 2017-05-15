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

package gedi.core.genomic;

import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;

import java.util.Iterator;
import java.util.LinkedList;

public class Annotation<T> {

	private String id;
	
	private LinkedList<GenomicRegionStorage<T>> storages = new LinkedList<GenomicRegionStorage<T>>();;
	private MemoryIntervalTreeStorage<T> mem;
	
	
	public Annotation(String id) {
		this.id = id;
	}
	
	public Annotation<T> set(GenomicRegionStorage<T> storage) {
		if (this.storages.size()>0) throw new IllegalArgumentException("Already set!");
		this.storages.add(storage);
		return this;
	}
	
	public void merge(Annotation<T> other) {
		if (mem!=null) {
			if (other.mem!=null)
				addToMem(other.mem);
			else {
				Iterator<GenomicRegionStorage<T>> it = other.storages.iterator();
				while (it.hasNext()) {
					MemoryIntervalTreeStorage<T> o = it.next().toMemory();
					addToMem(o);
				}
			}
		}
		else {
			if (other.mem!=null)
				storages.add(other.mem);
			else
				storages.addAll(other.storages);
		}
		
	}
	
	public MemoryIntervalTreeStorage<T> get() {
		if (mem==null) {
			Iterator<GenomicRegionStorage<T>> it = storages.iterator();
			mem = it.next().toMemory();
			while (it.hasNext()) {
				MemoryIntervalTreeStorage<T> o = it.next().toMemory();
				addToMem(o);
			}
		}
		return mem;
	}


	private void addToMem(MemoryIntervalTreeStorage<T> o) {
		long before = mem.size();
		long add = o.size();
		mem.fill(o);
		if (mem.size()<before+add)
			throw new RuntimeException("Removed multi entries!");		
	}

	public String getId() {
		return id;
	}

}
