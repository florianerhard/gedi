package gedi.app.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import org.controlsfx.dialog.ExceptionDialog;
import org.controlsfx.tools.ValueExtractor;
import org.controlsfx.validation.ValidationSupport;

import gedi.app.Config;
import gedi.app.classpath.ClassPath;
import gedi.app.classpath.ClassPathCache;
import gedi.app.classpath.DirectoryClassPath;
import gedi.app.classpath.JARClassPath;
import gedi.util.FileUtils;
import gedi.util.io.Directory;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.io.text.StreamLineReader;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class BowtieCommandConfigurator extends CommandConfigurator {

	public static String name = "bowtie-starter";
	
	public BowtieCommandConfigurator() {
		super("Read mapping","Bowtie","bowtie --version && bowtie-build --version","http://bowtie-bio.sourceforge.net/index.shtml");
	}

	
	@Override
	public String getHelp() {
		return "For mapping of short reads (e.g. in Ribo-seq -> PRICE), bowtie must be installed and runnable from command line!";
	}
	
}
