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

public abstract class RPackageConfigurator implements Configurator<String>{

	
	private String pack;
	
	public RPackageConfigurator(String pack) {
		super();
		this.pack = pack;
	}


	@Override
	public String getSection() {
		return "R";
	}


	@Override
	public String getLabel() {
		return "R package "+pack;
	}

	@Override
	public String getHelp() {
		return "Some use cases need the package "+pack+"";
	}

	
	@Override
	public Node getAdditionalNode(Control parent) {
		return null;
	}

	@Override
	public Control getControl() {
		TextField tf = new TextField("Checking for the R package '"+pack+"'");
		tf.setPrefColumnCount(50);
		tf.setDisable(true);
		return tf;
	}


	private boolean isConfigured() {
		try {
			String out = RunUtils.output(Config.getInstance().getRscriptCommand(),"-e","\""+pack+"\" %in% rownames(installed.packages())");
			return out.endsWith("TRUE\n");
		} catch (Exception e) {
			return false;
		}
	}
	
	@Override
	public String validate(String value) {
		return isConfigured()?null:"R package "+pack+" not installed!. Call: sudo Rscript -e 'install.packages('"+pack+"')!";
	}


	@Override
	public void set(String value) {
	}

}
