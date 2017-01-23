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

public class RPackagereshape2Configurator extends RPackageConfigurator {

	public RPackagereshape2Configurator() {
		super("reshape2");
	}

}
