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

package gedi.util.gui;

import gedi.util.datastructure.tree.Trie;

import java.awt.EventQueue;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.TreeSet;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;

public class JSuggestTextField extends JComboBox<String> {
	
	
	private final JTextField tf;
	
	
	public JSuggestTextField(final Trie<?> index) {
		setEditable(true);
		tf = (JTextField) getEditor().getEditorComponent();
		tf.addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent e) {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						String text = tf.getText();
						if(text.length()==0) {
							hidePopup();
							setModel(new DefaultComboBoxModel<String>(index.keySet().toArray(new String[0])), "");
						}else{
							DefaultComboBoxModel<String> m = getSuggestedModel(index, text);
							if(m.getSize()==0 || hide_flag) {
								hidePopup();
								hide_flag = false;
							}else{
								setModel(m, text);
								showPopup();
							}
						}
					}
				});
			}
			public void keyPressed(KeyEvent e) {
				String text = tf.getText();
				int code = e.getKeyCode();
				if(code==KeyEvent.VK_ESCAPE) {
					hide_flag = true; 
				}
			}
		});
		setModel(new DefaultComboBoxModel<String>(index.keySet().toArray(new String[0])), "");
	}
	
	public JTextField getTextField() {
		return tf;
	}
	
	private boolean hide_flag = false;
	private void setModel(DefaultComboBoxModel<String> mdl, String str) {
		setModel(mdl);
//		setSelectedIndex(-1);
		tf.setText(str);
	}
	private static DefaultComboBoxModel<String> getSuggestedModel(Trie<?> trie, String text) {
		return new DefaultComboBoxModel<String>(trie.getKeysByPrefix(text, new TreeSet<String>()).toArray(new String[0]));
	}
}