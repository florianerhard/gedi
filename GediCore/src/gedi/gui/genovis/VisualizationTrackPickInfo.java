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

package gedi.gui.genovis;

import gedi.core.reference.ReferenceSequence;

public class VisualizationTrackPickInfo<D> {

	
	public enum TrackEventType {
		Down, Up, Clicked, RightClicked, DoubleClicked, Moved, EnteredObject, ExitedObject, Dragged
	}
	
	
	private double pixelX;
	private double pixelY;

	private ReferenceSequence reference;
	private int bp;
	
	private VisualizationTrack<?, D> track;
	private D data;
	private TrackEventType type;
	
	private boolean consumed = false;
	
	
	
	public VisualizationTrackPickInfo<D> clone() {
		VisualizationTrackPickInfo<D> re = new VisualizationTrackPickInfo<D>();
		re.pixelX = pixelX;
		re.pixelY = pixelY;
		re.reference = reference;
		re.bp = bp;
		re.track = track;
		re.data = data;
		re.type = type;
		re.consumed = consumed;
		return re;
	}
	
	public VisualizationTrackPickInfo<D> substitute(D data) {
		VisualizationTrackPickInfo<D> re = clone();
		re.data = data;
		return re;
	}
	
	
	public void setType(TrackEventType type) {
		this.type = type;
	}
	
	public void setup(double pixelX, double pixelY, int bp, ReferenceSequence reference) {
		this.pixelX = pixelX;
		this.pixelY = pixelY;
		this.bp = bp;
		this.reference = reference;
		consumed = false;
	}
	
	
	public TrackEventType getType() {
		return type;
	}
	
	public void consume() {
		consumed = true;
	}
	
	public boolean isConsumed() {
		return consumed;
	}
	
	public void setTrack(VisualizationTrack<?, D> track) {
		this.track = track;
	}
	
	public void setData(D data) {
		this.data = data;
	}

	public VisualizationTrack<?, D> getTrack() {
		return track;
	}
	
	public double getPixelX() {
		return pixelX;
	}

	public double getPixelY() {
		return pixelY;
	}

	public int getBp() {
		return bp;
	}
	
	public ReferenceSequence getReference() {
		return reference;
	} 

	public D getData() {
		return data;
	}

	

	@Override
	public String toString() {
		return "VisualizationTrackPickInfo [pixelX=" + pixelX + ", pixelY="
				+ pixelY + ", reference=" + reference + ", bp=" + bp
				+ ", track=" + track + ", data=" + data + ", type=" + type
				+ ", consumed=" + consumed + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + bp;
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		long temp;
		temp = Double.doubleToLongBits(pixelX);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(pixelY);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result
				+ ((reference == null) ? 0 : reference.hashCode());
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
		VisualizationTrackPickInfo other = (VisualizationTrackPickInfo) obj;
		if (bp != other.bp)
			return false;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		if (Double.doubleToLongBits(pixelX) != Double
				.doubleToLongBits(other.pixelX))
			return false;
		if (Double.doubleToLongBits(pixelY) != Double
				.doubleToLongBits(other.pixelY))
			return false;
		if (reference == null) {
			if (other.reference != null)
				return false;
		} else if (!reference.equals(other.reference))
			return false;
		if (track == null) {
			if (other.track != null)
				return false;
		} else if (!track.equals(other.track))
			return false;
		return true;
	}

	
	
	
	
}
