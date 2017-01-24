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


import gedi.fx.docking.outline.SplitOutline;
import gedi.fx.docking.outline.TabOutline;

import java.lang.reflect.Field;
import java.util.HashSet;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.StyleOrigin;
import javafx.css.StyleableProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import com.sun.glass.ui.Robot;
import com.sun.javafx.scene.control.skin.TabPaneSkin;

public class DraggableTabPane extends StackPane {

	private static MarkerFeedback CURRENT_FEEDBACK;
	private static Tab DRAGGED_TAB;
	private static boolean DROP_HANDLED;
	public static final DataFormat TAB_MOVE = new DataFormat("DnDTabPane:tabMove");
	private TabPane tabPane; 
	private DraggableTabPaneSkin skin;


	public DraggableTabPane() {

		Label label = new Label();
		label.setText("Click to close");

		addEventFilter(MouseEvent.MOUSE_PRESSED, me->{
			if (tabPane.getTabs().size()==0) {
				closeThis();
			}
		});

		getChildren().add(label);

		tabPane = new TabPane() {

			@Override
			protected javafx.scene.control.Skin<?> createDefaultSkin() {
				skin = new DraggableTabPaneSkin(this);
				skin.tabHeaderArea.addEventFilter(MouseEvent.MOUSE_EXITED, me->{
					double delta = 0.1;
					if (!skin.tabHeaderArea.localToScene(skin.tabHeaderArea.getBoundsInLocal()).contains(new BoundingBox(me.getSceneX()-delta, me.getSceneY()-delta,delta*2,delta*2))){
						// FIX: fluxbox invokes exited and entered before each clicked!
						skin.showHead(alwaysShowHead.get());
					}

				});
				
				skin.tabHeaderArea.addEventFilter(DragEvent.DRAG_EXITED, me->{
					skin.showHead(alwaysShowHead.get());
				});
				return skin;
			}
		};
		getChildren().add(tabPane);
		tabPane.setTabClosingPolicy(TabClosingPolicy.SELECTED_TAB);
		
		ListChangeListener<? super Tab> autoDetachedCloser = l->{
			if (tabPane.getTabs().size()==0
					 && getParentDockingPane(0).isDirectlyAtScene()
					 && isInDetachedWindow())
				closeThis();
		};
		tabPane.getTabs().addListener(autoDetachedCloser);

		addEventFilter(MouseEvent.MOUSE_MOVED,me->{
			Bounds bounds = localToScene(getBoundsInLocal());
			if (me.getSceneY()>=bounds.getMinY() && me.getSceneY()<=bounds.getMinY()+autoShowHeight.get()) {
				skin.showHead(true);
			}
		});
		
		addEventFilter(DragEvent.DRAG_OVER,me->{
			Bounds bounds = localToScene(getBoundsInLocal());
			if (me.getSceneY()>=bounds.getMinY() && me.getSceneY()<=bounds.getMinY()+autoShowHeight.get()) {
				skin.showHead(true);
			}
		});

		hideSingletonHeadProperty().addListener(ch->{
			if (hideSingletonHeadProperty().get()) 
				tabPane.getTabs().addListener(lcl);
			else 
				tabPane.getTabs().removeListener(lcl);
		});
		if (hideSingletonHeadProperty().get()) 
			tabPane.getTabs().addListener(lcl);
		else 
			tabPane.getTabs().removeListener(lcl);


		TabOutline marker = new TabOutline();
		marker.setManaged(false);
		marker.setMouseTransparent(true);
		getChildren().add(marker);
		marker.setVisible(false);

		SplitOutline marker2 = new SplitOutline();
		marker2.setManaged(false);
		marker2.setMouseTransparent(true);
		getChildren().add(marker2);
		marker2.setVisible(false);
	}

	
	public SingleSelectionModel<Tab> getSelectionModel() {
		return tabPane.getSelectionModel();
	}

	private void closeThis() {
		DockingPane grandParent = getParentDockingPane(1);
		DockingPane parent = getParentDockingPane(0);
		if (grandParent==null && isInDetachedWindow())
			((Stage) parent.getScene().getWindow()).close();
		else if (grandParent!=null)
			grandParent.unsplit(parent);
	}

	private ListChangeListener<? super Tab> lcl = l->{
		setAlwaysShowHead(tabPane.getTabs().size()>1);
	};

	private DoubleProperty autoShowHeight = new SimpleDoubleProperty(this, "autoShowHeight", 10);
	public final DoubleProperty autoShowHeightProperty() {
		return this.autoShowHeight;
	}
	public final double getAutoShowHeight() {
		return this.autoShowHeightProperty().get();
	}
	public final void setAutoShowHeight(final double autoShowHeight) {
		this.autoShowHeightProperty().set(autoShowHeight);
	}


	public BooleanProperty alwaysShowHead = new SimpleBooleanProperty(this,"alwaysShowHead",true);
	public final BooleanProperty alwaysShowHeadProperty() {
		return this.alwaysShowHead;
	}
	public final boolean isAlwaysShowHead() {
		return this.alwaysShowHeadProperty().get();
	}
	public final void setAlwaysShowHead(final boolean alwaysShowHead) {
		this.alwaysShowHeadProperty().set(alwaysShowHead);
	}


	public BooleanProperty hideSingletonHead = new SimpleBooleanProperty(this,"hideSingletonHead",true);
	public final BooleanProperty hideSingletonHeadProperty() {
		return this.hideSingletonHead;
	}
	public final boolean isHideSingletonHead() {
		return this.hideSingletonHeadProperty().get();
	}
	public final void setHideSingletonHead(final boolean hideSingletonHead) {
		this.hideSingletonHeadProperty().set(hideSingletonHead);
	}


	public ObservableList<Tab> getTabs() {
		return tabPane.getTabs();
	}


	private boolean mayDrag(Tab tab) {
		return ((Dockable)tab.getContent()).isDraggable();
	}


	private boolean mayDrop(Tab tab) {
		return DockingPane.getParentDockingPane(tab.getTabPane(), 0).getDockGroup()==getParentDockingPane(0).getDockGroup();
	}

	private boolean mayDetach(Tab tab) {
		return tab.isClosable() && DockingPane.getParentDockingPane(tab.getTabPane(), 0).getDockGroup().isDetachable();
	}


	public void dragFinished(Tab tab) {
		cleanup();
	}

	private void cleanup() {
		if (CURRENT_FEEDBACK != null) {
			CURRENT_FEEDBACK.hide();
			CURRENT_FEEDBACK = null;
		}
	}

	private static HashSet<Stage> detachedStages = new HashSet<Stage>();
	
	private boolean isInDetachedWindow() {
		return detachedStages.contains(getScene().getWindow());
	}

	private void detachTab(Tab tab) {
		TabPane pane = tab.getTabPane();
		pane.getTabs().remove(tab);
		Stage stage = new Stage();
		
		stage.titleProperty().bind(((Stage) getScene().getWindow()).titleProperty());
		
		Robot robot = com.sun.glass.ui.Application.GetApplication().createRobot();
		stage.setX(robot.getMouseX());
		stage.setY(robot.getMouseY());
		
		DockingPane dp = new DockingPane((Dockable)tab.getContent(),getParentDockingPane(0).getDockGroup());
		
		Scene scene = new Scene(dp,pane.getWidth(),pane.getHeight());
		
		stage.setScene(scene);

		stage.show();
		
		detachedStages.add(stage);
		stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, e->detachedStages.remove(stage));
		
	}

	public void dragOverTab(FeedbackData data) {
		if (data.dropType== DropType.NONE) {
			cleanup();
			return;
		}
		MarkerFeedback f = CURRENT_FEEDBACK;
		if (f == null || !f.data.equals(data)) {
			cleanup();
			CURRENT_FEEDBACK = handleOutline(data);
		}
	}

	public void dragOverContent(FeedbackData data) {
		if (data.dropType== DropType.NONE) {
			cleanup();
			return;
		}

		MarkerFeedback f = CURRENT_FEEDBACK;
		if (f == null || !f.data.equals(data)) {
			cleanup();
			CURRENT_FEEDBACK = handleContent(data);
		}
	}

	private MarkerFeedback handleOutline(FeedbackData data) {
		TabOutline marker = null;

		for (Node n : getChildren()) {
			if (n instanceof TabOutline) {
				marker = (TabOutline) n;
			}
		}

		marker.updateBounds(getBoundsInLocal(), new BoundingBox(data.bounds.getMinX(), data.bounds.getMinY(), data.bounds.getWidth(), data.bounds.getHeight()), data.dropType == DropType.BEFORE);
		marker.setVisible(true);

		final TabOutline fmarker = marker;

		return new MarkerFeedback(data) {

			@Override
			public void hide() {
				fmarker.setVisible(false);
			}
		};
	}


	private MarkerFeedback handleContent(FeedbackData data) {
		SplitOutline marker = null;

		for (Node n : getChildren()) {
			if (n instanceof SplitOutline) {
				marker = (SplitOutline) n;
			}
		}

		Bounds bounds = null;
		double dt = data.bounds.getMinY();
		double db = getHeight()-data.bounds.getMaxY();
		double dl = data.bounds.getMinX();
		double dr = getWidth()-data.bounds.getMaxX();
		dt/=getHeight();
		db/=getHeight();
		dl/=getWidth();
		dr/=getWidth();

		double min = Math.min(Math.min(dt,db),Math.min(dl,dr));
		if (dt==min) bounds = new BoundingBox(0, 0, getWidth(), getHeight()/2);
		else if (db==min) bounds = new BoundingBox(0, getHeight()/2, getWidth(), getHeight()/2);
		else if (dl==min) bounds = new BoundingBox(0, 0, getWidth()/2, getHeight());
		else bounds = new BoundingBox(getWidth()/2, 0, getWidth()/2, getHeight());

		if (tabPane.getTabs().size()==0)
			bounds = new BoundingBox(0, 0, getWidth(), getHeight());

		marker.updateBounds(bounds);
		marker.setVisible(true);

		final SplitOutline fmarker = marker;

		return new MarkerFeedback(data) {

			@Override
			public void hide() {
				fmarker.setVisible(false);
			}
		};
	}



	public void droppedTab(Tab draggedTab, Tab targetTab, DropType dropType) {
		TabPane targetPane = targetTab.getTabPane();
		draggedTab.getTabPane().getTabs().remove(draggedTab);
		int idx = targetPane.getTabs().indexOf(targetTab);
		if (dropType == DropType.AFTER) {
			if (idx + 1 <= targetPane.getTabs().size()) {
				targetPane.getTabs().add(idx + 1, draggedTab);
			} else {
				targetPane.getTabs().add(draggedTab);
			}
		} else {
			targetPane.getTabs().add(idx, draggedTab);
		}
		draggedTab.getTabPane().getSelectionModel().select(draggedTab);
	}



	public void droppedContent(Tab draggedTab, Point2D point, DropType dropType) {

		if (tabPane.getTabs().size()==0) {
			draggedTab.getTabPane().getTabs().remove(draggedTab);
			getTabs().add(draggedTab);
			draggedTab.getTabPane().getSelectionModel().select(draggedTab);
			return;
		}


		Side side;
		double dt = point.getY();
		double db = getHeight()-point.getY();
		double dl = point.getX();
		double dr = getWidth()-point.getX();
		dt/=getHeight();
		db/=getHeight();
		dl/=getWidth();
		dr/=getWidth();

		double min = Math.min(Math.min(dt,db),Math.min(dl,dr));
		if (dt==min) side = Side.TOP;
		else if (db==min) side = Side.BOTTOM;
		else if (dl==min) side = Side.LEFT;
		else side=Side.RIGHT;



		DockingPane dock = getParentDockingPane(0);
		draggedTab.getTabPane().getTabs().remove(draggedTab);
		dock.split(draggedTab, side);

	}


	private DockingPane getParentDockingPane(int depth) {
		return DockingPane.getParentDockingPane(this, depth);
	}



	/**
	 * The drop type
	 */
	public enum DropType {
		/**
		 * No dropping
		 */
		NONE,
		/**
		 * Dropped before a reference tab
		 */
		BEFORE,
		/**
		 * Dropped after a reference tab
		 */
		AFTER,
		CONTENT
	}

	private abstract static class MarkerFeedback {
		public final FeedbackData data;

		public MarkerFeedback(FeedbackData data) {
			this.data = data;
		}

		public abstract void hide();
	}

	/**
	 * Data to create a feedback
	 */
	private static class FeedbackData {
		/**
		 * The tab dragged
		 */
		public final Tab draggedTab;
		/**
		 * The reference tab
		 */
		public final Tab targetTab;
		/**
		 * The bounds of the reference tab
		 */
		public final Bounds bounds;
		/**
		 * The drop type
		 */
		public final DropType dropType;

		/**
		 * Create a feedback data
		 * 
		 * @param draggedTab
		 *            the dragged tab
		 * @param targetTab
		 *            the reference tab
		 * @param bounds
		 *            the bounds of the reference tab
		 * @param dropType
		 *            the drop type
		 */
		public FeedbackData(Tab draggedTab, Tab targetTab, Bounds bounds, DropType dropType) {
			this.draggedTab = draggedTab;
			this.targetTab = targetTab;
			this.bounds = bounds;
			this.dropType = dropType;
		}

		/**
		 * Create a feedback data
		 * 
		 * @param draggedTab
		 *            the dragged tab
		 * @param targetTab
		 *            the reference tab
		 * @param bounds
		 *            the bounds of the reference tab
		 * @param dropType
		 *            the drop type
		 */
		public FeedbackData(Tab draggedTab, Bounds bounds, DropType dropType) {
			this.draggedTab = draggedTab;
			this.targetTab = null;
			this.bounds = bounds;
			this.dropType = dropType;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((this.bounds == null) ? 0 : this.bounds.hashCode());
			result = prime * result + this.draggedTab.hashCode();
			result = prime * result + this.dropType.hashCode();
			result = prime * result + ((this.targetTab == null) ? 0 : this.targetTab.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			FeedbackData other = (FeedbackData) obj;
			if (this.bounds == null) {
				if (other.bounds != null)
					return false;
			} else if (!this.bounds.equals(other.bounds))
				return false;
			if (!this.draggedTab.equals(other.draggedTab))
				return false;
			if (this.dropType != other.dropType)
				return false;
			if (this.targetTab == null) {
				if (other.targetTab != null)
					return false;
			} else if (!this.targetTab.equals(other.targetTab))
				return false;
			return true;
		}

	}

	/**
	 * Skin for TabPane which support DnD
	 */
	@SuppressWarnings("restriction")
	public class DraggableTabPaneSkin extends TabPaneSkin  {
		/**
		 * Custom data format for move data
		 */

		private Object noneEnum;
		private StyleableProperty<Object> openAnimation;
		private StyleableProperty<Object> closeAnimation;
		private StackPane headersRegion;
		private StackPane tabHeaderArea;

		/**
		 * Create a new skin
		 * 
		 * @param tabPane
		 *            the tab pane
		 */
		public DraggableTabPaneSkin(TabPane tabPane) {
			super(tabPane);
			hookTabFolderSkin();

			alwaysShowHead.addListener(c->{
				showHead(alwaysShowHead.get());
			});
			showHead(alwaysShowHead.get());
		}

		void showHead(boolean show) {
			double val = show?-1:0;
			if (headersRegion.getMaxHeight()!=val) {
				headersRegion.setMinHeight(val);
				headersRegion.setPrefHeight(val);
				headersRegion.setMaxHeight(val);
				tabPane.requestLayout();
			}
		}

		@SuppressWarnings("unchecked")
		private void hookTabFolderSkin() {
			try {
				Field f_tabHeaderArea = TabPaneSkin.class.getDeclaredField("tabHeaderArea"); //$NON-NLS-1$
				f_tabHeaderArea.setAccessible(true);

				tabHeaderArea = (StackPane) f_tabHeaderArea.get(this);
				tabHeaderArea.setOnDragOver((e) -> e.consume());

				Field f_headersRegion = tabHeaderArea.getClass().getDeclaredField("headersRegion"); //$NON-NLS-1$
				f_headersRegion.setAccessible(true);

				headersRegion = (StackPane) f_headersRegion.get(tabHeaderArea);

				for (Node tabHeaderSkin : headersRegion.getChildren()) {
					tabHeaderSkin.addEventHandler(MouseEvent.DRAG_DETECTED, this::handleDragStart);
					tabHeaderSkin.addEventHandler(DragEvent.DRAG_DONE, this::handleDragDone);
				}

				headersRegion.getChildren().addListener((javafx.collections.ListChangeListener.Change<? extends Node> change) -> {
					while (change.next()) {
						if (change.wasRemoved()) {
							change.getRemoved().forEach((e) -> e.removeEventHandler(MouseEvent.DRAG_DETECTED, this::handleDragStart));
							change.getRemoved().forEach((e) -> e.removeEventHandler(DragEvent.DRAG_DONE, this::handleDragDone));
						}
						if (change.wasAdded()) {
							change.getAddedSubList().forEach((e) -> e.addEventHandler(MouseEvent.DRAG_DETECTED, this::handleDragStart));
							change.getAddedSubList().forEach((e) -> e.addEventHandler(DragEvent.DRAG_DONE, this::handleDragDone));
						}
					}
				});

				tabHeaderArea.addEventHandler(DragEvent.DRAG_OVER, this::handleDragOverTab);
				tabHeaderArea.addEventHandler(DragEvent.DRAG_DROPPED, this::handleDragDroppedTab);
				tabHeaderArea.addEventHandler(DragEvent.DRAG_EXITED, this::handleDragDone2);

				DraggableTabPane.this.addEventHandler(DragEvent.DRAG_OVER, this::handleDragOverContent);
				DraggableTabPane.this.addEventHandler(DragEvent.DRAG_DROPPED, this::handleDragDroppedContent);
				DraggableTabPane.this.addEventHandler(DragEvent.DRAG_EXITED, this::handleDragDone2);


				Field field = TabPaneSkin.class.getDeclaredField("openTabAnimation"); //$NON-NLS-1$
				field.setAccessible(true);
				this.openAnimation = (StyleableProperty<Object>) field.get(this);

				field = TabPaneSkin.class.getDeclaredField("closeTabAnimation"); //$NON-NLS-1$
				field.setAccessible(true);
				this.closeAnimation = (StyleableProperty<Object>) field.get(this);

				for (Class<?> cl : getClass().getDeclaredClasses()) {
					if ("TabAnimation".equals(cl.getSimpleName())) { //$NON-NLS-1$
						for (Enum<?> enumConstant : (Enum<?>[]) cl.getEnumConstants()) {
							if ("NONE".equals(enumConstant.name())) { //$NON-NLS-1$
								this.noneEnum = enumConstant;
								break;
							}
						}
						break;
					}

				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		void handleDragStart(MouseEvent event) {
			try {
				Field f_tab = event.getSource().getClass().getDeclaredField("tab"); //$NON-NLS-1$
				f_tab.setAccessible(true);
				Tab t = (Tab) f_tab.get(event.getSource());

				if (t != null && mayDrag(t)) {
					DRAGGED_TAB = t;
					DROP_HANDLED = false;
					Node node = (Node) event.getSource();
					Dragboard db = node.startDragAndDrop(TransferMode.MOVE);

					WritableImage snapShot = node.snapshot(new SnapshotParameters(), null);

					db.setDragView(snapShot, snapShot.getWidth(), snapShot.getHeight() * 0);

					ClipboardContent content = new ClipboardContent();
					String data = System.identityHashCode(t) + "";
					if (data != null) {
						content.put(TAB_MOVE, data);
					}
					db.setContent(content);
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		@SuppressWarnings("all")
		void handleDragOverTab(DragEvent event) {
			Tab draggedTab = DRAGGED_TAB;
			if (draggedTab == null) {
				return;
			}

			if (!mayDrop(draggedTab)) {
				dragOverTab(new FeedbackData(draggedTab, null, DropType.NONE));
				return;
			}


			// Consume the drag in any case
			event.consume();

			double x = event.getX() - headersRegion.getBoundsInParent().getMinX();

			Node referenceNode = null;
			DropType type = DropType.AFTER;
			for (Node n : headersRegion.getChildren()) {
				Bounds b = n.getBoundsInParent();
				if (b.getMaxX() > x) {
					if (b.getMinX() + b.getWidth() / 2 > x) {
						referenceNode = n;
						type = DropType.BEFORE;
					} else {
						referenceNode = n;
						type = DropType.AFTER;
					}
					break;
				}
			}
			if (referenceNode == null && headersRegion.getChildren().size() > 0) {
				referenceNode = headersRegion.getChildren().get(headersRegion.getChildren().size() - 1);
				type = DropType.AFTER;
			}
			if (referenceNode != null) {
				try {
					Field field = referenceNode.getClass().getDeclaredField("tab"); //$NON-NLS-1$
					field.setAccessible(true);
					Tab tab = (Tab) field.get(referenceNode);

					boolean noMove = false;
					if (tab == draggedTab) {
						noMove = true;
					} else if (type == DropType.BEFORE) {
						int idx = getSkinnable().getTabs().indexOf(tab);
						if (idx > 0) {
							if (getSkinnable().getTabs().get(idx - 1) == draggedTab) {
								noMove = true;
							}
						}
					} else {
						int idx = getSkinnable().getTabs().indexOf(tab);

						if (idx + 1 < getSkinnable().getTabs().size()) {
							if (getSkinnable().getTabs().get(idx + 1) == draggedTab) {
								noMove = true;
							}
						}
					}
					if (noMove) {
						dragOverTab(new FeedbackData(draggedTab, null, null, DropType.NONE));
						return;
					}

					Bounds b = referenceNode.getBoundsInLocal();
					b = referenceNode.localToScene(b);
					b = getSkinnable().sceneToLocal(b);

					dragOverTab(new FeedbackData(draggedTab, tab, b, type));
				} catch (Throwable e) {
					e.printStackTrace();
				}

				event.acceptTransferModes(TransferMode.MOVE);
			} else {
				dragOverTab(new FeedbackData(draggedTab, null, DropType.NONE));
			}
		}

		void handleDragOverContent(DragEvent event) {
			Tab draggedTab = DRAGGED_TAB;
			if (draggedTab == null) {
				return;
			}
			if (!mayDrop(draggedTab)) {
				dragOverContent(new FeedbackData(draggedTab, null, DropType.NONE));
				return;
			}

			// Consume the drag in any case
			event.consume();

			Bounds b = new BoundingBox(event.getSceneX(), event.getSceneY(), 0, 0);
			b = tabPane.sceneToLocal(b);
			dragOverContent(new FeedbackData(draggedTab, b, DropType.CONTENT));

			event.acceptTransferModes(TransferMode.MOVE);
		}

		@SuppressWarnings("all")
		void handleDragDroppedTab(DragEvent event) {
			Tab draggedTab = DRAGGED_TAB;
			if (draggedTab == null) {
				return;
			}

			DROP_HANDLED = true;

			double x = event.getX() - headersRegion.getBoundsInParent().getMinX();

			Node referenceNode = null;
			DropType type = DropType.AFTER;
			for (Node n : headersRegion.getChildren()) {
				Bounds b = n.getBoundsInParent();
				if (b.getMaxX() > x) {
					if (b.getMinX() + b.getWidth() / 2 > x) {
						referenceNode = n;
						type = DropType.BEFORE;
					} else {
						referenceNode = n;
						type = DropType.AFTER;
					}
					break;
				}
			}

			if (referenceNode == null && headersRegion.getChildren().size() > 0) {
				referenceNode = headersRegion.getChildren().get(headersRegion.getChildren().size() - 1);
				type = DropType.AFTER;
			}

			if (referenceNode != null) {
				try {
					Field field = referenceNode.getClass().getDeclaredField("tab"); //$NON-NLS-1$
					field.setAccessible(true);
					Tab tab = (Tab) field.get(referenceNode);

					boolean noMove = false;
					if( tab == null ) {
						event.setDropCompleted(false);
						return;
					} else if (tab == draggedTab) {
						noMove = true;
					} else if (type == DropType.BEFORE) {
						int idx = getSkinnable().getTabs().indexOf(tab);
						if (idx > 0) {
							if (getSkinnable().getTabs().get(idx - 1) == draggedTab) {
								noMove = true;
							}
						}
					} else {
						int idx = getSkinnable().getTabs().indexOf(tab);

						if (idx + 1 < getSkinnable().getTabs().size()) {
							if (getSkinnable().getTabs().get(idx + 1) == draggedTab) {
								noMove = true;
							}
						}
					}

					if (!noMove) {
						StyleOrigin openOrigin = this.openAnimation.getStyleOrigin();
						StyleOrigin closeOrigin = this.closeAnimation.getStyleOrigin();
						Object openValue = this.openAnimation.getValue();
						Object closeValue = this.closeAnimation.getValue();
						try {
							this.openAnimation.setValue(this.noneEnum);
							this.closeAnimation.setValue(this.noneEnum);
							droppedTab(draggedTab, tab, type);
							event.setDropCompleted(true);
						} finally {
							this.openAnimation.applyStyle(openOrigin, openValue);
							this.closeAnimation.applyStyle(closeOrigin, closeValue);
						}

					} else {
						event.setDropCompleted(false);
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}

				event.consume();
			}
		}


		@SuppressWarnings("all")
		void handleDragDroppedContent(DragEvent event) {
			Tab draggedTab = DRAGGED_TAB;
			if (draggedTab == null) {
				return;
			}

			DROP_HANDLED = true;

			StyleOrigin closeOrigin = this.closeAnimation.getStyleOrigin();
			Object closeValue = this.closeAnimation.getValue();
			try {
				this.closeAnimation.setValue(this.noneEnum);

				Point2D b = new Point2D(event.getSceneX(), event.getSceneY());
				b = tabPane.sceneToLocal(b);

				droppedContent(draggedTab, b, DropType.CONTENT);
				event.setDropCompleted(true);
			} finally {
				this.closeAnimation.applyStyle(closeOrigin, closeValue);
			}


			event.consume();
		}

		void handleDragDone(DragEvent event) {
			Tab tab = DRAGGED_TAB;
			if (tab == null) {
				return;
			}

			
			if (!DROP_HANDLED) {
				Robot robot = com.sun.glass.ui.Application.GetApplication().createRobot();
				
				Window stage = getScene().getWindow();
				Bounds stageBounds = new BoundingBox(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
				if (!stageBounds.contains(robot.getMouseX(), robot.getMouseY())) {
					if (mayDetach(tab))
						detachTab(tab);
				}
			}
			
			dragFinished(tab);
		}

		void handleDragDone2(DragEvent event) {
			Tab tab = DRAGGED_TAB;
			if (tab == null) {
				return;
			}
			dragFinished(tab);
		}

	}




}
