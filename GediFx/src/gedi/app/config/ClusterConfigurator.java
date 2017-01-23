package gedi.app.config;

import java.awt.Desktop;
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
import gedi.util.io.text.jph.Jhp;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ClusterConfigurator implements Configurator<String>{

	@Override
	public String getSection() {
		return "Cluster";
	}

	@Override
	public String getLabel() {
		return "Cluster";
	}

	@Override
	public String getHelp() {
		return "To run jobs in a cluster environment, config files must be placed in $HOME/.gedi/cluster.";
	}

	@Override
	public Button getAdditionalNode(Control parent) {
		
		Button copy = new Button("Copy example to $HOME/.gedi/cluster");
		copy.setOnAction(ae->{
			try {
				
				String src = new LineIterator(getClass().getResource("/resources/templates/example.slurm").openStream()).concat("\n");
				FileUtils.writeAllText(src, new File(Config.getInstance().getConfigFolder()+"/cluster/example.slurm"));
				
				src = new LineIterator(getClass().getResource("/resources/templates/example.slurm.json").openStream()).concat("\n");
				FileUtils.writeAllText(src, new File(Config.getInstance().getConfigFolder()+"/cluster/example.slurm.json"));
				copy.setDisable(true);
				
				if (Desktop.isDesktopSupported()) {
					new Thread(() -> {
		        	   try {
		        		   Desktop.getDesktop().open(new File(Config.getInstance().getConfigFolder()+"/cluster"));
						} catch (IOException e) {
						}
				       }).start();
					
				}
			} catch (Exception e) {
				new ExceptionDialog(e).showAndWait();
			}
		});
		
		return copy;
	}
	

	@Override
	public Control getControl() {
		Label tf = new Label("To run jobs in a cluster environment, config files must be placed in $HOME/.gedi/cluster. Use the button to copy example.slurm and examples.slurm.json (for the SLURM grid engine) and to open the folder.");
		tf.setWrapText(true);
		return tf;
	}


	private boolean isConfigured() {
		return true;
	}
	
	@Override
	public String validate(String value) {
		return null;
	}

	@Override
	public void set(String value) {
	}

}
