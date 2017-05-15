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

import gedi.app.Config;
import gedi.util.RunUtils;
import javafx.scene.Node;
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
		return isConfigured()?null:"R package "+pack+" not installed!. Call: sudo Rscript -e 'install.packages(\""+pack+"\") !";
	}


	@Override
	public void set(String value) {
	}

}
