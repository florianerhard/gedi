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
package gedi.riboseq.codonprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import gedi.core.data.annotation.Transcript;
import gedi.core.genomic.Genomic;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.feature.special.Downsampling;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.riboseq.analysis.MajorIsoform;
import gedi.riboseq.inference.orf.PriceOrf;
import gedi.riboseq.inference.orf.PriceOrfType;
import gedi.util.SequenceUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.tree.redblacktree.SimpleInterval;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.mutable.MutableTuple;

public class CodonProcessor implements UnaryOperator<ImmutableReferenceGenomicRegion<NumericArray>>{

	public static final int UPSTREAM_SEQUENCE_CONTEXT = 24;
	public static final int DOWNSTREAM_SEQUENCE_CONTEXT = 25;
	
	public static final int DOWNSTREAM_STRUCT_CONTEXT = 25;
	public static final int UPSTREAM_STRUCT_CONTEXT = 25;
	
	
	private Genomic genomic;
	private GenomicRegionStorage<PriceOrf> orfs;
	private GenomicRegionStorage<MajorIsoform> major;
	private HashMap<String, ArrayList<SimpleInterval>> localStruc;
	private MemoryIntervalTreeStorage<Transcript> majorTranscripts;
	private HashSet<String> majorTranscriptNames;
	private Downsampling downsampling = Downsampling.Max;
	
	private HashMap<String,Object> vars = new HashMap<String, Object>();

	private ArrayList<CodonProcessorCounter> counter = new ArrayList<CodonProcessorCounter>();
	
	public CodonProcessor(Genomic genomic, GenomicRegionStorage<PriceOrf> orfs, GenomicRegionStorage<MajorIsoform> major, HashMap<String, ArrayList<SimpleInterval>> localStruc) {
		this.genomic = genomic;
		this.orfs = orfs;
		this.major = major;
		this.localStruc = localStruc;
		
		majorTranscripts = major.ei().map(m->m.getData().getTranscript()).add(new MemoryIntervalTreeStorage<>(Transcript.class));
		majorTranscriptNames = majorTranscripts.ei().map(t->t.getData().getTranscriptId()).set();
	}
	
	public CodonProcessor addDefaultOutputs(String prefix) {
		addCounter(
				new SimpleCodonProcessorCounter(prefix+".frametype",BuiltinVars.OrfType.name(),BuiltinVars.Frame.name())
					.addOutput(new RDataOutput())
					.addOutput(new TsvOutput())
			);		
	
		addCounter(
				new ExpandingCodonProcessorCounter(prefix+".codoncontext",CodonProcessor::iteratePositionCodon,
						BuiltinVars.ContextPosition.name(),BuiltinVars.ContextCodon.name(),BuiltinVars.ContextAA.name())
					.addCondition(BuiltinVars.Frame.name(),0)
					.addCondition(BuiltinVars.InMajor.name(),true)
					.addCondition(vars->((Integer)vars.get(BuiltinVars.AbsoluteCdsPositionStart.name()))>UPSTREAM_SEQUENCE_CONTEXT)
					.addCondition(vars->((Integer)vars.get(BuiltinVars.AbsoluteCdsPositionStop.name()))<-DOWNSTREAM_SEQUENCE_CONTEXT)
					.addOutput(new RDataOutput())
					.addOutput(new TsvOutput())
			);		
		
		addCounter(
				new ExpandingCodonProcessorCounter(prefix+".structurecontext",CodonProcessor::iteratePositionStruct,
						BuiltinVars.LocalStructPos.name())
					.addCondition(BuiltinVars.Frame.name(),0)
					.addCondition(BuiltinVars.InMajor.name(),true)
					.addCondition(vars->((Integer)vars.get(BuiltinVars.AbsoluteCdsPositionStart.name()))>UPSTREAM_STRUCT_CONTEXT)
					.addCondition(vars->((Integer)vars.get(BuiltinVars.AbsoluteCdsPositionStop.name()))<-DOWNSTREAM_STRUCT_CONTEXT)
					.addOutput(new RDataOutput())
					.addOutput(new TsvOutput())
			);		
		
		return this;
	}
	
	public enum BuiltinVars {
		Orf,Transript,Frame,OrfType,
		SequenceContext, ContextPosition, ContextCodon, ContextAA,
		AbsoluteOrfPositionStart,AbsoluteOrfPositionStop,
		AbsoluteCdsPositionStart,AbsoluteCdsPositionStop,
		AbsoluteTranscriptPositionTSS,AbsoluteTranscriptPositionTTS,
		InMajor,
		LocalStructs,LocalStructPos
	}
	

	public CodonProcessor addCounter(CodonProcessorCounter counter) {
		this.counter.add(counter);
		return this;
	}
	
	public ExtendedIterator<CodonProcessorCounter> counters() {
		return EI.wrap(counter);
	}
	
	
	@Override
	public ImmutableReferenceGenomicRegion<NumericArray> apply(ImmutableReferenceGenomicRegion<NumericArray> codon) {

		ImmutableReferenceGenomicRegion<PriceOrf> orf = findOrf(codon);
		int frame = getFrame(orf, codon);
		PriceOrfType orftype = orf==null?PriceOrfType.orphan:orf.getData().getType();
		String sequenceContext = getSequenceContext(orf,codon,UPSTREAM_SEQUENCE_CONTEXT,DOWNSTREAM_SEQUENCE_CONTEXT);
		ReferenceGenomicRegion<Transcript> trans = orf==null?null:genomic.getTranscriptMapping().apply(orf.getData().getTranscript());
		ReferenceGenomicRegion<Transcript> cds = trans!=null && trans.getData().isCoding()?trans.getData().getCds(trans):null;
		
		int orfpos = orf==null?-1:orf.induce(codon.getRegion()).getStart();
		int cdspos = cds==null || !cds.contains(codon)?-1:cds.induce(codon.getRegion()).getStart();
		int tpos = trans==null?-1:trans.induce(codon.getRegion()).getStart();
		boolean isFromMajor = orf!=null && majorTranscriptNames.contains(orf.getData().getTranscript());
		
		
		vars.put(BuiltinVars.Orf.name(), orf);
		vars.put(BuiltinVars.Transript.name(), trans);
		vars.put(BuiltinVars.Frame.name(), frame);
		vars.put(BuiltinVars.OrfType.name(), orftype);
		vars.put(BuiltinVars.SequenceContext.name(), sequenceContext);
		
		vars.put(BuiltinVars.AbsoluteOrfPositionStart.name(), orfpos);
		vars.put(BuiltinVars.AbsoluteOrfPositionStop.name(), orf==null?1:orfpos-orf.getRegion().getTotalLength());
		vars.put(BuiltinVars.AbsoluteCdsPositionStart.name(), cdspos);
		vars.put(BuiltinVars.AbsoluteCdsPositionStop.name(), cds==null?1:cdspos-cds.getRegion().getTotalLength());
		vars.put(BuiltinVars.AbsoluteTranscriptPositionTSS.name(), tpos);
		vars.put(BuiltinVars.AbsoluteTranscriptPositionTTS.name(), trans==null?1:tpos-trans.getRegion().getTotalLength());
		
		vars.put(BuiltinVars.InMajor.name(), isFromMajor);
		vars.put(BuiltinVars.LocalStructs.name(), trans==null?null:localStruc.get(trans.getData().getTranscriptId()));
		
		NumericArray act = downsampling.downsample(NumericArray.copyMemory(codon.getData()));
//		act.applyInPlace(i->i<1?0:i);

		for (CodonProcessorCounter o : counter)
			o.count(vars, act);
		
		
		
		return codon;
	}
	
	
	private String getSequenceContext(ImmutableReferenceGenomicRegion<PriceOrf> orf,
			ImmutableReferenceGenomicRegion<NumericArray> codon, int upstream, int downstream) {
		
		if (orf==null) {
			GenomicRegion reg = codon.getRegion();
			if (codon.getReference().isMinus())
				reg = reg.extendFront(DOWNSTREAM_SEQUENCE_CONTEXT).extendBack(UPSTREAM_SEQUENCE_CONTEXT);
			else
				reg = reg.extendBack(DOWNSTREAM_SEQUENCE_CONTEXT).extendFront(UPSTREAM_SEQUENCE_CONTEXT);
			return genomic.getSequence(codon.getReference(),reg).toString();
		}
		
		ReferenceGenomicRegion<Transcript> tr = genomic.getTranscriptMapping().apply(orf.getData().getTranscript());
	
		ArrayGenomicRegion codInTr = tr.induce(codon.getRegion());
		GenomicRegion context = codInTr.extendBack(downstream).extendFront(upstream);
		return genomic.getSequence(tr.getReference(), tr.mapMaybeOutSide(context)).toString();
	}
	

	private ImmutableReferenceGenomicRegion<PriceOrf> findOrf(ImmutableReferenceGenomicRegion<NumericArray> codon) {
		
		
		ImmutableReferenceGenomicRegion<PriceOrf> re = null;
		int reframe = 3;
		for (ImmutableReferenceGenomicRegion<PriceOrf> orf : orfs.ei(codon).filter(t->majorTranscriptNames.contains(t.getData().getTranscript())).filter(orf->orf.contains(codon)).loop()) {
			int frame = getFrame(orf, codon);
			if (frame==-1) frame = 3;
			if (frame<=reframe) {
				if (re==null || re.getRegion().getTotalLength()<orf.getRegion().getTotalLength())
					re = orf;
				frame = reframe;
			}
		}

		if (re!=null) return re;
		
		for (ImmutableReferenceGenomicRegion<PriceOrf> orf : orfs.ei(codon).filter(orf->orf.contains(codon)).loop()) {
			int frame = getFrame(orf, codon);
			if (frame==-1) frame = 3;
			if (frame<=reframe) {
				if (re==null || re.getRegion().getTotalLength()<orf.getRegion().getTotalLength())
					re = orf;
				frame = reframe;
			}
		}
		
		return re;
	}

	private int getFrame(ImmutableReferenceGenomicRegion<PriceOrf> orf,ImmutableReferenceGenomicRegion<NumericArray> codon) {
		if (orf==null) return -1;
		if (!orf.contains(codon)) return -1;
		return orf.induce(codon.getRegion()).getStart()%3;
	}

	public CodonProcessor merge(CodonProcessor other) {
		if (counter.size()!=other.counter.size())
			throw new RuntimeException("Cannot merge!");

		for (int i=0; i<counter.size(); i++) 
			counter.get(i).merge(other.counter.get(i));
		return this;
	}
	
	
	public static ExtendedIterator<HashMap<String,Object>> iteratePositionCodon(HashMap<String,Object> vars) {
		return EI.seq(-UPSTREAM_SEQUENCE_CONTEXT, DOWNSTREAM_SEQUENCE_CONTEXT).map(p->{
			vars.put(BuiltinVars.ContextPosition.name(), p);
			vars.put(BuiltinVars.ContextCodon.name(), vars.get(BuiltinVars.SequenceContext.name()).toString().substring(p+UPSTREAM_SEQUENCE_CONTEXT, p+3+UPSTREAM_SEQUENCE_CONTEXT));
			vars.put(BuiltinVars.ContextAA.name(), SequenceUtils.translate(vars.get(BuiltinVars.ContextCodon.name()).toString()));
			return vars;
		});
	}
	
	public static ExtendedIterator<HashMap<String,Object>> iteratePositionStruct(HashMap<String,Object> vars) {
		ArrayList<SimpleInterval> strs = (ArrayList<SimpleInterval>) vars.get(BuiltinVars.LocalStructs.name());
		if (strs==null) return EI.empty();
		int pos = (Integer)vars.get(BuiltinVars.AbsoluteTranscriptPositionTSS.name());
		
		return EI.wrap(strs).filter(s->pos-s.getEnd()<UPSTREAM_STRUCT_CONTEXT && s.getStart()-pos<DOWNSTREAM_STRUCT_CONTEXT)
			.map(s->{
				int cpos;
				if (s.getStart()<=pos && s.getStop()>=pos) cpos = 0;
				else if (pos<s.getStart()) cpos = pos-s.getStart();
				else cpos = pos-s.getEnd();
				vars.put(BuiltinVars.LocalStructPos.name(), cpos);
				return vars;
			});
		
	}
	
	

}
