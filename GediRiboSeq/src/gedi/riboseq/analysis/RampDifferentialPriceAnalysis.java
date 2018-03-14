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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.logging.Level;

import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.riboseq.javapipeline.PriceParameterSet;
import gedi.startup.PriceStartUp;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.collections.PositionIterator;
import gedi.util.functions.EI;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.math.stat.distributions.LfcDistribution;
import gedi.util.mutable.MutablePair;
import gedi.util.program.GediParameter;
import gedi.util.program.GediParameterSet;
import gedi.util.program.parametertypes.IntParameterType;
import gedi.util.program.parametertypes.StringParameterType;
import gedi.util.r.RRunner;

public class RampDifferentialPriceAnalysis implements PriceAnalysis<Void> {

	/**
	 * For auto inclusion in {@link PriceStartUp}
	 */
	public static final String name = "ramp";
	public static final Function<GediParameterSet,GediParameter[]> params = set->new GediParameter[] {
			new GediParameter<Integer>(set, name+"-offset", "Skip the first codons", false, new IntParameterType(),3),
			new GediParameter<Integer>(set, name+"-len", "Number of codons to consider", false, new IntParameterType(),15),
			new GediParameter<String>(set,name+"-contrasts", "Contrasts to compute changes around start codon", false, new StringParameterType(),"")
		
	};

	
	private int start;
	private int end;
	private LinkedHashMap<String, MutablePair<ToDoubleFunction<NumericArray>, ToDoubleFunction<NumericArray>>> contrasts;
	private int cond;
	
	public RampDifferentialPriceAnalysis(String[] conditions, PriceParameterSet param) throws IOException {
		this.start = (Integer)param.get(name+"-offset").get();
		this.end = (Integer)param.get(name+"-len").get()-this.start;
		this.cond = conditions.length;
		
		String contr = (String) param.get(name+"-contrasts").get();
		if (contr.length()==0) contr=null;
		
		HashMap<String, Integer> condIndex = ArrayUtils.createIndexMap(conditions);
		
		LinkedHashMap<String, MutablePair<ToDoubleFunction<NumericArray>, ToDoubleFunction<NumericArray>>> contrasts = new LinkedHashMap<>();
		
		if (contr==null && conditions.length==2)
			contrasts.put(conditions[0]+"/"+conditions[1], new MutablePair<>(a->a.getDouble(0), a->a.getDouble(1)));
		else if (contr!=null)
			for (String[] a : EI.lines(contr).skip(1).map(s->StringUtils.split(s, '\t')).loop()) 
				contrasts.put(a[0], new MutablePair<ToDoubleFunction<NumericArray>, ToDoubleFunction<NumericArray>>(createContrastPart(a[1],condIndex),createContrastPart(a[2],condIndex)));
		
		this.contrasts = contrasts.size()>0?contrasts:null;
	}
	
	private static ToDoubleFunction<NumericArray> createContrastPart(String d, HashMap<String, Integer> condIndex) {
		int[] ind = EI.split(d, '+').mapToDouble(condIndex::get).toIntArray();
		if (ind.length==0)
			return a->a.getDouble(ind[0]);
		else
			return a->{
				double re = 0;
				for (int i : ind)
					re+=a.getDouble(i);
				return re;
			};
	}

	@Override
	public void process(MajorIsoform data, LineWriter out, Void ctx) {
		
		if (contrasts!=null) {
			NumericArray sum = data.iterateAminoAcids(0).data().skip(end).removeNulls().reduce(NumericArray.createMemory(cond, NumericArrayType.Float), (e,s)->{s.add(e); return s;});
			NumericArray ramp = data.iterateAminoAcids(0).data().skip(start).head(end-start).removeNulls().reduce(NumericArray.createMemory(cond, NumericArrayType.Float), (e,s)->{s.add(e); return s;});
			
			for (String co : contrasts.keySet()) {
				MutablePair<ToDoubleFunction<NumericArray>, ToDoubleFunction<NumericArray>> ctr = contrasts.get(co);
				double shift = LfcDistribution.mean(ctr.Item1.applyAsDouble(sum)+1,ctr.Item2.applyAsDouble(sum)+1);
				
				
				double mean = LfcDistribution.mean(ctr.Item1.applyAsDouble(ramp)+1,ctr.Item2.applyAsDouble(ramp)+1);
				double var = LfcDistribution.var(ctr.Item1.applyAsDouble(ramp)+1,ctr.Item2.applyAsDouble(ramp)+1);
				out.writef2("%s\t%s\t%.3f\t%.3f\n", data.getTranscript().getData().getGeneId(),co,mean-shift,var);
				
			}
			
			
			
		}
	}

	@Override
	public void header(LineWriter out) throws IOException {
		if (contrasts!=null)
			out.writeLine("GeneId\tContrast\tMean\tVariance");
	}

	@Override
	public Void createContext() {
		return null;
	}

	@Override
	public void reduce(List<Void> ctx, LineWriter out) {
	}

	
	
}
