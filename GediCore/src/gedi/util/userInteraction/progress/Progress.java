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
package gedi.util.userInteraction.progress;

import java.util.Locale;
import java.util.function.Supplier;


/**
 * First call init, then set options; after that, update progress and in the end, call finish!
 * @author erhard
 *
 */
public interface Progress {

	/**
	 * May return a new instance!
	 * @return
	 */
	Progress init();
	
	Progress setCount(int count);
	Progress setDescription(CharSequence message);
	Progress setDescription(Supplier<CharSequence> message);
	
	CharSequence getDescription();
	
	default Progress setDescriptionf(String format, Object...args) {
		return setDescription(String.format(Locale.US,format, args));
	}
	Progress setProgress(int count);
	Progress incrementProgress();
	
	void updateView(int index, int total);
	void firstView(int total);
	void lastView(int total);
	
	
	boolean isGoalKnown();
	boolean isRunning();
	ProgressManager getManager();
	void setManager(ProgressManager man);
	
	void finish();
	
}
