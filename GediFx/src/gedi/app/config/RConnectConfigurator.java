package gedi.app.config;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import org.controlsfx.dialog.ExceptionDialog;
import org.controlsfx.validation.ValidationSupport;

import gedi.app.Config;
import gedi.util.FileUtils;
import gedi.util.RunUtils;
import gedi.util.io.text.LineIterator;
import gedi.util.r.RConnect;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;

public class RConnectConfigurator implements Configurator<String>{

	
	@Override
	public String getSection() {
		return "R";
	}


	@Override
	public String getLabel() {
		return "RServe";
	}

	@Override
	public String getHelp() {
		return Config.getInstance().getRserveCommand()+" must be executable from command line. Make sure to install the R package 'RServe'";
	}

	
	@Override
	public Node getAdditionalNode(Control parent) {
		return null;
	}

	@Override
	public Control getControl() {
		TextField tf = new TextField("Testing '"+Config.getInstance().getRserveCommand()+"'");
		tf.setPrefColumnCount(50);
		tf.setDisable(true);
		return tf;
	}


	private boolean isConfigured() {
		try {
			RConnect.R().close();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	@Override
	public String validate(String value) {
		return isConfigured()?null:"Cannot call "+Config.getInstance().getRserveCommand()+". Install the RServe package for R: sudo Rscript -e 'install.packages('RServe')!";
	}


	@Override
	public void set(String value) {
	}

}
