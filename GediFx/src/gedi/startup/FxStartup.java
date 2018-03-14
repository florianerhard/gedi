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
package gedi.startup;

import gedi.app.Startup;
import gedi.app.classpath.ClassPath;
import gedi.app.classpath.ClassPathCache;
import gedi.app.config.BowtieCommandConfigurator;
import gedi.app.config.Configurator;
import gedi.app.config.ConfiguratorExtensionPoint;
import gedi.app.config.EmailConfigurator;
import gedi.app.config.KrakenCommandConfigurator;
import gedi.app.config.RCommandConfigurator;
import gedi.app.config.StartCommandConfigurator;
import gedi.core.data.table.Table;
import gedi.core.workspace.action.WorkspaceItemActionExtensionPoint;
import gedi.fx.html.ShowHtmlAction;
import gedi.fx.image.ShowImageAction;
import gedi.fx.table.ShowTableAction;

import java.awt.image.BufferedImage;
import java.lang.reflect.Modifier;
import java.net.URI;

public class FxStartup implements Startup {

	@Override
	public void accept(ClassPath t) {
		
		WorkspaceItemActionExtensionPoint.getInstance().addExtension(ShowTableAction.class,Table.class);
		WorkspaceItemActionExtensionPoint.getInstance().addExtension(ShowImageAction.class,BufferedImage.class);
		WorkspaceItemActionExtensionPoint.getInstance().addExtension(ShowHtmlAction.class,URI.class);
		
		for (String cls : ClassPathCache.getInstance().getClassesOfPackage("gedi.app.config")) {
			try {
				Class c = (Class)Class.forName("gedi.app.config."+cls);
				if (Configurator.class.isAssignableFrom(c) && !c.isInterface() && !Modifier.isAbstract(c.getModifiers()))
					ConfiguratorExtensionPoint.getInstance().addExtension(c);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Could not load class "+cls,e);
			}
		}
		
	}
	
	
}
