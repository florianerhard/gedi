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
package gedi.util.algorithm.string.alignment.pairwise.chain;

import java.io.IOException;
import java.util.function.UnaryOperator;

import gedi.core.data.annotation.GenomicRegionMappable;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.text.LineWriter;

public class AlignmentChain {

	private ReferenceSequence ALI = Chromosome.obtain("ALI+");
	
	private int id;
	private long score;
	private int targetSequenceLength; // for compatibility with liftover
	private ReferenceGenomicRegion<Void> targetRegion;
	private int querySequenceLength; // for compatibility with liftover
	private ReferenceGenomicRegion<Void> queryRegion;
	
	private ArrayGenomicRegion targetBlocks; // in the coordinate system of the alignment
	private ArrayGenomicRegion queryBlocks; // in the coordinate system of the alignment
	
	private ImmutableReferenceGenomicRegion<Void> targetInAli;
	private ImmutableReferenceGenomicRegion<Void> queryInAli;
	private ArrayGenomicRegion interBlocks;
	
	public AlignmentChain(int id, long score, int targetSequenceLength, ReferenceSequence targetReference,
			GenomicRegion targetRegion, int querySequenceLength, ReferenceSequence queryReference,
			GenomicRegion queryRegion, ArrayGenomicRegion targetBlocks, ArrayGenomicRegion queryBlocks) {
		this.id = id;
		this.score = score;
		this.targetSequenceLength = targetSequenceLength;
		this.targetRegion = new ImmutableReferenceGenomicRegion<>(targetReference, targetRegion);
		this.querySequenceLength = querySequenceLength;
		this.queryRegion = new ImmutableReferenceGenomicRegion<>(queryReference, queryRegion);
		this.targetBlocks = targetBlocks;
		this.queryBlocks = queryBlocks;
		targetInAli = new ImmutableReferenceGenomicRegion<>(ALI, targetBlocks);
		queryInAli = new ImmutableReferenceGenomicRegion<>(ALI, queryBlocks);
		interBlocks = targetBlocks.intersect(queryBlocks);
	}
	
	public AlignmentChain opposite() {
		return new AlignmentChain(id, score, targetSequenceLength, targetRegion.getReference().toOppositeStrand(), targetRegion.getRegion(), 
				querySequenceLength, queryRegion.getReference().toOppositeStrand(), queryRegion.getRegion(), 
				targetBlocks, queryBlocks);
	}
	
	public AlignmentChain independent() {
		return new AlignmentChain(id, score, targetSequenceLength, targetRegion.getReference().toStrandIndependent(), targetRegion.getRegion(), 
				querySequenceLength, queryRegion.getReference().toStrandIndependent(), queryRegion.getRegion(), 
				targetBlocks, queryBlocks);
	}

	public int getId() {
		return id;
	}

	public long getScore() {
		return score;
	}

	public int getTargetSequenceLength() {
		return targetSequenceLength;
	}

	public int getQuerySequenceLength() {
		return querySequenceLength;
	}
	
	public ArrayGenomicRegion getTargetBlocks() {
		return targetBlocks;
	}
	
	public ArrayGenomicRegion getQueryBlocks() {
		return queryBlocks;
	}
	
	public <D> ImmutableReferenceGenomicRegion<D> map(ImmutableReferenceGenomicRegion<D> r) {
		if (!r.getReference().equals(queryRegion.getReference())) 
			throw new IllegalArgumentException("Invalid reference sequence!");
		
		System.err.println("AlignmentChain mapping not tested!");
		
		ArrayGenomicRegion rinter = r.getRegion().intersect(queryRegion.getRegion());
		if (!rinter.equals(r.getRegion())) {
			// handler partial chain
		}
		
		// r in alignment coordinates
		ArrayGenomicRegion rq = queryRegion.getRegion().induce(rinter);
		rq = queryBlocks.map(rq);
		
		rinter = rq.intersect(interBlocks);
		if (!rinter.equals(rq)) {
			// handler partial alignment
		}
		
		rinter = targetBlocks.induce(rinter);
		// mapped r in genomic coordinates
		GenomicRegion re = targetRegion.getRegion().map(rinter);
		D data = r.getData();
		
		if (data instanceof GenomicRegionMappable) {
			GenomicRegionMappable d = (GenomicRegionMappable) data;
			d = d.induce(queryRegion).map(queryInAli).induce(targetInAli).map(targetRegion);
			data = (D) d;
		}
		
		if (re.isEmpty())
			System.err.println(r+" "+re);
		 
		
		return new ImmutableReferenceGenomicRegion<>(targetRegion.getReference(), re, data);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		//     chain score tName tSize tStrand tStart tEnd qName qSize qStrand qStart qEnd id 
		sb.append("chain ").append(score).append(" ")
			.append(targetRegion.getReference().getName()).append(" ")
			.append(targetSequenceLength).append(" ")
			.append(targetRegion.getReference().getStrand().toString()).append(" ")
			.append(targetRegion.getRegion().getStart()).append(" ")
			.append(targetRegion.getRegion().getEnd()).append(" ")
			.append(queryRegion.getReference().getName()).append(" ")
			.append(querySequenceLength).append(" ")
			.append(queryRegion.getReference().getStrand().toString()).append(" ")
			.append(queryRegion.getRegion().getStart()).append(" ")
			.append(queryRegion.getRegion().getEnd()).append(" ")
			.append(id).append("\n");
		
		ArrayGenomicRegion inter = targetBlocks.intersect(queryBlocks);

		for (int i=0; i<inter.getNumParts(); i++) {
			sb.append(inter.getLength(i));
			if (i<inter.getNumParts()-1) {
				int gt = targetBlocks.induce(inter.getStart(i+1))-targetBlocks.induce(inter.getStop(i))-1;
				int gq = queryBlocks.induce(inter.getStart(i+1))-queryBlocks.induce(inter.getStop(i))-1;
				sb.append("\t").append(gt).append("\t").append(gq);
			}
			sb.append("\n");
		}
		return sb.toString();
	}
	
	
	public static MemoryIntervalTreeStorage<AlignmentChain> load(String file) throws IOException {
		MemoryIntervalTreeStorage<AlignmentChain> re = new MemoryIntervalTreeStorage<>(AlignmentChain.class);
		
		fromChain(EI.lines(file))
			.unfold(ac->EI.wrap(ac,ac.opposite(),ac.independent()))
			.map(ac->ac.queryRegion.toImmutable(ac))
			.add(re);
		
		return re;
	}
	
	public static ExtendedIterator<AlignmentChain> fromChain(ExtendedIterator<String> lines) {
		return lines.block(l->l.startsWith("chain")).map(l->fromString(EI.wrap(l)));
	}

	public static AlignmentChain fromString(String chain) {
		return fromString(EI.split(chain, '\n'));
	}
	public static AlignmentChain fromString(ExtendedIterator<String> lines) {
		String[] h = EI.split(lines.next(), ' ').toArray(String.class);
		if (!h[0].equals("chain") || h.length!=13) 
			throw new IllegalArgumentException("First line must be: chain score tName tSize tStrand tStart tEnd qName qSize qStrand qStart qEnd id");
		
		IntArrayList t = new IntArrayList();
		IntArrayList q = new IntArrayList();
		t.add(0);
		q.add(0);
		while (lines.hasNext()) {
			int[] p = StringUtils.parseInt(StringUtils.split(lines.next(), '\t'));
			int offt = t.getLastInt();
			int offq = t.getLastInt();
			
			t.add(offt+p[0]);
			q.add(offq+p[0]);
			if (p.length==3) {
				t.removeLast();
				t.add(offt+p[0]+p[1]);
				t.add(offt+p[0]+p[1]+p[2]);
				q.add(offq+p[0]+p[1]);
			}
			else if (p.length==1)
				break;
		}
		
		if (!lines.hasNext() || !lines.next().equals(""))
			throw new IllegalArgumentException("Must end with empty line!");
		
//	     chain score tName tSize tStrand tStart tEnd qName qSize qStrand qStart qEnd id 
		return new AlignmentChain(Integer.parseInt(h[12]),
				Long.parseLong(h[1]),
				Integer.parseInt(h[3]),
				Chromosome.obtain(h[2],h[4]),
				new ArrayGenomicRegion(Integer.parseInt(h[5]),Integer.parseInt(h[6])),
				Integer.parseInt(h[8]),
				Chromosome.obtain(h[7],h[9]),
				new ArrayGenomicRegion(Integer.parseInt(h[10]),Integer.parseInt(h[11])),
				new ArrayGenomicRegion(t),
				new ArrayGenomicRegion(q)
				);
				
		
	}
	
	/**
	 * Resorts unaligned parts of the alignment s.t. first come all positions from 0 and then from 1. 
	 * @param blocks
	 */
	public static void normalize(ArrayGenomicRegion[] blocks) {
		if (blocks.length!=2 || blocks[0].getStart()!=blocks[1].getStart() || blocks[0].getEnd()!=blocks[1].getEnd() || blocks[0].union(blocks[1]).getNumParts()!=1)
			throw new RuntimeException("Invalid alignment blocks!");
		
		ArrayGenomicRegion inter = blocks[0].intersect(blocks[1]);
		ArrayGenomicRegion notali = inter.invert();
		
		IntArrayList a = new IntArrayList();
		IntArrayList b = new IntArrayList();
		a.add(inter.getStart());
		b.add(inter.getStart());
		for (int i=0; i<notali.getNumParts(); i++) {
			int ga = notali.getPart(i).asRegion().intersect(blocks[0]).getTotalLength();
			int gb = notali.getPart(i).asRegion().intersect(blocks[1]).getTotalLength();
			a.add(notali.getStart(i)+ga);
			a.add(notali.getStart(i)+ga+gb);
			b.add(inter.getEnd(i));
			b.add(notali.getStart(i)+ga);
		}
		a.add(inter.getEnd());
		b.add(inter.getEnd());
		
		blocks[0] = new ArrayGenomicRegion(a);
		blocks[1] = new ArrayGenomicRegion(b);
	}

	
	public static AlignmentChain fromAlignment(int id, long score, ReferenceSequence targetRef, ReferenceSequence queryRef, String target, String query) {
		int al = query.length(); 
		if (target.length()!=al) throw new IllegalArgumentException("Alignment string must have same length!");
		
		
		// remove leading and trailing gaps
		// and gap gap columns due to multiple alignment
		StringBuilder qb = new StringBuilder(query.length());
		StringBuilder tb = new StringBuilder(target.length());
		int leadingTarget = 0;
		int leadingQuery= 0;
		int trailingTarget = 0;
		int trailingQuery= 0;
		
		int lastboth = -1;
		for (int i=0; i<al; i++) {
			boolean qgap = i==al||query.charAt(i)=='-';
			boolean tgap = i==al||target.charAt(i)=='-';
			if (!qgap && !tgap) 
				lastboth = qb.length();
			
			if (lastboth==-1) {
				leadingQuery+=!qgap?1:0;
				leadingTarget+=!tgap?1:0;
			}
			
			
			if (lastboth!=-1 && (!qgap || !tgap)) {
				qb.append(query.charAt(i));
				tb.append(target.charAt(i));
				
				trailingQuery+=!qgap?1:0;
				trailingTarget+=!tgap?1:0;
			}
			
			if (!qgap && !tgap) 
				trailingQuery = trailingTarget = 0;
		}
		
		target = tb.substring(0,lastboth+1);
		query = qb.substring(0,lastboth+1);
		
		
		ArrayGenomicRegion[] blocks = {
				SequenceUtils.getAlignedRegion(target),
				SequenceUtils.getAlignedRegion(query)
		};
		normalize(blocks);
		
		
		return new AlignmentChain(id, score, blocks[0].getTotalLength()+leadingTarget+trailingTarget, targetRef, new ArrayGenomicRegion(leadingTarget,blocks[0].getTotalLength()+leadingTarget), 
										blocks[1].getTotalLength()+leadingQuery+trailingQuery, queryRef, new ArrayGenomicRegion(leadingQuery,blocks[1].getTotalLength()+leadingQuery), 
											blocks[0], blocks[1]);
		
	}
	
	
}
