package gedi.util.job.schedule;

public interface PetriNetListener {


	void petriNetExecutionStarted(PetriNetEvent event);
	void petriNetExecutionFinished(PetriNetEvent event);
	void petriNetExecutionCancelled(PetriNetEvent event);
	
	
	
}
