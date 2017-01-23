package gedi.util.io.text.tsv;

import gedi.core.reference.Chromosome;
import gedi.core.reference.Strand;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.FunctorUtils;
import gedi.util.FunctorUtils.MappedIterator;
import gedi.util.StringUtils;
import gedi.util.datastructure.tree.redblacktree.IntervalCoordinateSystem;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.functions.ExtendedIterator;
import gedi.util.functions.TriConsumer;
import gedi.util.io.text.HeaderLine;
import gedi.util.io.text.LineOrientedFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class GenomicTsvFileReader<D>  extends BaseTsvFileReader<D>{

	
	protected String path;
	protected Iterator<String> it;
	protected boolean header;
	protected String sep;
	protected TriConsumer<HeaderLine, String[], MutableReferenceGenomicRegion<D>> parser;
	protected BiFunction<HeaderLine,String[],ReferenceSequence> getRef; 
	protected BiFunction<HeaderLine,String[],GenomicRegion> getRegion; 
	protected BiFunction<HeaderLine,String[],D> getData;
	protected Consumer<HeaderLine> init;
	
	/**
	 * Subclass must set all fields!
	 */
	protected GenomicTsvFileReader(Class<D> type) {
		super(type);
	}
	
	public GenomicTsvFileReader(String path, int position,
			BiFunction<HeaderLine,String[],D> getData,Class<D> type) {
		this(path,false,"\t",
				(h,f) -> Chromosome.obtain(beforeColon(f[position])),
				(h,f) -> GenomicRegion.parse(afterColon(f[position])),
				getData,null, type);
				
	}
	
	public GenomicTsvFileReader(String path, int chromosome, int strand, int start, int end, IntervalCoordinateSystem coord,
			BiFunction<HeaderLine,String[],D> getData,Class<D> type) {
		this(path,true,"\t",
				(h,f) -> Chromosome.obtain(f[chromosome],f[strand]),
				(h,f) -> new ArrayGenomicRegion(
						coord.convertStartToDefault(Integer.parseInt(f[start])),
						coord.convertEndToDefault(Integer.parseInt(f[end]))
						),
				getData,null, type);
				
	}
	
	public GenomicTsvFileReader(String path, int chromosome, int start, int end, IntervalCoordinateSystem coord,
			BiFunction<HeaderLine,String[],D> getData,Class<D> type) {
		this(path,true,"\t",
				(h,f) -> Chromosome.obtain(f[chromosome],Strand.Independent),
				(h,f) -> new ArrayGenomicRegion(
						coord.convertStartToDefault(Integer.parseInt(f[start])),
						coord.convertEndToDefault(Integer.parseInt(f[end]))
						),
				getData,null, type);
				
	}
	
	public GenomicTsvFileReader(String path, String position,
			BiFunction<HeaderLine,String[],D> getData,
			Consumer<HeaderLine> init,Class<D> type) {
		this(path,true,"\t",
				(h,f) -> Chromosome.obtain(beforeColon(f[h.get(position)])),
				(h,f) -> GenomicRegion.parse(afterColon(f[h.get(position)])),
				getData,init, type);
				
	}
	
	public GenomicTsvFileReader(String path, String chromosome, String strand, String start, String end, IntervalCoordinateSystem coord,
			BiFunction<HeaderLine,String[],D> getData,
			Consumer<HeaderLine> init,Class<D> type) {
		this(path,true,"\t",
				(h,f) -> Chromosome.obtain(f[h.get(chromosome)],f[h.get(strand)]),
				(h,f) -> new ArrayGenomicRegion(
						coord.convertStartToDefault(Integer.parseInt(f[h.get(start)])),
						coord.convertEndToDefault(Integer.parseInt(f[h.get(end)]))
						),
				getData,init,  type);
				
	}
	
	public GenomicTsvFileReader(String path, String chromosome, String start, String end, IntervalCoordinateSystem coord,
			BiFunction<HeaderLine,String[],D> getData,
			Consumer<HeaderLine> init,Class<D> type) {
		this(path,true,"\t",
				(h,f) -> Chromosome.obtain(f[h.get(chromosome)],Strand.Independent),
				(h,f) -> new ArrayGenomicRegion(
						coord.convertStartToDefault(Integer.parseInt(f[h.get(start)])),
						coord.convertEndToDefault(Integer.parseInt(f[h.get(end)]))
						),
				getData,init,  type);
				
	}
	
	
	
	public GenomicTsvFileReader(String path, String chromosome, String strand, String start, String end, IntervalCoordinateSystem coord,
			BiFunction<HeaderLine,String[],D> getData,Class<D> type) {
		this(path,chromosome,strand,start,end,coord,getData,null,type);
				
	}
	
	public GenomicTsvFileReader(String path, String chromosome, String start, String end, IntervalCoordinateSystem coord,
			BiFunction<HeaderLine,String[],D> getData,Class<D> type) {
		this(path,chromosome,start,end,coord,getData,null,type);
	}
	

	
	public GenomicTsvFileReader(String path, boolean header, String sep,
			BiFunction<HeaderLine, String[], ReferenceSequence> getRef,
			BiFunction<HeaderLine, String[], GenomicRegion> getRegion,
			BiFunction<HeaderLine, String[], D> getData,
			Consumer<HeaderLine> init, Class<D> type) {
		super(type);
		this.path = path;
		this.header = header;
		this.sep = sep;
		this.getRef = getRef;
		this.getRegion = getRegion;
		this.getData = getData;
		this.init = init;
	}
	
	
	public GenomicTsvFileReader(String path, boolean header, String sep,
			TriConsumer<HeaderLine, String[], MutableReferenceGenomicRegion<D>> parser,
			Consumer<HeaderLine> init, Class<D> type) {
		super(type);
		this.path = path;
		this.header = header;
		this.sep = sep;
		this.parser = parser;
		this.init = init;
	}
	
	public GenomicTsvFileReader(Iterator<String> it, int position,
			BiFunction<HeaderLine,String[],D> getData, Class<D> type) {
		this(it,false,"\t",
				(h,f) -> Chromosome.obtain(beforeColon(f[position])),
				(h,f) -> GenomicRegion.parse(afterColon(f[position])),
				getData,null,type);
				
	}
	
	public GenomicTsvFileReader(Iterator<String> it, int chromosome, int strand, int start, int end, IntervalCoordinateSystem coord,
			BiFunction<HeaderLine,String[],D> getData, Class<D> type) {
		this(it,true,"\t",
				(h,f) -> Chromosome.obtain(f[chromosome],f[strand]),
				(h,f) -> new ArrayGenomicRegion(
						coord.convertStartToDefault(Integer.parseInt(f[start])),
						coord.convertEndToDefault(Integer.parseInt(f[end]))
						),
				getData,null,type);
				
	}
	
	public GenomicTsvFileReader(Iterator<String> it, int chromosome, int start, int end, IntervalCoordinateSystem coord,
			BiFunction<HeaderLine,String[],D> getData, Class<D> type) {
		this(it,true,"\t",
				(h,f) -> Chromosome.obtain(f[chromosome],Strand.Independent),
				(h,f) -> new ArrayGenomicRegion(
						coord.convertStartToDefault(Integer.parseInt(f[start])),
						coord.convertEndToDefault(Integer.parseInt(f[end]))
						),
				getData,null,type);
				
	}
	
	public GenomicTsvFileReader(Iterator<String> it, String position,
			BiFunction<HeaderLine,String[],D> getData,
			Consumer<HeaderLine> init, Class<D> type) {
		this(it,true,"\t",
				(h,f) -> Chromosome.obtain(beforeColon(f[h.get(position)])),
				(h,f) -> GenomicRegion.parse(afterColon(f[h.get(position)])),
				getData,init,type);
				
	}
	
	public GenomicTsvFileReader(Iterator<String> it, String chromosome, String strand, String start, String end, IntervalCoordinateSystem coord,
			BiFunction<HeaderLine,String[],D> getData,
			Consumer<HeaderLine> init, Class<D> type) {
		this(it,true,"\t",
				(h,f) -> Chromosome.obtain(f[h.get(chromosome)],f[h.get(strand)]),
				(h,f) -> new ArrayGenomicRegion(
						coord.convertStartToDefault(Integer.parseInt(f[h.get(start)])),
						coord.convertEndToDefault(Integer.parseInt(f[h.get(end)]))
						),
				getData,init,type);
				
	}
	
	public GenomicTsvFileReader(Iterator<String> it, String chromosome, String start, String end, IntervalCoordinateSystem coord,
			BiFunction<HeaderLine,String[],D> getData,
			Consumer<HeaderLine> init, Class<D> type) {
		this(it,true,"\t",
				(h,f) -> Chromosome.obtain(f[h.get(chromosome)],Strand.Independent),
				(h,f) -> new ArrayGenomicRegion(
						coord.convertStartToDefault(Integer.parseInt(f[h.get(start)])),
						coord.convertEndToDefault(Integer.parseInt(f[h.get(end)]))
						),
				getData,init,type);
				
	}
	
	
	
	public GenomicTsvFileReader(Iterator<String> it, String chromosome, String strand, String start, String end, IntervalCoordinateSystem coord,
			BiFunction<HeaderLine,String[],D> getData, Class<D> type) {
		this(it,chromosome,strand,start,end,coord,getData,null,type);
				
	}
	
	public GenomicTsvFileReader(Iterator<String> it, String chromosome, String start, String end, IntervalCoordinateSystem coord,
			BiFunction<HeaderLine,String[],D> getData, Class<D> type) {
		this(it,chromosome,start,end,coord,getData,null,type);
	}
	

	
	public GenomicTsvFileReader(Iterator<String> it, boolean header, String sep,
			BiFunction<HeaderLine, String[], ReferenceSequence> getRef,
			BiFunction<HeaderLine, String[], GenomicRegion> getRegion,
			BiFunction<HeaderLine, String[], D> getData,
			Consumer<HeaderLine> init, Class<D> type) {
		super(type);
		this.it = it;
		this.header = header;
		this.sep = sep;
		this.getRef = getRef;
		this.getRegion = getRegion;
		this.getData = getData;
		this.init = init;
	}
	
	
	public GenomicTsvFileReader(Iterator<String> it, boolean header, String sep,
			TriConsumer<HeaderLine, String[], MutableReferenceGenomicRegion<D>> parser,
			Consumer<HeaderLine> init, Class<D> type) {
		super(type);
		this.it = it;
		this.header = header;
		this.sep = sep;
		this.parser = parser;
		this.init = init;
	}

	private static String beforeColon(String s) {
		return s.substring(0,s.lastIndexOf(':'));
	}
	private static String afterColon(String s) {
		return s.substring(1+s.lastIndexOf(':'));
	}


	/**
	 * The combiner must handle apply(null,data) appropriately! Init may be null if header is false
	 * @param combiner
	 * @return
	 * @throws IOException
	 */
	public <C> MemoryIntervalTreeStorage<C> readIntoMemory(MemoryIntervalTreeStorage<C> re, BiFunction<C, D, C> combiner) throws IOException {
		
		if (it==null) {
			it = createIterator();
		}
		
		HeaderLine header = this.header?new HeaderLine(it.next()):null;
		if (header!=null && init!=null) init.accept(header);
		
		ExtendedIterator<String[]> mit = FunctorUtils.mappedIterator(it, l -> StringUtils.split(l, sep));
		if (lineChecker!=null) {
			mit = mit.sideEffect(f->{
				String err = lineChecker.apply(header, f);
				if (err!=null) throw new RuntimeException("Error in line \n"+StringUtils.concat(sep, f)+"\n"+err);
			});
		}
		MutableReferenceGenomicRegion<D> box = new MutableReferenceGenomicRegion<D>();
		
		while (mit.hasNext()) {
			String[] tr = mit.next();
			
			
			if (parser!=null) {
				parser.accept(header, tr, box);
				if (box.getReference()==null || box.getRegion()==null || box.getData()==null)
					continue;
			} else {
				if (box.setReference(getRef.apply(header, tr)).getReference()==null) continue;
				if (box.setRegion(getRegion.apply(header, tr)).getRegion()==null) continue;
				if (box.setData(getData.apply(header, tr)).getData()==null) continue;
			}
			
			
			try {
				IntervalTree<GenomicRegion, C> tree = re.getTree(box.getReference());
				if (tree.containsKey(box.getRegion())) 
					tree.put(box.getRegion(), combiner.apply(tree.get(box.getRegion()), box.getData()));
				else	
					tree.put(box.getRegion(), combiner.apply(null, box.getData()));
			} catch (Exception e) {
				throw new RuntimeException("Could not read line "+Arrays.toString(tr)+" "+box.getReference()+":"+box.getRegion(),e);
			}
		}
		
		return re;
	}

	protected Iterator<String> createIterator() throws IOException {
		return new LineOrientedFile(path).lineIterator();
	}

	
}
