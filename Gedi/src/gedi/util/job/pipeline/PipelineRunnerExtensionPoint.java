package gedi.util.job.pipeline;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gedi.app.extension.DefaultExtensionPoint;
import gedi.app.extension.ExtensionContext;

public class PipelineRunnerExtensionPoint extends DefaultExtensionPoint<String, PipelineRunner>{

	private static PipelineRunnerExtensionPoint instance;

	public static PipelineRunnerExtensionPoint getInstance() {
		if (instance==null) 
			instance = new PipelineRunnerExtensionPoint();
		return instance;
	}
	
	protected PipelineRunnerExtensionPoint() {
		super(PipelineRunner.class);
	}
	
	private static final Pattern callPatter = Pattern.compile("^(.*)\\((.*)\\)$");
	public PipelineRunner get(ExtensionContext context, String key) {
		Matcher m = callPatter.matcher(key);
		if (m.find()) {
			context.add(m.group(2));
			return super.get(context, m.group(1));
		}
		return super.get(context, key);
	}

}
