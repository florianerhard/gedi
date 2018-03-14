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
import gedi.core.genomic.Genomic;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
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

public class StartCodonContextPriceAnalysis implements PriceAnalysis<Void> {

	/**
	 * For auto inclusion in {@link PriceStartUp}
	 */
	public static final String name = "startcontext";
	public static final Function<PriceParameterSet,GediParameter[]> params = set->new GediParameter[] {
			new GediParameter<Integer>(set, name+"-upstream", "how many bp to go upstream", false, new IntParameterType(),60),
			new GediParameter<Integer>(set, name+"-downstream", "how many bp to go downstream", false, new IntParameterType(),60),
			set.genomic
	};

	
	private int upstream;
	private int downstream;
	private Genomic genomic;
	
	public StartCodonContextPriceAnalysis(String[] conditions, PriceParameterSet param) throws IOException {
		this.upstream = (Integer)param.get(name+"-upstream").get();
		this.downstream = (Integer)param.get(name+"-downstream").get();
		genomic=param.genomic.get();
	}
	
	@Override
	public void process(MajorIsoform data, LineWriter out, Void ctx) {
		
		ArrayGenomicRegion mapped = data.getOrf(0);
		ArrayGenomicRegion context = new ArrayGenomicRegion(mapped.getStart()-upstream,mapped.getStart()+3+downstream);
		
		GenomicRegion gcontext = data.getTranscript().mapMaybeOutSide(context);
		out.writef2("%s\t%s\n", data.getTranscript().getData().getGeneId(),genomic.getSequence(data.getTranscript().getReference(), gcontext));
	}

	@Override
	public void header(LineWriter out) throws IOException {
		out.writeLine("GeneId\tSequence");
	}

	@Override
	public Void createContext() {
		return null;
	}

	@Override
	public void reduce(List<Void> ctx, LineWriter out) {
	}

	
	
}
