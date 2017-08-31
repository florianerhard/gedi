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

package gedi.util.io.text.tsv;

import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.FunctorUtils;
import gedi.util.GeneralUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.datastructure.tree.redblacktree.IntervalCoordinateSystem;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.functions.ExtendedIterator;
import gedi.util.functions.TriConsumer;
import gedi.util.io.text.HeaderLine;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.userInteraction.progress.Progress;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntBiFunction;
import java.util.function.UnaryOperator;

public class GenomicExonsTsvFileReader<D> extends BaseTsvFileReader<D> {

	
	protected String path;
	protected boolean header;
	protected String sep;
	protected BiFunction<HeaderLine,String[],ReferenceSequence> getRef; 
	protected ToIntBiFunction<HeaderLine,String[]> getStart; 
	protected ToIntBiFunction<HeaderLine,String[]> getEnd;	
	protected BiFunction<HeaderLine,String[][],D> getData;
	protected Comparator<String[]> sameRegionComparator;
	protected BiPredicate<String[],String[]> sameRegionLooseComparator;
	protected Consumer<HeaderLine> init;
	protected Consumer<MemoryIntervalTreeStorage<?>> finished;
	protected Function<D,Object> idgetter;
	protected BiPredicate<HeaderLine,String[]> readCoordinates;
	protected boolean mergeOverlap = false;
	protected UnaryOperator<ArrayGenomicRegion> regionMapper = a->a;
	
	private Progress progress;
	protected TriConsumer<ImmutableReferenceGenomicRegion<D>, HeaderLine, String[][]> sideEffect;
	
	
	/**
	 * Subclass must set all fields!
	 */
	protected GenomicExonsTsvFileReader(Class<D> type) {
		super(type);
	}
	
	public GenomicExonsTsvFileReader(String path, String chromosome, String strand, String start, String end, IntervalCoordinateSystem coord,
			BiFunction<HeaderLine,String[][],D> getData,
			Comparator<String[]> sameRegionComparator,
			Consumer<HeaderLine> init, Class<D> type) {
		this(path,true,"\t",
				(h,f) -> Chromosome.obtain(f[h.get(chromosome)],f[h.get(strand)]),
				(h,f) -> coord.convertStartToDefault(Integer.parseInt(f[h.get(start)])),
				(h,f) -> coord.convertEndToDefault(Integer.parseInt(f[h.get(end)])),
				getData,sameRegionComparator,init,type);
				
	}
	
	public GenomicExonsTsvFileReader(String path, boolean header, String sep,
			BiFunction<HeaderLine, String[], ReferenceSequence> getRef,
			ToIntBiFunction<HeaderLine, String[]> getStart,
			ToIntBiFunction<HeaderLine, String[]> getEnd,
			BiFunction<HeaderLine, String[][], D> getData,
			Comparator<String[]> sameRegionComparator,
			Consumer<HeaderLine> init, Class<D> type) {
		super(type);
		this.path = path;
		this.header = header;
		this.sep = sep;
		this.getRef = getRef;
		this.getStart = getStart;
		this.getEnd = getEnd;
		this.getData = getData;
		this.sameRegionComparator = sameRegionComparator;
		this.init = init;
	}
	
	public GenomicExonsTsvFileReader(String path, String chromosome, String strand, String start, String end, IntervalCoordinateSystem coord,
			BiFunction<HeaderLine,String[][],D> getData,
			BiPredicate<String[],String[]> sameRegionLooseComparator,
			Consumer<HeaderLine> init, Class<D> type) {
		this(path,true,"\t",
				(h,f) -> Chromosome.obtain(f[h.get(chromosome)],f[h.get(strand)]),
				(h,f) -> coord.convertStartToDefault(Integer.parseInt(f[h.get(start)])),
				(h,f) -> coord.convertEndToDefault(Integer.parseInt(f[h.get(end)])),
				getData,sameRegionLooseComparator,init,type);
				
	}
	
	public GenomicExonsTsvFileReader(String path, boolean header, String sep,
			BiFunction<HeaderLine, String[], ReferenceSequence> getRef,
			ToIntBiFunction<HeaderLine, String[]> getStart,
			ToIntBiFunction<HeaderLine, String[]> getEnd,
			BiFunction<HeaderLine, String[][], D> getData,
			BiPredicate<String[],String[]> sameRegionLooseComparator,
			Consumer<HeaderLine> init, Class<D> type) {
		super(type);
		this.path = path;
		this.header = header;
		this.sep = sep;
		this.getRef = getRef;
		this.getStart = getStart;
		this.getEnd = getEnd;
		this.getData = getData;
		this.sameRegionLooseComparator = sameRegionLooseComparator;
		this.init = init;
	}

	
	public GenomicExonsTsvFileReader<D> setProgress(Progress progress) {
		this.progress = progress;
		return this;
	}
	

	/**
	 * The combiner must handle apply(null,data) appropriately!
	 * @param combiner
	 * @return
	 * @throws IOException
	 */
	public <C> MemoryIntervalTreeStorage<C> readIntoMemory(MemoryIntervalTreeStorage<C> re, BiFunction<C, D, C> combiner) throws IOException {
		
		LineOrientedFile file = new LineOrientedFile(path);
		ExtendedIterator<String> it = file.lineIterator("#");
		if (progress!=null)
			it = it.progress(progress, -1, s->"Reading "+path);
		
		HeaderLine header = this.header?new HeaderLine(it.next()):null;
		if (init!=null)
			init.accept(header);

		ExtendedIterator<String[]> mit = FunctorUtils.mappedIterator(it, l -> StringUtils.split(l, sep));
		if (lineChecker!=null) {
			mit = mit.sideEffect(f->{
				if (readCoordinates==null || readCoordinates.test(header, f)) {
					String err = lineChecker.apply(header, f);
					if (err!=null) throw new RuntimeException("Error in line \n"+StringUtils.concat(sep, f)+"\n"+err);
				}
			});
		}
		Iterator<String[][]> tit;
		if (sameRegionComparator!=null) tit = (Iterator<String[][]>) FunctorUtils.multiplexIterator(mit, sameRegionComparator,String[].class);
		else tit = (Iterator<String[][]>) FunctorUtils.multiplexIterator(mit, sameRegionLooseComparator,String[].class);
		
		IntArrayList cr = new IntArrayList();
		
		HashSet<Object> tids = new HashSet<Object>(); 
		
		while (tit.hasNext()) {
			String[][] tr = tit.next();
			Arrays.sort(tr,(a,b)->GeneralUtils.saveCompare(getStart.applyAsInt(header,a),getStart.applyAsInt(header,b)));
			
			
			ReferenceSequence reference = getRef.apply(header, tr[0]);
			if (reference==null) continue;
			ArrayGenomicRegion reg ;
			
			for (int i=0; i<tr.length; i++) {
				if (readCoordinates==null || readCoordinates.test(header, tr[i])) {
					cr.add(getStart.applyAsInt(header,tr[i]));
					cr.add(getEnd.applyAsInt(header,tr[i]));
				}
			}
			if (!mergeOverlap ) {
				cr.sort();
				for (int i=0; i<tr.length; i++) {
					if (readCoordinates==null || readCoordinates.test(header, tr[i])) {
						int start = getStart.applyAsInt(header,tr[i]);
						int end = getEnd.applyAsInt(header,tr[i]);
						int si = cr.binarySearch(start);
						if (si<0 || (si%2)!=0)
							throw new IOException("Illegal exon start "+si+" for position "+start+" in "+cr+" for line "+StringUtils.concat("\t",tr[i]));
						int ei = cr.binarySearch(end);
						if (ei<0 || (ei%2)!=1)
							throw new IOException("Illegal exon end "+end+" for line "+StringUtils.concat("\t",tr[i]));
					}
				}
				reg = new ArrayGenomicRegion(cr);
				
			} else {
				reg = new ArrayGenomicRegion();
				for (int i=0; i<cr.size(); i+=2)
					reg = reg.union(new ArrayGenomicRegion(cr.getInt(i),cr.getInt(i+1)));
			}
			if (reg.getNumParts()==0) continue;
			
			reg = regionMapper.apply(reg);
			
			D data = getData.apply(header, tr);
			if (data==null) continue;
			if (idgetter!=null){
				if (!tids.add(idgetter.apply(data)))
					throw new RuntimeException(idgetter.apply(data)+" not consecutive in file: "+Arrays.toString(tr[0]));
			}
			
			if (sideEffect!=null)
				sideEffect.accept(new ImmutableReferenceGenomicRegion<D>(reference, reg,data),header,tr);
			
			IntervalTree<GenomicRegion, C> tree = re.getTree(reference);
			if (tree.containsKey(reg)) 
				tree.put(reg, combiner.apply(tree.get(reg), data));
			else	
				tree.put(reg, combiner.apply(null, data));
			
			cr.clear();
			
		}
		
		if (finished!=null)
			finished.accept(re);
		
		return re;
		
	}

	
}
