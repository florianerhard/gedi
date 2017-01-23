package gedi.util.r;

import gedi.app.Config;
import gedi.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.rosuda.REngine.Rserve.RserveException;


public class RConnect {

	private static final Logger log = Logger.getLogger( RConnect.class.getName() );

	private static RConnect instance;
	public static synchronized RConnect getInstance() {
		if (instance==null)
			instance = new RConnect();
		return instance;
	}
	public static final R R(boolean log) {
		if (!log) return R(Level.OFF);
		return R();
	}
	
	public static final R R(Level logLevel) {
		Logger.getLogger( R.class.getName() ).setLevel(logLevel);
		Logger.getLogger( RConnect.class.getName() ).setLevel(logLevel);
		return R();
	}
	
	public static final R R() {
		return getInstance().get();
	}
	
	private RConnect() {
		try {
			log.log(Level.CONFIG, "Start Rserve");//--RS-port port
			ProcessBuilder pb = new ProcessBuilder(StringUtils.split(Config.getInstance().getRserveCommand(),' '));
			Process p = pb.start();
			boolean[] bindError = {false};
			new LogInputStreamThread("stdout",Level.FINE,p.getInputStream(),null).start();
			new LogInputStreamThread("stderr",Level.SEVERE,p.getErrorStream(),l->!(bindError[0]|=l.contains("address already in use"))).start();
			int err = p.waitFor();
			if (err!=0) 
				throw new RuntimeException("R returned error code "+err);
			if (!bindError[0])
				Runtime.getRuntime().addShutdownHook(new Thread() {
					@Override
					public void run() {
						if (!connByThread.isEmpty()) {
							R conn = connByThread.values().iterator().next();
							try {
								conn.shutdown();
							} catch (RserveException e) {
							}
							log.log(Level.CONFIG, "Shutdown Rserve");
							RConnect.instance = null;
						}
					}
				});
			
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed running Rserve!",e);
			throw new RuntimeException("Could not connect to R!",e);
		}
	}


	public synchronized void clear() {
		while (!connByThread.isEmpty()) 
			closeThread(connByThread.keySet().iterator().next());
	}
	
	


	private HashMap<Thread, R> connByThread = new HashMap<Thread, R>();

	
	public synchronized void remove() {
		closeThread(Thread.currentThread());
	}
	
	public synchronized void removeWhenDevicesClosed() {
		R conn = get();
		while (conn.getOpenDevices()>0)
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
		closeThread(Thread.currentThread());
	}
	
	private synchronized void closeThread(Thread t) {
		log.log(Level.CONFIG, "Close RConnection for thread "+t);
		R conn = connByThread.remove(t);
		if (conn==null) return;
		
		if (connByThread.size()==0)
			try {
				log.log(Level.CONFIG, "Shutdown Rserve");
				conn.shutdown();
				RConnect.instance = null;
			} catch (RserveException e) {
				log.log(Level.SEVERE, "Failed shuting down Rserve!",e);
				throw new RuntimeException("Could not connect to R!",e);
			}
		conn.close();
	}
	
	public synchronized R get() {
		Thread t = Thread.currentThread();
		R re = connByThread.get(t);
		if (re==null)
			try {
				log.log(Level.CONFIG, "Create RConnection for thread "+t);
				connByThread.put(t,re = new R());
				if (re.eval("setwd('"+System.getProperty("user.dir")+"')")==null)
					log.log(Level.WARNING,"Could not set working directory!");
			} catch (RserveException e) {
				log.log(Level.SEVERE, "Failed creating Rconnection!",e);
				throw new RuntimeException("Could not connect to R!",e);
			}
		return re;
	}

	
	
	private class LogInputStreamThread extends Thread {

		private String channel;
		private Level level;
		private InputStream is;
		private Predicate<String> lineConsumer;
		
		public LogInputStreamThread(String channel, Level level, InputStream is, Predicate<String> lineConsumer) {
			this.channel = channel;
			this.level = level;
			this.is = is;
			setName("R connection "+channel);
			setDaemon(true);
		}

		BufferedReader br;
		@Override
		public void run() {
			br = new BufferedReader(new InputStreamReader(is));
			String line;
			try {
				log.log(Level.CONFIG, "Start reading "+channel);
				while ((line=br.readLine())!=null) {
					if (lineConsumer!=null && !lineConsumer.test(line))
						log.log(level, line);
				}
				br.close();
				log.log(Level.CONFIG, "Finished "+channel);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			
		}
	}
	
}
