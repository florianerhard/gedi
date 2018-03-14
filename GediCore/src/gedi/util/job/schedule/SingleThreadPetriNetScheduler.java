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

import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.datastructure.collections.longcollections.LongArrayList;
import gedi.util.job.ExecutionContext;
import gedi.util.job.FireTransition;
import gedi.util.job.Place;
import gedi.util.job.Transition;
import gedi.util.math.stat.RandomNumbers;
import gedi.util.mutable.MutableLong;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SingleThreadPetriNetScheduler implements PetriNetScheduler {

	private static final Logger log = Logger.getLogger( PetriNetScheduler.class.getName() );
	
	protected ExecutionContext context;
	protected Consumer<ExecutionContext> finishAction;
	
	private boolean logging = true;
	private ArrayList<PetriNetListener> listeners = new ArrayList<PetriNetListener>();
	
	public SingleThreadPetriNetScheduler(ExecutionContext context) {
		this.context = context;
	}
	
	
	@Override
	public void addListener(PetriNetListener rg) {
		listeners.add(rg);
	}
	
	@Override
	public void removeListener(PetriNetListener rg) {
		listeners.remove(rg);
	}
	
	@Override
	public void setFinishAction(Consumer<ExecutionContext> finishAction) {
		this.finishAction = finishAction;
	}
	
	public void setLogging(boolean logging) {
		this.logging = logging;
	}

	@Override
	public void run() {
		
		try {
			int eid = context.startExecution();
			
			PetriNetEvent event = new PetriNetEvent(eid, context);
			
			for (PetriNetListener l : listeners)
				l.petriNetExecutionStarted(event);
			
			long start = System.nanoTime();
			
			MutableLong total = new MutableLong();
			
			HashSet<Transition> ready = new HashSet<Transition>();
			for (Place p : context.getPetrNet().getSources())
				addReadyConsumers(p, ready);
			ArrayList<Transition> iter = new ArrayList<Transition>();
			
			HashSet<Future<FireTransition>> futures = new HashSet<Future<FireTransition>>();
			if (Thread.interrupted() || context.getExecutionId()!=eid) {
				if (logging) log.log(Level.INFO,"Canceled execution "+eid);
				for (PetriNetListener l : listeners)
					l.petriNetExecutionCancelled(event);
				return;
			}
						
			if (logging) log.log(Level.INFO,"Start executing Petri net (id="+eid+") Disabled: "+context.getDisabledTransitions());
			
			while (!ready.isEmpty()) {
				
				iter.clear();
				iter.addAll(ready);
				ready.clear();
				
				for (Transition n : iter) {
					if (Thread.interrupted() || context.getExecutionId()!=eid) {
						for (Future<FireTransition> f : futures)
							f.cancel(true);
						if (logging) log.log(Level.INFO,"Canceled execution "+eid);
						for (PetriNetListener l : listeners)
							l.petriNetExecutionCancelled(event);
						return;
					}
					
					if (logging) log.log(Level.INFO,"Submitting "+n+" (id="+eid+") "+context);
					FireTransition t = new FireTransition(n, eid, context, ft->{
						if (ft.getException()!=null) {
							StringWriter exmsg = new StringWriter();
							ft.getException().printStackTrace(new PrintWriter(exmsg));
							log.log(Level.SEVERE,"Exception in "+n+" (id="+eid+"):"+exmsg.toString());
						} else if (ft.isValidExecution()) {
							context.putToken(n.getOutput(), ft.getResult());
							if (logging) log.log(Level.INFO,"Finished "+n+" (id="+eid+") after "+ft.getTime()+"ns");
							total.N+=ft.getTime();
							addReadyConsumers(n.getOutput(), ready);
						}
					});
					t.call();
				}
			}
			
			
			if (logging) log.log(Level.INFO,String.format("Finished executing Petri net (id="+eid+") in %d ns, total time in transitions: %d ns\n",System.nanoTime()-start,total.N));
			
			for (PetriNetListener l : listeners)
				l.petriNetExecutionFinished(event);
			
			if (finishAction!=null)
				finishAction.accept(context);
			
		} catch (Throwable e) {
			log.log(Level.SEVERE,"Uncaught exception!",e);
			
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

	
	
	
}
