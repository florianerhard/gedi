package gedi.gui.genovis;

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.datastructure.tree.Trie;
import gedi.util.gui.JSuggestTextField;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.PlainDocument;

public class SetLocationField extends JPanel {

	private JTextField loc;

	public SetLocationField(SwingGenoVisViewer viewer) {
		this(viewer,false, null);
	}
	public SetLocationField(SwingGenoVisViewer viewer, boolean toIndependent) {
		this(viewer,toIndependent,null);
	}
	
	public SetLocationField(SwingGenoVisViewer viewer, boolean toIndependent, Trie<? extends ReferenceGenomicRegion<?>> index) {
		super(new BorderLayout());
		
		viewer.addReloadListener(v->updateLocation(viewer.getReference(),viewer.getRegion()));
		
		ActionListener l = e->{
			MutableReferenceGenomicRegion<Void> rgr = new MutableReferenceGenomicRegion<Void>().parse(loc.getText());
			if (rgr!=null && rgr.getReference()!=null && rgr.getRegion()!=null) {
				viewer.setLocation(toIndependent?rgr.getReference().toStrandIndependent():rgr.getReference(), rgr.getRegion());
			}
		};
		
		if (index!=null) {
			JSuggestTextField f = new JSuggestTextField(index);
			f.addActionListener(v->{
				if (index.containsKey(f.getTextField().getText())) {
					f.getTextField().setText(index.get(f.getTextField().getText()).toLocationString());
					l.actionPerformed(v);
				} else if (f.getTextField().getText().contains(":")){
					l.actionPerformed(v);
				}
			});
			loc = f.getTextField();
			loc.setColumns(40);
			add(f,BorderLayout.CENTER);
		} else {
			loc = new JTextField(40);
			add(loc,BorderLayout.CENTER);
			loc.addActionListener(l);
			
		}

			
		loc.setDocument(new PlainDocument() {
			public void insertString(int offs, String str, AttributeSet a) throws javax.swing.text.BadLocationException {
				if (str.length()>1) {
					remove(0, getLength());
					offs = 0;
				}
				super.insertString(offs, str, a);
			}
		});


		JButton but = new JButton("Go");
		add(but,BorderLayout.EAST);
		but.addActionListener(l);
		
	}

	private void updateLocation(ReferenceSequence[] reference,
			GenomicRegion[] region) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<reference.length; i++) {
			if (sb.length()>0) sb.append("; ");
			sb.append(reference[i]).append(":").append(region[i]);
		}
		loc.setText(sb.toString());
	}
	
	
	
	
	
	
	
}
