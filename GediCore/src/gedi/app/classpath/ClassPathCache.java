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
package gedi.app.classpath;


import gedi.app.Config;
import gedi.app.Startup;
import gedi.util.StringUtils;
import gedi.util.io.text.LineIterator;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Simple class resolution: Skip packages, take according to priority list
 * @author erhard
 *
 */
public class ClassPathCache {
	
	
	private static final Logger log = Logger.getLogger( ClassPathCache.class.getName() );
	
	
	private static ClassPathCache instance;
	public static ClassPathCache getInstance() {
		if (instance==null) instance = new ClassPathCache();
		return instance;
	}
	
	
	
	private CompositeClassPath classpath;
	private ClassPathCache() {
		ClassPathFactory fac = ClassPathFactory.getInstance();
		String clp = fac.getJVMClasspath()+File.pathSeparator+System.getProperty("java.home")+"/lib/rt.jar"+File.pathSeparator+System.getProperty("java.home")+"/lib/ext/jfxrt.jar";
		classpath = fac.createFromPath(clp);
	}
	
	private HashSet<String> fullnames;
	private HashMap<String,String> classes;
	private HashMap<String,HashSet<String>> packageToClasses;
	private HashMap<String,ClassPath> classPaths; // full name to jar/dir
	
	public ClassPath[] getClasspath() {
		return classpath.getClassPaths().clone();
	}
	
	public void discover() {
		if (classes==null){
			log.log(Level.INFO, "Discovering classes in classpath");
			
			fullnames = new HashSet<String>();
			classes = new HashMap<String, String>();
			packageToClasses = new HashMap<>();
			classPaths = new HashMap<String, ClassPath>();
			
			ClassPath[] cps = classpath.getClassPaths().clone();

			for (ClassPath cp : cps) {
				log.log(Level.FINE, "Reading "+cp.getURL());
				Stack<String> pack = new Stack<>();
				pack.push("");
				int n=0;
				while (!pack.isEmpty()) {
					String p = pack.pop();
					for (String c : cp.listPackages(p))
						if (c.length()>0)
							pack.push(p.length()==0?c:(p+"/"+c));
					for (String c : cp.listResources(p)) {
						if (c.endsWith(".class")) {
							c = c.substring(0,c.length()-6);
							String full = (p.length()==0?c:(p+"/"+c)).replace('/', '.');
//							if (classes.containsKey(c) && !full.equals(classes.get(c))) {
//								if (c.endsWith("Utils")) {
//									if (full.startsWith("gedi/util"))
//										classes.put(c, full);
//									else
//										log.log(Level.FINE, "Hide Utils class "+full);
//	
//								} else {
//									if (classes.get(c)!=null)
//										log.log(Level.FINE, "Multiple occurrences of "+c+"; hidden from simple referencing!");
//									classes.put(c,null);
//								}
//							} else
							fullnames.add(full);
							if (full.contains("."))
								classes.put(full, full);
						}
						String full = (p.length()==0?c:(p+"/"+c));
						classPaths.put(full, cp);
						String name = full.substring(full.lastIndexOf('/')+1);
						packageToClasses.computeIfAbsent(p.replace('/', '.'), x->new HashSet<>()).add(name);
						n++;
						
					}
				}
				log.log(Level.FINE, "Found "+n+" classes");
				
			}
			
			log.log(Level.INFO, "Preparing simple class references");
			try (LineIterator it = new LineIterator(Config.getInstance().getPackagePriorities(), "#")){
				while (it.hasNext()) {
					String line = it.next();
					String pref = line;
					log.log(Level.FINE, "Prioritize "+pref);
					boolean recursive = pref.endsWith("**") || pref.endsWith("?");
					boolean ignoreMulti = pref.endsWith("?");
					if (pref.endsWith("**")) pref = pref.substring(0, pref.length()-1);
					
					if (!pref.endsWith(".*") && !pref.endsWith(".?") && !pref.equals("?"))
						throw new RuntimeException("Illegal package "+pref);
					pref = pref.substring(0, pref.length()-1);
					
					HashMap<String,String> simple = new HashMap<String, String>();
					for (String f : fullnames) {
						if (f.startsWith(pref) && (recursive || !f.substring(pref.length()).contains("."))) {
							String c = f.substring(f.lastIndexOf('.')+1);
							boolean isinner = false;
							if (c.contains("$")) {
								c = c.substring(c.lastIndexOf('$')+1);
								if (StringUtils.isNumeric(c))
									continue; // no anonymous classes
								isinner = true;
							}
							if (!classes.containsKey(c)) {
								if (!ignoreMulti && simple.containsKey(c)) {
									if (!isinner) {
										if (simple.get(c).contains("$"))
											simple.put(c, f);
										else
											throw new RuntimeException(c+" exists more than once in package "+line+": "+f+" and "+simple.get(c));
									}
								} else
									simple.put(c, f);
							}
						}
					}
					for (String c : simple.keySet())
						if (!classes.containsKey(c))
							classes.put(c, simple.get(c));
				}
				
				for (String c : fullnames)
					if (!c.contains(".") && !classes.containsKey(c))
						classes.put(c, c);
			} catch (IOException e) {
				throw new RuntimeException("Cannot load package priorities!",e);
			}
		}
	}
	
	
	public void startup() {
		for (ClassPath cp : classpath.getClassPaths()) 
			for (String c : cp.listResources("gedi/startup")) 
				if (c.endsWith(".class")) {
					try {
						Class<?> cls = Class.forName("gedi.startup."+c.substring(0, c.length()-6));
						if (Startup.class.isAssignableFrom(cls)) {
							Startup su = ((Startup)cls.newInstance());
							log.log(Level.CONFIG, "Running startup "+c+" in "+cp);
							su.accept(cp);
						}
					} catch (Throwable e) {
						log.log(Level.SEVERE, "Could not load startup "+c+" in "+cp);
					}
					
				}
	}
	
	
	public HashSet<String> getResourcesOfPath(String path) {
		discover();
		return packageToClasses.get(path.replace('/', '.'));
	}
	
	public HashSet<String> getClassesOfPackage(String pack) {
		discover();
		return packageToClasses.get(pack);
	}
	
	public ClassPath getClassPathByURL(URL url) {
		return classpath.getChildByURL(url);
	}

	
	public ClassPath getClassPathOfClass(Class<?> cls) {
		discover();
		if (cls.isArray()) 
			cls = cls.getComponentType();
		String path = cls.getName();
		if (path.contains("$"))
			path = path.substring(0,path.indexOf('$'));
		path = path.replace('.', '/');
		return classPaths.get(path);
	}
	
	public ClassPath getClassPathOfFile(String path) {
		discover();
		return classPaths.get(path);
	}
	
	
	public boolean existsClass(String fullName) {
		discover();
		return fullnames.contains(fullName);
	}
	
	public HashMap<String,String> getNameToFullNameMap() {
		discover();
		return classes;
	}
	public boolean containsName(String name) {
		return getNameToFullNameMap().containsKey(name);
	}
	public String getFullName(String name) {
		String re = getNameToFullNameMap().get(name);
		if (re==null) throw new IllegalArgumentException("No class with name "+name+" found!");
		return re;
	}
	public <T> T createInstance(String name) {
		try {
			return (T) getClass(name).newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException("Could not instantiate class "+name,e);
		}
	}
	public <T> Class<T> getClass(String name) {
		try {
			return (Class<T>) Class.forName(getFullName(name));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Could not load class "+name,e);
		}
	}


	

	

	
	
}