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
package executables;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.freehep.graphicsio.swf.SWFAction.RandomNumber;

import gedi.app.Gedi;
import gedi.app.classpath.ClassPathCache;
import gedi.core.genomic.Genomic;
import gedi.proteomics.molecules.properties.mass.MassFactory;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.tree.Trie;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializer;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.io.text.fasta.FastaEntry;
import gedi.util.io.text.fasta.FastaFile;
import gedi.util.math.stat.RandomNumbers;
import gedi.util.program.CommandLineHandler;
import gedi.util.program.GediParameter;
import gedi.util.program.GediParameterSet;
import gedi.util.program.GediProgram;
import gedi.util.program.GediProgramContext;
import gedi.util.program.parametertypes.DoubleParameterType;
import gedi.util.program.parametertypes.FileParameterType;
import gedi.util.program.parametertypes.IntParameterType;
import gedi.util.program.parametertypes.StringParameterType;
import gedi.util.sequence.Alphabet;
import gedi.util.userInteraction.progress.Progress;

public class SplicedPeptides {

public static void main(String[] args) throws IOException {
		
	SplicedPeptidesParameterSet params = new SplicedPeptidesParameterSet();
		GediProgram pipeline = GediProgram.create("SplicedPeptides",
				new SplicedPeptidesProgram(params),
				new WritePseudoFastaProgram(params)
				);
		GediProgram.run(pipeline, null, new CommandLineHandler("SplicedPeptides","SplicedPeptides generates a sequence database containing spliced peptides as described in Liepe et al., Science 2017.",args));

	}

	public static class WritePseudoFastaProgram extends GediProgram {
	
	
	
		public WritePseudoFastaProgram(SplicedPeptidesParameterSet params) {
			addInput(params.f);
			addInput(params.splicedFile);
			addInput(params.bothFile);
			addInput(params.protFile);
			
			addOutput(params.pseudoProtFile);
		}
	
		@Override
		public String execute(GediProgramContext context) throws Exception {
			
			String fasta = getParameter(0);
			File fspl = getParameter(1);
			File fboth = getParameter(2);
			File fcon = getParameter(3);
			
			int[] lengths = new FastaFile(fasta).entryIterator().mapToInt(fe->fe.getSequence().length()).toIntArray();
			
			context.getLog().info("Writing fasta");
			LineWriter out = getOutputWriter(0);
			writeFasta(out,"Spliced",new LineOrientedFile(fspl.getPath()).lineIterator(),lengths);
			writeFasta(out,"Both",new LineOrientedFile(fboth.getPath()).lineIterator(),lengths);
			writeFasta(out,"Consec",new LineOrientedFile(fcon.getPath()).lineIterator(),lengths);
			
			out.close();
			
			return null;
		}
		
		private void writeFasta(LineWriter out, String label, ExtendedIterator<String> peps, int[] lengths) throws IOException {
			
			StringBuilder sb = new StringBuilder();
			int targetLen = lengths[RandomNumbers.getGlobal().getUnif(0, lengths.length)];
			int index = 0;
			
			for (String pep : peps.loop()) {
				
				sb.append(pep);
				if (sb.length()>targetLen-5) {
					out.writef(">%s_%d\n%s\n",label,index++,sb.toString());
					sb.delete(0, sb.length());
					targetLen = lengths[RandomNumbers.getGlobal().getUnif(0, lengths.length)];
				}
			}
			if (sb.length()>0)
				out.writef(">%s_%d\n%s\n",label,index++,sb.toString());
		}
	}

	public static class SplicedPeptidesProgram extends GediProgram {



		public SplicedPeptidesProgram(SplicedPeptidesParameterSet params) {
			addInput(params.f);
			addInput(params.o);
			addInput(params.intervening);
			addInput(params.min);
			addInput(params.max);
			addInput(params.msms);
			addInput(params.tol);
			
			addOutput(params.splicedFile);
			addOutput(params.bothFile);
			addOutput(params.protFile);
			
		}

		@Override
		public String execute(GediProgramContext context) throws Exception {
			String fasta = getParameter(0);
			int inter = getIntParameter(2);
			int minl = getIntParameter(3);
			int maxl = getIntParameter(4);
			String msms = getParameter(5);
			double ppm = getDoubleParameter(6);
			
			context.getLog().info("Read msms masses");
			double[] massesa = EI.lines(msms)
						.skip(1)
						.mapToDouble(a->Double.parseDouble(StringUtils.splitField(a, '\t', 15)))
						.filterDouble(x->!Double.isNaN(x))
						.toDoubleArray();
			long[] masses = new long[massesa.length];
			for (int i=0; i<masses.length; i++)
				masses[i] = MassFactory.getInstance().getMass(massesa[i]);
			Arrays.sort(masses);
			
			context.getLog().info("Creating all consecutive peptides from the proteome");
			int[] lengths = new FastaFile(fasta).entryIterator().mapToInt(fe->fe.getSequence().length()).toIntArray();
			
			HashMap<String,boolean[]> consec = new HashMap<>(); // value: also hit by spliced peptide
			new FastaFile(fasta).entryIterator()
					.progress(context.getProgress(), lengths.length, (fe)->"Creating peptides")
					.forEachRemaining(fe->{
						
				for (int l=minl; l<=maxl; l++) {
					for (int s=0; s<fe.getSequence().length()-l+1; s++) {
						String pep = fe.getSequence().substring(s,s+l);
						if (matchesMass(pep, masses, ppm))
							consec.put(pep,new boolean[]{false});
					}
				}
			});
			
			
			context.getLog().info("Creating all spliced peptides from the proteome");
			
			LineWriter spliced = getOutputWriter(0);
			for (String pep : new FastaFile(fasta).entryIterator()
					.progress(context.getProgress(), lengths.length, (fe)->"Creating spliced peptides")
					.parallelized(48,1,ei->ei
							.unfold(fe-> {
						ArrayList<String> buffer = new ArrayList<>(200_000);
						for (int l1=1; l1<maxl; l1++) {
							for (int l2=Math.max(1, minl-l1); l1+l2<=maxl; l2++) {
								for (int s=0; s+l1<fe.getSequence().length(); s++) {
									for (int s2=s+l1+1; s2<s+l1+inter && s2+l2<fe.getSequence().length(); s2++) {
										
										for (int r=0; r<1; r++) {
											
											String pep = r==0?(fe.getSequence().substring(s,s+l1)+fe.getSequence().substring(s2,s2+l2)):(fe.getSequence().substring(s2,s2+l2)+fe.getSequence().substring(s,s+l1));
											if (matchesMass(pep, masses, ppm)) {
												boolean[] hit = consec.get(pep);
												if (hit!=null) 
													hit[0] = true;
												else {
													buffer.add(pep);
												}
											}
											
											
										}
										
									}
								}
							}
						}
						return EI.wrap(buffer);
						})).sort(new StringSerializer()).unique(true).loop()) {
				spliced.writeLine(pep);
			}
			spliced.close();
			
			context.getLog().info("Writing other peptides");
			writeRest(getOutputWriter(1),true,consec);
			writeRest(getOutputWriter(2),false,consec);
			
			
			return null;
		}
		
		private void writeRest(LineWriter out, boolean hit, Map<String,boolean[]> consec) throws IOException {
			
			for (String pep : consec.keySet()) {
				if (consec.get(pep)[0]==hit) {
					out.writeLine(pep);
				}
			}
			out.close();
		}

		private static int[] aa = Alphabet.getProtein().createIndex();
		private static long carbamido = MassFactory.getInstance().getMass("C2H3NO");
		private static long water = MassFactory.getInstance().getMass("H2O");
		private static MassFactory mf = MassFactory.getInstance();
		public static boolean matchesMass(String pep, long[] masses,double ppm) {
			
			for (int i=0; i<pep.length(); i++)
				if (aa[pep.charAt(i)]==-1)
					return false;
			
			int carb = StringUtils.countChar(pep, 'C');
			long mass = mf.getPeptideMassLong(pep)+carb*carbamido+water;
			int index = Arrays.binarySearch(masses, mass);
			if (index>=0) return true;
			index = -index-1;
			if (index>0 && inPpm(masses[index-1],mass,ppm)) return true;
			return index<masses.length && inPpm(masses[index],mass,ppm);
		}

		private static boolean inPpm(long ref, long mass, double ppm) {
			long diff = Math.abs(ref-mass);
			return diff*1_000_000L<ref*ppm;
		}
	}


	public static class SplicedPeptidesParameterSet extends GediParameterSet {
		public GediParameter<String> f = new GediParameter<String>(this,"f", "Fasta file with protein sequences", true, new StringParameterType());
		public GediParameter<String> msms = new GediParameter<String>(this,"msms", "msmsScans.txt file from Maxquant for mass filtering", true, new StringParameterType());
		public GediParameter<Double> tol = new GediParameter<Double>(this,"tol", "Tolerance for mass filtering (in ppm)", true, new DoubleParameterType(),3.0);
		public GediParameter<String> o = new GediParameter<String>(this,"o", "Prefix for output files", false, new StringParameterType());
		public GediParameter<Integer> intervening = new GediParameter<Integer>(this,"b", "Maximal length of intervening sequence", false, new IntParameterType(),25);
		public GediParameter<Integer> min = new GediParameter<Integer>(this,"min", "Minimal peptide length", false, new IntParameterType(),9);
		public GediParameter<Integer> max = new GediParameter<Integer>(this,"max", "Maximal peptide length", false, new IntParameterType(),12);

		public GediParameter<File> splicedFile = new GediParameter<File>(this,"${o}.spliced.raw", "File containing all spliced peptide sequences", false, new FileParameterType());
		public GediParameter<File> bothFile = new GediParameter<File>(this,"${o}.both.raw", "File containing all peptide sequences that are either generated by splicing or occur in the proteome", false, new FileParameterType());
		public GediParameter<File> protFile = new GediParameter<File>(this,"${o}.proteome.raw", "File containing all proteomic peptide sequences", false, new FileParameterType());
		public GediParameter<File> pseudoProtFile = new GediParameter<File>(this,"${o}.pseudoproteins.fasta", "File containing concatenated peptides for database searches", false, new FileParameterType());

	}
	
	private static class StringSerializer implements BinarySerializer<String> {

		@Override
		public Class<String> getType() {
			return String.class;
		}

		@Override
		public void serialize(BinaryWriter out, String object) throws IOException {
			out.putString(object);
		}

		@Override
		public String deserialize(BinaryReader in) throws IOException {
			return in.getString();
		}

		@Override
		public void serializeConfig(BinaryWriter out) throws IOException {
		}

		@Override
		public void deserializeConfig(BinaryReader in) throws IOException {
		}
		
	}
}
