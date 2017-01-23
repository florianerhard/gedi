package gedi.util.io.text;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class StreamLineWriter implements LineWriter {

	
	private OutputStreamWriter writer;

	public StreamLineWriter(OutputStream stream) {
		writer = new OutputStreamWriter(stream);
	}
	
	
	@Override
	public void write(String line) throws IOException {
		writer.write(line);
	}

	@Override
	public void flush() throws IOException {
		writer.flush();
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}

	
	
}
