package gedi.util.job.petrinetmaker;

import gedi.util.job.Job;
import gedi.util.job.PetriNet;
import gedi.util.job.Place;
import gedi.util.job.Transition;

public class PetriNetMaker {

	private PetriNet pn;
	private Place[] currentPlaces;
	
	public PetriNetMaker(PetriNet pn) {
		this.pn = pn;
	}
	
	PetriNetMaker(PetriNet pn, Place...currentPlaces) {
		this.pn = pn;
		this.currentPlaces = currentPlaces;
	}
	
	
	public <T> PetriNetMakerSource<T> start() {
		if (currentPlaces!=null) throw new IllegalStateException();
		return new PetriNetMakerSource<T>(pn);
		
	}
	
	public <FROM,TO> PetriNetMakerForward<FROM,TO> next() {
		if (currentPlaces==null) throw new IllegalStateException();
		return new PetriNetMakerForward<FROM,TO>(pn,currentPlaces);
	}
	
	
	public <FROM> PetriNetMakerForward<FROM,Void> finish() {
		if (currentPlaces==null) throw new IllegalStateException();
		return new PetriNetMakerFinish<FROM>(pn,currentPlaces);
	}

	public <FROM,TO> PetriNetMakerForward<FROM,TO> merge(PetriNetMaker... others) {
		if (currentPlaces==null) throw new IllegalStateException();
		int l = currentPlaces.length;
		for (PetriNetMaker o : others)
			if (o.currentPlaces==null) throw new IllegalStateException();
			else l+=o.currentPlaces.length;
		
		Place[] c = new Place[l];
		l=0;
		System.arraycopy(currentPlaces, 0, c, l, currentPlaces.length);
		l+=currentPlaces.length;
		for (PetriNetMaker o : others)
		{
			System.arraycopy(o.currentPlaces, 0, c, l, o.currentPlaces.length);
			l+=o.currentPlaces.length;
		}
		return new PetriNetMakerForward<FROM, TO>(pn, c);
		
	}
	
	
}