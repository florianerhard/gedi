package gedi.util.io.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamLineReader extends BufferedReaderLineReader {

	public StreamLineReader(InputStream in) {
		super(new BufferedReader(new InputStreamReader(in)));
	}

}
