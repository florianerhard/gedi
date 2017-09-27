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
	private int maxDescrLen = 80;
	public PrintStream out;
	
	
	public ConsoleProgress() {
		this(System.out);
	}

	public ConsoleProgress(PrintStream out) {
		this(out,null);
	}

	public ConsoleProgress(ProgressManager man) {
		this(System.out,man);
	}

	public ConsoleProgress(PrintStream out, ProgressManager man) {
		this.out = out;
		if (man!=null)
			setManager(man);
	}

	public void setMaxDescriptionLength(int maxDescrLen) {
		this.maxDescrLen = maxDescrLen;
	}

	private int view;
	private String[] viewcs = {"|","/","-","\\"};

	private static boolean first = true;

	@Override
	public void firstView(int total) {
		if (total>1)
			out.println();
		if (total==1) {
			out.print("\33[?25l");
			if (first) {
				Runtime.getRuntime().addShutdownHook(new Thread(()->out.print("\33[?25h")));
				first = false;
			}
		}
	}

	@Override
	public void updateView(int index, int total) {
		CharSequence d = getDescription();
		if (d==null) d="";
		if (d.length()>maxDescrLen) d = d.subSequence(0, maxDescrLen-3).toString()+"...";
		if (d.length()>pad) pad = d.length();
		
		String description = StringUtils.padRight(d.toString(), pad,' ');
		
		view++;
		
		if (index==0 && total>1) {
			out.print("\33["+(total-1)+"A");
		}
		
		String viewc = viewcs[view%viewcs.length];
		
		if (progress>0) {
			if (isGoalKnown())
				out.printf(Locale.US,"\r\33[2K%s %s %d/%d (%.2f%%, %.1f/sec) Estimated time: %s",viewc,description,progress,count,(progress*100.0)/count,getPerSecond(),getEstimatedTime());
			else 
				out.printf(Locale.US,"\r\33[2K%s %s %d (%.1f/sec)",viewc,description,progress,getPerSecond());
		}
		else {
			out.printf(Locale.US,"\r\33[2K%s %s",viewc,description);
		}
		
		if (index+1<total)
			out.print("\n");
	}
	
	
	@Override
	public void lastView(int total) {
		if (total>1)
			out.print("\33["+(total-1)+"A");
		out.printf(Locale.US,"\r\33[2KProcessed %d elements in %s (Throughput: %.1f/sec)\n",progress,StringUtils.getHumanReadableTimespan(getTotalTime()),getPerSecond());
		if (total>1)
			out.print("\33["+(total-1)+"B");
		if (total==1)
			out.print("\33[?25h");
	}

}
