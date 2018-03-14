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
package gedi.fx.docking.outline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.javafx.css.converters.PaintConverter;

import javafx.beans.property.ObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.StrokeType;


public class TabOutline  extends Group {
	private Bounds containerBounds;
	private Bounds referenceBounds;
	private boolean before;

	/**
	 * Create a new tab outline
	 *
	 * @param containerBounds
	 *            the bounds of the container
	 * @param referenceBounds
	 *            the bounds of the reference tab
	 * @param before
	 *            <code>true</code> to mark the insert point before reference
	 *            bounds
	 */
	public TabOutline() {
		getStyleClass().add("tab-outline-marker"); //$NON-NLS-1$
	}

	/**
	 * Update the tab outline
	 *
	 * @param containerBounds
	 *            the bounds of the container
	 * @param referenceBounds
	 *            the bounds of the reference tab
	 * @param before
	 *            <code>true</code> to mark the insert point before reference
	 *            bounds
	 */
	public void updateBounds(Bounds containerBounds, Bounds referenceBounds, boolean before) {
		if (containerBounds.equals(this.containerBounds) && referenceBounds.equals(this.referenceBounds) && before == this.before) {
			return;
		}

		this.containerBounds = containerBounds;
		this.referenceBounds = referenceBounds;
		this.before = before;

		Polyline pl = new Polyline();

		Bounds _referenceBounds = referenceBounds;

		if (before) {
			_referenceBounds = new BoundingBox(Math.max(0, _referenceBounds.getMinX() - _referenceBounds.getWidth() / 2), _referenceBounds.getMinY(), _referenceBounds.getWidth(), _referenceBounds.getHeight());
		} else {
			_referenceBounds = new BoundingBox(Math.max(0, _referenceBounds.getMaxX() - _referenceBounds.getWidth() / 2), _referenceBounds.getMinY(), _referenceBounds.getWidth(), _referenceBounds.getHeight());
		}

		pl.getPoints().addAll(
		// -----------------
		// top
		// -----------------
		// start
				Double.valueOf(0.0), Double.valueOf(_referenceBounds.getMaxY()),

				// tab start
				Double.valueOf(_referenceBounds.getMinX()), Double.valueOf(_referenceBounds.getMaxY()),

				// // tab start top
				Double.valueOf(_referenceBounds.getMinX()), Double.valueOf(_referenceBounds.getMinY()),

				// tab end right
				Double.valueOf(_referenceBounds.getMaxX()), Double.valueOf(_referenceBounds.getMinY()),

				// tab end bottom
				Double.valueOf(_referenceBounds.getMaxX()), Double.valueOf(_referenceBounds.getMaxY()),

				// end
				Double.valueOf(containerBounds.getMaxX()), Double.valueOf(_referenceBounds.getMaxY()),

				// -----------------
				// right
				// -----------------
				Double.valueOf(containerBounds.getMaxX()), Double.valueOf(containerBounds.getMaxY()),

				// -----------------
				// bottom
				// -----------------
				Double.valueOf(containerBounds.getMinX()), Double.valueOf(containerBounds.getMaxY()),

				// -----------------
				// left
				// -----------------
				Double.valueOf(containerBounds.getMinX()), Double.valueOf(_referenceBounds.getMaxY()));
		pl.strokeProperty().bind(fillProperty());
		pl.setStrokeWidth(3);
		pl.setStrokeType(StrokeType.INSIDE);
		getChildren().setAll(pl);
	}

	private final ObjectProperty<Paint> fill = new SimpleStyleableObjectProperty<>(FILL, this, "fill", Color.ORANGE); //$NON-NLS-1$

	/**
	 * The fill property
	 *
	 * <p>
	 * The default color {@link Color#ORANGE} <span style=
	 * "background-color: orange; color: orange; border-width: 1px; border-color: black; border-style: solid; width: 15; height: 15;">__</span>
	 * </p>
	 *
	 * @return the property
	 */
	public ObjectProperty<Paint> fillProperty() {
		return this.fill;
	}

	/**
	 * Set a new fill
	 * <p>
	 * The default color {@link Color#ORANGE} <span style=
	 * "background-color: orange; color: orange; border-width: 1px; border-color: black; border-style: solid; width: 15; height: 15;">__</span>
	 * </p>
	 *
	 * @param fill
	 *            the fill
	 */
	public void setFill(Paint fill) {
		fillProperty().set(fill);
	}

	/**
	 * Get the current fill
	 * <p>
	 * The default color {@link Color#ORANGE} <span style=
	 * "background-color: orange; color: orange; border-width: 1px; border-color: black; border-style: solid; width: 15; height: 15;">__</span>
	 * </p>
	 *
	 * @return the current fill
	 */
	public Paint getFill() {
		return fillProperty().get();
	}

	private static final CssMetaData<TabOutline, Paint> FILL = new CssMetaData<TabOutline, Paint>("-fx-fill", PaintConverter.getInstance(), Color.ORANGE) { //$NON-NLS-1$

		@Override
		public boolean isSettable(TabOutline node) {
			return !node.fillProperty().isBound();
		}

		@SuppressWarnings("unchecked")
		@Override
		public StyleableProperty<Paint> getStyleableProperty(TabOutline node) {
			return (StyleableProperty<Paint>) node.fillProperty();
		}

	};

	private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;

	static {
		final List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<CssMetaData<? extends Styleable, ?>>(Group.getClassCssMetaData());
		styleables.add(FILL);
		STYLEABLES = Collections.unmodifiableList(styleables);
	}

	public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
		return STYLEABLES;
	}

	@Override
	public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
		return getClassCssMetaData();
	}
}
