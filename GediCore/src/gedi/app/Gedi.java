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
package gedi.app;

import gedi.app.classpath.ClassPath;
import gedi.app.classpath.ClassPathCache;
import gedi.app.classpath.JARClassPath;
import gedi.util.LogUtils;
import gedi.util.LogUtils.LogMode;
import gedi.util.StringUtils;
import gedi.util.functions.EI;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Logger;

public class Gedi {
	
	private static boolean started = false;
	
	public static void startup() {
		startup(false);
	}
	public synchronized static void startup(boolean discoverClasses) {
		startup(discoverClasses,isDebug()?LogMode.Debug:LogMode.Normal, null);
	}
	private static boolean isDebug() {
		return java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("jdwp") >= 0;
	}
	public synchronized static void startup(boolean discoverClasses, LogMode mode, String app) {
		if (!started) {
			Locale.setDefault(Locale.US);
			long start = System.currentTimeMillis();
			Config.getInstance();
			LogUtils.config(mode,false);
			
			Logger log = Logger.getLogger( Gedi.class.getName() );
			log.info("OS: "+System.getProperty("os.name")+" "+System.getProperty("os.version")+" "+System.getProperty("os.arch"));
			log.info("Java: "+System.getProperty("java.vm.name")+" "+System.getProperty("java.runtime.version"));
			log.info("Gedi version "+version()+" ("+develOption()+") startup");
			if (isDebug()) 
				log.info("Debug mode");
			
			log.info("Command: "+getStartupCommand());
			
			if (app!=null)
				log.info(app+" version "+version(app));
			
			Runtime.getRuntime().addShutdownHook(new Thread(()->{
				log.info("Finished: "+getStartupCommand());
				log.info("Took "+StringUtils.getHumanReadableTimespan(System.currentTimeMillis()-start));
				LogUtils.shutdown();
			}));
			
			if (discoverClasses)
				ClassPathCache.getInstance().discover();
			ClassPathCache.getInstance().startup();
		}
		else if (discoverClasses)
			ClassPathCache.getInstance().discover();
		
		started = true;
	}
	
	public static String getStartupCommand() {
		String cmd = System.getProperty("sun.java.command");
		if (cmd.startsWith("executables."))
			return "gedi -e "+cmd.substring("executables.".length());
		return "gedi "+cmd;
	}
	
	private static Properties versions;
	private synchronized static Properties getVersions() throws IOException {
		if (versions==null) {
			versions = new Properties();
			for (ClassPath cp : ClassPathCache.getInstance().getClasspath()) {
				for (String res : cp.listResources("resources"))
					if (res.endsWith(".version"))
						versions.load(cp.getResourceAsStream("/resources/"+res));
			}
		}
		return versions;
	}
	
	public static String develOption() {
		return ClassPathCache.getInstance().getClassPathOfClass(Gedi.class) instanceof JARClassPath?"JAR":"devel";
	}
	
	public static String version(String app) {
		try {
			return getVersions().getProperty(app);
		} catch (IOException e) {
			throw new RuntimeException("Could not read version!",e);
		}
	}
	
	public static String version() {
		return version("Gedi");
	}
	
	public static List<String> apps() {
		try {
			return EI.wrap(getVersions().keySet()).cast(String.class).list();
		} catch (IOException e) {
			throw new RuntimeException("Could not read versions!",e);
		}
	}
	
	
}
