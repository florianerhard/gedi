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
package gedi.util.genomic;

import java.awt.Color;

import gedi.util.dynamic.DynamicObject;

public class GediViewItem {
	
	private String file;
	private int condition;
	private int numCondition;
	private double total;
	private String label;
	private String track;
	private String color;
	private DynamicObject options;
	public GediViewItem(String file, int condition, int numCondition, double total, String label, String track,
			String color, DynamicObject options) {
		super();
		this.file = file;
		this.condition = condition;
		this.numCondition = numCondition;
		this.total = total;
		this.label = label;
		this.track = track;
		this.color = color;
		this.options = options;
	}
	public String getFile() {
		return file;
	}
	public int getCondition() {
		return condition;
	}
	public int getNumCondition() {
		return numCondition;
	}
	public double getTotal() {
		return total;
	}
	public String getLabel() {
		return label;
	}
	public String getTrack() {
		return track;
	}
	public String getColor() {
		return color;
	}
	public DynamicObject getOptions() {
		return options;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((color == null) ? 0 : color.hashCode());
		result = prime * result + condition;
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + numCondition;
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		long temp;
		temp = Double.doubleToLongBits(total);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((track == null) ? 0 : track.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GediViewItem other = (GediViewItem) obj;
		if (color == null) {
			if (other.color != null)
				return false;
		} else if (!color.equals(other.color))
			return false;
		if (condition != other.condition)
			return false;
		if (file == null) {
			if (other.file != null)
				return false;
		} else if (!file.equals(other.file))
			return false;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (numCondition != other.numCondition)
			return false;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		if (Double.doubleToLongBits(total) != Double.doubleToLongBits(other.total))
			return false;
		if (track == null) {
			if (other.track != null)
				return false;
		} else if (!track.equals(other.track))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "GediViewItem [file=" + file + ", condition=" + condition + ", numCondition=" + numCondition + ", total="
				+ total + ", label=" + label + ", track=" + track + ", color=" + color + ", options=" + options + "]";
	}
	
	
	
}
