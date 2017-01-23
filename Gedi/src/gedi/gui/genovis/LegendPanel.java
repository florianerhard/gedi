package gedi.gui.genovis;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;

public class LegendPanel extends JPanel {

	
	private VisualizationTrack<?,?> track;
	public LegendPanel(VisualizationTrack<?,?> track) {
		this.track = track;
	}

	@Override
	public Dimension getPreferredSize() {
		return track.getPreferredLegendBounds();
	}

	@Override
	protected void paintComponent(Graphics g) {
		track.paintLegend((Graphics2D)g, new Rectangle2D.Double(0, 0, getWidth(), getHeight()),1,-1);
	}
	
	
	
}
