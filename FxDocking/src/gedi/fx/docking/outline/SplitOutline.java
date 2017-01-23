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
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;


public class SplitOutline  extends Group {
	private Bounds bounds;

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
	public SplitOutline() {
		getStyleClass().add("tab-outline-marker");
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
	public void updateBounds(Bounds bounds) {
		if (bounds.equals(this.bounds)) {
			return;
		}

		this.bounds = bounds;

		
		Rectangle rect = new Rectangle(bounds.getMinX(), bounds.getMinY(),bounds.getWidth(), bounds.getHeight());
		rect.strokeProperty().bind(fillProperty());
		rect.setStrokeWidth(3);
		rect.setStrokeType(StrokeType.INSIDE);
		rect.fillProperty().set(Color.TRANSPARENT);
		getChildren().setAll(rect);
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

	private static final CssMetaData<SplitOutline, Paint> FILL = new CssMetaData<SplitOutline, Paint>("-fx-fill", PaintConverter.getInstance(), Color.ORANGE) { //$NON-NLS-1$

		@Override
		public boolean isSettable(SplitOutline node) {
			return !node.fillProperty().isBound();
		}

		@SuppressWarnings("unchecked")
		@Override
		public StyleableProperty<Paint> getStyleableProperty(SplitOutline node) {
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
