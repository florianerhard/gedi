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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.event.EventListenerList;

public class JRenderablePanel<T> extends JPanel {

	private static final Logger log = Logger.getLogger( JRenderablePanel.class.getName() );
	
	private ZoomAndPanListener zoomAndPanListener;
	private Renderer<T> renderer;
	
	private EventListenerList listeners = new EventListenerList();

	public JRenderablePanel(Renderer<T> renderer, boolean zoomAndPan) {
		this.renderer = renderer;
		this.zoomAndPanListener = new ZoomAndPanListener(this);
		
		if (zoomAndPan) {
			this.addMouseListener(zoomAndPanListener);
			this.addMouseMotionListener(zoomAndPanListener);
			this.addMouseWheelListener(zoomAndPanListener);
		}
		
		this.addMouseListener(new PickProxy());
		
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				if (clip!=null) {
					if (isFixAspect()) {
						double oasp = bounds.getWidth()/bounds.getHeight();
						double nasp = getBounds().getWidth()/getBounds().getHeight();
						
						if (nasp>oasp) { // the screen is now wider than before
							double nw = nasp*clip.getHeight();
							clip.setRect(clip.getX()-(nw-clip.getWidth())/2, clip.getY(), nw, clip.getHeight());
						} else {
							double nh = clip.getWidth()/nasp;
							clip.setRect(clip.getX(), clip.getY()-(nh-clip.getHeight())/2, clip.getWidth(), nh);
						}
					}
					zoomAndPanListener.setCoordTransform(clip);
					
				}
			}
		});
		
		if (renderer instanceof BoundedRenderer) {
			BoundedRenderer<T> br = (BoundedRenderer<T>)renderer;
			setWorld(br.getSize(), br.isFixAspectRatio());
			setPreferredSize(new Dimension((int)br.getSize().getWidth(),(int)br.getSize().getHeight()));
		}
	}

	private int transformStamp = -1;
	private AffineTransform world;
	private AffineTransform iworld;
	private Rectangle2D clip;
	private float[] clipPoints = new float[4];
	private Rectangle2D bounds = new Rectangle2D.Double();

	private Dimension2D size = null;
	private boolean fixAspect = false;

	
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setColor(getBackground() );
		g2.fillRect(0, 0, getWidth(), getHeight());
		
		checkUpdate();
		renderer.render(g2, clip, getBounds(), world, iworld);
	}
	
	
	private boolean isFixAspect() {
		if (renderer instanceof BoundedRenderer) {
			BoundedRenderer<T> br = (BoundedRenderer<T>)renderer;
			return br.isFixAspectRatio();
		}
		return false;
	}
	
	public void setWorld(Dimension2D size, boolean fixAspect) {
		this.size = size;
		this.fixAspect = fixAspect;
	}

	public ZoomAndPanListener getZoomAndPanListener() {
		return zoomAndPanListener;
	}

	private void checkUpdate() {
		int modCount = zoomAndPanListener.getModificationCount();
		if (modCount<=transformStamp) return;
		
		try {
			if (size!=null) {
				
				AffineTransform Px = new AffineTransform();
				double sx = getBounds().getWidth() / size.getWidth();
				double sy = getBounds().getHeight() / size.getHeight();
				double scale = Math.min(sx,sy);
				if (fixAspect)
					sx = sy = scale;
				Px.scale(sx, sy);
				double tx = (getBounds().getWidth()/sx - size.getWidth())/2;
				double ty = (getBounds().getHeight()/sy -size.getHeight())/2;
				Px.translate(tx, ty);
				
				zoomAndPanListener.setCoordTransform(Px);
				size=null;
			}
			
			transformStamp = modCount;
			world = zoomAndPanListener.getCoordTransform();
			iworld = world.createInverse();
			
			clipPoints[0] = clipPoints[1] = 0;
			clipPoints[2] = getWidth(); clipPoints[3] = getHeight();
			iworld.transform(clipPoints,0,clipPoints,0,2);
			bounds.setRect(getBounds());
			
			clip = new Rectangle2D.Float(clipPoints[0],clipPoints[1],clipPoints[2]-clipPoints[0],clipPoints[3]-clipPoints[1]);
			
			
		} catch (NoninvertibleTransformException e) {
			log.log(Level.SEVERE,"Transform not invertible!",e);
		}
	}


	public void addPickListener(PickListener<T> listener) {
		listeners.add(PickListener.class, listener);
	}
	
	public void removePickListener(PickListener<T> listener) {
		listeners.remove(PickListener.class, listener);
	}
	
	private class PickProxy extends MouseAdapter {
	
		
		@Override
		public void mouseClicked(MouseEvent e) {
			checkUpdate();
			
			Point2D component = new Point2D.Float(e.getX(), e.getY());
			Point2D world = iworld.transform(component, new Point2D.Float());
			T picked = renderer.pick(world, JRenderablePanel.this.world, iworld);
			PickEvent<T> ev = new PickEvent<T>(JRenderablePanel.this, picked, world, component);
			
			for (PickListener<T> l : listeners.getListeners(PickListener.class)) {
				if (l.picked(ev)) break;
			}
		}
		
	}
	
	

}