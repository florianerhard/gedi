/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */

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
