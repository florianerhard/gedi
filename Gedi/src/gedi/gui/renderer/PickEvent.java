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
