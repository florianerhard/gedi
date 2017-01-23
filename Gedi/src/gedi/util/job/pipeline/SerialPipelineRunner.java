package gedi.util.job.pipeline;

import gedi.util.io.text.LineWriter;
import gedi.util.nashorn.JS;

public class SerialPipelineRunner implements PipelineRunner {

	public static final String name = "serial";

	@Override
	public void prerunner(LineWriter writer, String name, String paramFile, JS js, int... tokens) {
	}

	@Override
	public int postrunner(LineWriter writer, String name, String paramFile, JS js) {
		return 0;
	}


}
