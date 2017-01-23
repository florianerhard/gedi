package gedi.util.gui;

import gedi.util.ArrayUtils;
import gedi.util.functions.EI;
import gedi.util.functions.IntBooleanConsumer;
import gedi.util.gui.JCheckBox3.State;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Stack;
import java.util.function.Function;

import javax.swing.AbstractCellEditor;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * Components, that shows a tree with check boxes. All what matters is the leafs, the inner nodes automatically propagate their check state to their descendands
 * 
 * 
 * @author Florian Erhard
 *
 */
public class JCheckTree extends JScrollPane {
	private JTree tree;
	private Object[] listData;
	private boolean[] checked;
	private IntBooleanConsumer callback;

	private Function<Object,String> stringer = o->String.valueOf(o);
	
	/**
	 * Creates a JCheckList. Results are reflected to the checked parameter.
	 * 
	 * @param listData the list items
	 * @param checked the checked items
	 */
	public JCheckTree(Object[] listData, HashMap<Object,Object> toParent, boolean[] checked, IntBooleanConsumer callback) {
		super(new JTree());

		this.listData = listData;
		this.checked = checked;
		this.callback = callback;
		tree = (JTree) getViewport().getView();
		tree.setRootVisible( false );
		
		IndexedTreeNode root = new IndexedTreeNode(null,-1);
		
		HashMap<Object,IndexedTreeNode> dataToNode = new HashMap<Object, IndexedTreeNode>();
		for (int i=0; i<listData.length; i++) {
			IndexedTreeNode node = new IndexedTreeNode(listData[i],i);
			insert(listData[i],node, root, toParent, dataToNode);
		}
		
		tree.setModel(new DefaultTreeModel(root));

		final CheckBoxNodeRenderer renderer = new CheckBoxNodeRenderer();
	    tree.setCellRenderer(renderer);

	    CheckBoxNodeEditor ed = new CheckBoxNodeEditor(tree);
	    tree.setCellEditor(ed);
	    tree.setEditable(true);
	    tree.setRootVisible(false);
	    EI.wrap(root.children()).forEachRemaining(n->tree.expandPath(new TreePath(((DefaultMutableTreeNode)n).getPath())));
		
	    tree.getModel().addTreeModelListener(new TreeModelListener() {
			
			@Override
			public void treeStructureChanged(TreeModelEvent e) {
			}
			
			@Override
			public void treeNodesRemoved(TreeModelEvent e) {
			}
			
			@Override
			public void treeNodesInserted(TreeModelEvent e) {
			}
			boolean doit = true;
			@Override
			public void treeNodesChanged(TreeModelEvent e) {
				if (!doit) return;
				doit = false;
//				System.out.println(e.getTreePath().getLastPathComponent()+" "+e.getSource());
				if (e.getChildren()==null || e.getChildren().length!=1) 
					return;
				IndexedTreeNode n = (IndexedTreeNode) e.getChildren()[0];
				if (!(n.getUserObject() instanceof CheckBoxNode))
					return;
				
				CheckBoxNode c = (CheckBoxNode) n.getUserObject();
				if(n.index<0) {
					Stack<IndexedTreeNode> dfs = new Stack<>();
					dfs.push(n);
					while (!dfs.isEmpty()) {
						IndexedTreeNode node = dfs.pop();
						if (node .index==-1)
							EI.wrap(node.children()).forEachRemaining(ch->dfs.push((IndexedTreeNode) ch));
						else {
							checked[node.index] = c.selected;
							callback.accept(node.index, c.selected);
							((DefaultTreeModel)tree.getModel()).nodeChanged(node);
						}
					}
					
				} else {
					checked[n.index] = c.selected;
					callback.accept(n.index, c.selected);
				}
				for (TreeNode p : n.getPath()) {
					((DefaultTreeModel)tree.getModel()).nodeChanged(p);
				}
				tree.treeDidChange();
				doit = true;
			}
		});
	    
		setPreferredSize(new Dimension(300,500));
	}
	
	public JCheckTree setStringer(Function<Object, String> stringer) {
		this.stringer = stringer;
		return this;
	}

	private class IndexedTreeNode extends DefaultMutableTreeNode {
		int index;
		public IndexedTreeNode(Object o, int index) {
			super(o,index>=-1);
			this.index = index;
		}
	}

	private void insert(Object l, IndexedTreeNode node, IndexedTreeNode root, HashMap<Object,Object> toParent, HashMap<Object,IndexedTreeNode> dataToNode) {
		Object p = toParent.get(l);
		if (p!=null) {
			IndexedTreeNode pnode = dataToNode.computeIfAbsent(p, pa->{
				IndexedTreeNode re = new IndexedTreeNode(p, -1);
				insert(pa,re,root,toParent,dataToNode);
				return re;
			});
			pnode.add(node);
		} else {
			root.add(node);
		}
	}


	class CheckBoxNodeRenderer implements TreeCellRenderer {
		private JCheckBox3 leafRenderer = new JCheckBox3();


		Color selectionBorderColor, selectionForeground, selectionBackground,
		textForeground, textBackground;



		protected JCheckBox3 getLeafRenderer() {
			return leafRenderer;
		}
		

		public CheckBoxNodeRenderer() {
			Font fontValue;
			fontValue = UIManager.getFont("Tree.font");
			if (fontValue != null) {
				leafRenderer.setFont(fontValue);
			}
			Boolean booleanValue = (Boolean) UIManager
					.get("Tree.drawsFocusBorderAroundIcon");
			leafRenderer.setFocusPainted((booleanValue != null)
					&& (booleanValue.booleanValue()));

			selectionBorderColor = UIManager.getColor("Tree.selectionBorderColor");
			selectionForeground = UIManager.getColor("Tree.selectionForeground");
			selectionBackground = UIManager.getColor("Tree.selectionBackground");
			textForeground = UIManager.getColor("Tree.textForeground");
			textBackground = UIManager.getColor("Tree.textBackground");
		}

		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean selected, boolean expanded, boolean leaf, int row,
				boolean hasFocus) {

			Component returnValue;

			String stringValue = stringer.apply(((IndexedTreeNode) value).getUserObject());//tree.convertValueToText(value, selected,	expanded, leaf, row, false);
			leafRenderer.setText(stringValue);
			leafRenderer.setState(inferState((IndexedTreeNode) value));

			leafRenderer.setEnabled(tree.isEnabled());

			if (selected) {
				leafRenderer.setForeground(selectionForeground);
				leafRenderer.setBackground(selectionBackground);
			} else {
				leafRenderer.setForeground(textForeground);
				leafRenderer.setBackground(textBackground);
			}

			returnValue = leafRenderer;
			return returnValue;
		}
	}

	class CheckBoxNodeEditor extends AbstractCellEditor implements TreeCellEditor {

		CheckBoxNodeRenderer renderer = new CheckBoxNodeRenderer();

		ChangeEvent changeEvent = null;

		JTree tree;

		public CheckBoxNodeEditor(JTree tree) {
			this.tree = tree;
		}

		
		public Object getCellEditorValue() {
			JCheckBox3 checkbox = renderer.getLeafRenderer();
			CheckBoxNode checkBoxNode = new CheckBoxNode(checkbox.getText(),
					checkbox.isSelected());
			return checkBoxNode;
		}

		public boolean isCellEditable(EventObject event) {
			return true;
		}

		public Component getTreeCellEditorComponent(JTree tree, Object value,
				boolean selected, boolean expanded, boolean leaf, int row) {

			Component editor = renderer.getTreeCellRendererComponent(tree, value,
					true, expanded, leaf, row, true);

			// editor always selected / focused
			ItemListener itemListener = new ItemListener() {
				public void itemStateChanged(ItemEvent itemEvent) {
					if (stopCellEditing()) {
						fireEditingStopped();
					}
				}
			};
			if (editor instanceof JCheckBox3) {
				((JCheckBox3) editor).addItemListener(itemListener);
			}

			return editor;
		}
	}

	class CheckBoxNode {
		String text;

		boolean selected;

		public CheckBoxNode(String text, boolean selected) {
			this.text = text;
			this.selected = selected;
		}

		public boolean isSelected() {
			return selected;
		}

		public void setSelected(boolean newValue) {
			selected = newValue;
		}

		public String getText() {
			return text;
		}

		public void setText(String newValue) {
			text = newValue;
		}

		public String toString() {
			return text;
		}
	}

	public State inferState(IndexedTreeNode value) {
		int ind = value.index;
		if (ind>=0) return checked[ind]?JCheckBox3.State.SELECTED:JCheckBox3.State.NOT_SELECTED;
		
		int checked = 0;
		int notchecked  = 0;
		Stack<IndexedTreeNode> dfs = new Stack<>();
		dfs.push(value);
		while (!dfs.isEmpty()) {
			IndexedTreeNode n = dfs.pop();
			if (n.index==-1)
				EI.wrap(n.children()).forEachRemaining(ch->dfs.push((IndexedTreeNode) ch));
			else if (this.checked[n.index])
				checked++;
			else 
				notchecked++;
		}
		
		if (checked==0) return JCheckBox3.State.NOT_SELECTED;
		if (notchecked==0) return JCheckBox3.State.SELECTED;
		
		return JCheckBox3.State.INDETERMINED;
	}


}
