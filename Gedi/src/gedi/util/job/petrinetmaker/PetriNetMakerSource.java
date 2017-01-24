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

package gedi.util.job.petrinetmaker;

import gedi.util.job.Job;
import gedi.util.job.PetriNet;
import gedi.util.job.Place;
import gedi.util.job.Transition;

public class PetriNetMakerSource<T> {

	private PetriNet pn;
	
	public PetriNetMakerSource(PetriNet pn) {
		this.pn = pn;
	}

	public PetriNetMaker set(Job<T> job) {
		Transition t = pn.createTransition(job);
		Class[] incls = job.getInputClasses();
		for (int i=0; i<incls.length; i++) {
			Place in = pn.createPlace(incls[i]);
			pn.connect(in, t, i);
		}
		
		Place out = pn.createPlace(job.getOutputClass());
		pn.connect(t, out);
		
		return new PetriNetMaker(pn,out);
	}
	
}
