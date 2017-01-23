package gedi.app;

import gedi.app.classpath.ClassPath;
import gedi.app.classpath.ClassPathCache;
import gedi.util.LogUtils;
import gedi.util.functions.EI;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Logger;

public class Gedi {
	private static final Logger log = Logger.getLogger( Gedi.class.getName() );
	
	private static boolean started = false;
	
	public static void startup() {
		startup(false);
	}
	public synchronized static void startup(boolean discoverClasses) {
		startup(discoverClasses,false);
	}
	public synchronized static void startup(boolean discoverClasses, boolean nolog) {
		if (!started) {
			Config.getInstance();
			LogUtils.config(nolog);
			log.info("Command: "+getStartupCommand());
			log.info("Gedi "+version()+" startup");
			Runtime.getRuntime().addShutdownHook(new Thread(()->log.info("Finished: "+getStartupCommand())));
			Locale.setDefault(Locale.US);
			
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
