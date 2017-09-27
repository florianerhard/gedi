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

import gedi.util.FunctorUtils;
import gedi.util.StringUtils;
import gedi.util.algorithm.string.alignment.pairwise.Alignment;
import gedi.util.algorithm.string.alignment.pairwise.AlignmentMode;
import gedi.util.algorithm.string.alignment.pairwise.algorithm.LongAligner;
import gedi.util.algorithm.string.alignment.pairwise.formatter.SimpleAlignmentFormatter;
import gedi.util.algorithm.string.alignment.pairwise.gapCostFunctions.AffineGapCostFunction;
import gedi.util.algorithm.string.alignment.pairwise.gapCostFunctions.GapCostFunction;
import gedi.util.algorithm.string.alignment.pairwise.gapCostFunctions.InfiniteGapCostFunction;
import gedi.util.algorithm.string.alignment.pairwise.gapCostFunctions.LinearGapCostFunction;
import gedi.util.algorithm.string.alignment.pairwise.scoring.LongScoring;
import gedi.util.algorithm.string.alignment.pairwise.scoring.MatchMismatchScoring;
import gedi.util.algorithm.string.alignment.pairwise.scoring.QuasarMatrix;
import gedi.util.algorithm.string.alignment.pairwise.util.CheckScore;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.fasta.FastaEntry;
import gedi.util.io.text.fasta.FastaFile;
import gedi.util.mutable.MutablePair;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.Locale;
import java.util.function.Function;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;


public class Align {

	

	@SuppressWarnings("static-access")
	public static void main(String[] args) throws IOException {
		Options options = new Options()
		.addOption(
			OptionBuilder.withArgName("f1")
				.withDescription("Fasta file 1")
				.hasArg()
				.create("f1")
			).addOption(
			OptionBuilder.withArgName("f2")
				.withDescription("Fasta file 2")
				.hasArg()
				.create("f2")
			).addOption(
				OptionBuilder.withArgName("s1")
				.withDescription("sequence 1")
				.hasArg()
				.create("s1")
			).addOption(
			OptionBuilder.withArgName("s2")
				.withDescription("sequence 2")
				.hasArg()
				.create("s2")
			).addOption(
			OptionBuilder.withArgName("gap-linear")
				.withDescription("Linear gap cost")
				.hasArg()
				.create("gl")
			).addOption(
			OptionBuilder.withArgName("gap-open")
				.withDescription("Affine gap open cost")
				.hasArg()
				.create("go")
			).addOption(
			OptionBuilder.withArgName("gap-extend")
				.withDescription("Affine gap extend cost")
				.hasArg()
				.create("ge")
			).addOption(
			OptionBuilder.withArgName("gap-function")
				.withDescription("Gap function file")
				.hasArg()
				.create("gf")
			).addOption(
			OptionBuilder.withArgName("gapless")
				.withDescription("Gapless alignment")
				.create("gapless")
			).addOption(
			OptionBuilder.withArgName("mode")
				.withDescription("Alignment mode: global,local,freeshift (Default: freeshift)")
				.hasArg()
				.create('m')
			).addOption(
			OptionBuilder.withArgName("match")
				.withDescription("Match score")
				.hasArg()
				.create("ma")
			).addOption(
			OptionBuilder.withArgName("mismatch")
				.withDescription("Mismatch score")
				.hasArg()
				.create("mi")
			).addOption(
					OptionBuilder
					.withDescription("Do not append unaligned flanking sequences")
					.create("noflank")
			).addOption(
					OptionBuilder.withArgName("check")
					.withDescription("Calculate checkscore")
					.create('c')
			).addOption(
					OptionBuilder.withArgName("format")
					.withDescription("Output format, see String.format, parameters are: id1,id2,score,alignment (alignment only, if -f is specified); (default: '%s %s %.4f' w/o -f and '%s %s %.4f\n%s' w/ -f)")
					.hasArg()
					.create("format")
			).addOption(
					OptionBuilder.withArgName("matrix")
					.withDescription("Output dynamic programming matrix as well")
					.create("matrix")
			).addOption(OptionBuilder.withArgName("quasar-format")
					.withDescription("Scoring matrix in quasar format")
					.hasArg()
					.create('q')
			).addOption(
					OptionBuilder.withArgName("pairs")
					.withDescription("Pairs file")
					.hasArg()
					.create("pairs")
			).addOption(
					OptionBuilder.withArgName("output")
					.withDescription("Output")
					.hasArg()
					.create('o')
			).addOption(
					OptionBuilder.withArgName("seqlib")
					.withDescription("Seqlib file")
					.hasArg()
					.create("seqlib")
			).addOption(
				OptionBuilder.withArgName("full")
				.withDescription("Full output")
				.create('f')
			);


		CommandLineParser parser = new PosixParser();
		try {
			CommandLine cmd = parser.parse(options, args);
			
			LongScoring<CharSequence> scoring = createScoring(cmd);
			AlignmentMode mode = createMode(cmd);
			if (mode==null)
				throw new ParseException("Mode unknown: "+cmd.getOptionValue('m'));
			
			Iterator<MutablePair<String,String>> idIterator = createSequences(scoring,cmd);
			
			
			GapCostFunction gap = createGapFunction(cmd);
			String format = getFormat(cmd);
			
			LongAligner<CharSequence> aligner;
			if (gap instanceof AffineGapCostFunction)
				aligner = new LongAligner<CharSequence>(scoring, ((AffineGapCostFunction) gap).getGapOpen(), ((AffineGapCostFunction) gap).getGapExtend(), mode);
			else if (gap instanceof LinearGapCostFunction)
				aligner = new LongAligner<CharSequence>(scoring, ((LinearGapCostFunction) gap).getGap(), mode);
			else if (gap instanceof InfiniteGapCostFunction)
				aligner = new LongAligner<CharSequence>(scoring, mode);
			else
				throw new RuntimeException("Gap cost function "+gap.toString()+" currently not supported!");
			
			SimpleAlignmentFormatter formatter = cmd.hasOption('f')?new SimpleAlignmentFormatter().setAppendUnaligned(!cmd.hasOption("noflank")):null;
			
			CheckScore checkscore = cmd.hasOption('c')?new CheckScore():null;
			Alignment alignment = checkscore!=null || formatter!=null?new Alignment():null;
			
			
			float score;
			String ali;
			LineOrientedFile out = new LineOrientedFile(cmd.hasOption('o')?cmd.getOptionValue('o'):LineOrientedFile.STDOUT);
			Writer wr = out.startWriting();
			
			while (idIterator.hasNext()) {
				MutablePair<String,String> ids = idIterator.next();
				
				score = alignment==null?aligner.alignCache(ids.Item1, ids.Item2):aligner.alignCache(ids.Item1, ids.Item2, alignment);
				ali = formatter!=null?formatter.format(alignment, scoring, gap,mode,scoring.getCachedSubject(ids.Item1), scoring.getCachedSubject(ids.Item2)):"";
				out.writeLine(String.format(Locale.US,format,ids.Item1,ids.Item2,score,ali));
				
				if (cmd.hasOption("matrix")) {
					aligner.writeMatrix(wr,
							aligner.getScoring().getCachedSubject(ids.Item1).toString().toCharArray(),
							aligner.getScoring().getCachedSubject(ids.Item2).toString().toCharArray()
							);
				}
				
				if (checkscore!=null)
					checkscore.checkScore(aligner,scoring.getCachedSubject(ids.Item1).length(),scoring.getCachedSubject(ids.Item2).length(), alignment, score);
				
			}
			
			out.finishWriting();
			
		} catch (ParseException e) {
			e.printStackTrace();
			HelpFormatter f = new HelpFormatter();
			f.printHelp("Align", options);
		}
	}

	private static String getFormat(CommandLine cmd) {
		if (cmd.hasOption("format"))
			return StringUtils.unescape(cmd.getOptionValue("format"));
		else if (cmd.hasOption('f'))
			return "%s %s %.4f\n%s";
		else
			return "%s %s %.4f";
	}

	private static Iterator<MutablePair<String, String>> createSequences(LongScoring<CharSequence> scoring,
			CommandLine cmd) throws ParseException, IOException {
		
		if (cmd.hasOption("s1") && cmd.hasOption("s2")) {
			scoring.cacheSubject("s1",cmd.getOptionValue("s1"));
			scoring.cacheSubject("s2",cmd.getOptionValue("s2"));
			return FunctorUtils.singletonIterator(new MutablePair<String,String>("s1","s2"));
		}
		else if (cmd.hasOption("f1") && cmd.hasOption("f2")) {
			Iterator<FastaEntry> it = new FastaFile(cmd.getOptionValue("f1")).entryIterator(true);
			while (it.hasNext()) {
				FastaEntry e = it.next();
				scoring.cacheSubject(e.getHeader().substring(1), e.getSequence());
			}
			it = new FastaFile(cmd.getOptionValue("f2")).entryIterator(true);
			while (it.hasNext()) {
				FastaEntry e = it.next();
				scoring.cacheSubject(e.getHeader().substring(1), e.getSequence());
			}
			return new FastaFilesEntryIterator(new FastaFile(cmd.getOptionValue("f1")),new FastaFile(cmd.getOptionValue("f2")));
		}
		else if (cmd.hasOption("pairs") && cmd.hasOption("seqlib")) {
			LineOrientedFile seqlib = new LineOrientedFile(cmd.getOptionValue("seqlib"));
			Iterator<String> it = seqlib.lineIterator();
			while (it.hasNext()) {
				String n = it.next().trim();
				if (n.length()==0) continue;
				int ind = n.indexOf(':');
				if (ind==-1)
					throw new ParseException("Seqlib contains a line w/o colon:\n"+n);
				scoring.cacheSubject(n.substring(0,ind), n.substring(ind+1));
			}
			return FunctorUtils.mappedIterator(new LineOrientedFile(cmd.getOptionValue("pairs")).lineIterator(), new PairsTransformer()); 
		}
		else if (cmd.getArgs().length>=2) {
			String[] args = cmd.getArgs();
			
			scoring.cacheSubject("s1", args[0]);
			scoring.cacheSubject("s2", args[1]);
			return FunctorUtils.singletonIterator(new MutablePair<String,String>("s1","s2"));
		}
		else
			throw new ParseException("Either you specify f1 and f2 or you give two sequences directly!");
	}
	
	private static class PairsTransformer implements Function<String,MutablePair<String, String>> {
		private MutablePair<String,String> pair = new MutablePair<String, String>(null, null);
		@Override
		public MutablePair<String, String> apply(String line) {
			pair.Item1 = line.substring(0,line.indexOf(' '));
			int i = line.indexOf(' ',pair.Item1.length()+1);
			if (i==-1) i = line.length();
			pair.Item2 = line.substring(pair.Item1.length()+1,i);
			return pair;
		}

		
	}
	
	private static class FastaFilesEntryIterator implements Iterator<MutablePair<String, String>> {
		private FastaFile file2;
		
		private Iterator<FastaEntry> it1;
		private Iterator<FastaEntry> it2;
	
		private FastaEntry current1;
		private MutablePair<String, String> next;
		
		public FastaFilesEntryIterator(FastaFile file1, FastaFile file2) throws IOException {
			this.file2 = file2;
			it1 = file1.entryIterator(true);
			it2 = file2.entryIterator(true);
		}
		@Override
		public boolean hasNext() {
			lookAhead();
			return next!=null;
		}
		@Override
		public MutablePair<String, String> next() {
			lookAhead();
			MutablePair<String, String> re = next;
			next = null;
			return re;
		}
		private void lookAhead() {
			if (next==null && (it1.hasNext() || it2.hasNext())) {
				if(current1==null || !it2.hasNext()) {
					current1 =it1.next();
					try {
						it2 = file2.entryIterator(true);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
				next = new MutablePair<String,String>(current1.getHeader().substring(1),it2.next().getHeader().substring(1));
			}
		}
		@Override
		public void remove() {}
		
	}
	

	private static LongScoring<CharSequence> createScoring(CommandLine cmd) throws ParseException, IOException {
		if (cmd.hasOption('q')) {
			if (cmd.hasOption("ma") || cmd.hasOption("mi"))
				throw new ParseException("Specify either -q or -ma and -mi!");
		} else {
			if (!cmd.hasOption("ma") || !cmd.hasOption("mi"))
			throw new ParseException("Specify either -q or -ma and -mi!");
		}
			
		
		if (cmd.hasOption('q'))
			return new QuasarMatrix(new File(cmd.getOptionValue('q')));
		else
			return new MatchMismatchScoring(
					Float.parseFloat(cmd.getOptionValue("ma")),
					Float.parseFloat(cmd.getOptionValue("mi")));
	}

	private static AlignmentMode createMode(CommandLine cmd) {
		String name = cmd.hasOption('m') ? cmd.getOptionValue('m').toLowerCase() : "freeshift";
		return AlignmentMode.fromString(name);
	}

	private static GapCostFunction createGapFunction(CommandLine cmd) throws ParseException, IOException {
		if (cmd.hasOption("gapless")) {
			if (cmd.hasOption("gl") || cmd.hasOption("go") || cmd.hasOption("ge") || cmd.hasOption("gf"))
				throw new ParseException("Specify either -gapless or -gl or -go and -ge or -gf!");
			
			return new InfiniteGapCostFunction();
			
		} else if (cmd.hasOption("gl")) {
			if (cmd.hasOption("go") || cmd.hasOption("ge") || cmd.hasOption("gf"))
				throw new ParseException("Specify either -gapless or -gl or -go and -ge or -gf!");
			
			return new LinearGapCostFunction(Float.parseFloat(cmd.getOptionValue("gl")));
			
		} else if (cmd.hasOption("gf")) {
			if (cmd.hasOption("gf"))
				throw new ParseException("Specify either -gapless or -gl or -go and -ge or -gf!");
			
			throw new RuntimeException("Currently not available!");
			
		} else  {
			if (!cmd.hasOption("go") && !cmd.hasOption("ge"))
				throw new ParseException("Specify either -gapless or -gl or -go and -ge or -gf!");
			
			return new AffineGapCostFunction(Float.parseFloat(cmd.getOptionValue("go")),Float.parseFloat(cmd.getOptionValue("ge")));
		}
		
		
	}


}
