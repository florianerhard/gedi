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
package gedi.gui.genovis;

import gedi.core.data.mapper.GenomicRegionDataMappingJob;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.util.gui.PixelBasepairMapper;
import gedi.util.job.ExecutionContext;
import gedi.util.job.PetriNet;
import gedi.util.job.Place;
import gedi.util.job.Transition;
import gedi.util.job.schedule.DefaultPetriNetScheduler;
import gedi.util.job.schedule.MetaDataPetriNetScheduler;
import gedi.util.job.schedule.PetriNetListener;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

public class TracksDataManager {

	private static final Logger log = Logger.getLogger( TracksDataManager.class.getName() );
	
	private ExecutorService pool;
//	private ExecutorService pool = Executors.newCachedThreadPool();
	private PetriNet dataPipeline = new PetriNet();
//	private ExecutionContext context;
	
//	private PetriNetScheduler scheduler;
	private LinkedBlockingQueue<Future<?>> currentSchedulers = new LinkedBlockingQueue<Future<?>>();
	
	private long hysteresis = 200;
	
	public TracksDataManager(PetriNet dataPipeline) {
		this.dataPipeline = dataPipeline;
		
		int threads = Math.max(Runtime.getRuntime().availableProcessors()-1, 2);
		pool = Executors.newCachedThreadPool(new TracksThreadFactory());//Executors.newFixedThreadPool(threads,new TracksThreadFactory());
		
		log.fine("Created thread pool (n="+threads+") for tracks visualization.");
		
//		context = dataPipeline.createExecutionContext()
//			.newContext(GenomicRegionDataMappingJob.REFERENCE, ReferenceSequence.class)
//			.newContext(GenomicRegionDataMappingJob.REGION, GenomicRegion.class)
//			.newContext(GenomicRegionDataMappingJob.PIXELMAPPING, PixelLocationMapping.class);
//		scheduler = new DefaultPetriNetScheduler(context, pool);
		
		setupMetadata();
	}
	
	private void setupMetadata() {
		ExecutionContext context = dataPipeline.createExecutionContext()
				.newContext(ExecutionContext.UID, String.class);
		MetaDataPetriNetScheduler scheduler = new MetaDataPetriNetScheduler(context);
		
		context.reset();
		scheduler.run();
	}

	public void setHysteresis(long hysteresis) {
		this.hysteresis = hysteresis;
	}
	
	public long getHysteresis() {
		return hysteresis;
	}
	
	public synchronized void setLocation(PixelBasepairMapper xmapper, ReferenceSequence reference, GenomicRegion region, Consumer<ExecutionContext> finishAction) {
		setLocation(xmapper, reference, region, finishAction, null);
	}
	
	
	public synchronized void setLocation(PixelBasepairMapper xmapper, ReferenceSequence reference, GenomicRegion region, Consumer<ExecutionContext> finishAction, BiConsumer<ExecutionContext, Place> newTokenObserver) {
		setLocation(xmapper, new ReferenceSequence[] {reference}, new GenomicRegion[] {region}, finishAction, newTokenObserver);
	}
	
	public synchronized void setLocation(PixelBasepairMapper xmapper, ReferenceSequence[] reference, GenomicRegion[] region, Consumer<ExecutionContext> finishAction) {
		setLocation(xmapper, reference, region, finishAction, null);
	}
	public synchronized void setLocation(PixelBasepairMapper xmapper, ReferenceSequence[] reference, GenomicRegion[] region, Consumer<ExecutionContext> finishAction, BiConsumer<ExecutionContext, Place> newTokenObserver) {
		if (!currentSchedulers.isEmpty()) {
			for (Future<?> f : currentSchedulers) {
				f.cancel(false);
			}
			currentSchedulers.clear();
			log.fine("Cancelling all running threads.");
		}
		
		log.fine("Start scheduling jobs (Thread="+Thread.currentThread().getName()+")");
		
		currentSchedulers.add(pool.submit(new Runnable() {
			
			@Override
			public void run() {
				
				try {

					if (hysteresis>0)
						try {
							Thread.sleep(hysteresis);
						} catch (InterruptedException e) {
							log.fine("Skip execution due to cancelling (Thread="+Thread.currentThread().getName()+")");
							return;
						}
					
					for (int i=0; i<reference.length; i++) {
						ExecutionContext context = dataPipeline.createExecutionContext()
								.newContext(ExecutionContext.UID, String.class)
								.newContext(GenomicRegionDataMappingJob.REFERENCE, ReferenceSequence.class)
								.newContext(GenomicRegionDataMappingJob.REGION, GenomicRegion.class)
								.newContext(GenomicRegionDataMappingJob.PIXELMAPPING, PixelLocationMapping.class);
						DefaultPetriNetScheduler scheduler = new DefaultPetriNetScheduler(context, pool);
						for (PetriNetListener l : listeners)
							scheduler.addListener(l);
						
						context.reset();
						context.setContext(GenomicRegionDataMappingJob.REFERENCE, reference[i]);
						context.setContext(GenomicRegionDataMappingJob.REGION, region[i]);
						context.setContext(GenomicRegionDataMappingJob.PIXELMAPPING, new PixelLocationMapping(reference[i], xmapper));
				
						// find all not visible tracks
				//		for (Transition t : dataPipeline.getTransitions()) {
				//			if (t.getJob() instanceof GenomicRegionDataMappingJob) {
				//				GenomicRegionDataMappingJob j = (GenomicRegionDataMappingJob) t.getJob();
				//				if (j.getMapper() instanceof SequenceTrack) {
				//					SequenceTrack tr = (SequenceTrack) j.getMapper();
				//					if (!tr.isVisible(reference, region))
				//						context.setDisabled(t, true);
				//				}
				//			}
				//		}
						for (Transition t : dataPipeline.getTransitions()) {
							if (t.getJob().isDisabled(context))
								context.setDisabled(t, true);
							
						}
						context.disableUnneccessary();
						
						log.fine("Disabled Transition: "+context.getDisabledTransitions().size()+"/"+ context.getPetrNet().getTransitions().size()+" (Thread="+Thread.currentThread().getName()+")");
						
						scheduler.setNewTokenAction(newTokenObserver);
						scheduler.setFinishAction(finishAction);
						log.fine("Scheduling job for reference "+reference[i]+" ("+(i+1)+"/"+reference.length+" Thread="+Thread.currentThread().getName()+")");
						scheduler.run();
//						currentSchedulers.add(pool.submit(scheduler));
					}
					
				} catch (Throwable t) {
					t.printStackTrace();
					throw new RuntimeException(t);
				}
			}
			
		}));
		
	}
		
	public PetriNet getDataPipeline() {
		return dataPipeline;
	}

	@Override
	public String toString() {
		return dataPipeline.toString();
	}
	
	
	private ArrayList<PetriNetListener> listeners = new ArrayList<PetriNetListener>();

	public void addListener(PetriNetListener rg) {
		listeners.add(rg);
	}
	
	public void removeListener(PetriNetListener rg) {
		listeners.remove(rg);
	}

	
	private static class TracksThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        TracksThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                                  Thread.currentThread().getThreadGroup();
            namePrefix = "TRACKS-" +
                          poolNumber.getAndIncrement() +
                         "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                                  namePrefix + threadNumber.getAndIncrement(),
                                  0);
            t.setDaemon(true);
            t.setPriority(3);
            return t;
        }
    }

}
