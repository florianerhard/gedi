package gedi.util;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;

public class RunUtils {
	
	
	public static String output(String...cmds)  {
		try {
			File tmp = File.createTempFile("run", ".out");
			Process proc = new ProcessBuilder(cmds)
				.redirectError(Redirect.INHERIT)
				.redirectOutput(Redirect.to(tmp))
				.redirectInput(Redirect.INHERIT)
				.start();
			proc.waitFor();
			
			String re = FileUtils.readAllText(tmp);
			tmp.delete();
			return re;
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("Cannot run "+StringUtils.concat(" ", cmds),e);
		}
	}

}
