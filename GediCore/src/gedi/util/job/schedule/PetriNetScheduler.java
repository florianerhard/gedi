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
package gedi.util.job.schedule;

import gedi.util.job.ExecutionContext;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public interface PetriNetScheduler extends Runnable {

	ExecutionContext getExecutionContext();
	void addListener(PetriNetListener rg);
	void removeListener(PetriNetListener rg);
	void setFinishAction(Consumer<ExecutionContext> finishAction);
	
	
}
