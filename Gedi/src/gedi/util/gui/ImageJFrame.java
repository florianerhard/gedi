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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

public class ImageJFrame extends JFrame {

	private JLabel image;

	public ImageJFrame() {
		this("");
	}
	
	public ImageJFrame(String title) {
		setTitle(title);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout());
		JScrollPane scroller = new JScrollPane(image = new JLabel());
		getContentPane().add(scroller,BorderLayout.CENTER);
		setSize(800, 630);
	}
	
	public ImageJFrame setVisible() {
		if (!isVisible())
			super.setVisible(true);
		return this;
	}
	
	public ImageJFrame setImage(Image img) {
		image.setIcon(new ImageIcon(img));
		setSize(new Dimension(img.getWidth(null)+4,img.getHeight(null)+30));
		return this;
	}
	
}
