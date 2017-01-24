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

package gedi.remote;

import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.SocketChannel;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RetryRemoteConnection implements Runnable, AutoCloseable {

	private static final Logger log = Logger.getLogger( RetryRemoteConnection.class.getName() );

	private URI uri;
	private Consumer<SocketChannel> initChannel;
	private long retryMillisec;
	private boolean killConnectionOnInterupt = true;
	
	private AtomicBoolean interupted = new AtomicBoolean(false);
	private AtomicReference<RemoteRetryEventType> currentType = new AtomicReference<RemoteRetryEventType>();
	
	private List<RetryListener> listeners = Collections.synchronizedList(new ArrayList<RetryListener>());
	private ChannelFuture currentFeature;
	
	public RetryRemoteConnection(URI uri, Consumer<SocketChannel> initChannel,
			long retryMillisec, boolean killConnectionOnInterupt) {
		this.uri = uri;
		this.initChannel = initChannel;
		this.retryMillisec = retryMillisec;
		this.killConnectionOnInterupt = killConnectionOnInterupt;
	}


	public void addListener(RetryListener l) {
		listeners.add(l);
	}

	public void removeListener(RetryListener l) {
		listeners.remove(l);
	}

	public RemoteRetryEventType getCurrentType() {
		return currentType.get();
	}
	
	@Override
	public void run() {
		
		log.log(Level.INFO, "Initial connection");
		while (!interupted.get()) {
			currentFeature = RemoteConnections.getInstance().connect(uri, initChannel, (e)->{
				// is already logged, so do nothing!
				synchronized (RetryRemoteConnection.this) {
					RemoteRetryEvent t = new RemoteRetryEvent(RemoteRetryEventType.EXCEPTION,e);
					currentType.set(RemoteRetryEventType.EXCEPTION);
					for (RetryListener l : listeners)
						l.remoteRetryEvent(t);
					RetryRemoteConnection.this.notify();
				}
			}, (f)->{
				synchronized (RetryRemoteConnection.this) {
					currentFeature = f.closeFuture();
					RemoteRetryEvent t = new RemoteRetryEvent(RemoteRetryEventType.CONNECTED);
					currentType.set(RemoteRetryEventType.CONNECTED);
					for (RetryListener l : listeners)
						l.remoteRetryEvent(t);
				}
			},()->{
				synchronized (RetryRemoteConnection.this) {
					RemoteRetryEvent t = new RemoteRetryEvent(RemoteRetryEventType.CLOSED);
					currentType.set(RemoteRetryEventType.CLOSED);
					for (RetryListener l : listeners)
						l.remoteRetryEvent(t);
					RetryRemoteConnection.this.notify();
				}
			});
			
			RemoteRetryEvent t = new RemoteRetryEvent(RemoteRetryEventType.CONNECTING);
			currentType.set(RemoteRetryEventType.CONNECTING);
			for (RetryListener l : listeners)
				l.remoteRetryEvent(t);
			
			try {
				synchronized (RetryRemoteConnection.this) {
					RetryRemoteConnection.this.wait();
				}
				synchronized (RetryRemoteConnection.this) {
					RetryRemoteConnection.this.wait(retryMillisec);
				}
				log.log(Level.INFO, "Retrying connection");
			} catch (InterruptedException e1) {
				interupted.set(true);
			}
			
		}
		
		if (killConnectionOnInterupt)
			currentFeature.cancel(true);
	}
	
	@Override
	public void close() {
		interupted.set(true);
	}

	@FunctionalInterface
	public interface RetryListener {
		
		void remoteRetryEvent(RemoteRetryEvent event);
		
	}
	
	public class RemoteRetryEvent {
		
		private RemoteRetryEventType type;
		private Throwable exception;
		
		public RemoteRetryEvent(RemoteRetryEventType type) {
			this.type = type;
		}
		
		public RemoteRetryEvent(RemoteRetryEventType type, Throwable exception) {
			this.type = type;
			this.exception= exception;
		}

		public URI getURI(){
			return uri;
		}
		
		public ChannelFuture getCurrentFuture() {
			return currentFeature;
		}
		
		public Throwable getException() {
			return exception;
		}
		
		public RemoteRetryEventType getType() {
			return type;
		}
		
	}
	
	public enum RemoteRetryEventType {
		CONNECTING,CONNECTED,CLOSED,EXCEPTION
	}
}
