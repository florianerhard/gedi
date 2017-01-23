package gedi.util.userInteraction.log;

import gedi.util.mutable.MutablePair;

import java.util.HashSet;

public abstract class UniqueErrorProtokoll implements ErrorProtokoll {

	private HashSet<MutablePair<String,Object>> occurred = new HashSet<MutablePair<String,Object>>();
	private boolean makeUnique;
	
	public UniqueErrorProtokoll(boolean makeUnique) {
		this.makeUnique = makeUnique;
	}
	
	@Override
	public void addError(String errorType, Object object, String message) {
		if (!makeUnique || occurred.add(new MutablePair<String, Object>(errorType,object)))
			report(errorType,object,message);
		
	}
	
	public boolean isMakeUnique() {
		return makeUnique;
	}

	public void setMakeUnique(boolean makeUnique) {
		this.makeUnique = makeUnique;
	}

	protected abstract void report(String errorType, Object object, String message) ;

}
