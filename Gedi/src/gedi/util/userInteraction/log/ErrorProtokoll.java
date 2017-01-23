package gedi.util.userInteraction.log;

public interface ErrorProtokoll {

	
	default void addError(String error) {
		addError(error,null);
	}
	
	default void addError(String errorType, Object object) {
		if (object!=null)
			addError(errorType, object, errorType+": "+object);
		else
			addError(errorType, object, errorType);
	}
	
	void addError(String errorType, Object object, String message); 
	
}
