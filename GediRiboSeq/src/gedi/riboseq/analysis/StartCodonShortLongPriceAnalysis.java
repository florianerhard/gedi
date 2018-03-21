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
package gedi.riboseq.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.riboseq.javapipeline.PriceParameterSet;
import gedi.startup.PriceStartUp;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.collections.PositionIterator;
import gedi.util.io.text.LineWriter;
import gedi.util.mutable.MutablePair;
import gedi.util.program.GediParameter;
import gedi.util.program.GediParameterSet;
import gedi.util.program.parametertypes.IntParameterType;
import gedi.util.r.RRunner;

public class StartCodonShortLongPriceAnalysis implements PriceAnalysis<Void> {

	/**
	 * For auto inclusion in {@link PriceStartUp}
	 */
	public static final String name = "start-codon-sl";
	public static final Function<PriceParameterSet,GediParameter[]> params = set->new GediParameter[] {
		set.reads,
		new GediParameter<Integer>(set, name+"-len", "Length after the start codon to consider", false, new IntParameterType(),50),
		new GediParameter<Integer>(set, name+"-long", "Minimal read length to be considered a long read", false, new IntParameterType(),25)	
	};

	private int minLong;
	private int len;
	private GenomicRegionStorage<DefaultAlignedReadsData> reads;
	private String[] conditions;
	
	public StartCodonShortLongPriceAnalysis(String[] conditions, PriceParameterSet param) {
//		super(conditions,"Position\tSize");
		this.conditions = conditions;
		this.len = (Integer)param.get(name+"-len").get();
		this.minLong = (Integer)param.get(name+"-long").get();
		this.reads= (GenomicRegionStorage<DefaultAlignedReadsData>) param.get("reads").get();
		
//		setKeyStringer(p->p.Item1+(p.Item2.booleanValue()?"\tShort":"\tLong"));
	}

//	@Override
//	public void process(MajorIsoform data, LineWriter out, NormalizingCounter<MutablePair<Integer,Boolean>>[] ctx) {
//		
//		int start = data.getOrf(0).getStart();
//		ImmutableReferenceGenomicRegion<Void> pos = new ImmutableReferenceGenomicRegion<>(data.getTranscript().getReference(), data.getTranscript().map(new ArrayGenomicRegion(Math.max(0, start-len),Math.min(data.getTranscript().getRegion().getTotalLength(),start+len))));
//
//		double[][] shortCount = null;
//		double[][] longCount = null;
//		
//		for (ImmutableReferenceGenomicRegion<DefaultAlignedReadsData> r : reads.ei(pos).filter(r->pos.getRegion().isIntronConsistent(r.getRegion())).loop()) {
//			if (shortCount==null) {
//				int d = r.getData().getNumConditions();
//				shortCount = new double[d][2*len];
//				longCount = new double[d][2*len];
//			}
//			boolean sh = r.getRegion().getTotalLength()<minLong;
//			int p = (r.getRegion().getStart()+r.getRegion().getEnd())/2;
//			if (pos.getRegion().contains(p)) {
//				p = pos.induce(p);
//				p = p-len;
//				for (int i=0; i<r.getData().getNumConditions(); i++)
//					(sh?shortCount:longCount)[i][p+len]+=r.getData().getTotalCountForCondition(i, ReadCountMode.Weight);
//			}
//		}
//		
//		if (shortCount==null) return;
//		
//		
//		for (int i=0; i<shortCount.length; i++) {
//			double sf = (ArrayUtils.sum(shortCount[i])+ArrayUtils.sum(longCount[i]))/(2*len);
//			if (sf>0)
//			for (int p=0; p<shortCount[i].length; p++) {
//				ctx[i].count(new MutablePair<>(p-len,true),shortCount[i][p]/sf);
//				ctx[i].count(new MutablePair<>(p-len,false),longCount[i][p]/sf);
//			}
//		}
//	}


	@Override
	public void plot(String data, String prefix) throws IOException {
//		RRunner r = new RRunner(FileUtils.getFullNameWithoutExtension(data)+".R");
//		r.set("prefix",prefix);
//		r.set("output",FileUtils.getFullNameWithoutExtension(data)+".png");
//		r.set("input",data);
//		r.addSource(getClass().getResourceAsStream("/resources/R/around.R"));
//		r.run(true);
	}

	@Override
	public void header(LineWriter out) throws IOException {
		out.writef("GeneId\tCondition\tPosition\tSize\tValue\n");
	}

	@Override
	public Void createContext() {
		return null;
	}

	@Override
	public void reduce(List<Void> ctx, LineWriter out) {
	}

	@Override
	public void process(MajorIsoform data, LineWriter out, Void ctx) {
		int start = data.getOrf(0).getStart();
		ImmutableReferenceGenomicRegion<Void> pos = new ImmutableReferenceGenomicRegion<>(data.getTranscript().getReference(), data.getTranscript().map(new ArrayGenomicRegion(Math.max(0, start-len),Math.min(data.getTranscript().getRegion().getTotalLength(),start+len))));

		double[][] shortCount = null;
		double[][] longCount = null;
		
		for (ImmutableReferenceGenomicRegion<DefaultAlignedReadsData> r : reads.ei(pos).filter(r->pos.getRegion().isIntronConsistent(r.getRegion())).loop()) {
			if (shortCount==null) {
				int d = r.getData().getNumConditions();
				shortCount = new double[d][2*len];
				longCount = new double[d][2*len];
			}
			boolean sh = r.getRegion().getTotalLength()<minLong;
			int p = (r.getRegion().getStart()+r.getRegion().getEnd())/2;
			if (pos.getRegion().contains(p)) {
				p = pos.induce(p);
				p = p-len;
				for (int i=0; i<r.getData().getNumConditions(); i++)
					(sh?shortCount:longCount)[i][p+len]+=r.getData().getTotalCountForCondition(i, ReadCountMode.Weight);
			}
		}
		
		if (shortCount==null) return;
		NumericArray sum = data.getSum(0);
		
		
		for (int i=0; i<shortCount.length; i++) {
			double sf = sum.getDouble(i)/data.getAminoAcidLength(0);
			if (sf>0)
			for (int p=0; p<shortCount[i].length; p++) {
				out.writef2("%s\t%s\t%d\t%s\t%.3f\n", data.getTranscript().getData().getGeneId(),conditions[i],p-len,"Short",shortCount[i][p]/sf);
				out.writef2("%s\t%s\t%d\t%s\t%.3f\n", data.getTranscript().getData().getGeneId(),conditions[i],p-len,"Long",longCount[i][p]/sf);
//				ctx[i].count(new MutablePair<>(p-len,true),shortCount[i][p]/sf);
//				ctx[i].count(new MutablePair<>(p-len,false),longCount[i][p]/sf);
			}
		}
	}
	
}
