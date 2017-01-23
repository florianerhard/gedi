package gedi.util.job.pipeline;

import gedi.util.io.text.LineWriter;
import gedi.util.nashorn.JS;

public interface PipelineRunner {
	void prerunner(LineWriter writer, String name, String paramFile, JS js, int...tokens);
	int postrunner(LineWriter writer, String name, String paramFile, JS js);
}
