package gedi.oml;

import gedi.core.workspace.loader.WorkspaceItemLoader;

import java.io.IOException;
import java.nio.file.Path;

public class OmlLoader<T> implements WorkspaceItemLoader<T> {

	private static String[] extensions = {"oml","oml.jhp"};
	
	@Override
	public String[] getExtensions() {
		return extensions;
	}

	@Override
	public T load(Path path) throws IOException {
		return Oml.create(path.toString());
	}

	@Override
	public Class<T> getItemClass() {
		return (Class<T>)Object.class;
	}

	@Override
	public boolean hasOptions() {
		return false;
	}

	@Override
	public void updateOptions(Path path) {
		
	}

}
