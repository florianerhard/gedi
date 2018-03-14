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
