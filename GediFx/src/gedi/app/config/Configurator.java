package gedi.app.config;

import org.controlsfx.validation.ValidationSupport;

import javafx.scene.Node;
import javafx.scene.control.Control;

public interface Configurator<V> {

	String getSection();
	String getLabel();
	String getHelp();
	Control getControl();
	Node getAdditionalNode(Control parent);
	
	/**
	 * Returns null if everything is ok, an error message otherwise
	 * @param value
	 * @return
	 */
	String validate(V value);
	void set(V value);
	
	
	
	
}
