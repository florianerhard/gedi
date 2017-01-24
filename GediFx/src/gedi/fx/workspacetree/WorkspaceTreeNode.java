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

package gedi.fx.workspacetree;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;

import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import de.jensd.fx.fontawesome.Icon;
import gedi.core.workspace.WorkspaceItem;
import gedi.fx.FX;
import gedi.util.StringUtils;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;
import javafx.collections.ObservableList;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class WorkspaceTreeNode extends TreeItem<WorkspaceItem> {
	
	private static final HashMap<Class,AwesomeIcon> icons = new HashMap<Class, AwesomeIcon>();
	static {
		try (LineIterator it = new LineIterator(WorkspaceTreeNode.class.getResourceAsStream("/icons.properties"), "#")){
			while (it.hasNext()) {
				String[] f = StringUtils.split(it.next(), '=');
				Class<?> cls = Class.forName(f[0]);
				icons.put(cls, AwesomeIcon.valueOf(f[1]));
			}
		} catch (Exception e) {
			FX.log.log(Level.SEVERE, "Could not load icons!", e);
		}
	}

	
	private WorkspaceTree tree;
	
	public WorkspaceTreeNode(WorkspaceTree tree, WorkspaceItem item) {
		super(item);
		this.tree = tree;
		
	}
	
	
	 private boolean isLeaf;
     private boolean isBuild = false;
     private boolean isFirstTimeLeaf = true;
      
     @Override public ObservableList<TreeItem<WorkspaceItem>> getChildren() {
         if (!isBuild) 
             buildChildren();
         return super.getChildren();
     }

     @Override public boolean isLeaf() {
    	 if (isBuild) return getChildren().isEmpty();
    	 
         if (isFirstTimeLeaf) {
             isFirstTimeLeaf = false;
             WorkspaceItem f = (WorkspaceItem) getValue();
             isLeaf = !f.hasChildren();
             
             if (!isLeaf) {
            	 addEventHandler(TreeItem.branchCollapsedEvent(), e->AwesomeDude.setIcon(this, AwesomeIcon.FOLDER));
            	 addEventHandler(TreeItem.branchExpandedEvent(), e->AwesomeDude.setIcon(this, AwesomeIcon.FOLDER_OPEN));
            	 AwesomeDude.setIcon(this, AwesomeIcon.FOLDER);
             } else {
            	 AwesomeIcon icon = icons.get(f.getItemClass());
            	 if (icon==null)
            		 icon = AwesomeIcon.FILE;
            	 AwesomeDude.setIcon(this, icon);
             }
             
         }

         return isLeaf;
     }

     public boolean isChildListBuild() {
    	 return isBuild;
     }
     
     private void buildChildren() {
    	 WorkspaceItem f = getValue();
    	 ObservableList<TreeItem<WorkspaceItem>> children = super.getChildren();
    	 f.forEachChild(wi->{
    		 children.add(new WorkspaceTreeNode(tree,wi));
    	 });
    	 children.sort(tree.getNodeComparator());
    	 isBuild = true;
     }

	public void addChild(WorkspaceItem wi) {
		getChildren().add(new WorkspaceTreeNode(tree,wi));
		getChildren().sort(tree.getNodeComparator());
	}

	public boolean removeChild(WorkspaceItem wi) {
		Iterator<TreeItem<WorkspaceItem>> it = getChildren().iterator();
		while (it.hasNext()) {
			if (it.next().getValue().equals(wi)) {
				it.remove();
				return true;
			}
		}
		return false;
	}
	
	public WorkspaceTreeNode findNode(WorkspaceItem wi) {
		for (TreeItem<WorkspaceItem> n : getChildren()) {
			if (n.getValue().equals(wi)) return (WorkspaceTreeNode) n;
		}
		return null;
	}

	
	
}
