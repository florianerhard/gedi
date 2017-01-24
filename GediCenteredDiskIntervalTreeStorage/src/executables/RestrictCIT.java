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

package executables;

import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.reads.AlignedReadsDataFactory;
import gedi.core.data.reads.ConditionMappedAlignedReadsData;
import gedi.core.data.reads.ContrastMapping;
import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.util.ParseUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.dynamic.DynamicObject;
import gedi.util.io.text.HeaderLine;
import gedi.util.userInteraction.progress.ConsoleProgress;

import java.io.IOException;

public class RestrictCIT {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException {
		
		boolean progress = false;
		
		int i;
		for (i=0; i<args.length; i++) {
			if (args[i].equals("-p"))
				progress = true;
			else
				break;
		}
		
		if (i+3!=args.length) {
			usage();
			System.exit(1);
		}
		
		CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData> in = new CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData>(args[i++]);
		
		int numCond = in.getRandomRecord().getNumConditions();
		String[] conditions = new String[numCond];
		if (in.getMetaData().isNull()) {
			for (int c=0; c<conditions.length; c++)
				conditions[c] = c+"";
		} else {
			for (int c=0; c<conditions.length; c++) {
				conditions[c] = in.getMetaData().getEntry("conditions").getEntry(c).getEntry("name").asString();
				if ("null".equals(conditions[c])) conditions[c] = c+"";
			}
		}
		
		int[] ranges = ParseUtils.parseRangePositions(args[i++], in.getRandomRecord().getNumConditions(), new IntArrayList(), new HeaderLine(conditions), ':', false).toIntArray();
		CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData> out = new CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData>(args[i++],DefaultAlignedReadsData.class);
		ContrastMapping mapping = new ContrastMapping();
		for (i=0; i<ranges.length; i++)
			mapping.addMapping(ranges[i], i);
		
		
		out.fill(in.ei()
				.iff(progress, e->e.progress(new ConsoleProgress(System.err),(int)in.size(),rgr->rgr.toLocationStringRemovedIntrons()))
				.map(rgr->rgr.toMutable().transformData(ard->restrict(ard,mapping)))
				.filter(rgr->rgr.getData()!=null)
				);
		
		
		
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for (String s : in.getMetaData().getProperties()) {
			if (sb.length()>1) sb.append(",");
			
			sb.append("\"").append(s).append("\":");
			DynamicObject e = in.getMetaData().getEntry(s);
			if (e.isArray() && e.length()==numCond) {
				DynamicObject[] a = e.asArray();
				sb.append("[");
				for (int ind=0; ind<ranges.length; ind++) {
					if (ind>0) sb.append(",");
					sb.append(a[ranges[ind]].toJson());
				}
				sb.append("]");
			}
			else {
				sb.append(in.getMetaData().getEntry(s).toJson());
			}
		}
		sb.append("}");
		out.setMetaData(DynamicObject.parseJson(sb.toString()));
		
		
	}

	private static DefaultAlignedReadsData restrict(DefaultAlignedReadsData ard, ContrastMapping mapping) {
		AlignedReadsDataFactory fac = new AlignedReadsDataFactory(mapping.getNumMergedConditions());
		fac.start();
		ConditionMappedAlignedReadsData coard = new ConditionMappedAlignedReadsData(ard, mapping);
		for (int d=0; d<coard.getDistinctSequences(); d++)
			if (coard.getTotalCountForDistinct(d, ReadCountMode.Weight)>0)
				fac.add(coard, d);
		return fac.getDistinctSequences()==0?null:fac.create();
	}

	private static void usage() {
		System.out.println("RestrictCIT [-p] <input> <ranges> <output> \n\n -p shows progress\n ranges is zerobased, can use condition names and -1; e.g. 1,3:5 is equal to 1,3,4,5, 0:-1 is all");
	}
	
}
