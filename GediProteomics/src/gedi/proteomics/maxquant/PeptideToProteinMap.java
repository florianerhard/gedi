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
package gedi.proteomics.maxquant;


import gedi.proteomics.digest.DigestIterator;
import gedi.proteomics.digest.Digester;
import gedi.proteomics.digest.FullAfterAADigester;
import gedi.proteomics.digest.RemovePrematureStopPeptidesFilter;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.io.text.fasta.DefaultFastaHeaderParser;
import gedi.util.io.text.fasta.FastaFile;
import gedi.util.io.text.fasta.index.FastaIndexFile;
import gedi.util.io.text.fasta.index.FastaIndexFile.FastaIndexEntry;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;



public class PeptideToProteinMap implements Serializable {

	private static final Logger log = Logger.getLogger( PeptideToProteinMap.class.getName() );

	private HashMap<String,FastaIndexEntry> proteins = new HashMap<String, FastaIndexEntry>();
	private String[] peptides;
	private HashMap<String,ProteinPositions> positions = new HashMap<String, ProteinPositions>();

	
	private Digester digest = new RemovePrematureStopPeptidesFilter(new FullAfterAADigester('K','R'));
	
	private PeptideToProteinMap() {}
	
	
	public void addFasta(String path) throws IOException {
		
		path = path.replace('\\', System.getProperty("file.separator").charAt(0));
		FastaFile file = new FastaFile(path);

		FastaIndexFile fi;
		
		log.fine("Obtaining fasta index for "+file);
		try {
			file.setFastaHeaderParser(new DefaultFastaHeaderParser(Pattern.compile(">([^ ]+)")));
			fi = file.obtainAndOpenDefaultIndex();
			for (String name : fi.getEntryNames()) {
				proteins.put(name, fi.getEntry(name));
				String seq = fi.getSequence(name);
				
				DigestIterator it = digest.iteratePeptides(seq);
				while (it.hasNext()) {
					String pep = it.next();
					ProteinPositions pp = positions.get(pep);
					if (pp==null) positions.put(pep, pp = new ProteinPositions(true));
					pp.add(name, it.getStartPosition());
				}
			}
			
		} catch (IOException e) {
			log.log(Level.SEVERE,"Could not read fasta file "+file+"!",e);
			throw new RuntimeException("Could not read fasta file "+file+"!",e);
		}
		
	}
	
	
	public String[] getPeptideSequences() {
		return peptides;
	}


	public boolean containsPeptide(String peptide) {
		return positions.containsKey(peptide);
	}


	public ProteinPositions getProteinPositions(String peptide) {
		return positions.get(peptide);
	}


	public String getSequence(ProteinPositions pp) {
		return peptides[pp.index];
	}

	public static class ProteinPositions {
		private int index;
		private ArrayList<String> proteinId;
		private IntArrayList pos;
		
		private ProteinPositions(boolean init) {
			if (init) {
				proteinId = new ArrayList<String>(1);
				pos = new IntArrayList(1);				
			}
		}
		
		public int getIndex() {
			return index;
		}
		
		private void add(String id, int pos) {
			proteinId.add(id);
			this.pos.add(pos);
		}

		public int size() {
			return proteinId.size();
		}

		public String getId(int i) {
			return proteinId.get(i);
		}

		public int getPosition(int i) {
			return pos.getInt(i);
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (int i=0; i<size(); i++) {
				if (i>0) sb.append(",");
				sb.append(getId(i));
				sb.append(" (");
				sb.append(getPosition(i));
				sb.append(")");
			}
			return sb.toString();
		}
		
//		public ProteinPositions restrict(Set<String> ids) {
//			ArrayList<String> reids = new ArrayList<String>();
//			IntArrayList repos = new IntArrayList();
//			for (int i=0; i<size(); i++)
//				if (ids.contains(proteinId[i])) {
//					reids.add(proteinId[i]);
//					repos.add(pos[i]);
//				}
//			ProteinPositions re = new ProteinPositions();
//			re.proteinId = reids.toArray(new String[0]);
//			re.pos = repos.toIntArray();
//			return re;
//		}
		
	}


//	private void writeObject(java.io.ObjectOutputStream out)
//			throws IOException {
//		// write all protein ids with numerical id first
//		HashMap<String,Integer> pi = new HashMap<String, Integer>();
//		int index = 0;
//		for (String pep : positions.keySet()) {
//			for (String p : positions.get(pep).proteinId) {
//				if (pi.containsKey(p)) continue;
//				pi.put(p, index++);
//				if (p.length()>Byte.MAX_VALUE) throw new IOException("Cannot write ids longer than "+Byte.MAX_VALUE);
//				out.writeByte(p.length());
//				for (int i=0; i<p.length(); i++)
//					out.writeByte(p.charAt(i));
//			}
//		}
//		out.writeByte(-1);
//		out.writeInt(positions.size());
//		for (String pep : positions.keySet()) {
//			if (pep.length()>255)throw new IOException("Cannot write peptides longer than 255 aa!");
//			out.writeByte(pep.length());
//			for (int i=0; i<pep.length(); i++)
//				out.writeByte(pep.charAt(i));
//			ProteinPositions p = positions.get(pep);
//			if (p.size()>Short.MAX_VALUE) throw new IOException("Cannot address that many protein ids for a peptide!");
//			out.writeShort(p.size());
//			for (int i=0; i<p.size(); i++) {
//				out.writeInt(pi.get(p.proteinId[i]));
//				if (p.pos[i]>=1<<16)throw new IOException("Cannot address position "+p.pos[i]);
//				out.writeShort(p.pos[i]);
//			}
//		}
//		
//	}
//
//	private void readObject(java.io.ObjectInputStream in)
//			throws IOException {
//
//		ArrayList<String> pi = new ArrayList<String>(); 
//		while (true) {
//			byte b = in.readByte();
//			if (b<0) break;
//			char[] id = new char[b];
//			for (int i=0; i<id.length; i++)
//				id[i] = (char) in.readByte();
//			pi.add(String.valueOf(id));
//		}
//		int n = in.readInt();
//		int S = 1<<16;
//		for (int e=0; e<n; e++) {
//			int l = in.readByte();
//			if(l<0) l = 256+l;
//			char[] id = new char[l];
//			for (int i=0; i<id.length; i++)
//				id[i] = (char) in.readByte();
//			String pep = String.valueOf(id);
//			int ns = in.readShort();
//			ProteinPositions pp = new ProteinPositions(ns);
//			for (int i=0; i<ns; i++) {
//				int pid=in.readInt();
//				int pos=in.readShort();
//				if (pos<0) pos=S+pos;
//				pp.add(pi.get(pid), pos);
//			}
//			positions.put(pep, pp);
//		}
//		
//	}
	
//	private void writeObject(java.io.ObjectOutputStream out)
//			throws IOException {
//		// write all protein ids with numerical id first
//		HashMap<String,Integer> pi = new HashMap<String, Integer>();
//		int index = 0;
//		for (String pep : positions.keySet()) {
//			for (String p : positions.get(pep).proteinId) {
//				if (pi.containsKey(p)) continue;
//				pi.put(p, index++);
//				out.writeInt(p.length());
//				for (int i=0; i<p.length(); i++)
//					out.writeChar(p.charAt(i));
//			}
//		}
//		out.writeInt(-1);
//		out.writeInt(positions.size());
//		for (String pep : positions.keySet()) {
//			out.writeInt(pep.length());
//			for (int i=0; i<pep.length(); i++)
//				out.writeChar(pep.charAt(i));
//			ProteinPositions p = positions.get(pep);
//			out.writeInt(p.size());
//			for (int i=0; i<p.size(); i++) {
//				out.writeInt(pi.get(p.proteinId[i]));
//				out.writeInt(p.pos[i]);
//			}
//		}
//		
//		out.flush();
//		
//	}
//
//	private void readObject(java.io.ObjectInputStream in)
//			throws IOException {
//
//		ArrayList<String> pi = new ArrayList<String>(); 
//		while (true) {
//			int b = in.readInt();
//			if (b<0) break;
//			char[] id = new char[b];
//			for (int i=0; i<id.length; i++)
//				id[i] = in.readChar();
//			pi.add(String.valueOf(id));
//		}
//		int n = in.readInt();
//		for (int e=0; e<n; e++) {
//			char[] id = new char[in.readInt()];
//			for (int i=0; i<id.length; i++)
//				id[i] = in.readChar();
//			String pep = String.valueOf(id);
//			int ns = in.readInt();
//			ProteinPositions pp = new ProteinPositions(ns);
//			for (int i=0; i<ns; i++) {
//				int pid=in.readInt();
//				int pos=in.readInt();
//				pp.add(pi.get(pid), pos);
//			}
//			positions.put(pep, pp);
//		}
//		
//	}


}
