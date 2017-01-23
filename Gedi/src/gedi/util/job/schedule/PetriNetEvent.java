package gedi.util.job.schedule;

import gedi.util.job.ExecutionContext;

public class PetriNetEvent {

	private int eid;
	private ExecutionContext context;
	
	public PetriNetEvent(int eid, ExecutionContext context) {
		this.eid = eid;
		this.context = context;
	}
	
	public int getEid() {
		return eid;
	}
	
	public ExecutionContext getContext() {
		return context;
	}
	
}
