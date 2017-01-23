package gedi.core.region.feature;

import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.userInteraction.results.Result;
import gedi.util.userInteraction.results.ResultConsumer;
import gedi.util.userInteraction.results.ResultProducer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import cern.colt.bitvector.BitVector;
import sun.nio.ch.ThreadPool;

public class OldGenomicRegionFeatureProgram<D> implements Consumer<ReferenceGenomicRegion<D>> {

	private static final int SIZE = 1000000;

	private final ReferenceGenomicRegion<D> TERM = new ImmutableReferenceGenomicRegion<D>(null, null);

	private HashMap<String,Integer> idMap = new HashMap<String, Integer>();   
			
	private ArrayList<int[]> inputs = new ArrayList<int[]>();
	private ArrayList<GenomicRegionFeature<?>> features = new ArrayList<GenomicRegionFeature<?>>();
	
	private BitVector newSet;
	private ArrayList<Set> data = new ArrayList<Set>();

	private String[] labels = null;
	
	private boolean parallel = false;
	
	
	private long intermediateInterval = 5000;
	
	
	private BiFunction<D,NumericArray,NumericArray> dataToCounts = (a,b)->{
		if (a instanceof NumericArray) return (NumericArray)a;
		if (b==null || b.length()!=1) b = NumericArray.createMemory(1, NumericArrayType.Integer);
		b.setInt(0, 1);
		return b;
	};
	
	public ArrayList<ResultProducer> getResultProducers() {
		ArrayList<ResultProducer> re = new ArrayList<ResultProducer>();
		for (GenomicRegionFeature<?> f : features)
			f.addResultProducers(re);
		return re;
	}
	
	public void setConditionFile(String file) throws IOException {
		this.labels = new LineOrientedFile(file).lineIterator().skip(1).map(s->StringUtils.splitField(s, '\t', 1)).toArray(new String[0]);
	}

	public void setLabels(String[] labels) {
		this.labels = labels;
	}
	
	public String[] getLabels() {
		return labels;
	}
	
	public void setIntermediateInterval(long intermediateInterval) {
		this.intermediateInterval = intermediateInterval;
	}
	
	public Set<String> getInputById(String id) {
		Integer index = idMap.get(id);
		if (index==null) return null;
		return data.get(index);
	}
	
	public <T> GenomicRegionFeature<T> getFeature(int index) {
		return (GenomicRegionFeature<T>) features.get(index);
	}
	
	public <T> GenomicRegionFeature<T> getFeature(String id) {
		return (GenomicRegionFeature<T>) features.get(idMap.get(id));
	}
	
	public void setDataToCounts(
			BiFunction<D, NumericArray, NumericArray> dataToCounts) {
		this.dataToCounts = dataToCounts;
	}
	
	public NumericArray dataToCounts(D data, NumericArray buffer) {
		return dataToCounts.apply(data, buffer);
	}
	
	public <O> void add(GenomicRegionFeature<O> feature, String... inputs) {
		
		if (running) throw new RuntimeException("Do not call before adding everything!");
		
		if (idMap.containsKey(feature.getId())) throw new RuntimeException("Feature with id "+feature.getId()+" already present!");
		
		idMap.put(feature.getId(), features.size());
		if (inputs==null) inputs = new String[0];
		
		int[] inpIndex = new int[inputs.length];
		for (int i=0; i<inputs.length; i++) {
			Integer index = idMap.get(inputs[i]);
			if (index==null) throw new RuntimeException("Input "+inputs[i]+" unknown!");
			inpIndex[i] = index;
		}
		
//		feature.setProgram(this);
		feature.setInputNames(inputs);
		this.inputs.add(inpIndex);
		features.add(feature);
		data.add(new HashSet());
	}
	
	private boolean running = false;

	private LinkedBlockingQueue<ReferenceGenomicRegion<D>> queue;
	private Thread thread;

	
	public void begin() {
		lastIntermediate = System.currentTimeMillis();
		running = true;
		if (parallel) {
			queue = new LinkedBlockingQueue<ReferenceGenomicRegion<D>>(SIZE);
			thread = new Thread("ProcessFeatureThread") {
				@Override
				public void run() {
					try {
						while (true) {
							ReferenceGenomicRegion<D> e = queue.take();
							if (e==TERM) return;
							process(e);
						}
					} catch (InterruptedException e1) {
					}
				}
			};
			thread.start();
		}
		
		for (GenomicRegionFeature<?> f : features)
			f.begin();
	}
	
	public void end() {
		if (parallel) {
			try {
				queue.put(TERM);
			} catch (InterruptedException e) {
			}
			while (!queue.isEmpty())
				Thread.yield();
			
			thread = null;
			queue = null;
		}
		try {
			pool.shutdown();
			pool.awaitTermination(1, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
		}
		for (GenomicRegionFeature<?> f : features) {
			f.end();
			f.produceResults(null);
		}
		running = false;
	}
	
	public void accept(ReferenceGenomicRegion<D> rgr) {
		if (!running) throw new RuntimeException("Call begin first!");

		if (parallel) {
			try {
				rgr = rgr.toImmutable();
				if (!queue.offer(rgr)) {
					while (!queue.isEmpty())
						Thread.yield();
					queue.put(rgr);
				}
			} catch (InterruptedException e) {
			}
		} else
			process(rgr);
		
		
		
	}
	
	private boolean benchmark = true;
	public void setBenchmark(boolean benchmark) {
		this.benchmark = benchmark;
	}
	
	private long[][] duration = null;
	private long[][] counter = null;
	
	private long lastIntermediate = 0;
	
	private ExecutorService pool = Executors.newFixedThreadPool(1);

	private void process(ReferenceGenomicRegion<D> rgr) {
		if (intermediateInterval>0 && System.currentTimeMillis()>lastIntermediate+intermediateInterval) {
			lastIntermediate = Long.MAX_VALUE-intermediateInterval;
			pool.execute(()->{
					for (int i=0; i<features.size(); i++) 
						features.get(i).produceResults(null);
					lastIntermediate = System.currentTimeMillis();
				}
			);
		}
		
		
		if (newSet==null || newSet.size()!=features.size()) newSet = new BitVector(features.size());
		newSet.clear();
		
		if (benchmark) {
			if (duration==null) duration = new long[features.size()][3];
			if (counter==null) counter = new long[features.size()][2];
			long start = 0;
			
			for (int i=0; i<features.size(); i++) {
				start = System.nanoTime();
				GenomicRegionFeature<?> f = features.get(i);
				
				boolean changed = f.setGenomicRegion(rgr.getReference(), rgr.getRegion());
				if (changed || f.dependsOnData() || isAnySet(newSet,inputs.get(i))) { 
					f.setData(rgr.getData());
					
					int[] inp = inputs.get(i);
					for (int j = 0; j < inp.length; j++) 
						f.setInput(j, data.get(inp[j]));
					duration[i][0]+=System.nanoTime()-start;
					
					start = System.nanoTime();
					data.get(i).clear();
					f.accept(data.get(i));
					duration[i][1]+=System.nanoTime()-start;
					
					start = System.nanoTime();
					if (!data.get(i).isEmpty())
						f.applyCommands(data.get(i));
					duration[i][2]+=System.nanoTime()-start;
					counter[i][0]++;
					newSet.putQuick(i, true);
					
				} else {
					duration[i][0]+=System.nanoTime()-start;
					counter[i][1]++;
				}
			}
			return;
		} 
		
		
		for (int i=0; i<features.size(); i++) {
			
			GenomicRegionFeature<?> f = features.get(i);
			
			boolean changed = f.setGenomicRegion(rgr.getReference(), rgr.getRegion());
			
			if (changed || f.dependsOnData() || f.hasCondition() || isAnySet(newSet,inputs.get(i))) { 
				f.setData(rgr.getData());
				
				int[] inp = inputs.get(i);
				for (int j = 0; j < inp.length; j++) 
					f.setInput(j, data.get(inp[j]));
				
				data.get(i).clear();
				
				f.accept(data.get(i));
				
				if (!data.get(i).isEmpty())
					f.applyCommands(data.get(i));
				
				newSet.putQuick(i, true);
			}
		}
	}
	

	private final static boolean isAnySet(BitVector bv, int[] ind) {
		for (int i : ind)
			if (bv.getQuick(i)) return true;
		return false;
	}

	public long[][] getDuration() {
		return duration;
	}
	
	public void printBenchmark() {
		for (int i=0; i<features.size(); i++)
			System.out.println(features.get(i).getId()+"\t"+
					counter[i][0]+" / "+counter[i][1]+"\t"+
					StringUtils.getHumanReadableTimespan(duration[i][0]/1000000L)+"\t"+
					StringUtils.getHumanReadableTimespan(duration[i][1]/1000000L)+"\t"+
					StringUtils.getHumanReadableTimespan(duration[i][2]/1000000L)+"\t"+
					StringUtils.getHumanReadableTimespan((duration[i][0]+duration[i][1]+duration[i][2])/1000000L));
	}

	public boolean isRunning() {
		return running;
	}

	
	
	
}
