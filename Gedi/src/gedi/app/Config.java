package gedi.app;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import gedi.util.FileUtils;
import gedi.util.dynamic.DynamicObject;

public class Config {

	
	private static Config instance;
	
	public static Config getInstance() {
		if (instance==null)
			instance = new Config();
		return instance;
	}
	
	
	private Config(){
		new File(DEFAULT_FOLDER).mkdir();
	}
	
	private static final String DEFAULT_FOLDER = System.getProperty("user.home")+"/.gedi";
	private static final String DEFAULT_BIN_FOLDER = DEFAULT_FOLDER+"/bin";
	private static final String DEFAULT_REAL_FORMAT = "%.4g";
	private static final String DEFAULT_RSERVE_COMMAND = "R CMD Rserve --no-save --no-restore --silent";
	private static final String DEFAULT_RSCRIPT_COMMAND = "Rscript";
	
	private static final String DEFAULT_GEDI_DATABASEPATH = DEFAULT_FOLDER+"/gedi.tables";
	private static final String DEFAULT_PACKAGE_PRIORITIES = DEFAULT_FOLDER+"/package.priorities";
	private static final String DEFAULT_GEDI_DATABASENAME = "gedi";
	private static final String DEFAULT_GEDI_DATABASEPASSWORD = "gedi";
	private static final String DEFAULT_CONFIG_NAME = "config.json";
	private static final String DEFAULT_GEDI_CLUSTERFOLDER = DEFAULT_FOLDER+"/cluster";;
	
	
	
	private String realFormat = DEFAULT_REAL_FORMAT;
	private String rserveCommand = DEFAULT_RSERVE_COMMAND;
	private String rscriptCommand = DEFAULT_RSCRIPT_COMMAND;
	private String gediDatabasePath = DEFAULT_GEDI_DATABASEPATH;
	private String gediDatabaseName = DEFAULT_GEDI_DATABASENAME;
	private String gediDatabasePassword = DEFAULT_GEDI_DATABASEPASSWORD;
	private String gediClusterFolder = DEFAULT_GEDI_CLUSTERFOLDER;
	
	private String packagePriorities = DEFAULT_PACKAGE_PRIORITIES;
	
	private DynamicObject config = null;
	
	public String getRealFormat() {
		return realFormat;
	}


	public String getRserveCommand() {
		return rserveCommand;
	}
	
	public String getRscriptCommand() {
		return rscriptCommand;
	}

	public String getClusterFolder() {
		return gediClusterFolder;
	}

	public String getGediDatabasePath() {
		return gediDatabasePath;
	}
	
	public String getGediDatabaseName() {
		return gediDatabaseName;
	}
	
	public String getGediDatabasePassword() {
		return gediDatabasePassword;
	}
	
	public InputStream getPackagePriorities() {
		try {
			return new FileInputStream(packagePriorities);
		} catch (FileNotFoundException e) {
			try {
				return getClass().getResource("/package.priorities").openStream();
			} catch (IOException e1) {
				throw new RuntimeException("Cannot read package priorities!",e);
			}
		}
	}
	

	public File[] getFiles(FileFilter filter) {
		return new File(DEFAULT_FOLDER).listFiles(filter);
	}
	
	
	public String getConfigFolder() {
		return DEFAULT_FOLDER;
	}
	
	public String getBinFolder() {
		return DEFAULT_BIN_FOLDER;
	}


	public DynamicObject getConfig() {
		if (config==null) {
			File cf = new File(getConfigFolder()+"/"+DEFAULT_CONFIG_NAME);
			if (cf.exists())
				try {
					config = DynamicObject.parseJson(FileUtils.readAllText(cf));
				} catch (IOException e) {
					throw new RuntimeException("Cannot read config!",e);
				}
			else
				config = DynamicObject.getEmpty();
		}
		return config;
	}


	public void setConfig(String key, Object value) {
		config = DynamicObject.cascade(getConfig(),DynamicObject.from(key,value));
		try {
			FileUtils.writeAllText(config.toJson(), new File(getConfigFolder()+"/"+DEFAULT_CONFIG_NAME));
		} catch (IOException e) {
			throw new RuntimeException("Could not write config!",e);
		}
	}


	
}
