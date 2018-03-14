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

import static java.util.Arrays.asList;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

public class CompositeClassPath implements ClassPath {

	private final ClassPath[] classPaths;
	
	private HashMap<URL,ClassPath> byURL = new HashMap<URL, ClassPath>();

	public CompositeClassPath(ClassPath... classPaths) {
		this.classPaths = classPaths;
		for (ClassPath cp : classPaths)
			byURL.put(cp.getURL(),cp);
	}
	
	
	public ClassPath getChildByURL(URL url) {
		return byURL.get(url);
	}

	public ClassPath[] getClassPaths() {
		return classPaths;
	}

	public boolean isResource(String resource) {
		for (ClassPath classPath : classPaths) {
			if (classPath.isResource(resource))
				return true;
		}
		return false;
	}

	public boolean isPackage(String packageName) {
		for (ClassPath classPath : classPaths) {
			if (classPath.isPackage(packageName))
				return true;
		}
		return false;
	}

	public String[] listPackages(String packageName) {
		SortedSet<String> packages = new TreeSet<String>();
		for (ClassPath classPath : classPaths) {
			packages.addAll(asList(classPath.listPackages(packageName)));
		}
		return (String[]) packages.toArray(new String[packages.size()]);
	}

	public String[] listResources(String packageName) {
		SortedSet<String> resources = new TreeSet<String>();
		for (ClassPath classPath : classPaths) {
			resources.addAll(asList(classPath.listResources(packageName)));
		}
		return (String[]) resources.toArray(new String[resources.size()]);
	}
	
	@Override
	public URL getURL() {
		return null;
	}

	@Override
	public URL getResourceAsURL(String resource) throws MalformedURLException {
		return null;
	}
	
	public InputStream getResourceAsStream(String resource) {
		for (ClassPath classPath : classPaths) {
			InputStream is = classPath.getResourceAsStream(resource);
			if (is != null)
				return is;
		}
		return null;
	}

	public String[] findResources(String rootPackageName,
			ResourceFilter resourceFilter) {
		SortedSet<String> resources = new TreeSet<String>();
		for (ClassPath classPath : classPaths) {
			resources.addAll(asList(classPath.findResources(rootPackageName, resourceFilter)));
		}
		return (String[]) resources.toArray(new String[resources.size()]);
	}

}
