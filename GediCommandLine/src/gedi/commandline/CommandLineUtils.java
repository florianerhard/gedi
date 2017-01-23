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
