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
import java.util.function.Function;

import gedi.core.genomic.Genomic;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.riboseq.javapipeline.PriceParameterSet;
import gedi.startup.PriceStartUp;
import gedi.util.FileUtils;
import gedi.util.SequenceUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.collections.PositionIterator;
import gedi.util.functions.EI;
import gedi.util.io.text.LineWriter;
import gedi.util.mutable.MutablePair;
import gedi.util.program.GediParameter;
import gedi.util.program.GediParameterSet;
import gedi.util.program.parametertypes.DoubleParameterType;
import gedi.util.program.parametertypes.IntParameterType;
import gedi.util.r.RRunner;

public class CodonUsagePriceAnalysis extends MetaGenePriceAnalysis<MutablePair<String,Integer>> {

	/**
	 * For auto inclusion in {@link PriceStartUp}
	 */
	public static final String name = "codonusage";
	public static final Function<PriceParameterSet,GediParameter[]> params = set->new GediParameter[] {
		set.genomic,
		new GediParameter<Integer>(set, name+"-upstream", "Length before each codon to consider", false, new IntParameterType(),15),
		new GediParameter<Integer>(set, name+"-downstream", "Length before each codon to consider", false, new IntParameterType(),15),
		new GediParameter<Integer>(set, name+"-excludeStart", "Length after start codon to exclude", false, new IntParameterType(),90),
		new GediParameter<Integer>(set, name+"-excludeStop", "Length after start codon to exclude", false, new IntParameterType(),90)
	};
	
	private Genomic genomic;
	private int excludeStart;
	private int excludeStop;
	private int upstream;
	private int downstream;
	
	public CodonUsagePriceAnalysis(String[] conditions, PriceParameterSet param) throws IOException {
		super(conditions,"Codon\tOffset");
		genomic=param.genomic.get();
		this.upstream = (Integer)param.get(name+"-upstream").get();
		this.downstream = (Integer)param.get(name+"-downstream").get();
		this.excludeStart = (Integer)param.get(name+"-excludeStart").get();
		this.excludeStop = (Integer)param.get(name+"-excludeStop").get();
		
		setKeyStringer(p->p.Item1+"\t"+p.Item2);
	}


	@Override
	public void process(MajorIsoform data, LineWriter out, NormalizingCounter<MutablePair<String,Integer>>[] ctx) {
		
		NumericArray sum = NumericArray.createMemory(conditions.length, NumericArrayType.Double);
		int pos = 0;
		PositionIterator<NumericArray> it = data.iterateAminoAcids(0);
		while (it.hasNext()) {
			int p = it.nextInt();
			if (p>excludeStart && p<data.getNumberOfCodons(0)-excludeStop) {
				NumericArray a = it.getData();
				if (a!=null)
					sum.add(a);
				pos++;
			}
		}
		
		
		String fseq = genomic.getSequence(data.getTranscript()).toString(); 
		
		it = data.iterateAminoAcids(0);
		while (it.hasNext()) {
			int p = it.nextInt();
			if (p>excludeStart && p<data.getNumberOfCodons(0)-excludeStop) {
				NumericArray a = it.getData();
				for (int off=-upstream; off<=downstream; off++) {
					ArrayGenomicRegion icod = new ArrayGenomicRegion((off+p)*3,(off+p)*3+3);
					if (icod.getEnd()<data.getOrf(0).getTotalLength()) {
						GenomicRegion codon = data.getOrf(0).map(icod);
						String seq = SequenceUtils.extractSequence(codon, fseq);//genomic.getSequence(data.getTranscript().getReference(),codon).toString(); 
						for (int i=0; i<conditions.length; i++){
							if (sum.getDouble(i)>0){
								double aa = a==null?0:a.getDouble(i);
								aa=aa/(sum.getDouble(i)/pos);
								ctx[i].count(new MutablePair<String,Integer>(seq,off),aa); 
	//									a==null||sum.getDouble(i)==0?0:(a.getDouble(i)/(sum.getDouble(i)/pos)));
							}
						}
					}
				}
			}
		}
	}


	@Override
	public void plot(String data, String prefix) throws IOException {
		RRunner r = new RRunner(FileUtils.getFullNameWithoutExtension(data)+".R");
		r.set("prefix",prefix);
		r.set("output",FileUtils.getFullNameWithoutExtension(data)+".pdf");
		r.set("aoutput",FileUtils.getFullNameWithoutExtension(data)+".asite.png");
		r.set("input",data);
		r.addSource(getClass().getResourceAsStream("/resources/R/codonusage.R"));
		r.run(true);
	}
	
}
