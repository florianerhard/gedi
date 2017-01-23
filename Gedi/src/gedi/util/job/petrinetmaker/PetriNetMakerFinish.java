package gedi.util.job.petrinetmaker;

import gedi.util.job.Job;
import gedi.util.job.PetriNet;
import gedi.util.job.Place;

public class PetriNetMakerFinish<FROM> extends PetriNetMakerForward<FROM, Void> {

	
	public PetriNetMakerFinish(PetriNet pn, Place[] currentPlaces) {
		super(pn,currentPlaces);
	}

	public PetriNetMaker set(Job<Void> job) {
		PetriNetMaker re = super.set(job);
		pn.createMissingPlaces();
		return re;
	}
	
}
