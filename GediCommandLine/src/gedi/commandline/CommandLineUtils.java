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

package gedi.commandline;

import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.util.List;

public class CommandLineUtils {

	public static void shellProcess(CommandLineSession context, String command) throws Exception {
		shellProcess(context, command, null);
	}
	public static void shellProcess(CommandLineSession context, String command, List<CharSequence> inputLines) throws Exception {
		context.reader.getTerminal().restore();
		
		try{
			
			ProcessBuilder b = new ProcessBuilder();
			if (inputLines==null)
				b.redirectInput(Redirect.INHERIT);
			b.redirectOutput(Redirect.INHERIT);
			b.redirectError(Redirect.INHERIT);
			
			b.command("bash","-i", "-c",command+"; exit &> /dev/null");
			final Process p = b.start();
			context.blockTermHandler = true;
			
			if (inputLines!=null) {
				PrintWriter w = new PrintWriter(p.getOutputStream());
				inputLines.iterator().forEachRemaining(e->w.println(e));
				w.close();
			}
			
			
			p.waitFor();
		
			context.blockTermHandler = false;
		} finally {
			context.reader.getTerminal().init();
		}
		
	}
	
	
}
