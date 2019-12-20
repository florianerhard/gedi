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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import gedi.app.extension.ExtensionContext;
import gedi.core.data.annotation.Transcript;
import gedi.core.data.reads.AlignedReadsDataFactory;
import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.genomic.Genomic;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strandness;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.GenomicRegionStorageCapabilities;
import gedi.core.region.GenomicRegionStorageExtensionPoint;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.ArrayUtils;
import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.math.stat.RandomNumbers;
import gedi.util.math.stat.inference.EquivalenceClassEffectiveLengths;
import gedi.util.mutable.MutableMonad;
import gedi.util.mutable.MutablePair;
import gedi.util.program.CommandLineHandler;
import gedi.util.program.GediParameter;
import gedi.util.program.GediParameterSet;
import gedi.util.program.GediProgram;
import gedi.util.program.GediProgramContext;
import gedi.util.program.parametertypes.EnumParameterType;
import gedi.util.program.parametertypes.FileParameterType;
import gedi.util.program.parametertypes.GenomicParameterType;
import gedi.util.program.parametertypes.IntParameterType;
import gedi.util.program.parametertypes.LongParameterType;
import gedi.util.program.parametertypes.StringParameterType;

public class ReadSim {

	
	


	public static void main(String[] args) throws IOException {
		
		ReadSimParameterSet params = new ReadSimParameterSet();
		GediProgram pipeline = GediProgram.create("ReadSim",
				new ReadSimProgram(params)
				);
		GediProgram.run(pipeline, null, new CommandLineHandler("ReadSim","ReadSim simulates reads from a very simple model.",args));

	}
	
	
	

	public static class ReadSimProgram extends GediProgram {

		public ReadSimProgram(ReadSimParameterSet params) {
			addInput(params.genomic);
			addInput(params.fragMean);
			addInput(params.fragSd);
			addInput(params.c);
			addInput(params.n);
			addInput(params.rep);
			addInput(params.seed);
			addInput(params.nthreads);
			addInput(params.strandness);
			addInput(params.rlen);
			addInput(params.prefix);
			
			addOutput(params.citOut);
			addOutput(params.countOut);
			addOutput(params.eqOut);
		}

		@Override
		public String execute(GediProgramContext context) throws Exception {
			Genomic g = getParameter(0);
			int mean = getIntParameter(1);
			int sd = getIntParameter(2);
			String cf = getParameter(3);
			int n = getIntParameter(4);
			int rep = getIntParameter(5);
			long seed = getLongParameter(6);
			int nthreads = getIntParameter(7);
			Strandness strand = getParameter(8);
			int rlen = getIntParameter(9);
			String prefix = getParameter(10);
			
			g.getTranscriptMapping();
			
			context.getLog().info("Reading concentrations and preparing transcripts...");
			MutableMonad<String[]> conds = new MutableMonad<>();
			TranscriptConfig[] trans = EI.lines(cf).split('\t')
					.skip(1, a->conds.Item=ArrayUtils.slice(a, 1))
					.map(a->{
						ReferenceGenomicRegion<Transcript> rgr = g.getTranscriptMapping().apply(a[0]);
						if (rgr==null) {
							context.getLog().warning("Transcript "+a[0]+" unknown. Skipping...");
							return null;
						}
						double[] conc = new double[conds.Item.length];
						for (int i=0; i<conc.length; i++)
							conc[i] = Double.parseDouble(a[i+1]);
						return new TranscriptConfig(rgr,g.getSequence(rgr).toString(),conc);
					}).removeNulls().toArray(TranscriptConfig.class);
			
			EquivalenceClassEffectiveLengths<String> efflenComp = new EquivalenceClassEffectiveLengths<String>(EI.wrap(trans).map(c->new ImmutableReferenceGenomicRegion<String>(Chromosome.obtain(c.trans.getData().getTranscriptId()), c.trans.getRegion(), c.trans.getData().getTranscriptId())),EquivalenceClassEffectiveLengths.preprocessEff(mean, sd));
			EI.wrap(trans).forEachRemaining(c->c.efflen=efflenComp.getEffectiveLength(new String[] {c.trans.getData().getTranscriptId()}));

			// probability model for drawing reads from transcripts
			double[][] p = new double[conds.Item.length*rep][];
			for (int c=0; c<p.length; c++) {
				int uc = c;
				p[c] = EI.wrap(trans).mapToDouble(tc->tc.efflen*tc.conc[uc%conds.Item.length]).toDoubleArray();
				ArrayUtils.normalize(p[c]);
				ArrayUtils.cumSumInPlace(p[c], +1);
			}

			EI.wrap(trans).forEachRemaining(tc->tc.reads = new int[p.length]);
			
			// prepare fastqs
			LineWriter[] fastq_1 = new LineWriter[p.length];
			LineWriter[] fastq_2 = new LineWriter[p.length];
			String[] condNames = new String[p.length];
			for (int i=0; i<fastq_1.length; i++) {
				condNames[i] = conds.Item[i%conds.Item.length]+(rep>1?"."+String.valueOf((char)('A'+i/conds.Item.length)):"");
				fastq_1[i] = new LineOrientedFile(prefix+"."+condNames[i]+"_R1.fq.gz").write();
				fastq_2[i] = new LineOrientedFile(prefix+"."+condNames[i]+"_R2.fq.gz").write();
			}
			
			GenomicRegionStorage<DefaultAlignedReadsData> out = GenomicRegionStorageExtensionPoint.getInstance().get(new ExtensionContext().add(String.class, getOutputFile(0).getPath()).add(Class.class, DefaultAlignedReadsData.class), GenomicRegionStorageCapabilities.Disk, GenomicRegionStorageCapabilities.Fill);
			
			HashMap<String,int[]> eqToCounts = new HashMap<>();
			
			out.fill(
				EI.seq(0,n).progress(context.getProgress(), n, iii->"Simulating reads!")
					.parallelized(nthreads, 500, (i)->new MutablePair<StringBuilder[],RandomNumbers>(createSbs(p.length*2),new RandomNumbers(seed+i*13)), (b,pair)->{pair.Item2=new RandomNumbers(seed+b*13);}, (ei,pair)->ei.map(i->{
						ArrayList<ImmutableReferenceGenomicRegion<DefaultAlignedReadsData>> re = new ArrayList<>();
						
						StringBuilder[] fq = pair.Item1;
						RandomNumbers rnd = pair.Item2;
						for (int c=0; c<p.length; c++) {
							// select transcript
							TranscriptConfig tc = trans[rnd.getCategorial(p[c])];
							int tlen = tc.trans.getRegion().getTotalLength();
							// select fragment
							int fstart = 0,flen = tlen+1;
							while (fstart+flen>tlen || flen<18) {
								flen = (int) Math.round(rnd.getNormal(mean, sd));
								if (flen>0)
									fstart = rnd.getUnif(0, tlen-flen);
							}
							
							ArrayGenomicRegion frag = new ArrayGenomicRegion(fstart,fstart+flen);
							int overlap = Math.max(0, Math.min(flen,2*rlen-flen));
							
							String seq = SequenceUtils.extractSequence(frag, tc.seq);
							
							String s1 = seq.substring(0,Math.min(flen, rlen));
							String s2 = SequenceUtils.getDnaReverseComplement(seq.substring(seq.length()-Math.min(flen, rlen)));
							
							ImmutableReferenceGenomicRegion<Void> gfrag = new ImmutableReferenceGenomicRegion<>(tc.trans.getReference(), tc.trans.map(frag));
							String eq=g.getTranscripts().ei(gfrag).filter(tr->tr.getRegion().containsUnspliced(gfrag.getRegion())).map(tr->tr.getData().getTranscriptId()).sort().concat(",");
							
							ImmutableReferenceGenomicRegion<Void> g1 = new ImmutableReferenceGenomicRegion<>(gfrag.getReference(), gfrag.map(new ArrayGenomicRegion(0,Math.min(flen, rlen))));
							ImmutableReferenceGenomicRegion<Void> g2 = new ImmutableReferenceGenomicRegion<>(gfrag.getReference().toOppositeStrand(), gfrag.map(new ArrayGenomicRegion(seq.length()-Math.min(flen, rlen),seq.length())));
														
							if (strand.equals(Strandness.Antisense) || (strand.equals(Strandness.Unspecific) && rnd.getBool())) {
								String tmp = s1;
								s1 = s2;
								s2 = tmp;
								ImmutableReferenceGenomicRegion<Void> tmp2 = g1;
								g1 = g2;
								g2 = tmp2;
							}
							
							fq[c].append("@"+i).append("\n");//+"#"+g1.toLocationString()+"#"+tc.trans.getData().getTranscriptId());
							fq[c].append(s1).append("\n");
							fq[c].append("+").append("\n");
							fq[c].append(StringUtils.repeat("J", s1.length())).append("\n");
								
							fq[c+p.length].append("@"+i).append("\n");//+"#"+g2.toLocationString()+"#"+tc.trans.getData().getTranscriptId());
							fq[c+p.length].append(s2).append("\n");
							fq[c+p.length].append("+").append("\n");
							fq[c+p.length].append(StringUtils.repeat("J", s2.length())).append("\n");
									
							AlignedReadsDataFactory fac = new AlignedReadsDataFactory(p.length);
							fac.start();
							fac.newDistinctSequence();
							fac.setCount(c, 1);
							fac.setGeometry(g1.getRegion().getTotalLength()-overlap, overlap, g2.getRegion().getTotalLength()-overlap);
							fac.setMultiplicity(1);

							eqToCounts.computeIfAbsent(eq, x->new int[p.length])[c]++;
							tc.reads[c]++;
							
							re.add(new ImmutableReferenceGenomicRegion<>(g1.getReference(), g1.getRegion().union(g2.getRegion()), fac.create()));
						}
						
						return new MutablePair<>(makeStrings(fq),re);
					})).unfold(pair->{
						try {
							for (int i=0; i<fastq_1.length; i++) {
								fastq_1[i].write(pair.Item1[i]);
								fastq_2[i].write(pair.Item1[i+fastq_1.length]);
							}
						} catch (IOException e) {
							throw new RuntimeException("Could not write fastq!",e);
						}
						return EI.wrap(pair.Item2);
					}),
					context.getProgress());
			
			StringBuilder json = new StringBuilder().append("{\"conditions\" : [\n");
			for (int i=0; i<condNames.length; i++) 
				json.append("\t{ \"name\" : \""+condNames[i]+"\"}").append(i+1<condNames.length?",\n":"\n");
			out.setMetaData(DynamicObject.parseJson(json.append("]}").toString()));
			
			
			for (int i=0; i<fastq_1.length; i++) {
				fastq_1[i].close();
				fastq_2[i].close();
			}
			
			LineWriter tab = getOutputWriter(1);
			tab.write("Transcript");
			for (int i=0; i<condNames.length; i++)
				tab.writef("\t%s",condNames[i]);
			tab.writeLine();
			
			for (TranscriptConfig tc : trans) {
				tab.writef("%s\t",tc.trans.getData().getTranscriptId());
				tab.writeLine(StringUtils.concat("\t", tc.reads));
			}
			
			tab.close();
			
			
			EquivalenceClassEffectiveLengths<String> el = new EquivalenceClassEffectiveLengths<>(
					g.getTranscripts().ei().map(t->new ImmutableReferenceGenomicRegion<>(t.getReference(), t.getRegion(), t.getData().getTranscriptId())), 
					EquivalenceClassEffectiveLengths.preprocessEff(mean, sd));
			
			tab = getOutputWriter(2);
			tab.write("Equivalence Class\tEffective Length");
			for (int i=0; i<condNames.length; i++)
				tab.writef("\t%s",condNames[i]);
			tab.writeLine();
			
			for (String eq : eqToCounts.keySet()) {
				tab.writef("%s\t%.2f\t",eq, el.getEffectiveLength(StringUtils.split(eq, ',')));
				tab.writeLine(StringUtils.concat("\t", eqToCounts.get(eq)));
			}
			
			tab.close();
			
			return null;
		}

		
	}
	private static StringBuilder[] createSbs(int length) {
		StringBuilder[] fq = new StringBuilder[length];
		for (int i=0; i<fq.length; i++)
			fq[i] = new StringBuilder();
		return fq;
	}
	
	private static String[] makeStrings(StringBuilder[] sb) {
		String[] re = new String[sb.length];
		for (int i=0; i<re.length; i++) {
			re[i] = sb[i].toString();
			sb[i].delete(0, sb[i].length());
		}
		return re;
	}

	private static class TranscriptConfig {
		ReferenceGenomicRegion<Transcript> trans;
		String seq;
		double[] conc;
		double efflen;
		int[] reads;
		
		public TranscriptConfig(ReferenceGenomicRegion<Transcript> rgr, String seq, double[] conc) {
			this.trans = rgr;
			this.seq = seq;
			this.conc = conc;
		}
		
	}

	
	public static class ReadSimParameterSet extends GediParameterSet {

		public GediParameter<String> c = new GediParameter<String>(this,"c", "Concentration file (First col: transcript ids, then concentration per condition)", true, new StringParameterType());
		public GediParameter<String> prefix = new GediParameter<String>(this,"prefix", "Prefix for output files", true, new StringParameterType());
		public GediParameter<Genomic> genomic = new GediParameter<Genomic>(this,"g", "Genomic name", true, new GenomicParameterType());

		public GediParameter<Integer> fragMean = new GediParameter<Integer>(this,"mean", "Mean of fragment length distribution", false, new IntParameterType(), 180);
		public GediParameter<Integer> fragSd = new GediParameter<Integer>(this,"sd", "Standard deviation of fragment length distribution", false, new IntParameterType(), 40);
		public GediParameter<Integer> n = new GediParameter<Integer>(this,"n", "Number of reads to generate", false, new IntParameterType(), 1_000_000);
		public GediParameter<Integer> rlen = new GediParameter<Integer>(this,"len", "Read length", false, new IntParameterType(), 101);

		public GediParameter<Strandness> strandness = new GediParameter<Strandness>(this,"strandness", "The strandness", false, new EnumParameterType<>(Strandness.class), Strandness.Antisense);

		public GediParameter<Integer> rep = new GediParameter<Integer>(this,"rep", "Number of replicates", false, new IntParameterType(), 1);
		
		public GediParameter<Long> seed = new GediParameter<Long>(this,"seed", "Seed for random number generator", false, new LongParameterType(), 42L);
		
		public GediParameter<File> citOut = new GediParameter<File>(this,"${prefix}.cit", "Mapped reads", false, new FileParameterType());
		public GediParameter<File> countOut = new GediParameter<File>(this,"${prefix}.count.tsv", "True count table", false, new FileParameterType());
		public GediParameter<File> eqOut = new GediParameter<File>(this,"${prefix}.eq.tsv", "True count table per equivalence class", false, new FileParameterType());
		
		public GediParameter<Integer> nthreads = new GediParameter<Integer>(this,"nthreads", "The number of threads to use for computations", false, new IntParameterType(), Runtime.getRuntime().availableProcessors());
				
	}

	
}
