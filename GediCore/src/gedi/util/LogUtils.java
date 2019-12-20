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
package gedi.util;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.LogManager;


/**
 * Call config before any logging method is used, and then use
 * @author erhard
 *
 */
public class LogUtils {
	
	static {
        // must be called before any Logger method is used.
        System.setProperty("java.util.logging.manager", MyLogManager.class.getName());
    }


	public enum LogMode {
		Normal,Silent,Debug
	}
	
//	public static void setFile(String path) throws SecurityException, IOException {
//		setFile(path,false);
//	}
//	
//	public static void setFile(String path, boolean append) throws SecurityException, IOException {
//		removeHandlers();
//		Logger.getLogger("global").getParent().addHandler(new ConsoleHandler(){{setLevel(Level.SEVERE);}});
//		Logger.getLogger("global").getParent().addHandler(new FileHandler(path, append));
//	}
//	
//	public static void removeHandlers() {
//		for (Handler h : Logger.getLogger("global").getParent().getHandlers())
//			Logger.getLogger("global").getParent().removeHandler(h);
//	}
	
    public static class MyLogManager extends LogManager {
        static MyLogManager instance;
        public MyLogManager() { instance = this; }
        @Override public void reset() { if (doShutdown) reset0(); }
        private void reset0() { super.reset(); }
        public static void resetFinally() { if (instance!=null) instance.reset0(); }
    }
    
    public static void shutdown() {
    	MyLogManager.resetFinally();
    }

    private static boolean doShutdown = true;
	
	public static void config() {
		config(LogMode.Normal,true);
	}
	
	public static void config(LogMode mode, boolean doShutdown) {
		LogUtils.doShutdown = doShutdown;
		
		String fname = System.getProperty("java.util.logging.config.file");
		if (fname == null) { // already configured, do nothing!
			try {
				String res;
				if (mode==LogMode.Silent)
					res = "/logging_silent.properties";
				else if (mode==LogMode.Debug)
					res = "/logging_debug.properties";
				else
					res = "/logging.properties";
				URL intlog = LogUtils.class.getResource(res);
				InputStream str = intlog.openStream();
				LogManager.getLogManager().readConfiguration(str);
				
				str.close();
			} catch (Exception e) {
				throw new RuntimeException("Could not read internal logging configuration!",e);
			}
			
		}
			
	}
//	
//	
//	public static void setLevel(Level level) throws SecurityException, IOException {
//		removeHandlers();
//		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).getParent().addHandler(new ConsoleHandler(){{
//			setLevel(level);
//			setFormatter(new Formatter(){
//				private final Date dat = new Date();
//				public synchronized String format(LogRecord record) {
//			        dat.setTime(record.getMillis());
//			        String source;
//			        if (record.getSourceClassName() != null) {
//			            source = record.getSourceClassName();
//			            if (record.getSourceMethodName() != null) {
//			               source += " " + record.getSourceMethodName();
//			            }
//			        } else {
//			            source = record.getLoggerName();
//			        }
//			        String message = formatMessage(record);
//			        String throwable = "";
//			        if (record.getThrown() != null) {
//			            StringWriter sw = new StringWriter();
//			            PrintWriter pw = new PrintWriter(sw);
//			            pw.println();
//			            record.getThrown().printStackTrace(pw);
//			            pw.close();
//			            throwable = sw.toString();
//			        }
//			        return String.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL %4$s %5$s%6$s%n",
//			                             dat,
//			                             source,
//			                             record.getLoggerName(),
//			                             record.getLevel().getName(),
//			                             message,
//			                             throwable);
//			    }
//			});
//		}});
//	}
	
}
