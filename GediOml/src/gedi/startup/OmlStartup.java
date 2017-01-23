package gedi.startup;

import gedi.app.Startup;
import gedi.app.classpath.ClassPath;
import gedi.core.workspace.loader.WorkspaceItemLoaderExtensionPoint;
import gedi.oml.OmlLoader;

public class OmlStartup implements Startup {

	@Override
	public void accept(ClassPath t) {
		
		WorkspaceItemLoaderExtensionPoint.getInstance().addExtension(OmlLoader.class,"oml");
		WorkspaceItemLoaderExtensionPoint.getInstance().addExtension(OmlLoader.class,"oml.jhp");
		
		
		
	}
	
	
}
