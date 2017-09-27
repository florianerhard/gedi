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

package gedi.core.data.mapper;

import gedi.core.data.annotation.MappedReferenceSequencesProvider;
import gedi.core.data.annotation.ReferenceSequencesProvider;
import gedi.core.genomic.Genomic;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.ReferenceSequenceConversion;
import gedi.core.reference.Strand;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.workspace.loader.WorkspaceItemLoaderExtensionPoint;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

@GenomicRegionDataMapping(fromType=Void.class,toType=IntervalTree.class)
public class StorageSource<D> implements GenomicRegionDataSource<IntervalTree<GenomicRegion,D>>{
	
	private ArrayList<GenomicRegionStorage<D>> storages = new ArrayList<GenomicRegionStorage<D>>();
	private Strand filter;
	
	private ReferenceSequenceConversion referenceSequenceConversion = ReferenceSequenceConversion.none;
	private HashMap<String,String> nameMapping = new HashMap<String, String>();
	private HashMap<String,String> invNameMapping = new HashMap<String, String>();
	private boolean noReferenceSequences = false;
	
	public StorageSource() {
	}
	
	public StorageSource(Strand filter) {
		this.filter = filter;
	}
	
	
	public void addFile(String path) throws IOException {
		Path p = Paths.get(path);
		this.storages.add((GenomicRegionStorage<D>) WorkspaceItemLoaderExtensionPoint.getInstance().get(p).load(p));
	}
	
	public ArrayList<GenomicRegionStorage<D>> getStorages() {
		return storages;
	}
	
	public void add(GenomicRegionStorage<D> storage) {
		this.storages.add(storage);
	}
	
	public void addTranscripts(Genomic genomic) {
		this.storages.add((GenomicRegionStorage<D>) genomic.getTranscripts());
	}
	
	public void addGenes(Genomic genomic) {
		this.storages.add((GenomicRegionStorage<D>) genomic.getGenes());
	}
	
	public void setReferenceSequenceConversion(
			ReferenceSequenceConversion referenceSequenceConversion) {
		this.referenceSequenceConversion = referenceSequenceConversion;
	}
	
	public void noReferenceSequences() {
		this.noReferenceSequences = true;
	}
	
	public void map(String incoming, String here) {
		nameMapping.put(incoming, here);
		invNameMapping.put(here,incoming);
	}
	
	@Override
	public IntervalTree<GenomicRegion,D> get(ReferenceSequence reference, GenomicRegion region,PixelLocationMapping pixelMapping) {
		reference = referenceSequenceConversion.apply(reference);
		
		if (nameMapping.containsKey(reference.getName())) 
			reference = Chromosome.obtain(nameMapping.get(reference.getName()), reference.getStrand());
		
		IntervalTree<GenomicRegion, D> re = new IntervalTree<GenomicRegion, D>(reference.toStrand(filter));
		for (GenomicRegionStorage<D> storage : storages) 
			if (filter==null || filter==reference.getStrand())
				storage.getRegionsIntersecting(reference, region, re);
		
		if (reference.getStrand()==Strand.Independent)
			for (GenomicRegionStorage<D> storage : storages) {
				if (filter==null || filter==Strand.Plus)
					storage.getRegionsIntersecting(reference.toPlusStrand(), region, re);
				if (filter==null || filter==Strand.Minus)
					storage.getRegionsIntersecting(reference.toMinusStrand(), region, re);
			}
			
		return re;
	}


	private String id = null;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public <T> void applyForAll(Class<T> cls, Consumer<T> consumer) {
		for (GenomicRegionStorage<D> p : storages)
			if (cls.isInstance(p)) {
				if (!referenceSequenceConversion.altersName() && !noReferenceSequences){
					if (cls==ReferenceSequencesProvider.class && nameMapping.size()>0) {
						consumer.accept((T)new MappedReferenceSequencesProvider((ReferenceSequencesProvider)p, n->invNameMapping.get(n)));
					} else
						consumer.accept(cls.cast(p));
				}
			}
				
	}
	

}
