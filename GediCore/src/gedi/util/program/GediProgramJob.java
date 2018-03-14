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


import java.util.logging.Logger;

import gedi.util.job.ExecutionContext;
import gedi.util.job.Job;
import gedi.util.mutable.MutableTuple;

public class GediProgramJob extends DummyJob {

	private GediProgram s;
	private Class[] input;
	private boolean dry;
	
	public GediProgramJob(GediProgram s) {
		this.s = s;
		input = new Class[s.getInputSpec().size()];
		for (int i=0; i<input.length; i++)
			input[i] = Boolean.class;
	}
	
	public GediProgramJob setDry(boolean dry) {
		this.dry = dry;
		return this;
	}

	@Override
	public Class[] getInputClasses() {
		return input;
	}
	
	@Override
	public Boolean execute(ExecutionContext context, MutableTuple input) {
		if (dry) {
			Logger log = ((GediProgramContext)context.getContext("context")).getLog();
			synchronized (log) {
				log.info("Running "+s.getName()+"  In: "+s.getInputSpec().getNames()+" Out: "+s.getOutputSpec().getNames());	
			}
			return true;
		}
		
		try {
			s.execute(context.getContext("context"));
		} catch (Exception e) {
			throw new RuntimeException("Could not run "+s,e);
		}
		return true;
	}

	@Override
	public String getId() {
		return s.getName()+ind;
	}
	
	private boolean disabled = false;
	
	@Override
	public boolean isDisabled(ExecutionContext context) {
		return disabled;
	}
	
	public void disable() {
		this.disabled = true;
	}

}
