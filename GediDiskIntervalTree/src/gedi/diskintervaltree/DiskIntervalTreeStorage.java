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

package gedi.diskintervaltree;


import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.AlignedReadsDataSumCountOperator;
import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.utils.OnlyMatchingGenomicIntervalCollectionPredicate;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.MappedSpliterator;
import gedi.util.io.Directory;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Spliterator;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;



public class DiskIntervalTreeStorage<D extends BinarySerializable>  implements GenomicRegionStorage<D> {
	
	private Directory folder;
	private HashMap<String,DiskIntervalTree<D>> cache = new HashMap<String,DiskIntervalTree<D>>(); 

	public DiskIntervalTreeStorage(String path) throws IOException {
		this(new Directory(path));
	}
	
	public DiskIntervalTreeStorage(Directory folder) throws IOException {
		this.folder = folder;
		if (!folder.exists()) folder.mkdirs();
	}
	
	public synchronized DiskIntervalTree<D> get(ReferenceSequence ref) {
		return get(ref.toString());
	}
	
	public synchronized DiskIntervalTree<D> get(String chromosome) {
		if (!cache.containsKey(chromosome)) {
			try {
				File f = new File(folder,chromosome+".interval");
//					System.out.println("checking "+f.getPath());
				if (f.exists()) {
					DiskIntervalTree<D> rbt = new DiskIntervalTree<D>(f.getPath());
					cache.put(chromosome, rbt);
//						System.out.println("created "+chromosome+" "+rbt);
					return rbt;
				}
				throw new RuntimeException("Could not fetch data", new FileNotFoundException(chromosome));
			} catch (Exception e) {
				throw new RuntimeException("Could not fetch data", e);
			}
		}
		return cache.get(chromosome);
	}
	
	@Override
	public Class<D> getType() {
		return get(getReferenceSequences().iterator().next()).getType();
	}
	
	@Override
	public int hashCode() {
		return folder.hashCode();
	}
	
	public Directory getFolder() {
		return folder;
	}

	
	@Override
	public String toString() {
		return "DiskIntervalTree root "+folder;
	}

	private HashSet<ReferenceSequence> chromosomes;
		
	

	@Override
	public Set<ReferenceSequence> getReferenceSequences() {
		if (chromosomes==null) {
			chromosomes = new HashSet<ReferenceSequence>();
			for (File f : folder.listFiles()) 
				if (f.getName().endsWith(".interval"))
					chromosomes.add(Chromosome.obtain(f.getName().substring(0, f.getName().length()-".interval".length())));
		}
		return chromosomes;
	}

	@Override
	public Spliterator<MutableReferenceGenomicRegion<D>> iterateMutableReferenceGenomicRegions(
			ReferenceSequence reference) {
		
		Supplier<Function<GenomicRegion, MutableReferenceGenomicRegion<D>>> supp = 
				()->{
					MutableReferenceGenomicRegion<D> mut = new MutableReferenceGenomicRegion<D>();
					return (e)->mut.set(reference, e, ((DiskIntervalGenomicRegion<D>)e).getData());
				};
		return new MappedSpliterator<GenomicRegion, MutableReferenceGenomicRegion<D>>(get(reference).spliterator(),supp);
	}

	@Override
	public Spliterator<MutableReferenceGenomicRegion<D>> iterateIntersectingMutableReferenceGenomicRegions(
			ReferenceSequence reference, GenomicRegion region) {
		Supplier<Function<DiskIntervalGenomicRegion<D>, MutableReferenceGenomicRegion<D>>> supp = 
				()->{
					MutableReferenceGenomicRegion<D> mut = new MutableReferenceGenomicRegion<D>();
					return (e)->mut.set(reference, e, e.getData());
				};
		return new MappedSpliterator<DiskIntervalGenomicRegion<D>, MutableReferenceGenomicRegion<D>>(get(reference).getIntervalsIntersecting(region,new ArrayList<>()).spliterator(),supp);
	}

	@Override
	public boolean add(ReferenceSequence reference, GenomicRegion region, D data) {
		throw new UnsupportedOperationException();
	}
	
//	@Override
//	public void fill(GenomicRegionStorage<D> source) {
//		
//		for (ReferenceSequence reference : source.getReferenceSequences()) {
//			if (getReferenceSequences().contains(reference)) throw new RuntimeException("Cannot generate "+reference+", already exisits!");
//			long s = source.size(reference);
//			if (s==0) continue;
//			
//			try {
//				System.out.println("Create "+reference);
//				DiskIntervalTree.create(
//						reference.toString()+" []", 
//						source,reference,
//						(ref,reg)->getData(ref, reg),
//						s,
//						new File(getFolder(),reference.toString()+".interval").getPath(),
//						DefaultAlignedReadsData.class,
//						new AlignedReadsDataSumCountOperator(),
//						new int[] {100000,1000000,10000000});
//				System.out.println("done, entries: "+s);
//			} catch (IOException e) {
//				throw new RuntimeException("Cannot write file!",e);
//			}
//			
//		}
//	}

	@Override
	public boolean remove(ReferenceSequence reference, GenomicRegion region) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(ReferenceSequence reference, GenomicRegion region) {
		throw new UnsupportedOperationException();
	}

	@Override
	public D getData(ReferenceSequence reference, GenomicRegion region) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long size(ReferenceSequence reference) {
		return get(reference).size();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public DynamicObject getMetaData() {
		return DynamicObject.getEmpty();
	}

	@Override
	public void setMetaData(DynamicObject meta) {
	}

	
	
	
}