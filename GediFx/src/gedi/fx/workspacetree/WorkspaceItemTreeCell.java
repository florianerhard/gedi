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

import java.lang.ref.WeakReference;

import gedi.core.workspace.WorkspaceItem;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;

public class WorkspaceItemTreeCell extends TreeCell<WorkspaceItem> {

	private HBox hbox;

	private WeakReference<TreeItem<WorkspaceItem>> treeItemRef;

	private InvalidationListener treeItemGraphicListener = observable -> {
		updateDisplay(getItem(), isEmpty());
	};
	

	private InvalidationListener treeItemListener = new InvalidationListener() {
		@Override public void invalidated(Observable observable) {
			TreeItem<WorkspaceItem> oldTreeItem = treeItemRef == null ? null : treeItemRef.get();
			if (oldTreeItem != null) {
				oldTreeItem.graphicProperty().removeListener(weakTreeItemGraphicListener);
			}

			TreeItem<WorkspaceItem> newTreeItem = getTreeItem();
			if (newTreeItem != null) {
				newTreeItem.graphicProperty().addListener(weakTreeItemGraphicListener);
				treeItemRef = new WeakReference<TreeItem<WorkspaceItem>>(newTreeItem);
			}
		}
	};

	private WeakInvalidationListener weakTreeItemGraphicListener =
			new WeakInvalidationListener(treeItemGraphicListener);

	private WeakInvalidationListener weakTreeItemListener =
			new WeakInvalidationListener(treeItemListener);

	{
		treeItemProperty().addListener(weakTreeItemListener);

		if (getTreeItem() != null) {
			getTreeItem().graphicProperty().addListener(weakTreeItemGraphicListener);
		}
	}

	private void updateDisplay(WorkspaceItem item, boolean empty) {
		if (item == null || empty) {
			hbox = null;
			setText(null);
			setGraphic(null);
			setTooltip(null);
		} else {
			// update the graphic if one is set in the TreeItem
			TreeItem<WorkspaceItem> treeItem = getTreeItem();
			if (treeItem != null && treeItem.getGraphic() != null) {
				if (item instanceof Node) {
					setText(null);

					// the item is a Node, and the graphic exists, so 
					// we must insert both into an HBox and present that
					// to the user (see RT-15910)
					if (hbox == null) {
						hbox = new HBox(3);
					}
					hbox.getChildren().setAll(treeItem.getGraphic(), (Node)item);
					setGraphic(hbox);
				} else {
					hbox = null;
					setText(item.getName());
					setGraphic(treeItem.getGraphic());
				}
			} else {
				hbox = null;
				if (item instanceof Node) {
					setText(null);
					setGraphic((Node)item);
				} else {
					setText(item.getName());
					setGraphic(null);
				}
			}
			setTooltip(new Tooltip(item.getName()));
		}                
	}

	@Override public void updateItem(WorkspaceItem item, boolean empty) {
		super.updateItem(item, empty);
		updateDisplay(item, empty);
	}
}
