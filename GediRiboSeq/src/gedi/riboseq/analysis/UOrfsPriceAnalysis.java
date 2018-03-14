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
import gedi.riboseq.inference.orf.PriceOrfType;
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

public class UOrfsPriceAnalysis implements PriceAnalysis<Void> {

	/**
	 * For auto inclusion in {@link PriceStartUp}
	 */
	public static final String name = "uorfs";

	
	
	@Override
	public void process(MajorIsoform data, LineWriter out, Void ctx) {
		
		int icisdist = Integer.MAX_VALUE;
		int uorfs = 0;
		int uoorfs = 0;
		int uorfg = 0;
		int uoorfg = 0;
		
		int start = data.getOrf(0).getStart();
		
		for (int g=0; g<data.getOrfGroups(); g++) {
			int forf = data.getOrfId(g, 0);
			PriceOrfType t = data.getOrfType(forf);
			if (t==PriceOrfType.uoORF)
				uoorfg++;
			else if (t==PriceOrfType.uORF)
				uorfg++;
			
			for (int i=0; i<data.getOrfsInGroup(g); i++) {
				int orf = data.getOrfId(g, i);
			
				if (t==PriceOrfType.uoORF)
					uoorfs++;
				else if (t==PriceOrfType.uORF)
					uorfs++;
				
				if (t==PriceOrfType.uoORF || t==PriceOrfType.uORF)
					icisdist = Math.min(icisdist, start-data.getOrf(orf).getEnd());
				
			}
		}
		
		out.writef2("%s\t%d\t%d\t%d\t%d\t%d\n", data.getTranscript().getData().getGeneId(),
				uorfs,uoorfs,uorfg,uoorfg,icisdist
				);
	}

	@Override
	public void header(LineWriter out) throws IOException {
		out.writeLine("GeneId\tuORF count\tuoORF count\tuORF group count\tuoORF group count\tIntercistronic distance");
	}

	@Override
	public Void createContext() {
		return null;
	}

	@Override
	public void reduce(List<Void> ctx, LineWriter out) {
	}

	
	
}
