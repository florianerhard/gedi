package gedi.util.job.pipeline;

import gedi.util.functions.EI;
import gedi.util.io.text.LineWriter;
import gedi.util.nashorn.JS;

public class ClusterPipelineRunner implements PipelineRunner {

	public static final String name = "cluster";

	private String cluster;
	private int index;
	
	public ClusterPipelineRunner(String cluster) {
		this.cluster = cluster;
	}

	@Override
	public void prerunner(LineWriter writer, String name, String paramFile, JS js, int... tokens) {
		String dep = tokens.length>0?" -dep "+EI.wrap(tokens).map(i->"${PIDS["+i+"]}").concat(","):"";
		writer.write2("echo Starting "+name+dep+"\n");
		writer.writef2("PIDS[%d]=$( gedi -e Cluster%s -j %s %s %s '",++index,dep,paramFile,cluster,name);
	}

	@Override
	public int postrunner(LineWriter writer, String name, String paramFile, JS js) {
		writer.write2("' )");
		return index;
	}

}
