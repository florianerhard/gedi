package gedi.util.job;

import gedi.util.mutable.MutableTuple;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class FireTransition implements Callable<FireTransition> {

	private Transition transition;
	private int execId;
	private ExecutionContext context;
	
	private Object result;
	private long time;
	private Consumer<FireTransition> callback;

	private Throwable exception;
	
	public FireTransition(Transition transition, int execId, ExecutionContext context, Consumer<FireTransition> callback) {
		this.transition = transition;
		this.execId = execId;
		this.context = context;
		this.callback = callback;
	}
	
	public boolean isValidExecution() {
		return execId==context.getExecutionId();
	}
	
	@Override
	public FireTransition call() throws Exception {
		try {
			long start = System.nanoTime();
			if (!isValidExecution()) return this;
			MutableTuple in = context.createInput(transition);
			result = transition.getJob().execute(context, in);
			time = System.nanoTime()-start;
			callback.accept(this);
		} catch (Throwable e) {
			this.exception = e;
			callback.accept(this);
			throw e;
		}
		return this;
	}
	
	public long getTime() {
		return time;
	}
	
	public Object getResult() {
		return result;
	}
	
	public Throwable getException() {
		return exception;
	}

}
