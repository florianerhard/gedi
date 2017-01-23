package gedi.fx.docking;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;


public class DockGroup {


	public DockGroup() {
		this.detachable = new ReadOnlyBooleanWrapper(this, "detachable", true);
	}
	

	public DockGroup(boolean detachable) {
		this.detachable = new ReadOnlyBooleanWrapper(this, "detachable", detachable);
	}
	
	private ReadOnlyBooleanProperty detachable;

	public final ReadOnlyBooleanProperty detachableProperty() {
		return this.detachable;
	}

	public final boolean isDetachable() {
		return this.detachableProperty().get();
	}

	
}
