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
