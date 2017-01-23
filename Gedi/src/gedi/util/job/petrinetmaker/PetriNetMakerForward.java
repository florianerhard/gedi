package gedi.util.job.petrinetmaker;

import gedi.util.job.Job;
import gedi.util.job.PetriNet;
import gedi.util.job.Place;
import gedi.util.job.Transition;

public class PetriNetMakerForward<FROM,TO> {

	PetriNet pn;
	Place[] currentPlaces;
	
	public PetriNetMakerForward(PetriNet pn, Place[] currentPlaces) {
		this.pn = pn;
		this.currentPlaces = currentPlaces;
	}

	public PetriNetMaker set(Job<TO> job) {
		
		Class[] incls = job.getInputClasses();
		if (currentPlaces.length!=incls.length)
			throw new IllegalStateException("Input number inconsistent!");
		
		Transition t = pn.createTransition(job);
		for (int i=0; i<incls.length; i++) {
			pn.connect(currentPlaces[i], t, i);
		}
		
		Place out = pn.createPlace(job.getOutputClass());
		pn.connect(t, out);
		
		return new PetriNetMaker(pn,out);
	}
	
}
