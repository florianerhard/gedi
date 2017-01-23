package gedi.util.io.text.fasta.index;


import gedi.util.StringUtils;
import gedi.util.datastructure.tree.redblacktree.Interval;
import gedi.util.datastructure.tree.redblacktree.IntervalTreeSet;
import gedi.util.io.Directory;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.fasta.FastaFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;


public class AlignmentFastaIndexFile extends LineOrientedFile {

	private RandomAccessFile fastaFile;
	private FastaFile ff;
	protected HashMap<String,IntervalTreeSet<AlignmentFastaIndexEntry>> tree;
	

	public AlignmentFastaIndexFile(String path) {
		super(path);
	}

	public AlignmentFastaIndexFile(Directory dir, String name) {
		super(dir, name);
	}

	public FastaFile getFastaFile() {
		return ff;
	}
	

	public String getChromosome(String key) {
		return StringUtils.split(key, ":")[0];
	}
	
	public int getStart(String key) {
		return Integer.parseInt(StringUtils.split(StringUtils.split(key, ":")[1], "-")[0]);
	}
	public int getEnd(String key) {
		return Integer.parseInt(StringUtils.split(StringUtils.split(key, ":")[1], "-")[1]);
	}

	public AlignmentFastaIndexFile create(FastaFile ff) throws IOException {
		fastaFile = new RandomAccessFile(ff, "r");
		this.ff = ff;
		tree = new HashMap<String, IntervalTreeSet<AlignmentFastaIndexEntry>>();
		long L = fastaFile.length();

		startWriting();
		writeLine(ff.getAbsoluteFile().getParentFile().equals(getAbsoluteFile().getParentFile())?ff.getName():ff.getAbsolutePath());
		
		while (fastaFile.getFilePointer()<L) {
			long start = fastaFile.getFilePointer();
			String header = fastaFile.readLine();
			String key = header.substring(header.indexOf('.')+1);
			
			AlignmentFastaIndexEntry fe = new AlignmentFastaIndexEntry(start, getStart(key),getEnd(key)-1);
			String chr = getChromosome(key);
			
			IntervalTreeSet<AlignmentFastaIndexEntry> t = tree.get(chr);
			if (t==null) tree.put(chr,t = new IntervalTreeSet<AlignmentFastaIndexEntry>(null));
			t.add(fe);
			
			int lineLength = fastaFile.readLine().length();
			header = fastaFile.readLine();
			
			while (header.length()>0) {
				if (!header.startsWith(">")) throw new IOException("Malformed alignment fasta file!");
				fastaFile.skipBytes(lineLength+1);
				header = fastaFile.readLine();
				
			}
			fe.fend = fastaFile.getFilePointer()-2;
			
			writeLine(key+"\t"+fe.toString());
		}

		finishWriting();
		
		return this;
	}



	public AlignmentFastaIndexFile open() throws IOException {
		if (fastaFile==null) {
			Iterator<String> it = lineIterator("#");
			ff = new FastaFile(new File(getParentFile(),it.next()).getAbsolutePath());
			fastaFile = new RandomAccessFile(ff, "r");
			tree = new HashMap<String, IntervalTreeSet<AlignmentFastaIndexEntry>>();
			while (it.hasNext()) {
				String[] a = StringUtils.split(it.next(),'\t');
				String key = a[0];
				
				AlignmentFastaIndexEntry fe = new AlignmentFastaIndexEntry(Long.parseLong(a[1]), getStart(key),getEnd(key)-1);
				fe.fend = Long.parseLong(a[2]);
				
				String chr = getChromosome(key);
				IntervalTreeSet<AlignmentFastaIndexEntry> t = tree.get(chr);
				if (t==null) tree.put(chr,t = new IntervalTreeSet<AlignmentFastaIndexEntry>(null));
				t.add(fe);
			}
		}
		return this;
	}

	public boolean isOpen() {
		return fastaFile!=null;
	}

	public void close() throws IOException {
		fastaFile.close();
		fastaFile = null;
		tree = null;
	}

	public Set<String> getChromosomes() {
		return tree.keySet();
	}
	
	
	public IntervalTreeSet<AlignmentFastaIndexEntry> getIntervalTree(String chr) {
		return tree.get(chr);
	}

	public class AlignmentFastaIndexEntry implements Interval {
		private long fstart;
		private long fend;
		private int start;
		private int stop;
		
		public AlignmentFastaIndexEntry(long fstart, int start, int stop) {
			this.fstart = fstart;
			this.start = start;
			this.stop = stop;
		}
		public long getFileStart() {
			return fstart;
		}
		public long getFileEnd() {
			return fend;
		}
		public String getContent() throws IOException {
			byte[] re = new byte[(int) (fend-fstart)];
			fastaFile.seek(fstart);
			fastaFile.read(re, 0, re.length);
			fastaFile.skipBytes(1);
			return new String(re);
		}
		@Override
		public String toString() {
			return String.format("%d\t%d", fstart,fend);
		}
		@Override
		public int getStart() {
			return start;
		}
		@Override
		public int getStop() {
			return stop;
		}
		

	}

	

}
