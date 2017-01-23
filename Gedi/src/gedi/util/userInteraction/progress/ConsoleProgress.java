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
