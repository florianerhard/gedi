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


import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.Stack;
import java.util.function.Consumer;







import gedi.app.extension.ExtensionContext;
import gedi.core.data.table.Table;
import gedi.core.data.table.ConditionOperator;
import gedi.core.data.table.Tables;
import gedi.core.reference.Chromosome;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.workspace.Workspace;
import gedi.core.workspace.WorkspaceItem;
import gedi.core.workspace.WorkspaceItemChangeEvent;
import gedi.core.workspace.WorkspaceItemChangeEvent.ChangeType;
import gedi.core.workspace.action.WorkspaceItemAction;
import gedi.core.workspace.action.WorkspaceItemActionExtensionPoint;
import gedi.fx.FXWindow;
import gedi.util.FileUtils;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.scene.Node;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

public class WorkspaceTree extends TreeView<WorkspaceItem>{

	
	
	private Consumer<WorkspaceItemChangeEvent> watcher = e->{
		if (e.getType()==ChangeType.CREATED){
			WorkspaceTreeNode par = findNode(e.getItem().getParent());
			if (par!=null && par.isChildListBuild()) par.addChild(e.getItem());
			if (par!=null && par.isLeaf()) par.getChildren(); // not build yet, so building it adds it already!
		} else if (e.getType()==ChangeType.DELETED) {
			WorkspaceTreeNode par = findNode(e.getItem().getParent());
			if (par!=null && par.isChildListBuild()) par.removeChild(e.getItem());
		}
	};

	public WorkspaceTree(FXWindow mainWindow) {
		Workspace.addWorkspaceListener(e->{
			if (e.isOpened()) {
				setRoot(new WorkspaceTreeNode(this,e.getWorkspace().getRoot()));
				getRoot().setExpanded(true);
				e.getWorkspace().addChangeListener(watcher );
			} else {
				e.getWorkspace().removeChangeListener(watcher);
				setRoot(null);
			}
		});
		
		setCellFactory(view->new WorkspaceItemTreeCell());
		
		addEventHandler(MouseEvent.MOUSE_CLICKED, me->{
			if (me.getClickCount()==2) {
				try {
					Table<?> table = Workspace.getCurrent().getItem(getSelectionModel().getSelectedItem().getValue());
//					FileUtils.writeAllText(table.toString(),new File("test.txt"));
					
					Tables.getInstance().buildIntervalIndex(table);
					
					
					WorkspaceItemAction<Table<?>> action = WorkspaceItemActionExtensionPoint.getInstance().get(new ExtensionContext().add(FXWindow.class, mainWindow), Table.class);
					action.accept(table);
					
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		
	}

	private WorkspaceTreeNode findNode(WorkspaceItem wi) {
		WorkspaceItem rootWi = getRoot().getValue();
		Stack<WorkspaceItem> pathToRoot = new Stack<WorkspaceItem>();
		pathToRoot.push(wi);
		while (!pathToRoot.peek().equals(rootWi)) {
			if (pathToRoot.peek().getParent()==null) {
				throw new RuntimeException(wi+" is not descendent of workspace: "+rootWi);
			}
			pathToRoot.push(pathToRoot.peek().getParent());
		}
		
		// now root wi is on top of the stack
		pathToRoot.pop();
		WorkspaceTreeNode node = (WorkspaceTreeNode) getRoot();
		while (!pathToRoot.isEmpty()) {
			WorkspaceItem child = pathToRoot.pop();
			if (!node.isChildListBuild())
				return null;
			
			node = node.findNode(child);
			if (node==null)
				throw new RuntimeException("Project tree out of synch!");
		}
		return node;
		
	}

	public Comparator<? super TreeItem<WorkspaceItem>> getNodeComparator() {
		return (a,b)->a.getValue().getName().compareTo(b.getValue().getName());
	}
	
	
	
}
