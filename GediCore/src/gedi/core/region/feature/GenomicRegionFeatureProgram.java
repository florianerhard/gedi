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
package gedi.core.region.feature;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.feature.special.ProcessRegionPredicate;
import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.userInteraction.results.ResultProducer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Logger;

import cern.colt.bitvector.BitVector;

@SuppressWarnings({"rawtypes","unchecked"})
public class GenomicRegionFeatureProgram<D> implements Consumer<ReferenceGenomicRegion<D>> {

	public static final Logger log = Logger.getLogger( GenomicRegionFeatureProgram.class.getName() );
	
	
	private HashMap<String,Integer> idMap = new HashMap<String, Integer>();   
			
	private ArrayList<int[]> inputs = new ArrayList<int[]>();
	private ArrayList<GenomicRegionFeature<?>> features = new ArrayList<GenomicRegionFeature<?>>();
	

	private String[] labels = null;
	
	private int threads = Runtime.getRuntime().availableProcessors();
	
	
	private long intermediateInterval = 0;//1000*60*5;
	
	private boolean checkSorting = false;
	
	private BiFunction<D,NumericArray,NumericArray> dataToCounts = (a,b)->{
		if (a instanceof NumericArray) return (NumericArray)a;
		if (b==null || b.length()!=1) b = NumericArray.createMemory(1, NumericArrayType.Integer);
		b.setInt(0, 1);
		return b;
	};
	
	public void setCheckSorting(boolean checkSorting) {
		this.checkSorting = checkSorting;
	}
	
	public int getThreads() {
		return threads;
	}

	/**
	 * 0 is single-threaded
	 * @param threads
	 */
	public void setThreads(int threads) {
		this.threads = threads;
	}
	
	public ArrayList<ResultProducer> getResultProducers() {
		ArrayList<ResultProducer> re = new ArrayList<ResultProducer>();
		for (GenomicRegionFeature<?> f : features)
			f.addResultProducers(re);
		return re;
	}
	
	public void setConditionFile(String file) throws IOException {
		this.labels = new LineOrientedFile(file).lineIterator().skip(1).map(s->StringUtils.splitField(s, '\t', 1)).toArray(new String[0]);
	}
	
	public void setLabelsFromStorage(GenomicRegionStorage<?> st) {
		setLabels(st.getMetaDataConditions());
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
	
	public Set getInputById(String id) {
		Integer index = idMap.get(id);
		if (index==null) return null;
		
		ArrayList<Set> data = threads==0?this.data:((Runner)Thread.currentThread()).data;
		 
		return data.get(index);
	}
	
	public <I> I getUniqueInputById(String name, I notUnique) {
		Set in = getInputById(name);
		if (in.size()==1) return (I) in.iterator().next();
		return notUnique;
	}
	
	/**
	 * Gets either the parental feature object, or the thread specific, if called from a Runner thread.
	 * @param index
	 * @return
	 */
	public <T> GenomicRegionFeature<T> getFeature(int index) {
		if (Thread.currentThread() instanceof Runner) {
			Runner r = (Runner) Thread.currentThread();
			return (GenomicRegionFeature<T>)r.features.get(index);
		}
		return (GenomicRegionFeature<T>) features.get(index);
	}
	
	public ExtendedIterator<GenomicRegionFeature<?>> getFeatures() {
		return EI.seq(0,getNumFeatures()).map(i->getFeature(i));
	}
	
	public int getNumFeatures() {
		return features.size();
	}
	
	public <T> GenomicRegionFeature<T> getFeature(String id) {
		return getFeature(idMap.get(id));
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
		
		feature.setProgram(this);
		feature.setInputNames(inputs);
		this.inputs.add(inpIndex);
		features.add(feature);
		data.add(new HashSet());
	}
	
	private boolean running = false;
	
	private static class Runner extends Thread{
		private BitVector newSet;
		private ArrayList<Set> data = new ArrayList<Set>();
		private ArrayList<GenomicRegionFeature<?>> features = new ArrayList<GenomicRegionFeature<?>>();
		private ArrayList<int[]> inputs;
		private Throwable exception;
		
		private ReferenceGenomicRegion[] buffer = new ReferenceGenomicRegion[0];
		private ReferenceGenomicRegion[] tasks;
		private LinkedBlockingQueue<Runner> freeRunner;
		private Benchmark benchmark;
		
		public Runner(int n, Benchmark benchmark, LinkedBlockingQueue<Runner> freeRunner, ArrayList<GenomicRegionFeature<?>> features, ArrayList<int[]> inputs) {
			super("RunnerThread"+n);
			this.freeRunner = freeRunner;
			this.inputs = inputs;
			this.benchmark = benchmark;
			setDaemon(true);
			for (GenomicRegionFeature<?> f : features) {
				this.features.add(f.copy());
				this.data.add(new HashSet());
			}
		}
		
		@Override
		public void run() {
			try {
				for (GenomicRegionFeature<?> f : features)
					f.begin();
				
				synchronized (this) {
					while (!isInterrupted()) {
						
						try {
							freeRunner.add(this);
							wait();
						} catch (InterruptedException e) {
							break;
						}
						if (tasks == null) break;
						
							for (ReferenceGenomicRegion rgr : tasks) {
								if (rgr==null) break;
								process(benchmark,features, inputs, newSet, data, rgr);
							}
	
							tasks = null;
					}
				}
				
				
				for (GenomicRegionFeature<?> f : features)
					f.end();
			} catch (Throwable e) {
				this.exception = e;
				freeRunner.add(this);
			}
		}
		
		public synchronized void shutdown() {
			if (this.tasks!=null) throw new RuntimeException("Concurrency problem!");
			notify();
		}
		
		
		public synchronized void setTasks(ArrayList<ReferenceGenomicRegion<?>> tasks) {
			if (this.tasks!=null) throw new RuntimeException("Concurrency problem!");
			this.buffer = this.tasks = tasks.toArray(this.buffer);
			notify();
		}

		
	}

	private Runner[] runners;
	private GenomicRegionFeature[][] runnerFeatures;
	
	private BitVector newSet;
	private ArrayList<Set> data = new ArrayList<Set>();
	public void begin() {
		lastIntermediate = System.currentTimeMillis();
		running = true;
		last = null;
		
		if (threads==0) {
//			log.info("Executing program in single-thread mode.");
			for (GenomicRegionFeature<?> f : features)
				f.begin();
		} else {
//			log.info("Executing program in multi-thread mode with "+threads+" threads");
			runners = new Runner[threads];
			for (int i=0; i<runners.length; i++) 
				runners[i] = new Runner(i,benchmark==null?null:benchmark.sub(), freeRunner,features, inputs);
			
			runnerFeatures = new GenomicRegionFeature[features.size()][threads];
			for (int i = 0; i < runnerFeatures.length; i++) 
				for (int j = 0; j < runnerFeatures[i].length; j++) 
					runnerFeatures[i][j] = runners[j].features.get(i);
			
			for (int i=0; i<runners.length; i++)
				runners[i].start();
			
		}
	}
	
	/**
	 * Gets the current data for the given id
	 * @param id
	 * @return
	 */
	public Set getData(String id) {
		Integer key = idMap.get(id);
		if (key==null) throw new RuntimeException("Key "+id+" unknown!");
		return data.get(key);
	}
	
	/**
	 * Gets the current data for the given id; throws an exception if there are more or less than 1 elements available
	 * @param id
	 * @return
	 */
	public Object getDataAsSingleton(String id) {
		Set d = getData(id);
		if (d.size()!=1) throw new RuntimeException("Singleton expected for "+id);
		return d.iterator().next();
	}
	
	/**
	 * Gets the current data for the given id; throws an exception if there are more than 1 elements available and returns the default value
	 * if there is none.
	 * @param id
	 * @param defaultValue
	 * @return
	 */
	public Object getDataAsSingleton(String id, Object defaultValue) {
		Set d = getData(id);
		if (d.size()==0) return defaultValue;
		if (d.size()!=1) throw new RuntimeException("Singleton expected for "+id);
		return d.iterator().next();
	}
	
	public void end() {
		try {
			intermediateThread.shutdown();
			intermediateThread.awaitTermination(1, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
		}
		if (threads==0) {
			for (GenomicRegionFeature<?> f : features) 
				f.end();
			running = false;
			produceResults();
		} else {
			
			try {
				Runner runner = freeRunner.take();
				runner.setTasks(block);
				block.clear();
				
				boolean done = false;
				while (!done) {
					done = true;
					for (Runner r : runners) {
						if (r.tasks!=null)
							done = false;
					}
					Thread.yield();
				}

			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			
			for (int i=0; i<runners.length; i++) {
				runners[i].shutdown();
			}
			
			// wait until all threads die
			int alives;
			do {
				alives = 0;
				for (int i=0; i<runners.length; i++) {
					if (runners[i].isAlive())
						alives++;
				}
				Thread.yield();
			} while (alives>0);
			
			for (int i=0; i<runners.length; i++) 
				if (runners[i].exception!=null) throw new RuntimeException("Exception occurred during processing!",runners[i].exception);
			
			
			running = false;
			produceResults();
		}
	}
	
	private void produceResults() {
		if (threads==0) {
			for (GenomicRegionFeature<?> f : features) 
				f.produceResults(null);
		}
		else {
			
			for (GenomicRegionFeature<?> f : features) {
				f.end();
			}
			ExecutorService pool = Executors.newFixedThreadPool(threads);
			CountDownLatch latch = new CountDownLatch(features.size());
			for (int i=0; i<features.size(); i++)  {
				int ui = i;
				pool.execute(()->{
					features.get(ui).produceResults(runnerFeatures[ui]);
					latch.countDown();
				});
			}
			pool.shutdown();
			try {
				latch.await();
			} catch (InterruptedException e) {
			}
		}
	}
	
	private int blockSize = 1024;
	private ArrayList<ReferenceGenomicRegion<?>> block = new ArrayList<ReferenceGenomicRegion<?>>();
	private LinkedBlockingQueue<Runner> freeRunner = new LinkedBlockingQueue<Runner>();
	
	private ReferenceGenomicRegion<D> last = null;
	
	public void accept(ReferenceGenomicRegion<D> rgr) {
		if (!running) throw new RuntimeException("Call begin first!");
		
		if (checkSorting) {
			if (last!=null && last.compareTo(rgr)>0) {
				throw new RuntimeException("Input not sorted: "+last+" > "+rgr);
			}
			last = rgr.toImmutable();
		}
		
		if (intermediateInterval>0 && System.currentTimeMillis()>lastIntermediate+intermediateInterval) {
			lastIntermediate = Long.MAX_VALUE-intermediateInterval;
			intermediateThread.execute(()->{
					produceResults();
					lastIntermediate = System.currentTimeMillis();
				}
			);
		}
		
		if (threads==0) {
			process(benchmark,features,inputs,newSet,data,rgr);
		} else {
			if (block.size()==blockSize) { 
				try {
//					long start = System.nanoTime();
					Runner runner = freeRunner.take();
					if (runner.exception!=null) throw new RuntimeException("Exception occurred during processing!",runner.exception);
					runner.setTasks(block);
					block.clear();

				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
			block.add(rgr.toImmutable());
		}
		
	}
	
	
	private long lastIntermediate = 0;
	
	private ExecutorService intermediateThread = Executors.newFixedThreadPool(1, new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        }
    });


	private Benchmark benchmark;

	private static void process(Benchmark benchmark, ArrayList<GenomicRegionFeature<?>> features, ArrayList<int[]> inputs, BitVector newSet, ArrayList<Set> data, ReferenceGenomicRegion<?> rgr) {
		
		
		if (newSet==null || newSet.size()!=features.size()) newSet = new BitVector(features.size());
		newSet.clear();
		
		
		if (benchmark!=null) {
			if (benchmark.duration==null) benchmark.duration = new long[features.size()][3];
			if (benchmark.counter==null) benchmark.counter = new long[features.size()][2];
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
					benchmark.duration[i][0]+=System.nanoTime()-start;
					
					start = System.nanoTime();
					data.get(i).clear();
					f.accept(data.get(i));
					benchmark.duration[i][1]+=System.nanoTime()-start;
					
					start = System.nanoTime();
					if (!data.get(i).isEmpty())
						f.applyCommands(data.get(i));
					benchmark.duration[i][2]+=System.nanoTime()-start;
					benchmark.counter[i][0]++;
					newSet.putQuick(i, true);
					
					if (f instanceof ProcessRegionPredicate && !(Boolean)data.get(i).iterator().next())
						return;
				} else {
					benchmark.duration[i][0]+=System.nanoTime()-start;
					benchmark.counter[i][1]++;
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
				
				if (f instanceof ProcessRegionPredicate && !(Boolean)data.get(i).iterator().next())
					return;
			}
		}
	}
	

	private final static boolean isAnySet(BitVector bv, int[] ind) {
		for (int i : ind)
			if (bv.getQuick(i)) return true;
		return false;
	}

	public boolean isRunning() {
		return running;
	}
	
	private static class Benchmark {
		private long[][] duration = null;
		private long[][] counter = null;
		private ArrayList<Benchmark> subs = new ArrayList<Benchmark>();
		public Benchmark sub() {
			Benchmark re = new Benchmark();
			subs.add(re);
			return re;
		}
		public long[][] getDuration() {
			if (duration!=null) return duration;
			
			long[][] duration = null;
			for (Benchmark b : subs) {
				if (duration==null) duration = new long[b.duration.length][3];
				for (int i=0;i<duration.length; i++)
					ArrayUtils.add(duration[i], b.duration[i]);
			}
			return duration;
		}
		public long[][] getCounter() {
			if (counter!=null) return counter;
			
			long[][] counter = null;
			for (Benchmark b : subs) {
				if (counter==null) counter = new long[b.counter.length][2];
				for (int i=0;i<counter.length; i++)
					ArrayUtils.add(counter[i], b.counter[i]);
			}
			return counter;
		}
		
	}

	public void printBenchmark() {
		if (benchmark==null) return;
		long[][] counter = benchmark.getCounter();
		long[][] duration = benchmark.getDuration();
		System.out.println("Id\tCalled/Cached\tsetInput\tAccept\tPostprocess\tTotal");
		for (int i=0; i<features.size(); i++)
			System.out.println(features.get(i).getId()+"\t"+
					counter[i][0]+" / "+counter[i][1]+"\t"+
					StringUtils.getHumanReadableTimespan(duration[i][0]/1000000L)+"\t"+
					StringUtils.getHumanReadableTimespan(duration[i][1]/1000000L)+"\t"+
					StringUtils.getHumanReadableTimespan(duration[i][2]/1000000L)+"\t"+
					StringUtils.getHumanReadableTimespan((duration[i][0]+duration[i][1]+duration[i][2])/1000000L));
	}
	
	public void setBenchmark(boolean benchmark) {
		this.benchmark = benchmark?new Benchmark():null;
	}
	
	
}
