package gedi.util;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.LogManager;


public class LogUtils {

	
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
	
	public static void config() {
		config(false);
	}
	
	public static void config(boolean silent) {
		String fname = System.getProperty("java.util.logging.config.file");
		if (fname == null) { // already configured, do nothing!
			try {
				URL intlog = LogUtils.class.getResource(silent?"/logging_silent.properties":"/logging.properties");
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
