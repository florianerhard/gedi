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

package gedi.core.processing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.core.sequence.SequenceProvider;
import gedi.util.io.text.LineOrientedFile;

public class FillStorageProcessor implements GenomicRegionProcessor {

	ReferenceGenomicRegion<?> END = new ReferenceGenomicRegion<Object>() {

		@Override
		public ReferenceSequence getReference() {
			return null;
		}

		@Override
		public GenomicRegion getRegion() {
			return null;
		}

		@Override
		public Object getData() {
			return null;
		}
	};
	
	
	private GenomicRegionStorage out;
	private LinkedBlockingQueue<ReferenceGenomicRegion> filler;
	private LinkedBlockingQueue<Boolean> finished;
	private boolean running = false;
	private Thread fillerThread;
	private Exception fillerException;

	public FillStorageProcessor(GenomicRegionStorage out) {
		this.out = out;
	}


	@Override
	public void begin(ProcessorContext context) throws Exception {
		filler = new LinkedBlockingQueue<ReferenceGenomicRegion>(1024);
		finished = new LinkedBlockingQueue<Boolean>(1);
		running = true;
		fillerThread = new Thread() {
			public void run() {
				try {
					out.fill(new Itr());
				} catch (Exception t) {
					fillerException = t;
				}
				finished.add(true);
			}
		};
		fillerThread.start();
	}
	
	private class Itr implements Iterator<ReferenceGenomicRegion> {

		ReferenceGenomicRegion n = null;
		
		@Override
		public boolean hasNext() {
			if (n!=null) return true;
			try {
				n = filler.take();
			} catch (InterruptedException e) {
			}
			return n!=END;
		}

		@Override
		public ReferenceGenomicRegion next() {
			ReferenceGenomicRegion r = n;
			n = null;
			return r;
		}
		
	}

	
	@Override
	public void beginRegion(MutableReferenceGenomicRegion<?> region,
			ProcessorContext context) throws Exception {
	}
	private ArrayList<GenomicRegion> buff = new ArrayList<GenomicRegion>();
	@Override
	public void read(MutableReferenceGenomicRegion<?> region,
			MutableReferenceGenomicRegion<AlignedReadsData> read,
			ProcessorContext context) throws Exception {
		
		OverlapMode mode = context.get(ProcessorContext.OverlapMode);
		buff.clear();
		for (GenomicRegion encountered : encounteredRegions.getRegionsIntersecting(region.getReference(), read.getRegion(), buff)) {
			if (mode.test(encountered, context.get(ProcessorContext.EXON_TREE),read.getRegion()))
				return;
		}
		
		try {
			if (!fillerThread.isAlive()) throw fillerException;
			filler.put((ReferenceGenomicRegion) read.toImmutable());
		} catch (InterruptedException e) {
		}
	}
	
	private MemoryIntervalTreeStorage<Void> encounteredRegions = new MemoryIntervalTreeStorage<Void>(Void.class);
	
	@Override
	public void endRegion(MutableReferenceGenomicRegion<?> region,
			ProcessorContext context) throws Exception {
		encounteredRegions.add(region.getReference(), region.getRegion(), null);
	}

	@Override
	public void end(ProcessorContext context) throws Exception {
		running = false;
		filler.put(END);
		finished.take();
		if (fillerException!=null) throw fillerException;
		
	}


	




}
