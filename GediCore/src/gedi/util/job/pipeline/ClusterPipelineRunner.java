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
