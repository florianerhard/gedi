package gedi.fx.inspector;

import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;

public class InspectorPane extends BorderPane{

	private Object o;
	
	
	public InspectorPane(Object o) {
		this.o = o;
		setPadding(new Insets(5, 0, 5, 0));
		setStyle("-fx-background-color: DAE6F3;");
	}
	
}
