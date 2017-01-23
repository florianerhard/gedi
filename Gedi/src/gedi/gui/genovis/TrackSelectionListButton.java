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
