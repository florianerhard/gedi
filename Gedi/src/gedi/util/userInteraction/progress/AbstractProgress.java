package gedi.util.userInteraction.progress;

import java.time.Duration;
import java.util.function.Supplier;

import gedi.util.StringUtils;

public abstract class AbstractProgress implements Progress {

	protected int count = -1;
	private CharSequence description;
	private Supplier<CharSequence> descriptionFactory;
	protected int progress;
	
	protected long minimumTimeBetweenUpdates = 100;
	
	protected long lastUpdate = 0;
	protected long start = 0;
	
	@Override
	public Progress init() {
		start = System.currentTimeMillis();
		count = -1;
		progress = 0;
		lastUpdate = 0;
		start();
		return this;
	}
	
	@Override
	public CharSequence getDescription() {
		if (description!=null) return description;
		if (descriptionFactory!=null) return descriptionFactory.get();
		return null;
	}
	
	public double getPerSecond() {
		return 1000.0*progress/(System.currentTimeMillis()-start);
	}
	
	public int getRemaining() {
		return count-progress;
	}
	
	public long getTotalTime() {
		return System.currentTimeMillis()-start;
	}

	@Override
	public Progress setMinimumTimeBetweenUpdates(long milli) {
		this.minimumTimeBetweenUpdates = milli;
		return this;
	}

	@Override
	public Progress setCount(int count) {
		this.count = count;
		return this;
	}
	
	@Override
	public boolean isGoalKnown() {
		return count>=0;
	}

	@Override
	public Progress setProgress(int count) {
		this.progress = count;
		if (System.currentTimeMillis()-lastUpdate>minimumTimeBetweenUpdates) {
			update();
			lastUpdate = System.currentTimeMillis();
		}
		return this;
	}
	
	@Override
	public Progress incrementProgress() {
		this.progress++;
		if (System.currentTimeMillis()-lastUpdate>minimumTimeBetweenUpdates) {
			update();
			lastUpdate = System.currentTimeMillis();
		}
		return this;
	}

	protected abstract void start();
	protected abstract void update();

	@Override
	public boolean isUserCanceled() {
		return false;
	}

	@Override
	public void finish() {
	}

	@Override
	public Progress setDescription(CharSequence message) {
		this.descriptionFactory = null;
		this.description = message;
		if (System.currentTimeMillis()-lastUpdate>minimumTimeBetweenUpdates) {
			update();
			lastUpdate = System.currentTimeMillis();
		}
		return this;
	}
	
	@Override
	public Progress setDescription(Supplier<CharSequence> message) {
		this.description = null;
		this.descriptionFactory = message;
		if (System.currentTimeMillis()-lastUpdate>minimumTimeBetweenUpdates) {
			update();
			lastUpdate = System.currentTimeMillis();
		}
		return this;
	}
	
	public String getEstimatedTime() {
		int sek = (int) (getRemaining()/getPerSecond());
		StringBuilder sb = new StringBuilder();
		if (sek>24*60*60) {
			sb.append(sek/(24*60*60)).append("d ");
			sek%=(24*60*60);
		}
		if (sek>60*60) {
			sb.append(sek/(60*60)).append(":");
			sek%=(60*60);
		}
		sb.append(StringUtils.padLeft(sek/60+"",2,'0')).append(":").append(StringUtils.padLeft(sek%60+"",2,'0'));
		return sb.toString();
	}


}
