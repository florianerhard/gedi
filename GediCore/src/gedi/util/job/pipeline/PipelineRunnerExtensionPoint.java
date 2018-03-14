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
