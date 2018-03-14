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
package gedi.util.program;

import java.util.concurrent.atomic.AtomicInteger;

import gedi.util.dynamic.DynamicObject;
import gedi.util.job.ExecutionContext;
import gedi.util.job.Job;
import gedi.util.mutable.MutableTuple;

public class DummyJob implements Job<Boolean> {
	
	private static AtomicInteger index = new AtomicInteger(0);
	private Class[] inpCls;
	
	protected int ind;
	
	public DummyJob() {
		this(1);
	}
	public DummyJob(int inputs) {
		this.ind = index.getAndIncrement();
		inpCls = new Class[inputs];
		for (int i = 0; i < inpCls.length; i++) 
			inpCls[i] = Boolean.class;
			
	}

	@Override
	public Class[] getInputClasses() {
		return inpCls;
	}

	@Override
	public Class<Boolean> getOutputClass() {
		return Boolean.class;
	}


	@Override
	public Boolean execute(ExecutionContext context, MutableTuple input) {
		return true;
	}
	

	@Override
	public String getId() {
		return "Dummy"+ind;
	}
	@Override
	public boolean isDisabled(ExecutionContext context) {
		return false;
	}

}
