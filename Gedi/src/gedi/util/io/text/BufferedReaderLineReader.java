package gedi.util.io.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class BufferedReaderLineReader implements LineReader {

	private BufferedReader br;
	
	public BufferedReaderLineReader(BufferedReader br) {
		this.br = br;
	}
	
	public BufferedReaderLineReader(InputStream is) {
		this.br = new BufferedReader(new InputStreamReader(is));
	}

	@Override
	public void close() throws IOException {
		br.close();
	}

	@Override
	public String readLine() throws IOException {
		return br.readLine();
	}


	
}
