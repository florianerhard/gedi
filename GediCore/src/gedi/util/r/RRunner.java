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
package gedi.util.r;

import gedi.app.Config;
import gedi.util.StringUtils;
import gedi.util.io.text.BufferedReaderLineReader;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;

public class RRunner {
	
	private String scriptName;
	private LineWriter lw;
	
	public RRunner(String scriptName) {
		this.scriptName = scriptName;
		lw = new LineOrientedFile(scriptName).write();
	}

	private void setup() {
		if (lw==null)
			lw = new LineOrientedFile(scriptName).append();
	}
	
	public void set(String name, String value) throws IOException {
		setup();
		lw.writef("%s <- '%s'\n",name,StringUtils.escape(value,'\''));
	}


	public void addSource(InputStream source) throws IOException {
		setup();
		new BufferedReaderLineReader(source).toWriter(lw);
	}

	public boolean run(boolean cleanup) throws IOException {
		lw.close();
		lw = null;
		
		ProcessBuilder pb = new ProcessBuilder(Config.getInstance().getRscriptCommand(),scriptName);
		pb.redirectError(Redirect.INHERIT);
		pb.redirectOutput(Redirect.INHERIT);
		pb.redirectInput(Redirect.INHERIT);
		Process p = pb.start();
		try {
			boolean re = p.waitFor()==0;
			if (cleanup)
				new File(scriptName).delete();
			return re;
		} catch (InterruptedException e) {
			return false;
		}
	}
}
