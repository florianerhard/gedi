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
package gedi.util.datastructure.tree.suffixTree;
 
import java.awt.BorderLayout;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;

import javax.swing.JFrame;

import gedi.util.datastructure.tree.suffixTree.tree.Localization;
import gedi.util.datastructure.tree.suffixTree.tree.SuffixTree;
import gedi.util.datastructure.tree.suffixTree.tree.traversal.DfsDownAndUpTraverser;
import gedi.util.datastructure.tree.suffixTree.tree.traversal.DfsDownTraverser;
import gedi.util.datastructure.tree.suffixTree.tree.traversal.Traverser;

public class SuffixTreeUtils {
	
//	public static void display(SuffixTree st) {
//		TreeLayout<Integer, Box<String>> layout = new TreeLayout<Integer, Box<String>>(createJungTree(st));
//		VisualizationViewer<Integer, Box<String>> vv = new VisualizationViewer<Integer, Box<String>>(layout);
//		
//		vv.getRenderContext().setEdgeLabelTransformer(FunctorUtils.<Box<String>>toStringTransformer());
//		vv.getRenderContext().setVertexLabelTransformer(FunctorUtils.<Integer>toStringTransformer());
//		vv.setGraphMouse(new DefaultModalGraphMouse());
//		
//		GraphZoomScrollPane pane = new GraphZoomScrollPane(vv);
//		JFrame frame = new JFrame("SuffixTree");
//		frame.getContentPane().setLayout(new BorderLayout());
//		frame.getContentPane().add(pane,BorderLayout.CENTER);
//		frame.pack();
//		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		frame.setVisible(true);
//	}
//
//	public static DelegateTree<Integer, Box<String>> createJungTree(SuffixTree tree) {
//		
//		DelegateTree<Integer, Box<String>> re = new DelegateTree<Integer, Box<String>>();
//		re.setRoot(-1);
//		
//		DfsDownTraverser t = new DfsDownTraverser(tree,tree.getRoot().getNode());
//		while (t.hasNext()) {
//			int node = t.nextInt();
//			if (t.getDirection()==Traverser.DOWN)
//				re.addChild(new Box<String>(tree.getSubSequence(t.getPrevious(),node).toString(),false), t.getPrevious(), node);
//		}
//		return re;
//	}
	
	public static void indexReport(SuffixTree tree, Writer wr) throws IOException {
		String[] names = tree.getAttributeNames();
		Object[] attr = new Object[names.length];
		for (int i=0; i<names.length; i++)
			attr[i] = tree.getAttributes(names[i]);
		
		wr.append("i");
		
		for (int j=0; j<names.length; j++) {
			wr.append('\t');
			wr.append(names[j]);
		}
		wr.append('\n');
		
		int nodes = tree.getNumNodes();
		for (int i=0; i<nodes; i++) {
			wr.append(i+"");
			
			for (int j=0; j<names.length; j++) {
				wr.append('\t');
				wr.append(Array.get(attr[j], i).toString());
			}
			wr.append('\n');
		}
		
	}

	public static Localization read(SuffixTree tree,CharSequence pattern) {
		Localization l = tree.getRoot();
		int pos = 0;
		
		while (pos<pattern.length()) {
			tree.getStorage().canonize(l);
			CharSequence read = tree.follow(l, pattern.charAt(pos));
			if (read==null)
				return null;
			
			int end = Math.min(read.length(),pattern.length()-pos);
			
			if (!read.subSequence(0, end).equals(pattern.subSequence(pos, pos+end)))
				return null;
			
			pos+=read.length();
		}
		return l;
	}
	
	
}
