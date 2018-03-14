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

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.core.workspace.Workspace;
import gedi.util.algorithm.string.alignment.pairwise.chain.AlignmentChain;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.program.CommandLineHandler;
import gedi.util.program.GediParameter;
import gedi.util.program.GediParameterSet;
import gedi.util.program.GediProgram;
import gedi.util.program.GediProgramContext;
import gedi.util.program.parametertypes.FileParameterType;
import gedi.util.program.parametertypes.StringParameterType;

public class LiftCIT {

	public static void main(String[] args) throws IOException {

		LiftCITParameterSet params = new LiftCITParameterSet();
		GediProgram pipeline = GediProgram.create("LiftCIT",
				new LiftCITProgram(params)
				);
		GediProgram.run(pipeline, null, new CommandLineHandler("LiftCIT","LiftCIT transforms coordinates for CIT files.",args));
		
	}
	
	public static class LiftCITProgram extends GediProgram {

		
		
		public LiftCITProgram(LiftCITParameterSet params) {
			addInput(params.in);
			addInput(params.lo);
			addInput(params.prefix);
			addOutput(params.outCit);
			addOutput(params.outReport);
		}
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public String execute(GediProgramContext context) throws Exception {
			String in = getParameter(0);
			String lo = getParameter(1);
			String oname = getParameter(2);
			
			MemoryIntervalTreeStorage<AlignmentChain> lov = AlignmentChain.load(lo);
			
			GenomicRegionStorage<?> inStorage = Workspace.<GenomicRegionStorage<?>>loadItem(in);
			CenteredDiskIntervalTreeStorage out = new CenteredDiskIntervalTreeStorage<>(oname+".cit",inStorage.getType());
			LineWriter report = new LineOrientedFile(oname+".report").write();
			
			out.fill(inStorage.ei().progress(context.getProgress(), (int)inStorage.size(), e->e.toLocationString()).map(r->{
				
				HashSet<?> set = lov.ei(r).map(chain->chain.getData().map(r)).set();
				if (set.isEmpty())  {
					report.writef2("%s\t\t%s\t\n", r.toString().replace('\t', ' '),"not mapped");
					set = lov.ei(r).map(chain->chain.getData().map(r)).set();
				}
				else if (set.size()>1) 
					report.writef2("%s\t\t%s\t\n", r.toString().replace('\t', ' '),"multimapped");
				else {
					ImmutableReferenceGenomicRegion<?> m = (ImmutableReferenceGenomicRegion<?>) set.iterator().next();
					if (m.getRegion().getTotalLength()!=r.getRegion().getTotalLength())
						report.writef2("%s\t%s\t%s\t%d->%d\n", r.toString().replace('\t', ' '),m.toString().replace('\t', ' '),"partial",r.getRegion().getNumParts(),m.getRegion().getNumParts());
					else
						report.writef2("%s\t%s\t%s\t%d->%d\n", r.toString().replace('\t', ' '),m.toString().replace('\t', ' '),"ok",r.getRegion().getNumParts(),m.getRegion().getNumParts());
					return m;
				}
				return null;
				
			}).removeNulls());
			
			report.close();
			
			return null;
		}
	}
	
	
	public static class LiftCITParameterSet extends GediParameterSet {
		public GediParameter<String> in = new GediParameter<String>(this,"i", "Input cit file.", false, new StringParameterType());
		public GediParameter<String> prefix = new GediParameter<String>(this,"o", "Prefix for output (cit and report).", false, new StringParameterType());
		public GediParameter<String> lo = new GediParameter<String>(this,"l", "Liftover file.", false, new StringParameterType());
		
		public GediParameter<File> outCit = new GediParameter<File>(this,"${o}.cit", "Output cit file.", false, new FileParameterType());
		public GediParameter<File> outReport = new GediParameter<File>(this,"${o}.report", "Output report file.", false, new FileParameterType());
		
	}

	
	
}
