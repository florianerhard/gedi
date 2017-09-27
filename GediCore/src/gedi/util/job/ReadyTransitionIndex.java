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
