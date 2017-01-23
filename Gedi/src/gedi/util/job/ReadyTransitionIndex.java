package gedi.util.job;

import gedi.util.mutable.MutableTuple;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import cern.colt.bitvector.BitVector;

/**
 * Ready transition: all inputs available, at least one output missing or has not fired yet (for sink transitions)
 * @author erhard
 *
 */
public class ReadyTransitionIndex {

	private ExecutionContext context;
	private Collection<Transition> ready;
	

	
	public ReadyTransitionIndex(ExecutionContext context) {
		this.context = context;
		this.ready = Collections.synchronizedSet(new HashSet<Transition>());
		
		ready.clear();
		for (Transition t : context.getPetrNet().getTransitions())
			if (context.isReady(t))
				ready.add(t);
		
	}

	public Collection<Transition> getReady() {
		return ready;
	}

	/**
	 * Updates this set
	 * @param t
	 * @param result
	 */
	public void fired(Transition t) {
		Place outPlace = t.getOutput();
		// this output could have made a transition ready, so check all candidates
		for (Transition cons : outPlace.getConsumers()) {
			if (context.isReady(cons))
				ready.add(cons);
		}
		// it could also have made a transition unready
		if (ready.contains(outPlace.getProducer()) && !context.isReady(outPlace.getProducer())) 
			ready.remove(outPlace.getProducer());
	}
	
}
