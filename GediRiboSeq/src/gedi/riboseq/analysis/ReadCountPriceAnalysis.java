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
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import gedi.riboseq.javapipeline.PriceParameterSet;
import gedi.startup.PriceStartUp;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.io.text.LineWriter;
import gedi.util.math.stat.counting.Counter;
import gedi.util.math.stat.counting.FractionalCounter;
import gedi.util.program.GediParameter;
import gedi.util.program.GediParameterSet;
import gedi.util.program.parametertypes.IntParameterType;

public class ReadCountPriceAnalysis implements PriceAnalysis<Void> {

	/**
	 * For auto inclusion in {@link PriceStartUp}
	 */
	public static final String name = "counts";

	private String[] conditions;
	
	
	public ReadCountPriceAnalysis(String[] conditions, PriceParameterSet param) {
		this.conditions = conditions;
	}

	@Override
	public void process(MajorIsoform data, LineWriter out, Void ctx) {
		
		NumericArray sum = data.getSum(0);
		out.writef2(data.getTranscript().getData().getGeneId());
		out.writef2("\t%d\t%d",data.getTranscript().getRegion().getTotalLength(),data.getAminoAcidLength(0));
		for (int i=0; i<sum.length(); i++)
			out.writef2("\t%.1f",sum.getDouble(i));
		out.writeLine2();
	}

	@Override
	public void header(LineWriter out) throws IOException {
		out.writeLine("GeneId\tmRNA length\tCDS length\t"+StringUtils.concat('\t', conditions));
	}

	@Override
	public Void createContext() {
		return null;
	}

	@Override
	public void reduce(List<Void> ctx, LineWriter out) {
	}
	
	
}
