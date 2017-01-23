package gedi.util.job;

import gedi.util.mutable.MutableTuple;


/**
 * Needs to be stateless!
 * @author erhard
 *
 * @param <T>
 */
public interface Job<T> {

	Class[] getInputClasses();
	Class<T> getOutputClass();
	
	boolean isDisabled();
	
	T execute(ExecutionContext context,MutableTuple input);
	String getId();
	default void setInput(int i, Job job) {}
	default void addOutput(Job job) {}
	
}
