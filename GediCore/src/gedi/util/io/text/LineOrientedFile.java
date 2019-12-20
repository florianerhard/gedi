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
package gedi.util.io.text;



import gedi.util.FunctorUtils;
import gedi.util.FunctorUtils.MergeIterator;
import gedi.util.ReflectionUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;



public class LineOrientedFile extends File {

	private static final long serialVersionUID = 367633002394184790L;
	public static final String STDOUT = "STDOUT";
	public static final String STDERR = "STDERR";
	
	protected String[] lines;
	protected Writer fw;
	private Boolean gzipped;
	private Boolean bzipped2;
	protected boolean autoFlush = false;
	
	public LineOrientedFile(File dir, String name) {
		super(dir,name);
		if (isPipe()) {
			gzipped = false;
			autoFlush = true;
		}
	}
	
	public LineOrientedFile() {
		this(STDOUT);
	}
	
	public LineOrientedFile(String path) {
		super(path);
		if (isPipe()) {
			gzipped = false;
			autoFlush = true;
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		if (isWriting())
			finishWriting();
	}
	
	public boolean isGZIP() throws IOException {
		if (gzipped==null) {
			if (!exists())
				gzipped = getPath().endsWith(".gz");
			else {
		        InputStream in = createInputStream();
		        try {
		            gzipped = in.read() == (GZIPInputStream.GZIP_MAGIC & 0xFF)
		                && in.read() == (GZIPInputStream.GZIP_MAGIC >> 8);
		        } finally {
		            in.close();
	        }
			}
		}
		return gzipped;
    }
	
	public boolean isBZIP2() throws IOException {
		if (bzipped2==null) {
			if (!exists())
				bzipped2 = getPath().endsWith(".bz2");
			else {
		        InputStream in = createInputStream();
		        try {
		        	bzipped2 = in.read() == 'B' && in.read()=='Z' && in.read()=='h';
		        } finally {
		            in.close();
	        }
			}
		}
		return bzipped2;
    }
	
	public long guessNumOfLines() throws IOException {
		return guessNumOfLines(100);
	}
	public long guessNumOfLines(int useFirstLines) throws IOException {
		long firstLinesLength = 0;
		Iterator<String> it = lineIterator();
		int i = 0;
		while (it.hasNext() && i++<useFirstLines)
			firstLinesLength+=it.next().length();
		if (!it.hasNext())
			return firstLinesLength;
		else
			return (long)((double)length()/((double)firstLinesLength/(double)useFirstLines));
	}
	
	public void setAutoFlush(boolean autoFlush) {
		this.autoFlush = autoFlush;
	}
	
	public boolean isAutoFlush() {
		return autoFlush;
	}
	
	public void startAppending() throws IOException {
		startWriting(true);
	}
	
	public boolean isWriting() {
		return fw!=null;
	}
	
	public Writer startWriting() throws IOException {
		return startWriting(false);
	}
	
	public Writer getWriter() {
		return fw;
	}
	
	public LineWriter write() {
		return write(false);
	}
	public LineWriter write(final boolean autoflush) {
		return new LineWriter() {
			
			@Override
			protected void finalize() throws Throwable {
				close();
			}
			@Override
			public void write(String line) throws IOException {
				if (!isWriting())
					startWriting();
				LineOrientedFile.this.write(line);
				if (autoflush)
					flush();
			}
			
			@Override
			public void close() throws IOException {
				finishWriting();
			}
			
			@Override
			public void flush() throws IOException {
				if (fw!=null)
					fw.flush();
			}
			
			public String toString() {
				return getPath();
			}
		};
	}
	
	public LineWriter append() {
		return new LineWriter() {
			
			@Override
			protected void finalize() throws Throwable {
				close();
			}
			@Override
			public void write(String line) throws IOException {
				if (!isWriting())
					startWriting(true);
				LineOrientedFile.this.write(line);
			}
			
			@Override
			public void close() throws IOException {
				finishWriting();
			}

			@Override
			public void flush() throws IOException  {
				fw.flush();
			}
		};
	}
	
	public Writer startWriting(boolean appending) throws IOException {
		if (getParentFile()!=null && !getParentFile().exists())
			getParentFile().mkdirs();
		fw = createWriter(appending);
		return fw;
	}
	public void writeLine() throws IOException {
		writeLine("");
	}
	public void writeLine(String line) throws IOException {
		if (fw==null) throw new RuntimeException("Not started writing in file "+getPath()+"!");
		fw.write(line);
		fw.write("\n");
		if (autoFlush)
			fw.flush();
	}
	
	public void write(String line) throws IOException {
		if (fw==null) throw new RuntimeException("Not started writing in file "+getPath()+"!");
		fw.write(line);
		if (autoFlush)
			fw.flush();
	}
	
	public void writef(String line, Object...args) throws IOException {
		if (fw==null) throw new RuntimeException("Not started writing in file "+getPath()+"!");
		fw.write(String.format(Locale.US,line,args));
		if (autoFlush)
			fw.flush();
	}
	
	public void finishWriting() throws IOException {
		if (isWriting()) {
			fw.flush();
			if (!isPipe()) {
				fw.close();
			}
			fw = null;
		}
	}
	
	public void loadIntoMemory() throws IOException {
		if (lines==null) {
			lines = FunctorUtils.toArray(lineIterator(),String.class);
		}
	}
	
	public boolean isPipe() {
		return getPath().equals(STDOUT) || getPath().equals(STDERR);
	}
	
	boolean forbiddenLineIterator = false;
	private String enc = Charset.defaultCharset().name();
	
	public LineIterator lineIterator(String...commentPrefixes) throws IOException {
		if (forbiddenLineIterator)
			throw new IOException("Creating another line iterator is forbidden!");
		
		if (isPipe())
			forbiddenLineIterator = true;
		
		LineIterator re = new LineIterator(createReader(),commentPrefixes);
		firstLineRead(re.getFirstLine());
		return re;
	}
	
	public void setEncoding(String charset) {
		this.enc = charset;
	}
	
	public String getEncoding() {
		return enc;
	}
	
	/**
	 * Not buffered!
	 * @return
	 * @throws IOException
	 */
	public InputStreamReader createReader() throws IOException {
		if (isGZIP())
			return new InputStreamReader(new GZIPInputStream(createInputStream()));
		else if (isBZIP2())
			try {
				return new InputStreamReader((InputStream) ReflectionUtils.newInstance(Class.forName("org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream"),createInputStream()));
			} catch (NoSuchFieldException | SecurityException
					| IllegalArgumentException | IllegalAccessException
					| InstantiationException | InvocationTargetException | ClassNotFoundException e) {
				throw new RuntimeException("Bzip2 not supported!");
			}
		else
			return new InputStreamReader(createInputStream());
	}
	
	protected InputStream createInputStream() throws FileNotFoundException {
		if (isPipe())
			return System.in;
		else
			return new FileInputStream(this);
	}
	
	public Writer createWriter() throws IOException {
		return createWriter(false);
	}
	
	public Writer createWriter(boolean append) throws IOException {
		if (exists() && !append)delete();
		isGZIP();
		isBZIP2();
		
		OutputStream st = createOutputStream(append);
		if (isGZIP())
			st = new GZIPOutputStream(st);
		if (isBZIP2())
			st = new BZip2CompressorOutputStream(st);
		Writer re = new OutputStreamWriter(st,enc);
		if (isPipe()) return re;
		return new BufferedWriter(re);
	}
	
	protected OutputStream createOutputStream(boolean append) throws FileNotFoundException  {
		if (this.getPath().equals(STDOUT))
			return System.out;
		else if (this.getPath().equals(STDERR))
			return System.err;
		else
			return new FileOutputStream(this,append);
	}
	
	protected void firstLineRead(String firstLine) {};
	
	
	public void writeAllText(String t) throws IOException {
		startWriting();
		write(t);
		finishWriting();
	}
	
	public void writeAllLines(String[] lines) throws IOException {
		startWriting();
		for (String t : lines)
			writeLine(t);
		finishWriting();
	}

	public String readAllText2() {
		try {
			return readAllText();
		} catch (IOException e) {
			throw new RuntimeException("Could nor read file "+getPath(),e);
		}
	}

	public String readAllText() throws IOException {
		StringBuilder sb = new StringBuilder();
		Iterator<String> it = lineIterator();
		while (it.hasNext()) {
			sb.append(it.next());
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public String[] readAllLines2(String...commentPrefix) {
		try {
			return readAllLines(commentPrefix);
		} catch (IOException e) {
			throw new RuntimeException("Could nor read file "+getPath(),e);
		}
	}
	public String[] readAllLines(String...commentPrefix) throws IOException {
		ArrayList<String> re = new ArrayList<String>();
		Iterator<String> it = lineIterator(commentPrefix);
		while (it.hasNext()) 
			re.add(it.next());
		return re.toArray(new String[re.size()]);
	}
	
	public void sort(Comparator<String> comp) throws IOException {
		sort(comp,100*1024*1024,this);
	}
	
	public void sort(Comparator<String> comp, int size) throws IOException {
		sort(comp,size,this);
	}
	public void sort(Comparator<String> comp, LineOrientedFile out) throws IOException {
		sort(comp,100*1024*1024, out);
	}
	
	public void sort(Comparator<String> comp, int size, LineOrientedFile out) throws IOException {
		
		LinkedList<String> lines = new LinkedList<String>();
		int N = 0;
		ArrayList<LineOrientedFile> tmps = new ArrayList<LineOrientedFile>();
		
		Iterator<String> it = lineIterator();
		while (it.hasNext()) {
			String l = it.next();
			N+=l.length()/2;
			if (N>size) {
				writeTmp(tmps, lines, comp, out);
				N=0;
			}
			lines.add(l);
		}
		if (N>0) 
			writeTmp(tmps, lines, comp, out);

		if (tmps.size()==0) return;
		
		if (this.equals(out))
				this.delete();
		
		if (tmps.size()==1) 
			tmps.get(0).renameTo(out);
		else {
			
			// merge all tmps
			ArrayList<Iterator<String>> iterators = new ArrayList<Iterator<String>>();
			for (LineOrientedFile lof : tmps) iterators.add(lof.lineIterator());
			@SuppressWarnings("unchecked")
			MergeIterator<String> merge = new MergeIterator<String>(iterators.toArray(new Iterator[0]), comp);
			out.startWriting();
			while (merge.hasNext()) 
				out.writeLine(merge.next());
			out.finishWriting();
			
			for (LineOrientedFile lof : tmps)
				lof.delete();
		}
		
	}
	
	private void writeTmp(List<LineOrientedFile> tmps, LinkedList<String> lines, Comparator<String> comp, LineOrientedFile out) throws IOException {
		LineOrientedFile lof = new LineOrientedFile(out.getAbsolutePath()+".tmp"+tmps.size());
		tmps.add(lof);
		Collections.sort(lines, comp);
		lof.startWriting();
		for (String s : lines) lof.writeLine(s);
		lof.finishWriting();
		lines.clear();
	}

	/**
	 * Gets a file with the same path except for a different extension
	 * @param extension
	 * @return
	 */
	public LineOrientedFile getExtensionSibling(String extension) {
		if (extension.startsWith(".")) extension = extension.substring(1);
		
		int dot = getPath().lastIndexOf('.');
		int slash = getPath().lastIndexOf('/');
		if (dot<slash || dot==-1) return new LineOrientedFile(getPath()+"."+extension);
		return new LineOrientedFile(getPath().substring(0, dot)+"."+extension);
	}

	


}
