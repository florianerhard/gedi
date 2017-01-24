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

import java.util.regex.Pattern;

import org.controlsfx.validation.ValidationSupport;

import gedi.app.Config;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;

public class EmailConfigurator implements Configurator<String>{

	private static final Pattern VALID_EMAIL_ADDRESS_REGEX = 
		    Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
	public static String name = "User Email";

	
	@Override
	public String getSection() {
		return "Gedi";
	}


	@Override
	public String getLabel() {
		return "User Email";
	}

	@Override
	public String getHelp() {
		return "The email adress is e.g. needed when submitting jobs to a cluster (slurm, sge)";
	}

	@Override
	public TextField getControl() {
		TextField re = new TextField(Config.getInstance().getConfig().getEntry("email").asString(""));
		re.setPromptText("Email");
		re.setPrefColumnCount(50);
		return re;
	}
	
	@Override
	public Node getAdditionalNode(Control parent) {
		return null;
	}


	@Override
	public String validate(String value) {
		if (!VALID_EMAIL_ADDRESS_REGEX.matcher(value).find()) 
			return "Not a valid email adress!";
		return null;
	}

	@Override
	public void set(String value) {
		Config.getInstance().setConfig("email",value);
	}

}
