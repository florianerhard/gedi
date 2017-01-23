package gedi.commandline;

import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.nashorn.JS;
import gedi.util.orm.OrmSerializer;
import gedi.util.r.RProcess;
import gedi.util.userInteraction.progress.Progress;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import jline.console.ConsoleReader;
import jline.console.history.FileHistory;

public class CommandLineSession {

	private static final String SESSION_FILE = ".ncl.session";
	public JS js;
	public ConsoleReader reader;
	public CancelableJSThread jsThread;
	public StringBuilder prevBuffer;
//	public Process runningProcess;
	public RProcess rprocess;
	public FileHistory history;
	public Progress progress;
	public boolean blockTermHandler;

	public void save() throws IOException {
		save(SESSION_FILE);
	}
	
	/*
	 * Output file format: 
	 * Each entry is composed of this:
	 * name, size,fst serialized
	 */
	public void save(String file) throws IOException {
		Map<String, Object> map = js.getVariables(false);
		if (map.isEmpty()) {
			File f = new File(file);
			if (f.exists()) f.delete();
			return;
		}
		
		PageFileWriter writer = new PageFileWriter(file);
		progress.init().setCount(map.size());
		
		OrmSerializer orm = new OrmSerializer(true, true);

		writer.putAsciiChars("NCL");
		writer.putCInt(map.size());
		
		long offset = writer.position();
		writer.putLong(0);
		
		for (String n : map.keySet()) {
			progress.setDescription("Writing "+n).incrementProgress();
			writer.putString(n);
			orm.serialize(writer, map.get(n));
		}
		
		long table = writer.position();
		orm.serializeClasstable(writer);
		writer.putLong(offset, table);
		progress.finish();

		writer.close();
		
	}

	public void load() throws IOException {
		load(SESSION_FILE);
	}
	
	public void load(String file) throws IOException {
		PageFile in = new PageFile(file);
		try {
			OrmSerializer orm = new OrmSerializer(true, true);

			if (!in.getAsciiChars(3).equals("NCL"))
				throw new RuntimeException("Illegal format of .ncl.session!");
			
			int count = in.getCInt();
			progress.init().setCount(count);
			
			long data = in.position()+8;
			in.position(in.getLong());
			orm.deserializeClasstable(in);
			in.position(data);
			
			for (int i=0; i<count; i++) {
				String n = in.getString();
				progress.setDescription("Reading "+n).incrementProgress();
				js.putVariable(n, orm.deserialize(in));
			}
		}
		finally {
			progress.finish();
			in.close();
		}
	}

	public RProcess getRProcess() throws IOException {
		if (rprocess==null || !rprocess.isRunning())
			rprocess = new RProcess();
		return rprocess;
	}
	
}
