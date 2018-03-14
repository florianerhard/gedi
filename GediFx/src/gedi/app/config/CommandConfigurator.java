/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */
package gedi.app.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;
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
import gedi.util.RunUtils;
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

public abstract class CommandConfigurator implements Configurator<String>{

	private String section;
	private String name;
	private String command;
	private String url;
	protected int exitCode = 0;
	protected Predicate<String> outputChecker = null;
	
	
	public CommandConfigurator(String section, String name, String command, String url) {
		super();
		this.section = section;
		this.name = name;
		this.command = command;
		this.url = url;
	}

	@Override
	public String getSection() {
		return section;
	}

	@Override
	public String getLabel() {
		return name+" executable";
	}

	@Override
	public String getHelp() {
		return "For some use cases, "+name+" must be runnable from command line.";
	}

	@Override
	public VBox getAdditionalNode(Control parent) {
		return null;
	}
	
	@Override
	public Control getControl() {
		TextField tf = new TextField("Testing '"+command+"'");
		tf.setPrefColumnCount(50);
		tf.setDisable(true);
		return tf;
	}


	private boolean isConfigured() {
		try {
			if (outputChecker!=null)
				return outputChecker.test(RunUtils.output("bash","-i", "-c", command+"; exit &> /dev/null"));
			return new ProcessBuilder().command("bash","-i", "-c", command+"; exit &> /dev/null").start().waitFor()==exitCode;
		} catch (InterruptedException | IOException e) {
			return false;
		}
	}
	
	@Override
	public String validate(String value) {
		return isConfigured()?null:"Cannot call "+name+"! Install "+name+" from "+url+", and put it into the path variable!";
	}

	@Override
	public void set(String value) {
	}

}
