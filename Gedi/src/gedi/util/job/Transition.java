package gedi.util.job;

import gedi.util.ArrayUtils;

import java.util.Collection;

public class Transition {

	private int id;
	private Place[] inPlaces;
	private Place outPlace;
	private Job job;
	
	Transition(Job job, int id) {
		this.job = job;
		this.id = id;
		inPlaces = new Place[job.getInputClasses().length];
		if (inPlaces.length==0) throw new RuntimeException("Must have an input; supply Void!");
	}
	
	
	void createMissingPlaces(PetriNet pn) {
		for (int i=0; i<inPlaces.length; i++)
			if (inPlaces[i]==null)
				pn.connect(pn.createPlace(job.getInputClasses()[i]), this, i);
		
		if (outPlace==null)
			pn.connect(this,pn.createPlace(job.getOutputClass()));
	}
	
	void setOutPlace(Place p) {
		if (!p.getTokenClass().isAssignableFrom(job.getOutputClass()) && job.getOutputClass()!=Object.class)
			// dynamic binding for Object
			throw new IllegalArgumentException("Classes not consistent!");
		if (outPlace!=null)
			throw new RuntimeException("Already set!");
		outPlace = p;
	}
	
	public int getId() {
		return id;
	}
	
	void setInPlace(Place p, int index) {
		if (!job.getInputClasses()[index].isAssignableFrom(p.getTokenClass()) && p.getTokenClass()!=Object.class)
			throw new IllegalArgumentException("Classes not consistent ("+index+"): Place "+p.getId()+" has "+p.getTokenClass()+" Transition "+getId()+" needs "+job.getInputClasses()[index]);
		if (inPlaces[index]!=null)
			throw new RuntimeException("Already set!");
		inPlaces[index] = p;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (job.getId()!=null)
			sb.append(job.getId()+"("+id+"):[");
		else
			sb.append(id+":[");
		for (int i=0; i<job.getInputClasses().length; i++) {
			if (i>0) sb.append(",");
			sb.append(inPlaces[i]);
		}
		sb.append("]->[");
		sb.append(outPlace);
		sb.append("]");
		return sb.toString();
	}
	
	public Job getJob() {
		return job;
	}
	
	public int getInDegree() {
		return inPlaces.length;
	}
	
	public Place getOutput() {
		return outPlace;
	}
	
	public Place getInput(int index) {
		return inPlaces[index];
	}
	
	public int getInIndex(Place p) {
		return ArrayUtils.find(inPlaces, p);
	}

	public <C extends Collection<Place>> C getInputs(C re) {
		for (int i=0; i<getInDegree(); i++)
			re.add(getInput(i));
		return re;
	}
	
}
