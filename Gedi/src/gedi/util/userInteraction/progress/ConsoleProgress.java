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

import gedi.util.StringUtils;

import java.io.PrintStream;
import java.util.Locale;

public class ConsoleProgress extends AbstractProgress {

	private int pad = 0;
	private boolean newline = false;
	public PrintStream out;
	
	
	public ConsoleProgress() {
		this(System.out);
	}

	public ConsoleProgress(PrintStream out) {
		this.out = out;
	}


	@Override
	protected void update() {
		CharSequence d = getDescription();
		if (d!=null && d.length()>pad) pad = d.length();
		String description = d==null?"":StringUtils.padRight(d.toString(), pad,' ');
		
		if (progress>0) {
			if (isGoalKnown())
				out.printf(Locale.US,"\33[2K\r%s %d/%d (%.2f%%, %.1f/sec) Estimated time: %s",description,progress,count,(progress*100.0)/count,getPerSecond(),getEstimatedTime());
			else 
				out.printf(Locale.US,"\33[2K\r%s %d (%.1f/sec)",description,progress,getPerSecond());
		}
		else {
			out.printf(Locale.US,"\33[2K\r%s",description);
		}
		if (newline)
			out.println("");
	}
	
	public ConsoleProgress setNewline(boolean newline) {
		this.newline = newline;
		return this;
	}
	
	
	@Override
	public void finish() {
		update();
		out.printf(Locale.US,"\33[2K\rProcessed %d elements in %s (Throughput: %.1f/sec)\n",progress,StringUtils.getHumanReadableTimespan(getTotalTime()),getPerSecond());
	}

	@Override
	protected void start() {
		
	}

	
}
