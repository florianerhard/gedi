package gedi.util.io.text;

import java.io.IOException;

public interface LineReader extends AutoCloseable {

	String readLine() throws IOException;
	public void close() throws IOException;
	
	
	default void toWriter(LineWriter lw) throws IOException {
		for (String l :  new LineIterator(this).loop())
			lw.writeLine(l);
	}
	
	
}
