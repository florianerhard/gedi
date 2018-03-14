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

import java.util.function.Supplier;

import gedi.util.StringUtils;

public abstract class AbstractProgress implements Progress {

	protected int count = -1;
	private CharSequence description;
	private Supplier<CharSequence> descriptionFactory;
	protected int progress;
	
	
	protected long lastUpdate = 0;
	protected long start = 0;
	protected boolean running = false;
	
	ProgressManager manager = new ProgressManager();
	
	@Override
	public Progress init() {
		start = System.currentTimeMillis();
		running = true;
		count = -1;
		progress = 0;
		lastUpdate = 0;
		manager.started(this);
		return this;
	}
	
	@Override
	public boolean isRunning() {
		return running;
	}
	
	@Override
	public void firstView(int total) {
	}
	@Override
	public void updateView(int index, int total) {
	}
	@Override
	public void lastView(int total) {
	}
	
	@Override
	public ProgressManager getManager() {
		return manager;
	}
	
	@Override
	public void setManager(ProgressManager man) {
		if (isRunning()) throw new RuntimeException("Cannot switch manager for a running progress!");
		this.manager = man;
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
		return this;
	}
	
	@Override
	public Progress incrementProgress() {
		this.progress++;
		return this;
	}



	@Override
	public void finish() {
		running=false;
		if (manager!=null)
			manager.finished(this);
	}

	@Override
	public Progress setDescription(CharSequence message) {
		this.descriptionFactory = null;
		this.description = message;
		return this;
	}
	
	@Override
	public Progress setDescription(Supplier<CharSequence> message) {
		this.description = null;
		this.descriptionFactory = message;
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
