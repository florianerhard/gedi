package gedi.util.job.pipeline;

import gedi.util.functions.EI;
import gedi.util.io.text.LineWriter;
import gedi.util.nashorn.JS;

public class ParallelPipelineRunner implements PipelineRunner {

	public static final String name = "parallel";

	private int index = 0;
	
	@Override
	public void prerunner(LineWriter writer, String name, String paramFile, JS js, int... tokens) {
		if (tokens.length>0) {
			writer.write2("wait "+EI.wrap(tokens).map(i->"${PIDS["+i+"]}").concat(" ")+"\n");
		}
		writer.write2("echo Starting "+name+"\n");
	}

	@Override
	public int postrunner(LineWriter writer, String name,String paramFile, JS js) {
		String o = js.getVariable("logfolder")+"/"+name+".out";
		String e = js.getVariable("logfolder")+"/"+name+".err";
		writer.write2(" >> "+o+" 2>> "+e+" &\nPIDS["+(++index)+"]=$!");
		return index;
	}

}
