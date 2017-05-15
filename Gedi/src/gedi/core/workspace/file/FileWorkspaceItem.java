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

package gedi.core.workspace.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;

import gedi.core.workspace.Workspace;
import gedi.core.workspace.WorkspaceItem;
import gedi.core.workspace.WorkspaceItemChangeEvent;
import gedi.core.workspace.loader.WorkspaceItemLoader;
import gedi.core.workspace.loader.WorkspaceItemLoaderExtensionPoint;

public class FileWorkspaceItem implements WorkspaceItem {
	
	private Path path;
	private WorkspaceItemLoader<?,?> loader;
	
	public FileWorkspaceItem(Path path) {
		this.path = path;
	}

	
	private void lazyInit() {
		if (loader==null) {
			loader = WorkspaceItemLoaderExtensionPoint.getInstance().get(path);
		}
	}
	
	@Override
	public int getOptions() {
		lazyInit();
		int options = 0;
		if (loader!=null) options|=LOADABLE;
		return options;
	}

	@Override
	public <T> T load() throws IOException {
		lazyInit();
		if (loader!=null) return (T) loader.load(path);
		return null;
	}

	@Override
	public <T> Class<T> getItemClass() {
		lazyInit();
		if (loader!=null) return (Class<T>) loader.getItemClass();
		return null;
	}

	@Override
	public void forEachChild(Consumer<WorkspaceItem> consumer) {
		try {
			if (Files.isDirectory(path))
				Files.newDirectoryStream(path).forEach(p->consumer.accept(new FileWorkspaceItem(p)));
		} catch (IOException e) {
			Workspace.log.log(Level.SEVERE, "Cannot open "+path, e);
		}
	}

	@Override
	public WorkspaceItem getParent() {
		return new FileWorkspaceItem(path.getParent());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileWorkspaceItem other = (FileWorkspaceItem) obj;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "FileWorkspaceItem [path=" + path + "]";
	}


	@Override
	public String getName() {
		if (Workspace.getCurrent().getRoot()==this) return "Workspace";
		return path.getFileName().toString();
	}


	
}
