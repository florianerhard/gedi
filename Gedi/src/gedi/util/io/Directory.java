package gedi.util.io;

import java.io.File;

public class Directory extends File {

	private static final long serialVersionUID = -82127428023950989L;

	public Directory(Directory parent, String name) {
		super(parent,name);
		if (exists() && !isDirectory())
			throw new IllegalArgumentException("Path does not point to a valid directory!");
	}
	
	public Directory(String path) {
		super(path);
		if (exists() && !isDirectory())
			throw new IllegalArgumentException("Path does not point to a valid directory!");
	}

	public Directory getParentDirectory() {
		return new Directory(getParent());
	}

}
