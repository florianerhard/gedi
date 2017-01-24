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

package gedi.util.functions;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * Put is thread-safe!
 * @author erhard
 *
 * @param <T>
 */
public class IterateIntoSink<T> {

	private LinkedBlockingQueue<T> queue;
	private T terminator = (T) new Object();
	private Thread t;
	
	public IterateIntoSink(Consumer<ExtendedIterator<T>> sink) {
		this.queue = new LinkedBlockingQueue<T>(1024);
		
		
		t = new Thread() {
			@Override
			public void run() {
				try {
					sink.accept(new SinkIterator());
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		};
		t.setDaemon(true);
		t.setName("SinkIterator");
		t.start();
		
	}
	
	
	public void put(T element) throws InterruptedException {
		queue.put(element);
	}
	
	public void finish() throws InterruptedException {
		queue.put(terminator);
		t.join();
	}

	
	private class SinkIterator implements ExtendedIterator<T> {
		private T n;
	
		@Override
		public boolean hasNext() {
			if(n==null)
				try {
					n=queue.take();
				} catch (InterruptedException e) {
					return false;
				}
			return n!=terminator;
		}
	
	
		@Override
		public T next() {
			if(n==null)
				try {
					n=queue.take();
				} catch (InterruptedException e) {
				}
			T re = n;
			n = null;
			return re;
		}
	}
	
	
}
