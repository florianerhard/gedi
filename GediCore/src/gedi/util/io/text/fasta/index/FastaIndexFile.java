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

package gedi.util.io.text.fasta.index;


import gedi.util.StringUtils;
import gedi.util.io.randomaccess.ConcurrentPageFile;
import gedi.util.io.randomaccess.ConcurrentPageFileView;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.fasta.FastaFile;
import gedi.util.io.text.fasta.FastaHeaderParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;


/**
 * This is finally thread safe
 * @author erhard
 *
 */
public class FastaIndexFile extends LineOrientedFile {

	private ConcurrentPageFileView view;
	private FastaFile ff;
	private HashMap<String,FastaIndexEntry> index;

	public FastaIndexFile(String path) {
		super(path);
	}

	public FastaIndexFile(File dir, String name) {
		super(dir, name);
	}

	public FastaIndexFile create(FastaFile ff) throws IOException {
		return create(ff,ff.getFastaHeaderParser(),true);
	}
	
	public FastaFile getFastaFile() {
		return ff;
	}
	
	public FastaIndexFile create(FastaFile ff, FastaHeaderParser headerParser, boolean write) throws IOException {
		PageFile fastaFile = new PageFile(ff.getAbsolutePath());
		this.ff = ff;
		index = new HashMap<String, FastaIndexFile.FastaIndexEntry>();
		long L = fastaFile.size();

		ArrayList<String> order = new ArrayList<String>();
		fastaFile.get();
		while (fastaFile.position()<L) {
			String id = headerParser.getId(">"+fastaFile.readLine());
			long start = fastaFile.position();
			int lineWidth = fastaFile.readLine().length();
			long end = start;
			int first = fastaFile.position()<L?fastaFile.get():0;
			while (fastaFile.position()<L && first!='>') {
				if (first=='>') break;
				if (first=='\n'||first=='\r') {
					end=fastaFile.position()-2;
					advanceToNextEntry(id,L,fastaFile);
					break;
				}
				if (skipLine(fastaFile)+1!=lineWidth) {
					end=fastaFile.position()-1;
					advanceToNextEntry(id,L,fastaFile);
					break;
				}
				if (fastaFile.position()<L) 
					first = fastaFile.get();
				else
					first = 0;
				
			}
			if (end==start) {
				if (fastaFile.position()==L)
					end=fastaFile.position()-1;
				else
					end=fastaFile.position()-2;
			}

			index.put(id,new FastaIndexEntry(id,start, end, lineWidth));
			order.add(id);
		}
		if (write) {
			startWriting();
			writeLine(ff.getAbsoluteFile().getParentFile().equals(getAbsoluteFile().getParentFile())?ff.getName():ff.getAbsolutePath());
			for (String id : order) 
				writeLine(index.get(id).toString());
			finishWriting();
		}
		close();
		open();
		
		return this;
	}


	private int skipLine(PageFile fastaFile) throws IOException {
		boolean eol = false;
		int l = 0;
		while (!eol) {
			int r = fastaFile.get();
			switch (r) {
			case -1:
			case '\n':
				eol = true;
				break;
			case '\r':
				eol = true;
				long cur = fastaFile.position();
				if ((fastaFile.get()) != '\n') {
					fastaFile.position(cur);
				}
				break;
			default:
				l++;
				break;
			}
		}
		return l;
	}

	private void advanceToNextEntry(String id, long L, PageFile fastaFile) throws IOException {
		for (byte b=10; fastaFile.position()<L && b!=62; b=fastaFile.get())
			if (b!=13 && b!=10)
				throw new IOException("Invalid fasta at "+id+" ("+(int)b+")!");
	}

//	private boolean isReadEntryStart() throws IOException {
//		return fastaFile.readByte()==62;
//	}

	public FastaIndexFile open() throws IOException {
		if (ff==null) {
			Iterator<String> it = lineIterator("#");
			String fn = it.next();
			if (new File(fn).isAbsolute())
				ff = new FastaFile(fn);
			else
				ff = new FastaFile(new File(getParentFile(),fn).getAbsolutePath());
//			ConcurrentPageFile fastaFile = new ConcurrentPageFile(ff.getAbsolutePath());
//			view = new ConcurrentPageFileView(fastaFile);
			index = new HashMap<String, FastaIndexFile.FastaIndexEntry>();
			while (it.hasNext()) {
				String[] a = StringUtils.split(it.next(),'\t');
				index.put(a[0],new FastaIndexEntry(a[0],Long.parseLong(a[1]),Long.parseLong(a[2]),Integer.parseInt(a[3])));
			}
		}
		return this;
	}
	
	
	protected ConcurrentPageFileView getView() throws IOException {
		if (view==null) {
			ConcurrentPageFile fastaFile = new ConcurrentPageFile(ff.getAbsolutePath());
			view = new ConcurrentPageFileView(fastaFile);
		}
		return view;
	}

	public boolean isOpen() {
		return ff!=null;
	}

	public void close() throws IOException {
		if (view!=null)
			view.close();
		ff = null;
		view = null;
		index = null;
	}

	public Set<String> getEntryNames() {
		return index.keySet();
	}

	public int getLength(String name) {
		return getEntry(name).length();
	}

	/**
	 * start zerobased inclusive, end zerobased exclusive
	 * @param name
	 * @param start
	 * @param end
	 * @return
	 * @throws IOException
	 */
	public String getSequence(String name, int start, int end) throws IOException {
		FastaIndexEntry e = getEntry(name);
		if (e==null)return null;
		return e.getSequence(start, end);
	}
	public String getSequence(String name, int start, int end, int flank) throws IOException {
		FastaIndexEntry e = getEntry(name);
		if (e==null)return null;
		return e.getSequence(start, end,flank);
	}
	
	public String getSequence(String name) throws IOException {
		FastaIndexEntry e = getEntry(name);
		if (e==null)
			return null;
		return e.getSequence();
	}
	
	public boolean containsEntry(String name) {
		return getEntry(name)!=null;
	}

	public FastaIndexEntry getEntry(String name) {
		FastaIndexEntry re = index.get(name);
		if (re==null && name.indexOf(' ')!=-1)
			re = index.get(name.substring(0,name.indexOf(' ')));
		if (re==null && index.containsKey("chr"+name))
			re = index.get("chr"+name);
		if (re==null && index.containsKey(StringUtils.removeHeader(name, "chr")))
			re = index.get(StringUtils.removeHeader(name, "chr"));
		return re;
	}

	public class FastaIndexEntry {
		private String id;
		private long start;
		private long end;
		private int lineWidth;
		private int length;
//		private MappedByteBuffer buffer;
		
		public FastaIndexEntry(String id,long start, long end, int lineWidth) throws IOException {
			this.id = id;
			this.start = start;
			this.end = end;
			this.lineWidth = lineWidth;
			long longEnd = end-start-(end-start)/(lineWidth+1);
			if (longEnd>Integer.MAX_VALUE)
				throw new IOException("Sequence "+id+" is too long!");
			this.length = (int) longEnd;

//			buffer = fastaFile.getChannel().map(MapMode.READ_ONLY, start, end-start);
		}
		public int length() {
			return length;
		}
		public String getSequence() throws IOException {
			return getSequence(0,length);
		}
		public String getSequence(int start, int end) throws IOException {
			return getSequence(start,end,0);
		}
		public String getSequence(int start, int end, int flank) throws IOException {
			int addS = -start;
			int addE = end-length;
			start = Math.max(start, 0);
			end = Math.min(end,length);
			int fstart = Math.max(start-flank, 0);
			int fend = Math.min(end+flank,length);
			if (fend<fstart) return null;
			
			int relength = (int) (fend-fstart);
			byte[] re = new byte[relength];
			int rem = fstart%lineWidth;
			int bin = fstart+fstart/lineWidth;
			
//			buffer.position((int) bin);
//			buffer.get(re,0,Math.min((int) (lineWidth-rem),re.length));
//			for (int i=(int) (lineWidth-rem); i<re.length; i+=lineWidth){
//				buffer.get();
//				buffer.get(re,i, Math.min(re.length-i,lineWidth));
//			}
//			fastaFile.close();
//			fastaFile = new RandomAccessFile(ff, "r");
			
//			synchronized (fastaFile) {
//				fastaFile.seek(this.start+bin);
//				fastaFile.read(re, 0, Math.min((int) (lineWidth-rem),re.length));
//				fastaFile.skipBytes(1);
//				for (int i=(int) (lineWidth-rem); i<re.length; i+=lineWidth){
//					fastaFile.read(re, i, Math.min(re.length-i,lineWidth));
//					fastaFile.skipBytes(1);
//				}
//			}
				
			ConcurrentPageFileView view = getView();
			view.position(this.start+bin);
			view.get(re, 0, Math.min((int) (lineWidth-rem),re.length));
			view.get();
			for (int i=(int) (lineWidth-rem); i<re.length; i+=lineWidth){
				view.get(re, i, Math.min(re.length-i,lineWidth));
				view.get();
			}

			if (flank>0) {
				String s = new String(re);
				s = s.substring(0,(int) (start-fstart)).toLowerCase()+
					s.substring((int) (start-fstart),(int) (end-fstart)).toUpperCase()+
					s.substring((int) (end-fstart)).toLowerCase();
				return s;
			}
			if (addS>0 || addE>0)
				return StringUtils.repeat("N", (int)addS) + new String(re) + StringUtils.repeat("N", (int)addE);
			
			return new String(re);
		}
		public long getStart() {
			return start;
		}
		public long getEnd() {
			return end;
		}
		public int getLineWidth() {
			return lineWidth;
		}
		@Override
		public String toString() {
			return String.format("%s\t%d\t%d\t%d", id,start,end,lineWidth);
		}
		public FastaIndexFile getFile() {
			return FastaIndexFile.this;
		}
		

	}

	

}
