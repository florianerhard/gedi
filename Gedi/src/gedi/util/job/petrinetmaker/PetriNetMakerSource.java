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
