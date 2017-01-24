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


import gedi.app.extension.DefaultExtensionPoint;

public class ConfiguratorExtensionPoint extends DefaultExtensionPoint<String, Configurator>{

	private static ConfiguratorExtensionPoint instance;

	public static ConfiguratorExtensionPoint getInstance() {
		if (instance==null) 
			instance = new ConfiguratorExtensionPoint();
		return instance;
	}
	
	protected ConfiguratorExtensionPoint() {
		super(Configurator.class);
	}
	
	
	public void addExtension(Class<? extends Configurator> extension) {
		super.addExtension(extension, extension.getName());
	}

}

