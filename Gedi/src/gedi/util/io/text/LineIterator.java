package gedi.util.io.text;

import gedi.util.functions.ExtendedIterator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

public class LineIterator implements ExtendedIterator<String>, AutoCloseable {

	private String nextLine = null;
	private LineReader lr;
	private String[] commentPrefixes;
	private boolean closed = false;
	private String firstLine;
	private boolean skipEmpty = false;

	private long bytesRead = 0;
	private long lastOffset = 0;


	public LineIterator(InputStream stream, String... commentPrefixes)  {
		this(new InputStreamReader(stream),commentPrefixes);
	}


	public LineIterator(String content, String... commentPrefixes) {
		this(new StringReader(content),commentPrefixes);
	}

	public LineIterator(Reader reader, String... commentPrefixes) {
		this.commentPrefixes = commentPrefixes;
		lr = new BufferedReaderLineReader(new BufferedReader(reader));
		lookAhead();
		firstLine = nextLine;
	}

	public LineIterator(LineReader reader, String... commentPrefixes) {
		this.commentPrefixes = commentPrefixes;
		lr = reader;
		lookAhead();
		firstLine = nextLine;

	}

	public boolean isSkipEmpty() {
		return skipEmpty;
	}
	public void setSkipEmpty(boolean skipEmpty) {
		this.skipEmpty = skipEmpty;
	}

	public void close() throws IOException {
		if (!closed) {
			if (lr!=null) lr.close();
			closed=true;
		}
	}

	public String getFirstLine() {
		return firstLine;
	}

	@Override
	public boolean hasNext() {
		lookAhead();
		return nextLine!=null;
	}

	@Override
	public String next() {
		lookAhead();
		String re = nextLine;
		lastOffset = bytesRead-re.length()-1;
		nextLine=null;
		return re;
	}

	/**
	 * Gets the offset of the line previously returned by next
	 * @return
	 */
	public long getOffset() {
		return lastOffset;
	}


	private void lookAhead() {
		if (nextLine==null) {
			if (closed)
				return;
			try {
				nextLine = lr.readLine();
				if (nextLine!=null)
					bytesRead+=nextLine.length()+1;

				if (commentPrefixes.length>0) 
					while (nextLine!=null && toSkip(nextLine)) {
						nextLine = lr.readLine();
						if (nextLine!=null)
							bytesRead+=nextLine.length()+1;
					}

				if (nextLine==null) {
					close();
				}
			} catch (Exception e) {
				throw new RuntimeException("Could not iterate over lines!",e);
			}
		}
	}

	private boolean toSkip(String line) {
		if(skipEmpty && line.length()==0) return true;

		for (String cp : commentPrefixes)
			if (cp.length()==0 && line.length()==0)
				return true;
			else if (cp.length()>0 && nextLine.startsWith(cp)){
				return true;
			}
		return false;
	}

	@Override
	public void remove() {}


}
