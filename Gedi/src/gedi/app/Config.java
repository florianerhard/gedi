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

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.sun.org.apache.bcel.internal.util.ClassPath;

import gedi.app.classpath.ClassPathCache;
import gedi.util.FileUtils;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.EI;
import gedi.util.io.text.LineIterator;

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
			try {
				config = DynamicObject.parseJson(new LineIterator(getClass().getResource("/resources/default.config").openStream()).readAllText());
				File cf = new File(new File(ClassPathCache.getInstance().getClassPathOfClass(getClass()).toString()).getParentFile(),DEFAULT_CONFIG_NAME);
				if (cf.exists())
					config = config.cascade(DynamicObject.parseJson(FileUtils.readAllText(cf)));
				
				cf = new File(getConfigFolder()+"/"+DEFAULT_CONFIG_NAME);
				if (cf.exists())
					config = config.cascade(DynamicObject.parseJson(FileUtils.readAllText(cf)));
				
				
//				FileUtils.writeAllText(config.toJson(), cf);
			} catch (IOException e) {
				throw new RuntimeException("Cannot read config!",e);
			}
			
		}
		return config;
	}
	
	public List<String> getTemplateSearchURLs() {
		return EI.wrap(getConfig().getEntry("templates").asArray()).map(d->d.asString()).list();
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
