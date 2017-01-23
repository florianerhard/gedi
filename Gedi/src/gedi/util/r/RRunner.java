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
