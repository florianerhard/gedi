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
		writer.write2(" >> "+o+" 2>> "+e+" &\nPIDS["+(++index)+"]=$!\n");
		return index;
	}

}
