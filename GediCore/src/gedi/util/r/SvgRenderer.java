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

package gedi.util.r;

import gedi.gui.renderer.BoundedRenderer;
import gedi.gui.renderer.JRenderablePanel;
import gedi.util.PaintUtils;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

import javax.swing.JFrame;

import org.apache.batik.anim.dom.SVGOMDocument;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.bridge.svg12.SVG12BridgeContext;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.gvt.renderer.ConcreteImageRendererFactory;
import org.apache.batik.gvt.renderer.ImageRenderer;
import org.apache.xmlgraphics.java2d.Dimension2DDouble;
import org.w3c.dom.svg.SVGDocument;

public class SvgRenderer implements BoundedRenderer<GraphicsNode> {

	private GraphicsNode gvtRoot;
	private ImageRenderer renderer;
	private Dimension2D svgDim;
	private boolean fixAspectRatio = true;

	public SvgRenderer(String path) throws IOException {
		this(PaintUtils.loadSvgDoc(path));
	}
	
	public SvgRenderer(SVGDocument doc) {
		renderer = new ConcreteImageRendererFactory().createStaticImageRenderer();
		BridgeContext ctx = doc instanceof SVGOMDocument && ((SVGOMDocument)doc).isSVG12()?new SVG12BridgeContext(new UserAgentAdapter()):new BridgeContext(new UserAgentAdapter());
		gvtRoot = new GVTBuilder().build(ctx, doc);
		svgDim = new Dimension2DDouble(doc.getRootElement().getWidth().getBaseVal().getValue(),doc.getRootElement().getHeight().getBaseVal().getValue());
	}

	public GraphicsNode getGvtRoot() {
		return gvtRoot;
	}
	
	public Dimension2D getSvgDim() {
		return svgDim;
	}
	
	
	@Override
	public boolean isFixAspectRatio() {
		return fixAspectRatio;
	}
	
	public void setFixAspectRatio(boolean fixAspectRatio) {
		this.fixAspectRatio = fixAspectRatio;
	}
	
	@Override
	public Dimension2D getSize() {
		return svgDim;
	}
	
	@Override
	public void render(Graphics2D g2, Rectangle2D world, Rectangle2D screen,
			AffineTransform worldToScreen, AffineTransform screenToWorld) {
			
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		
		AffineTransform tr = g2.getTransform();
		tr.concatenate(worldToScreen);
		
		g2.setTransform(tr);
		gvtRoot.paint(g2);

	}


	@Override
	public GraphicsNode pick(Point2D world, AffineTransform worldToScreen,
			AffineTransform screenToWorld) {
		return gvtRoot.nodeHitAt(world);
	}
	
	public static void main(String[] args) throws IOException {
		SVGDocument doc = PaintUtils.loadSvgDoc(args[0]);
		SvgRenderer ren = new SvgRenderer(doc);
		
		JFrame f = new JFrame();
		f.getContentPane().setLayout(new BorderLayout());
		JRenderablePanel<GraphicsNode> pan = new JRenderablePanel<GraphicsNode>(ren,true);
		pan.addPickListener(e->{
			System.out.println(e);
			return false;
		});
		f.getContentPane().add(pan,BorderLayout.CENTER);
		f.pack();
		f.setSize(900, 500);
		f.setVisible(true);
	}

}
