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
package gedi.fx.docking;


import java.util.ArrayList;
import java.util.Collection;

import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;



/**
 * 
 * Usage: 
 * 
 * @author erhard
 *
 */
public class DockingPane extends BorderPane {

	
	private DockGroup group;
	private DraggableTabPane tabs;
	private SplitPane splitter;
	
	
	public DockingPane() {
		this(new DockGroup(true));
	}
	
	public DockingPane(Dockable singleControl) {
		this(singleControl,new DockGroup(true));
	}
	
	public DockingPane(DraggableTabPane tabs) {
		this(tabs,new DockGroup(true));
	}

	public DockingPane(SplitPane splitter) {
		this(splitter,new DockGroup(true));
	}
	
	public DockingPane(DockGroup group) {
		this.group = group;
		setNode((Node)null);
	}
	
	public DockingPane(Dockable singleControl, DockGroup group) {
		this.group = group;
		setNode(singleControl);
	}
	
	public DockingPane(DraggableTabPane tabs, DockGroup group) {
		this.group = group;
		setNode(tabs);
	}

	public DockingPane(SplitPane splitter, DockGroup group) {
		this.group = group;
		setNode(splitter);
	}
	
	public DockGroup getDockGroup() {
		return group;
	}
	
	
	public void split(Dockable other, Orientation orientation) {
		split(new DockingPane(other, group),orientation);
	}
	
	public void split(DraggableTabPane other, Orientation orientation) {
		split(new DockingPane(other, group),orientation);
	}
	
	public void split(SplitPane other, Orientation orientation) {
		split(new DockingPane(other, group),orientation);
	}
	
	void split(DockingPane other, Orientation orientation) {
		SplitPane splitter = new SplitPane();
		DockingPane curr = setNode(splitter);
		
		splitter.setOrientation(orientation);
		splitter.getItems().add(curr);
		splitter.getItems().add(other);
	}
	
	boolean isDirectlyAtScene() {
		return getParent()==null;
	}
	
	
	void split(Tab tab, Side side) {
		SplitPane splitter = new SplitPane();
		DockingPane curr = setNode(splitter);
		
		DraggableTabPane other = createTabPane();
		other.getTabs().add(tab);
		
		if (side==Side.BOTTOM||side==Side.TOP)
			splitter.setOrientation(Orientation.VERTICAL);
		else
			splitter.setOrientation(Orientation.HORIZONTAL);
		
		if (side==Side.LEFT||side==Side.TOP) {
			splitter.getItems().add(new DockingPane(other,group));
			splitter.getItems().add(curr);
		} else {
			splitter.getItems().add(curr);
			splitter.getItems().add(new DockingPane(other,group));
		}
		
	}
	
	public void unsplit(DockingPane child) {
		if (isSplittingPane()) {
			DockingPane other = (DockingPane) splitter.getItems().get(0);
			if (other==child)
				other = (DockingPane) splitter.getItems().get(1);
			setNode(other.getNode());
		}
	}

	public <C extends Collection<Node>> C getDockedNodes(C c) {
		ArrayList<Node> re = new ArrayList<Node>();
		if (isTabPane()) {
			for (Tab t : tabs.getTabs()) {
				if (t.getContent() instanceof DockingPane)
					((DockingPane)t.getContent()).getDockedNodes(c);
				else
					re.add(((Dockable) t.getContent()).getCenter());
			}
		} else if (isSplittingPane()) {
			((DockingPane)splitter.getItems().get(0)).getDockedNodes(c);
			((DockingPane)splitter.getItems().get(1)).getDockedNodes(c);
		}
		return c;
	}
	
	public void add(Dockable other, boolean makeActive) {
		if (isTabPane()) {
			Tab t = createTab(other);
			tabs.getTabs().add(t);
			if (makeActive)
				tabs.getSelectionModel().select(t);
		} else if (isSplittingPane()) {
			((DockingPane) splitter.getItems().get(0)).add(other,makeActive);
		} else {
			setNode(other);
		}
	}
	

	private Tab createTab(Dockable dockable) {
		Tab tab = new Tab();
//		tab.getStyleClass().add("tab-docking");
		tab.textProperty().bind(dockable.labelProperty());
		tab.graphicProperty().bind(dockable.imageProperty());
		tab.closableProperty().bind(dockable.closableProperty());
		tab.setContent(dockable);
		return tab;
	}


	private DraggableTabPane createTabPane() {
		DraggableTabPane re = new DraggableTabPane();
		re.setAlwaysShowHead(false);
		
		return re;
	}
	
	
	public static DockingPane getParentDockingPane(Node node, int depth) {
		for (Node p =node; p!=null; p = p.getParent()) {
			if (p instanceof DockingPane) {
				if (depth--==0)
					return (DockingPane) p;
			}
		}
		return null;
	}

	
	private DockingPane wrapNode() {
		if (tabs!=null) return new DockingPane(tabs,group);
		else if (splitter!=null) new DockingPane(splitter,group);
		return null;
	}
	
	private Node getNode() {
		if (tabs!=null) return tabs;
		else if (splitter!=null) return splitter;
		return null;
	}
	
	
	private DockingPane setNode(Node node) {
		if (node==null) return setNode(createTabPane());
		if (node instanceof Dockable) return setNode((Dockable)node);
		if (node instanceof DraggableTabPane) return setNode((DraggableTabPane)node);
		if (node instanceof SplitPane) return setNode((SplitPane)node);
		throw new RuntimeException();
	}
	
	private DockingPane setNode(Dockable dockable) {
		DockingPane re = wrapNode();
		this.splitter = null;
		this.tabs = createTabPane();
		this.tabs.getTabs().add(createTab(dockable));
		setCenter(tabs);
		return re;
	}
	
	private DockingPane setNode(DraggableTabPane tabs) {
		DockingPane re = wrapNode();
		this.splitter = null;
		this.tabs = tabs;
		setCenter(tabs);
		return re;
	}
	
	private DockingPane setNode(SplitPane splitter) {
		DockingPane re = wrapNode();
		setCenter(splitter);
		this.tabs = null;
		this.splitter = splitter;
		return re;
	}
	
	
	

	public boolean isSplittingPane() {
		return splitter!=null;
	}
	
	public boolean isTabPane() {
		return tabs!=null;
	}

	
	
	
	
}
