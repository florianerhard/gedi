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
package gedi.util.job.schedule;

import gedi.util.GeneralUtils;
import gedi.util.StringUtils;
import gedi.util.job.ExecutionContext;
import gedi.util.job.FireTransition;
import gedi.util.job.Place;
import gedi.util.job.Transition;
import gedi.util.math.stat.RandomNumbers;
import gedi.util.mutable.MutableLong;
import gedi.util.mutable.MutableMonad;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DefaultPetriNetScheduler implements PetriNetScheduler {

	private static final Logger log = Logger.getLogger( PetriNetScheduler.class.getName() );
	
	protected ExecutionContext context;
	protected ExecutorService threadpool;
	protected Consumer<ExecutionContext> finishAction;
	protected BiConsumer<ExecutionContext, Place> newTokenObserver;
	
	private boolean rethrowExceptions = false;
	
//	private long hysteresis = 200;
	
	private boolean logging = true;
	private ArrayList<PetriNetListener> listeners = new ArrayList<PetriNetListener>();
	
	public DefaultPetriNetScheduler(ExecutionContext context, ExecutorService threadpool) {
		this.context = context;
		this.threadpool = threadpool; 
	}
	
	public void setRethrowExceptions(boolean rethrowExceptions) {
		this.rethrowExceptions = rethrowExceptions;
	}
	
	@Override
	public void addListener(PetriNetListener rg) {
		listeners.add(rg);
	}
	
	@Override
	public void removeListener(PetriNetListener rg) {
		listeners.remove(rg);
	}
	
	public void setFinishAction(Consumer<ExecutionContext> finishAction) {
		this.finishAction = finishAction;
	}
	
	public void setNewTokenAction(BiConsumer<ExecutionContext, Place> newTokenObserver) {
		this.newTokenObserver = newTokenObserver;
	}

	
	public void setLogging(boolean logging) {
		this.logging = logging;
	}

	private static RandomNumbers rnd = new RandomNumbers();
	
	@Override
	public void run() {
		
//		try {
//			Thread.sleep(hysteresis);
//		} catch (InterruptedException e) {
//			return;
//		}
		
		try {
			String uid;
			synchronized (rnd) {
				uid = StringUtils.sha1(rnd.getUnif()+"").substring(0, 8);	
			}
			context.setContext(ExecutionContext.UID,uid);
			
			int eid = context.startExecution();
			 
			
			PetriNetEvent event = new PetriNetEvent(eid, context);
			
			for (PetriNetListener l : listeners)
				l.petriNetExecutionStarted(event);
			
			// to protect ready and running!
			Object lock = new Object();
			
			long start = System.nanoTime();
			
			MutableLong total = new MutableLong();
			
			HashSet<Transition> ready = new HashSet<Transition>();
			for (Place p : context.getPetrNet().getSources())
				addReadyConsumers(p, ready);
			ArrayList<Transition> iter = new ArrayList<Transition>();
			
			HashSet<Transition> ran = new HashSet<Transition>();
			HashSet<Transition> runnings = new HashSet<Transition>();
			HashSet<Future<FireTransition>> futures = new HashSet<Future<FireTransition>>();
			if (Thread.interrupted() || context.getExecutionId()!=eid) {
				if (logging) log.log(Level.FINE,()->"Canceled execution "+uid);
				for (PetriNetListener l : listeners)
					l.petriNetExecutionCancelled(event);
				return;
			}
						
			if (logging) { 
				log.log(Level.FINE, ()->{
					StringBuilder sb = new StringBuilder();  
					Set<Transition> dis = context.getDisabledTransitions();
					for (Transition t : context.getPetrNet().getTransitions())
						if (!dis.contains(t))
							sb.append("\n ")
								.append(dis.contains(t)?"* ":"")
								.append(t.toString());
							
					return "Start executing Petri net (id="+uid+") "+Thread.currentThread().getName()+sb.toString();
				});
			}
			
			MutableMonad<Throwable> exception = new MutableMonad<>();
			while (!runnings.isEmpty() || !ready.isEmpty()) {
				
				if (Thread.interrupted() || context.getExecutionId()!=eid) {
					for (Future<FireTransition> f : futures)
						f.cancel(true);
					if (logging) log.log(Level.FINE,()->"Canceled execution "+uid);
					for (PetriNetListener l : listeners)
						l.petriNetExecutionCancelled(event);
					return;
				}
				iter.clear();
				synchronized (lock) {
					ready.removeAll(ran);
					iter.addAll(ready);
					ready.clear();
					if (exception.Item!=null)
						throw exception.Item;
				}
				for (Transition n : iter) {
					if (logging) log.log(Level.FINE,()->"Submitting "+n+" (id="+uid+") "+context);
					synchronized (lock) {
						futures.add(threadpool.submit(new FireTransition(n, eid, context, ft->{
							if (ft.getException()!=null) {
								StringWriter exmsg = new StringWriter();
								ft.getException().printStackTrace(new PrintWriter(exmsg));
								boolean interrupted = GeneralUtils.isCause(ft.getException(),InterruptedException.class);
								if (interrupted) {
									log.log(Level.FINE,"Exception in "+n+" (id="+uid+"):"+exmsg.toString());	
								} else {
									if (rethrowExceptions)
										exception.Item = ft.getException();
									else
										log.log(Level.SEVERE,"Exception in "+n+" (id="+uid+"):"+exmsg.toString());
								}
								
							} else if (ft.isValidExecution()) {
								context.putToken(n.getOutput(), ft.getResult());
								if (logging) log.log(Level.FINER,()->"Finished "+n+" (id="+uid+") after "+ft.getTime()+"ns");
								total.N+=ft.getTime();
								if (newTokenObserver!=null)
									newTokenObserver.accept(context, n.getOutput());
								synchronized (lock) {
									addReadyConsumers(n.getOutput(), ready);
									runnings.remove(n);
								}
							}
						})));
						runnings.add(n);
						ran.add(n);
					}
				}
			}
			
			
			if (logging) log.log(Level.FINE,()->String.format("Finished executing Petri net (id="+uid+") in %s, total time in transitions: %s",
					StringUtils.getHumanReadableTimespanNano(System.nanoTime()-start),StringUtils.getHumanReadableTimespanNano(total.N)));
			
			for (PetriNetListener l : listeners)
				l.petriNetExecutionFinished(event);
			
			if (logging) log.log(Level.FINER,()->String.format("Running finish action: %b",finishAction!=null));
			
			
			if (finishAction!=null)
				finishAction.accept(context);
			
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private void addReadyConsumers(Place p, HashSet<Transition> ready) {
		for (Transition t : p.getConsumers())
			if (!ready.contains(t) && context.isReady(t))
				ready.add(t);
	}


	@Override
	public ExecutionContext getExecutionContext() {
		return context;
	}

	public ExecutorService getExecutorService() {
		return threadpool;
	}


	
	
	
	
}
