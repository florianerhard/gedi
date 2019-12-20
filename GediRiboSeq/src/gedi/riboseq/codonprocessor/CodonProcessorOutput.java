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
package gedi.riboseq.codonprocessor;

import java.io.IOException;
import java.util.function.Consumer;

public abstract class CodonProcessorOutput {

	protected CodonProcessorCounter counter;
	
	public void setCounter(CodonProcessorCounter counter) {
		if (this.counter!=null) throw new RuntimeException("This output is already attached to another counter!");
		this.counter = counter;
	}
	
	
	public void createOutput(String[] conditions) {
		try {
			createOutput2(conditions);
		} catch (IOException e) {
			throw new RuntimeException("Could not write output for "+counter.getPrefix()+"!",e);
		}
	}
	
	public abstract void createOutput2(String[] conditions) throws IOException;

}
