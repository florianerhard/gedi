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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import org.jdom.JDOMException;

import fern.network.AmountManager;
import fern.network.ArrayKineticConstantPropensityCalculator;
import fern.network.Network;
import fern.network.PropensityCalculator;
import fern.network.fernml.FernMLNetwork;
import fern.simulation.Simulator;
import fern.simulation.Simulator.FireType;
import fern.simulation.algorithm.GillespieEnhanced;
import fern.simulation.observer.InstantOutputObserver;
import fern.simulation.observer.IntervalObserver;
import fern.simulation.observer.Observer;
import gedi.util.ReflectionUtils;
import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.dataframe.DataFrame;
import gedi.util.functions.EI;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.io.text.tsv.formats.Csv;
import gedi.util.io.text.tsv.formats.CsvReaderFactory;

public class Tasep {

	public static void main(String[] args) throws IOException, JDOMException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		
		String prot = "ATGTCCACCAGGTCCGTGTCCTCGTCCTCCTACCGCAGGATGTTCGGCGGCCCGGGCACC"+
"GCGAGCCGGCCGAGCTCCAGCCGGAGCTACGTGACTACGTCCACCCGCACCTACAGCCTG"+
"GGCAGCGCGCTGCGCCCCAGCACCAGCCGCAGCCTCTACGCCTCGTCCCCGGGCGGCGTG"+
"TATGCCACGCGCTCCTCTGCCGTGCGCCTGCGGAGCAGCGTGCCCGGGGTGCGGCTCCTG"+
"CAGGACTCGGTGGACTTCTCGCTGGCCGACGCCATCAACACCGAGTTCAAGAACACCCGC"+
"ACCAACGAGAAGGTGGAGCTGCAGGAGCTGAATGACCGCTTCGCCAACTACATCGACAAG"+
"GTGCGCTTCCTGGAGCAGCAGAATAAGATCCTGCTGGCCGAGCTCGAGCAGCTCAAGGGC"+
"CAAGGCAAGTCGCGCCTGGGGGACCTCTACGAGGAGGAGATGCGGGAGCTGCGCCGGCAG"+
"GTGGACCAGCTAACCAACGACAAAGCCCGCGTCGAGGTGGAGCGCGACAACCTGGCCGAG"+
"GACATCATGCGCCTCCGGGAGAAATTGCAGGAGGAGATGCTTCAGAGAGAGGAAGCCGAA"+
"AACACCCTGCAATCTTTCAGACAGGATGTTGACAATGCGTCTCTGGCACGTCTTGACCTT"+
"GAACGCAAAGTGGAATCTTTGCAAGAAGAGATTGCCTTTTTGAAGAAACTCCACGAAGAG"+
"GAAATCCAGGAGCTGCAGGCTCAGATTCAGGAACAGCATGTCCAAATCGATGTGGATGTT"+
"TCCAAGCCTGACCTCACGGCTGCCCTGCGTGACGTACGTCAGCAATATGAAAGTGTGGCT"+
"GCCAAGAACCTGCAGGAGGCAGAAGAATGGTACAAATCCAAGTTTGCTGACCTCTCTGAG"+
"GCTGCCAACCGGAACAATGACGCCCTGCGCCAGGCAAAGCAGGAGTCCACTGAGTACCGG"+
"AGACAGGTGCAGTCCCTCACCTGTGAAGTGGATGCCCTTAAAGGAACCAATGAGTCCCTG"+
"GAACGCCAGATGCGTGAAATGGAAGAGAACTTTGCCGTTGAAGCTGCTAACTACCAAGAC"+
"ACTATTGGCCGCCTGCAGGATGAGATTCAGAATATGAAGGAGGAAATGGCTCGTCACCTT"+
"CGTGAATACCAAGACCTGCTCAATGTTAAGATGGCCCTTGACATTGAGATTGCCACCTAC"+
"AGGAAGCTGCTGGAAGGCGAGGAGAGCAGGATTTCTCTGCCTCTTCCAAACTTTTCCTCC"+
"CTGAACCTGAGGGAAACTAATCTGGATTCACTCCCTCTGGTTGATACCCACTCAAAAAGG"+
"ACACTTCTGATTAAGACGGTTGAAACTAGAGATGGACAGGTTATCAACGAAACTTCTCAG"+
"CATCACGATGACCTTGAATAA";
		double init = 0.01;
		
		writeFernml(prot, init,"vim.fernml");
		


		Network net = new FernMLNetwork(new File("vim.fernml"));
		double[] constants = ReflectionUtils.get(net.getPropensityCalculator(), "constants");
		ReflectionUtils.set(net, "propensitiyCalculator", new ArrayKineticConstantPropensityCalculator(new int[0][],constants) {
			@Override
			public double calculatePropensity(int reaction, AmountManager amount, Simulator sim) {
				for (int rea : sim.getNet().getReactants(reaction))
					if (amount.getAmount(rea)==0) return 0;
//				System.out.println("Enabled: "+sim.getNet().getReactionName(reaction)+" "+constants[reaction]);
				return constants[reaction];
			}
		});
		
		
		double[] totalTime = new double[prot.length()/3]; 
		double[] occTime = new double[prot.length()/3]; 
		
		int[] np = {0};
		
		Simulator sim = new GillespieEnhanced(net);
		sim.addObserver(new Observer(sim) {
			
			@Override
			public void theta(double theta) {
			}
			
			@Override
			public void step() {
			}
			
			@Override
			public void started() {
			}
			
			@Override
			public void finished() {
			}
			
			@Override
			public void activateReaction(int mu, double tau, FireType fireType, int times) {
				int p = mu-1;
				if (p>=0) occTime[p] = tau;
				if (p>0) totalTime[p-1]+=tau-occTime[p-1];
				if (mu==getSimulator().getNet().getNumReactions()-1)
					np[0]++;
//				System.out.println(p+" "+getSimulator().getNet().getReactionName(mu)+" "+tau);
//				System.out.println();
			}
		});

		sim.start(100000);

		
		HashMap<String,Double> rates = new HashMap<>();
		new LineIterator(Tasep.class.getResource("/resources/tasep/times.tsv").openStream()).skip(1).map(s->StringUtils.split(s, '\t')).forEachRemaining(a->rates.put(a[0], Double.parseDouble(a[1])));
		
		LineWriter out = new LineOrientedFile("vim.tsv").write();
		out.writeLine("Position\tTime\tRate");
		for (int i=0; i<totalTime.length-2; i++)
			out.writef("%d\t%.2f\t%.3f\n",i,totalTime[i],rates.get(prot.substring(i*3, i*3+3)));
		out.close();
		
		System.out.println(np[0]);
	}
	
	private static void writeFernml(String prot, double init, String path) throws NumberFormatException, IOException {
		HashMap<String,Double> rates = new HashMap<>();
		new LineIterator(Tasep.class.getResource("/resources/tasep/times.tsv").openStream()).skip(1).map(s->StringUtils.split(s, '\t')).forEachRemaining(a->rates.put(a[0], Double.parseDouble(a[1])));
		
		Species freeRibo = new Species("free",1);
		Species scanning = new Species("UTR",0);
		Species[] occ  = new Species[prot.length()/3-1];
		Species[] a  = new Species[prot.length()/3-1];
		Species[] free = new Species[prot.length()/3-1];
		
		for (int i=0; i<prot.length()-3; i+=3) {
			occ[i/3] = new Species(SequenceUtils.translate(prot.substring(i,i+3))+(i/3)+"_o",0);
			a[i/3] = new Species(SequenceUtils.translate(prot.substring(i,i+3))+(i/3)+"_A",0);
			free[i/3] = new Species(SequenceUtils.translate(prot.substring(i,i+3))+(i/3)+"_f",1);
		}
		a[0] = scanning;
		
		Reaction[] rxn = new Reaction[prot.length()/3+1];
		// arrive
		rxn[0] = new Reaction(init).out(scanning).in(freeRibo);
		// init
		rxn[1] = new Reaction(0.2).in(scanning).in(free, 0, 6).out(occ, 0, 6).out(a[1]).out(freeRibo);
		for (int i=3; i<prot.length()-3; i+=3) 
			rxn[i/3+1] = new Reaction(rates.get(prot.substring(i,i+3))).in(free,i/3+5).in(occ,i/3-5).out(occ,i/3+5).out(free,i/3-5).in(a,i/3).out(a,i/3+1);
		rxn[rxn.length-1] = new Reaction(rates.get(prot.substring(prot.length()-6,prot.length()-3))).in(occ,occ.length-5,occ.length).out(free,free.length-5,free.length).in(a,prot.length()/3-1);
		
		rxn[200].rate/=10;
		
		LineWriter out = new LineOrientedFile(path).write();
		out.writeLine("<fernml version=\"1.0\"><listOfSpecies>");
		out.writeLine(freeRibo.toString());
		for (Species s : a)
			out.writeLine(s.toString());
		for (Species s : occ)
			out.writeLine(s.toString());
		for (Species s : free)
			out.writeLine(s.toString());
		out.writeLine("</listOfSpecies><listOfReactions>");
		for (Reaction r : rxn)
			out.writeLine(r.toString());
		out.writeLine("</listOfReactions></fernml>");
		out.close();
		
	}
	
	private static class Species {
		String id;
		int init;
		public Species(String id, int init) {
			this.id = id;
			this.init = init;
		}
		
		@Override
		public String toString() {
			return "<species name=\""+id+"\" initialAmount=\""+init+"\" />";
		}
		
	}
	
	private static class Reaction {
		double rate;
		ArrayList<Species> in = new ArrayList<>();
		ArrayList<Species> out = new ArrayList<>();
		
		public Reaction(double rate) {
			this.rate = rate;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("<reaction kineticConstant=\""+rate+"\">\n"
					+ "	<listOfReactants>\n");
			for (Species i : in)
					sb.append("		<speciesReference name=\""+i.id+"\"/>\n");
			sb.append("	</listOfReactants>\n	<listOfProducts>\n");
			for (Species i : out)
				sb.append("		<speciesReference name=\""+i.id+"\"/>\n");
			sb.append("	</listOfProducts>\n</reaction>");
			return sb.toString();
		}
		
		public Reaction out(Species s) {
			this.out.add(s);
			return this;
		}
		
		public Reaction in(Species s) {
			this.in.add(s);
			return this;
		}
		
		public Reaction out(Species[] s, int start, int end) {
			for (int i=start; i<end; i++)
				this.out.add(s[i]);
			return this;
		}
		
		public Reaction in(Species[] s, int start, int end) {
			for (int i=start; i<end; i++)
				this.in.add(s[i]);
			return this;
		}
		
		public Reaction out(Species[] s, int i) {
			if (i>=0 && i<s.length)
				this.out.add(s[i]);
			return this;
		}
		
		public Reaction in(Species[] s, int i) {
			if (i>=0 && i<s.length)
				this.in.add(s[i]);
			return this;
		}
		
	}
	
	
}
