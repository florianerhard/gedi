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

/*
 * Copyright 2007 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package gedi.app.classpath;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

public class JARClassPath implements ClassPath {

	private static class Package {
		private final Map<String, Package> packages = new TreeMap<String, Package>();
		private final SortedSet<String> resources = new TreeSet<String>();
	}

	private final File file;
	private final Package root = new Package();
	private JarFile jarFile;
	private static final Logger logger = Logger.getLogger(JARClassPath.class.getCanonicalName());

	public JARClassPath(File jarFile) {
		this.file = jarFile;
		try {
			loadEntries();
		} catch (IOException e) {
			throw new RuntimeException("Could not load JAR file "+jarFile.getName(),e);
		}
	}

	
	public ClassPath loadEntries() throws IOException {
		try {
			jarFile = new JarFile(file);
		} catch (ZipException e) {
			logger.warning("Failed to read Jar file " + file.getAbsolutePath());
			throw e;
		}
		Enumeration<JarEntry> enumeration = jarFile.entries();
		while (enumeration.hasMoreElements()) {
			JarEntry entry = enumeration.nextElement();
			String path = entry.getName();
			if (entry.isDirectory()) {
				addPackage(path);
			} else {
				addResource(path);
			}
		}
		return this;
	}
	
	public boolean isResource(String resource) {
		int index = resource.lastIndexOf("/");
		String path = index == -1 ? "" : resource.substring(0, index);
		String name = index == -1 ? resource : resource.substring(index + 1);
		Package pkg = getPackage(path);
		return pkg != null && pkg.resources.contains(name);
	}

	public boolean isPackage(String packageName) {
		return getPackage(packageName) != null;
	}

	public String[] listPackages(String packageName) {
		Package pkg = getPackage(packageName);
		if (pkg == null) {
			return new String[0];
		}
		Set<String> packages = pkg.packages.keySet();
		return (String[]) packages.toArray(new String[packages.size()]);
	}

	public String[] listResources(String packageName) {
		Package pkg = getPackage(packageName);
		if (pkg == null) {
			return new String[0];
		}
		SortedSet<String> resources = pkg.resources;
		return (String[]) resources.toArray(new String[resources.size()]);
	}
	
	@Override
	public URL getURL() {
		try {
			return file.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException();
		}
	}
	
	@Override
	public URL getResourceAsURL(String resource) throws MalformedURLException {
		while (resource.startsWith("/")) {
			resource = resource.substring(1);
		}
		return new URL("jar:"+file.toURI().toURL().toString()+"!/"+resource);
	}

	public InputStream getResourceAsStream(String resource) {
		while (resource.startsWith("/")) {
			resource = resource.substring(1);
		}
		ZipEntry entry = jarFile.getEntry(resource);
		if (entry == null) {
			return null;
		} else {
			try {
				return jarFile.getInputStream(entry);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public String[] findResources(String rootPackageName, ResourceFilter resourceFilter) {
		return new ResourceFinder(this).findResources(rootPackageName, resourceFilter);
	}

	private Package addPackage(String path) {
		String[] parts = path.split("/");
		Package pkg = root;
		for (int i = 0; i < parts.length; i++) {
			String name = parts[i];
			if (pkg.packages.containsKey(name)) {
				pkg = pkg.packages.get(name);
			} else {
				Package newPkg = new Package();
				pkg.packages.put(name, newPkg);
				pkg = newPkg;
			}
		}
		return pkg;
	}

	private void addResource(String resource) {
		int index = resource.lastIndexOf("/");
		String path = index == -1 ? "" : resource.substring(0, index);
		String name = index == -1 ? resource : resource.substring(index + 1);
		Package pkg = addPackage(path);
		pkg.resources.add(name);
	}

	private Package getPackage(String packageName) {
		String[] parts = packageName.split("/");
		Package pkg = root;
		for (int i = 0; i < parts.length; i++) {
			String name = parts[i];
			if (name.equals("")) {
				continue;
			}
			pkg = pkg.packages.get(name);
			if (pkg == null) {
				return null;
			}
		}
		return pkg;
	}

	@Override
	public String toString() {
		return file.getPath();
	}
	
}
