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

import gedi.util.functions.EI;
import gedi.util.gui.JCheckList;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;

public class TrackSelectionListButton extends JButton {

	
	public TrackSelectionListButton(SwingGenoVisViewer viewer) {
		setText("Tracks");
			
		addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JDialog dia = new JDialog(SwingUtilities.getWindowAncestor(TrackSelectionListButton.this));
				dia.setTitle("Tracks");
				dia.getContentPane().setLayout(new BorderLayout());
				dia.getContentPane().add(new JCheckList("ID",
						viewer.getTracks().toArray(),
						EI.wrap(viewer.getTracks()).map(t->!t.isHidden()).toBooleanArray(),
						(ind,check)->viewer.getTracks().get(ind).setHidden(!check)
						), BorderLayout.CENTER);
				JButton close = new JButton();
				close.setText("Ok");
				close.addActionListener(ev->dia.setVisible(false));
				dia.getContentPane().add(close, BorderLayout.SOUTH);
				dia.pack();
				dia.setVisible(true);
			}
		});
	}
	
	
}
