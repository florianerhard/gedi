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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listener that can be attached to a Component to implement Zoom and Pan functionality.
 *
 */
public class ZoomAndPanListener implements MouseListener, MouseMotionListener, MouseWheelListener {
	
	private static final Logger log = Logger.getLogger( ZoomAndPanListener.class.getName() );
	
	public enum Directions {
		Vertically, Horizontally, Both, None
	}
	
	public static final double DEFAULT_ZOOM_MULTIPLICATION_FACTOR = 1.2;

	private Component targetComponent;

	private int zoomLevel = 0;
	private double zoomMultiplicationFactor = DEFAULT_ZOOM_MULTIPLICATION_FACTOR;
	private Directions directions = Directions.Both;
	

	private Point dragStartScreen;
	private Point dragEndScreen;
	private AffineTransform coordTransform = new AffineTransform();
	
	private Rectangle2D bounds;
	
	private int modCount = 0;

	public ZoomAndPanListener(Component targetComponent) {
		this(targetComponent,Directions.Both,DEFAULT_ZOOM_MULTIPLICATION_FACTOR);
	}

	public ZoomAndPanListener(Component targetComponent, Directions directions, double zoomMultiplicationFactor) {
		this.targetComponent = targetComponent;
		this.directions = directions;
		this.zoomMultiplicationFactor = zoomMultiplicationFactor;
	}
	
	public void setBounds(Rectangle2D bounds) {
		this.bounds = bounds;
	}
	
	public Rectangle2D getBounds() {
		return bounds;
	}
	
	public void setDirections(Directions directions) {
		this.directions = directions;
	}
	
	public Directions getDirections() {
		return directions;
	}
	
	public int getZoomLevel() {
		return zoomLevel;
	}

	public void setZoomLevel(int zoomLevel) {
		this.zoomLevel = zoomLevel;
	}

	public AffineTransform getCoordTransform() {
		return coordTransform;
	}

	public void setCoordTransform(AffineTransform coordTransform) {
		this.coordTransform = coordTransform;
		targetComponent.repaint();
		modCount++;
	}

	public void setCoordTransform(Rectangle2D worldVisible) {
		
		Dimension size = targetComponent.getSize();
		coordTransform.setToScale(size.getWidth()/worldVisible.getWidth(), size.getHeight()/worldVisible.getHeight());
		coordTransform.translate(-worldVisible.getX(),-worldVisible.getY());
		targetComponent.repaint();
		modCount++;
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
		dragStartScreen = e.getPoint();
		dragEndScreen = null;
	}

	public void mouseReleased(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mouseMoved(MouseEvent e) {
	}

	public void mouseDragged(MouseEvent e) {
		moveCamera(e);
	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		zoomCamera(e);
	}
	
	public int getModificationCount() {
		return modCount;
	}
	
	private boolean mayHorizontally(MouseEvent e) {
		if (e.isControlDown()) return false;
		return directions==Directions.Horizontally || directions==Directions.Both;
	}
	
	private boolean mayVertically(MouseEvent e) {
		if (e.isAltDown()) return false;
		return directions==Directions.Vertically || directions==Directions.Both;
	}

	private void moveCamera(MouseEvent e) {
		try {
			dragEndScreen = e.getPoint();
			Point2D.Float dragStart = transformPoint(dragStartScreen);
			Point2D.Float dragEnd = transformPoint(dragEndScreen);
			double dx = mayHorizontally(e)?dragEnd.getX() - dragStart.getX():0;
			double dy = mayVertically(e)?dragEnd.getY() - dragStart.getY():0;
			modCount++;
			coordTransform.translate(dx, dy);
			dragStartScreen = dragEndScreen;
			dragEndScreen = null;
			targetComponent.repaint();
		} catch (NoninvertibleTransformException ex) {
			log.log(Level.SEVERE,"Transform not invertible!",e);
		}
	}

	private void zoomCamera(MouseWheelEvent e) {
		try {
			int wheelRotation = e.getWheelRotation();
			Point p = e.getPoint();
			double fac = 0;
			
			if (wheelRotation>0) {
				zoomLevel++;
				fac = 1 / zoomMultiplicationFactor;
			}
			if (wheelRotation<0) {
				zoomLevel--;
				fac = zoomMultiplicationFactor;
			}
			if (fac==0) return;
			
			modCount++;
			Point2D p1 = transformPoint(p);
			coordTransform.scale(
					mayHorizontally(e)?fac:1,
					mayVertically(e)?fac:1
					);
			Point2D p2 = transformPoint(p);
			coordTransform.translate(
					mayHorizontally(e)?p2.getX() - p1.getX():0,
					mayVertically(e)?p2.getY() - p1.getY():0
					);
			targetComponent.repaint();
		} catch (NoninvertibleTransformException ex) {
			log.log(Level.SEVERE,"Transform not invertible!",e);
		}
	}

	private Point2D.Float transformPoint(Point p1) throws NoninvertibleTransformException {
		AffineTransform inverse = coordTransform.createInverse();
		Point2D.Float p2 = new Point2D.Float();
		inverse.transform(p1, p2);
		return p2;
	}


}

