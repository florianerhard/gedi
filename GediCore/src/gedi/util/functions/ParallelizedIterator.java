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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.logging.Logger;


/**
 * n+1 threads are started:
 * 
 * 1. Controller thread: drains the input iterator into a buffer (with given size), retrieves an idle worker thread (if available, otherwise it blocks)
 * and copies the contents of the buffer into the input buffer of the worker. I.e. this thread will block when blockSize*threads elements are 
 * retrieved from the input iterator, and no parallel operation has finished yet.
 * 
 * 2. Worker threads: once notified by the controller, it executes the given actions, filling its output buffer. When finished, it copies it into a ResultBlock object
 * (also containing the block index), and puts it into a blocking priority queue. Then it waits until notified from the drain thread that the result has been taken
 * (otherwise the pq would fill up with all the results).
 * 
 * 3. Drain thread: The thread that calls hasnext and next of this. It will block until one worker thread has put a result into the pq. Then it
 * will retrieve the result (based on the block index) and get its elements for iteration. If there are no more elements, it will again block until
 * a result becomes available.
 * 
 * Once the input iterator has no more elements, the controller thread will notify  a worker to process the last (not full) buffer, will shutdown all workers
 * and put a termination result object into the queue, that is taken by the drain thread as a signal that no more elements are available. 
 * 
 * @author erhard
 *
 * @param <I>
 * @param <O>
 */
public class ParallelizedIterator<I,O,S> implements ExtendedIterator<O>{

	private static final Logger log = Logger.getLogger( ParallelizedIterator.class.getName() );
	
	private Worker[] runners;
	private int blockSize;
	
	private ArrayList<I> block = new ArrayList<I>();
	
	private LinkedBlockingQueue<Worker> freeRunner = new LinkedBlockingQueue<Worker>();
	
	private LinkedBlockingQueue<ResultBlock> output = new LinkedBlockingQueue<>();
	
//	private Function<ExtendedIterator<I>,ExtendedIterator<O>> process;
	private BiFunction<ExtendedIterator<I>,S,ExtendedIterator<O>> process;
	private Thread controller;
	private Thread drainThread;
	private Object drainLock = new Object();
	
	
	private Queue<O> checkQueue;
	private BiFunction<O,O,String> checker;
	
	private AtomicInteger inputContr = new AtomicInteger();
	private AtomicInteger inputWork = new AtomicInteger();
	private AtomicInteger outputWork = new AtomicInteger();
	private AtomicInteger outputContr = new AtomicInteger();
	
	private Object[] states;
	
	private IntObjectConsumer<S> blockStateMaker;
	
	public ParallelizedIterator(ExtendedIterator<I> in, BiFunction<O,O,String> checker, Function<ExtendedIterator<I>,ExtendedIterator<O>> sub) {
		this(in,Runtime.getRuntime().availableProcessors(),1024, checker, sub);
	}
	
	public ParallelizedIterator(ExtendedIterator<I> in, Function<ExtendedIterator<I>,ExtendedIterator<O>> sub) {
		this(in,Runtime.getRuntime().availableProcessors(),1024, null, sub);
	}
	public ParallelizedIterator(ExtendedIterator<I> in, int threads, int blocksize, BiFunction<O,O,String> checker, Function<ExtendedIterator<I>,ExtendedIterator<O>> sub) {
		this(in, threads, blocksize, checker, (i)->null, null, (it,s)->sub.apply(it));
	}
	public ParallelizedIterator(ExtendedIterator<I> in, int threads, int blocksize, BiFunction<O,O,String> checker, IntFunction<S> stateMaker, BiFunction<ExtendedIterator<I>,S,ExtendedIterator<O>> sub) {
		this(in, threads, blocksize, checker, stateMaker, null, sub);
	}
	public ParallelizedIterator(ExtendedIterator<I> in, int threads, int blocksize, BiFunction<O,O,String> checker, IntFunction<S> stateMaker, IntObjectConsumer<S> blockStateMaker, BiFunction<ExtendedIterator<I>,S,ExtendedIterator<O>> sub) {
		this.blockSize = blocksize;
		this.process = sub;
		this.states = new Object[threads];
		this.blockStateMaker = blockStateMaker;
		
		runners = (Worker[]) Array.newInstance(Worker.class, threads);
		log.fine("Starting "+runners.length+" workers");
		for (int i=0; i<runners.length; i++)  {
			runners[i] = new Worker(i,stateMaker.apply(i));
			states[i]=runners[i].state;
			runners[i].start();
		}
		while(freeRunner.size()!=threads)
			Thread.yield();
		log.fine("done");
		
		if (checker!=null) {
			this.checker = checker;
			checkQueue = new LinkedList<O>();
			S state = stateMaker.apply(-1);
			in = in.sideEffect(i->sub.apply(EI.wrap(i),state).toCollection(checkQueue));
		}
		
		ExtendedIterator<I> fin = in;
		controller = new Thread("ControllerThread") {
			public void run() {
				try {
					while (fin.hasNext()) 
						if (!consume(fin))
							return;
					finish();
				} catch (Throwable e) {
					ex=e;
					synchronized (drainLock) {
						if (drainThread!=null)
							drainThread.interrupt();	
					}
				}
			}	
		};
		controller.setDaemon(true);
		controller.start();
		log.fine("Started controller");
		
	}

	public int getNthreads() {
		return states.length;
	}
	
	public ExtendedIterator<S> drainStates() {
		drain();
		return (ExtendedIterator<S>) EI.wrap(states);
	}
	
	public S getState(int index) {
		return (S) states[index];
	}
	
	private long resultIndex = 0;
	int nindex = 0;
	private Object[] next = null;
	private Throwable ex = null;
	
	
	@Override
	public boolean hasNext() {
		tryNext();
		if (checkQueue!=null) {
			if (nindex<0 && !checkQueue.isEmpty())
				fatal(new RuntimeException("Check failed: Additional objects in queue!"));
		}
		return nindex>=0;
	}
	
	@Override
	public O next() {
		tryNext();
		if (checkQueue!=null) {
			O re = (O) next[nindex++];
			O ce = checkQueue.poll();
			String err = checker.apply(re, ce);
			if (err!=null)
				fatal(new RuntimeException(err));
			return re;
		}
		return (O) next[nindex++];
	}
	
	private void tryNext() {
		if (nindex==-1) return;
		if (next!=null && nindex<next.length)
			return;
		
		synchronized (drainLock) {
			if (ex!=null) fatal(ex);
			drainThread = Thread.currentThread();
		}
		
		while (next==null || nindex>=next.length) {
			
			ResultBlock res;
			try {
				res = output.take();
			} catch (InterruptedException e) {
				nindex = -1;
				if (ex!=null) fatal(ex);
				return;
			}
			if (res.r==null) {
				nindex = -1;
				if (ex!=null) fatal(ex);
				if (inputContr.get()!=inputWork.get())
					throw new RuntimeException("Fatal error in parallelized iterator: input sizes do not match!");
				if (outputContr.get()!=outputWork.get())
					throw new RuntimeException("Fatal error in parallelized iterator: output sizes do not match!");
				return;
			}
			outputContr.addAndGet(res.r.length);
			
			
			resultIndex++;
			nindex = 0;
			next = res.take();
				
		}
		
		synchronized (drainLock) {
			if (ex!=null) fatal(ex);
			drainThread = null;
		}
	}
	
	
	private void fatal(Throwable e) {
		for (int i=0; i<runners.length; i++) {
			runners[i].shutdown();
		}
		controller.interrupt();
		throw new RuntimeException("Exception in parallel iterator thread!",e);
	}
	
	private int blockIndex = 0;
	private boolean consume(ExtendedIterator<I> in) {
		if (block.size()==blockSize) { 
			try {
				Worker runner = freeRunner.take();
				if (blockStateMaker!=null)
					blockStateMaker.accept(blockIndex++, runner.state);
				runner.setTasks(block);
				inputContr.addAndGet(block.size());
				block.clear();
				log.fine("Put block to worker thread "+runner.getName());
				
			} catch (InterruptedException e) {
				return false;
			}
		}
		block.add(in.next());
		return true;
	}

	
	private void finish() {
		try {
			Worker runner = freeRunner.take();
			runner.setTasks(block);
			if (blockStateMaker!=null)
				blockStateMaker.accept(blockIndex++, runner.state);
			inputContr.addAndGet(block.size());
			block.clear();
			log.fine("Put last block to worker thread "+runner.getName());
			
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		for (int i=0; i<runners.length; i++) {
			runners[i].shutdown();
		}
		
		waitForFinish();
		log.fine("Finished processing input");
		
		try {
			output.put(new ResultBlock(null, null));
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	private void waitForFinish() {
		// wait until all threads finish
		int alives;
		do {
			alives = 0;
			for (int i=0; i<runners.length; i++) {
				if (runners[i].isAlive())
					alives++;
			}
			Thread.yield();
		} while (alives>0);		
	}


	private static class ResultBlock {
		private ParallelizedIterator.Worker runner;
		private Object[] r;
		
		public ResultBlock(ParallelizedIterator.Worker runner, Object[] r) {
			this.runner = runner;
			this.r = r;
		}
		
		public Object[] take() {
			synchronized (runner) {
				runner.notify();	
			}
			return r;
		}

	}
	
	
	private class Worker extends Thread{
		
		private ArrayList<I> tasks = new ArrayList<I>();
		private ArrayList<O> output = new ArrayList<O>();
		private boolean shut = false;
		private S state;
		
		public Worker(int n, S state) {
			super("RunnerThread"+n);
			this.state = state;
			setDaemon(true);
		}
		
		@Override
		public void run() {
			
			try {
				synchronized (this) {
					while (!isInterrupted()) {
						
						try {
							freeRunner.add(this);
							wait();
						} catch (InterruptedException e) {
							break;
						}
						if (tasks.size()==0 && shut) break;
						log.fine(getName()+": processing block");
						
						inputWork.addAndGet(tasks.size());
						
						ExtendedIterator<O> it = process.apply(EI.wrap(tasks),state);
						it.toCollection(output);
						tasks.clear();
						
						outputWork.addAndGet(output.size());
						
						try {
							ParallelizedIterator.this.output.put(new ResultBlock(this,output.toArray()));
							output.clear();
							wait();
						} catch (InterruptedException e) {
							break;
						}
						if (shut) break;
						log.fine(getName()+": done");
						
					}
				}
			} catch (Throwable e) {
				ex = e;
				
				synchronized (drainLock) {
					if (drainThread!=null)
						drainThread.interrupt();	
				}
				
			}
			
		}
		
		public synchronized void shutdown() {
			shut = true;
			notify();
		}
		
		
		public synchronized void setTasks(ArrayList<I> tasks) {
			this.tasks.addAll(tasks);
			notify();
		}

		
	}
}
