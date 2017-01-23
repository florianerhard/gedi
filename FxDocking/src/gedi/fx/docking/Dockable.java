package gedi.fx.docking;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

public class Dockable extends BorderPane {
	
	
	public Dockable(Node node) {
		super(node);
	}

	
	public Dockable(Node node, String label, boolean closable, boolean draggable) {
		super(node);
		setLabel(label);
		setDraggable(draggable);
		setClosable(closable);
	}
	public Dockable(Node node, String label, boolean closable, boolean draggable, Node image) {
		super(node);
		setLabel(label);
		setDraggable(draggable);
		setClosable(closable);
		setImage(image);
	}

	public void set(Dockable d) {
		setCenter(d.getCenter());
		setLabel(d.getLabel());
		setDraggable(d.isDraggable());
		setClosable(d.isClosable());
		setImage(d.getImage());
	}

	private ObjectProperty<Node> image = new SimpleObjectProperty<Node>(null, "image");
	public final ObjectProperty<Node> imageProperty() {
		return this.image;
	}

	public final javafx.scene.Node getImage() {
		return this.imageProperty().get();
	}

	public final void setImage(final javafx.scene.Node image) {
		this.imageProperty().set(image);
	}
	
	
	private StringProperty label = new SimpleStringProperty(null, "image");
	public final StringProperty labelProperty() {
		return this.label;
	}

	public final String getLabel() {
		return this.labelProperty().get();
	}

	public final void setLabel(final String label) {
		this.labelProperty().set(label);
	}

	
	private BooleanProperty closable = new SimpleBooleanProperty(this, "closable", true);
	public final BooleanProperty closableProperty() {
		return this.closable;
	}
	public final boolean isClosable() {
		return this.closableProperty().get();
	}
	public final void setClosable(final boolean closable) {
		this.closableProperty().set(closable);
	}
	
	private BooleanProperty draggable = new SimpleBooleanProperty(this, "draggable", true);
	public final BooleanProperty draggableProperty() {
		return this.draggable;
	}
	public final boolean isDraggable() {
		return this.draggableProperty().get();
	}
	public final void setDraggable(final boolean draggable) {
		this.draggableProperty().set(draggable);
	}

}
