package gedi.core.workspace.loader;

import gedi.app.extension.DefaultExtensionPoint;
import gedi.app.extension.ExtensionContext;

import java.nio.file.Path;
import java.util.logging.Logger;

public class WorkspaceItemLoaderExtensionPoint extends DefaultExtensionPoint<String,WorkspaceItemLoader> {

	protected WorkspaceItemLoaderExtensionPoint() {
		super(WorkspaceItemLoader.class);
	}


	private static final Logger log = Logger.getLogger( WorkspaceItemLoaderExtensionPoint.class.getName() );

	private static WorkspaceItemLoaderExtensionPoint instance;

	public static WorkspaceItemLoaderExtensionPoint getInstance() {
		if (instance==null) 
			instance = new WorkspaceItemLoaderExtensionPoint();
		return instance;
	}


	public WorkspaceItemLoader get(Path path) {
		for (String e : ext.keySet())
			if (path.toString().endsWith(e))
				return get(ExtensionContext.emptyContext(),e);
		return null;
	}


}
