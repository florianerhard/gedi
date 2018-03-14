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
package gedi.riboseq.javapipeline.analyze;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import gedi.app.extension.ExtensionContext;
import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.riboseq.analysis.MajorIsoform;
import gedi.riboseq.analysis.PriceAnalysis;
import gedi.riboseq.analysis.PriceAnalysisExtensionPoint;
import gedi.riboseq.javapipeline.PriceParameterSet;
import gedi.util.FileUtils;
import gedi.util.ReflectionUtils;
import gedi.util.functions.EI;
import gedi.util.functions.ParallelizedIterator;
import gedi.util.io.text.LineWriter;
import gedi.util.io.text.StringLineWriter;
import gedi.util.program.GediParameter;
import gedi.util.program.GediParameterSet;
import gedi.util.program.GediProgram;
import gedi.util.program.GediProgramContext;
import gedi.util.program.parametertypes.FileParameterType;

public class PriceAnalyzeMajorIsoforms extends GediProgram {

	private Class[] clss = PriceAnalysisExtensionPoint.getInstance().getExtensionClasses().toArray(new Class[0]);
	private String[] names = getNames(clss);
	
	
	public PriceAnalyzeMajorIsoforms(PriceParameterSet params) {
		addInput(params.majorIsoformCit);
		addInput(params.nthreads);
		addInput(params.plot);
		addInput(params.prefix);
		addInput(params.removeGenes);
		
		for (int i=0; i<names.length; i++) {
			String name = names[i];
			addOutput(new GediParameter<File>(params,"${prefix}.analyze."+name+".tsv", name+" analysis.", false, new FileParameterType()));
			Function<GediParameterSet,GediParameter<Boolean>> p = (Function<GediParameterSet, GediParameter<Boolean>>) ReflectionUtils.getStatic2(clss[i], "runflag");
			if (p!=null)
				setRunFlag(p.apply(params));
		}
		
		EI.wrap(clss).unfold(cls->{
			Function<GediParameterSet,GediParameter[]> p = (Function<GediParameterSet, GediParameter[]>) ReflectionUtils.getStatic2(cls, "params");
			if (p!=null) return EI.wrap(p.apply(params));
			return EI.empty();
		}).unique(false).forEachRemaining(this::addInput);
		
	}
	
	private static String[] getNames(Class[] clss) {
		String[] names = new String[clss.length];
		for (int i=0; i<names.length; i++) 
			names[i] = (String) ReflectionUtils.getStatic2(clss[i], "name");
		return names;
//		EI.wrap(clss).<String>map(cls->(String)ReflectionUtils.getStatic2(cls, "name")).list().toArray(new String[0]);
	}

	public String execute(GediProgramContext context) throws IOException, InterruptedException {
		
		File mifile = getParameter(0);
		int nthreads = getIntParameter(1);
		boolean plot = getBooleanParameter(2);
		String prefix = getParameter(3);
		String removeList = getParameter(4);
		
		CenteredDiskIntervalTreeStorage<MajorIsoform> cit = new CenteredDiskIntervalTreeStorage<>(mifile.getAbsolutePath());
		
		ExtensionContext ctx = new ExtensionContext();
		ctx.add(cit);
		ctx.add(cit.getMetaDataConditions());
		ctx.add(parameterSet);
		
		LineWriter[] outs = new LineWriter[names.length];
		for (int i=0; i<names.length; i++)
			outs[i] = getOutputWriter(i);
		PriceAnalysis[] an = new PriceAnalysis[names.length];
		for (int i=0; i<names.length; i++) {
			an[i] = PriceAnalysisExtensionPoint.getInstance().get(ctx, names[i]);
			an[i].header(outs[i]);
		}
		
		Predicate<ImmutableReferenceGenomicRegion<MajorIsoform>> filter;
		if (removeList!=null) {
			HashSet<String> rm = EI.lines(removeList).set();
			filter = rgr->!rm.contains(rgr.getData().getTranscript().getData().getGeneId());
		} else 
			filter = null;
		
		
		ParallelizedIterator<ImmutableReferenceGenomicRegion<MajorIsoform>, Object, Object[]> pit = cit.ei().progress(context.getProgress(), (int)cit.size(), r->r.toLocationString())
			.iff(filter!=null, ei->ei.filter(filter))
				.parallelized(nthreads, 10, 
					()->{
						Object[] re = new Object[an.length];
						for (int i=0; i<re.length; i++)
							re[i] = an[i].createContext();
						return re;
					},
					(ei,s)->ei.map(mi->{
				for (int i=0; i<names.length; i++) {
					StringLineWriter out = new StringLineWriter();
					an[i].process(mi.getData(),out,s[i]);
					if (out.length()>0) 
						synchronized (outs[i]) {outs[i].write2(out.toString());}
				}
				return 1;
			}));
		pit.drain();
		
		List<Object>[] states = new List[an.length];
		for (int i=0; i<states.length; i++) {
			states[i] = new ArrayList<>();
			for (int j=0; j<pit.getNthreads(); j++)
				states[i].add(pit.getState(j)[i]);
		}
		
		for (int i=0; i<outs.length; i++)
			an[i].reduce(states[i], outs[i]);
		
		for (int i=0; i<names.length; i++)
			outs[i].close();
		
		if (plot) {
			context.getLog().info("Create plots...");
			for (int i=0; i<names.length; i++)
				an[i].plot(getOutputFile(i).getAbsolutePath(), prefix);
		}
		
		return null;
	}

	
	

}

