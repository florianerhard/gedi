package gedi.core.workspace.loader;

import java.io.IOException;
import java.nio.file.Path;

public interface WorkspaceItemLoader<T> {

	String[] getExtensions();

	T load(Path path) throws IOException;

	Class<T> getItemClass();

	boolean hasOptions();
	
	void updateOptions(Path path);
	
}
