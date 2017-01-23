package gedi.commandline;

import gedi.util.nashorn.JS;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.script.CompiledScript;
import javax.script.ScriptException;

public class CancelableJSThread {

	
	private ThreadPoolExecutor jsThread;
	private JS js;
	private Future<Object> res;
	public Thread thread;
	
	private long timeout = 500;
	private boolean showWarning = true;
	
	public CancelableJSThread(JS js) {
		this.js = js;
		initPool();
	}

	private void initPool() {
		jsThread = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new CancelableThreadFactory());		
	}

	public boolean isRunning() {
		return res!=null;
	}
	
	public CancelableJSThread configCancel(boolean forceCancel, boolean showWarning, long timeoutMilli) {
		if (forceCancel) {
			this.timeout = timeoutMilli;
			this.showWarning = showWarning;
		}
		else
			this.timeout = -1;
		return this;
	}
	
	public boolean cancelCurrent() {
		if (res!=null) {
			Future<Object> res = this.res;
			if (!res.isCancelled()) {
				res.cancel(true);
				return true;
			}
		}
		return false;
	}

	public synchronized JSResult execute(String cmd) throws ScriptException {
		CompiledScript comp = js.compileSource(cmd);

		JSCallable callable = new JSCallable(comp);
		res = jsThread.submit(callable);
		try {
			Object o = res.get();
			res = null;
			return new JSResult(o,callable.e);
		} catch (InterruptedException | ExecutionException | CancellationException e) {
			try {
				if (timeout<0)
					jsThread.submit(()->{}).get();
				else
					jsThread.submit(()->{}).get(timeout,TimeUnit.MILLISECONDS);
			} catch (InterruptedException | ExecutionException | CancellationException e1) {
				throw new RuntimeException("Should not happen!");
			} catch (TimeoutException e1) {
				thread.stop();
				initPool();
				if (showWarning)
					System.err.println("Warning, forced thread to stop, problems may occurr due to deadlocks or resource leaks!");
			}
			res = null;
			return new JSResult(true);
		}
	}
	
	private class JSCallable implements Callable<Object> {
		
		private Throwable e;
		private CompiledScript comp;
		
		public JSCallable(CompiledScript comp) {
			this.comp = comp;
		}

		@Override
		public Object call()  {
			try {
				Object re = comp.eval();
				return re;
			} catch (Throwable e){
				this.e = e;
				return null;
			}
		}
		
	}

	public void shutdown() {
		jsThread.shutdownNow();
	}
	
	
	 /**
     * The default thread factory
     */
	private static final AtomicInteger poolNumber = new AtomicInteger(1);
    class CancelableThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        CancelableThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                                  Thread.currentThread().getThreadGroup();
            namePrefix = "Command-executor-" +
                          poolNumber.getAndIncrement() +
                         "-thread-";
        }

        public Thread newThread(Runnable r) {
            thread = new Thread(group, r,
                                  namePrefix + threadNumber.getAndIncrement(),
                                  0);
            if (!thread.isDaemon())
            	thread.setDaemon(true);
            if (thread.getPriority() != Thread.NORM_PRIORITY)
            	thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        }
    }
	
}
