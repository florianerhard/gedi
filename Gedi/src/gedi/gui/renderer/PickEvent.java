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

package gedi.gui.renderer;

import java.awt.geom.Point2D;
import java.util.EventObject;

public class PickEvent<T> extends EventObject {

	private T picked;
	private Point2D world;
	private Point2D component;
	
	public PickEvent(Object source, T picked, Point2D world, Point2D component) {
		super(source);
		this.picked =picked;
		this.world = world;
		this.component = component;
	}

	public T getPicked() {
		return picked;
	}

	public Point2D getWorld() {
		return world;
	}

	public Point2D getComponent() {
		return component;
	}

	@Override
	public String toString() {
		return "Picked: "+picked;
	}
	
	
}
