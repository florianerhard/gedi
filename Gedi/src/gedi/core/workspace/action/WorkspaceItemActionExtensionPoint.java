package gedi.core.workspace.action;

import gedi.app.extension.DefaultExtensionPoint;

import java.util.logging.Logger;

public class WorkspaceItemActionExtensionPoint extends DefaultExtensionPoint<Class, WorkspaceItemAction>{

	protected WorkspaceItemActionExtensionPoint() {
		super(WorkspaceItemAction.class);
	}

	private static final Logger log = Logger.getLogger( WorkspaceItemActionExtensionPoint.class.getName() );

	private static WorkspaceItemActionExtensionPoint instance;

	public static WorkspaceItemActionExtensionPoint getInstance() {
		if (instance==null) 
			instance = new WorkspaceItemActionExtensionPoint();
		return instance;
	}




}
