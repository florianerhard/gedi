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

package gedi.virology.hsv1annotation;

import java.io.IOException;

import javax.script.ScriptException;

import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.commandline.GediCommandline;
import gedi.core.data.annotation.Transcript;
import gedi.core.genomic.Genomic;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.riboseq.inference.orf.Orf;
import gedi.util.FileUtils;
import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.math.stat.testing.DirichletLikelihoodRatioTest;

public class NterminalExtensionAnalysis {

	
	private MemoryIntervalTreeStorage<Orf> orfs1;
	private MemoryIntervalTreeStorage<Orf> orfs2;
	
	private Genomic hsv;
	
	private LineOrientedFile out = new LineOrientedFile();
	private double threshold = 0.9;
	private NumericArray sf1;
	private NumericArray sf2;
	
	
	public NterminalExtensionAnalysis(String orfs1, String orfs2, String sf1, String sf2) throws IOException {
		this.orfs1 = new CenteredDiskIntervalTreeStorage(orfs1).toMemory();
		this.orfs2 = new CenteredDiskIntervalTreeStorage(orfs2).toMemory();
		
		this.sf1 = FileUtils.deserialize(sf1);
		this.sf2 = FileUtils.deserialize(sf2);
		
		hsv = Genomic.get("JN555585");
		out.startWriting();
	}
	
	public void writeHeader() throws IOException {
		out.writeLine("Gene\tLocation\tExtension ID\tSequence\tPeptide\tA 2hpi\tA 4hpi\tA 6hpi\tA 8hpi\tB 2hpi\tB 4hpi\tB 6hpi\tB 8hpi\tQuant p val A\tQuant p val B\tStart score A\tStart score B\tStart activity CDS A\tStart activity CDS B\tStart activity A\tStart activity B\tDownstream activity CDS A\tDownstream activity CDS B\tDownstream activity A\tDownstream activity B\tStart log fc A\tStart log fc B\tDownstream log fc A\tDownstream log fc B\tWorst Start log fc\tWorst Downstream log fc");
	}
	
	public void processAll() throws IOException {
		writeHeader();
		for (ReferenceGenomicRegion<Transcript> r : hsv.getTranscripts().ei().loop())
			process(r);
	}
	
	public void process(String gene) throws IOException {
		process(hsv.getTranscriptMapping().apply(gene));
	}

	public void process(ReferenceGenomicRegion<Transcript> reg) throws IOException {
		ReferenceGenomicRegion<Transcript> cds = Transcript.getCds(reg);
		
		ImmutableReferenceGenomicRegion<Orf> o1 = orfs1.ei(cds).filter(o->o.contains(cds)).sort((a,b)->Double.compare(b.getData().getActivityFraction(), a.getData().getActivityFraction())).first();
		ImmutableReferenceGenomicRegion<Orf> o2 = orfs2.ei(cds).filter(o->o.contains(cds)).sort((a,b)->Double.compare(b.getData().getActivityFraction(), a.getData().getActivityFraction())).first();
		
		assert o1.map(o1.getRegion().getTotalLength()-1)==cds.map(cds.getRegion().getTotalLength()-1);
		assert o2.map(o2.getRegion().getTotalLength()-1)==cds.map(cds.getRegion().getTotalLength()-1);
		
		int l1 = (o1.getRegion().getTotalLength()-cds.getRegion().getTotalLength())/3; 
		int l2 = (o2.getRegion().getTotalLength()-cds.getRegion().getTotalLength())/3; 
		double[] codons1 = o1.getData().getEstimatedTotalCodons();
		double[] codons2 = o2.getData().getEstimatedTotalCodons();
		double[][] acodons1 = o1.getData().getEstimatedCodons();
		double[][] acodons2 = o2.getData().getEstimatedCodons();
		
		NtermInfo info1 = getInfo(o1, codons1, acodons1,sf1,l1, o1.getRegion().getTotalLength()/3-1);
		NtermInfo info2 = getInfo(o2, codons2, acodons2,sf2,l2, o2.getRegion().getTotalLength()/3-1);
		
		int id = 0;
		int last = 0;
		for (int i=0; i<Math.max(l1, l2); i++) {
			String startCodon;
			if (i<l1)
				startCodon = o1.getData().getSequence().substring((l1-1-i)*3, (l1-1-i)*3+3);
			else
				startCodon = o2.getData().getSequence().substring((l2-1-i)*3, (l2-1-i)*3+3);
			if (StringUtils.hamming("ATG",startCodon)<=1) {
				NtermInfo s1 = getInfo(o1,codons1,acodons1,sf1,l1-1-i,l1-1-last);
				NtermInfo s2 = getInfo(o2,codons2,acodons2,sf2,l2-1-i,l2-1-last);
				
				if (s1.start>threshold && s2.start>threshold) {
					String seq = i<l1?
						o1.getData().getSequence().substring((l1-1-i)*3, (l1-1-last)*3)
					:
						o2.getData().getSequence().substring((l2-1-i)*3, (l2-1-last)*3);
						
					out.writef("%s\t%s\t%d\t%s\t%s\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.3g\t%.3g\t%.2f\t%.2f\t%.0f\t%.0f\t%.0f\t%.0f\t%.0f\t%.0f\t%.0f\t%.0f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\n", 
							cds.getData().getTranscriptId(),cds.toLocationString(),id,seq,SequenceUtils.translate(seq),
							lfc(info1.nquant[0],s1.nquant[0],1),lfc(info1.nquant[1],s1.nquant[1],1),lfc(info1.nquant[2],s1.nquant[2],1),lfc(info1.nquant[3],s1.nquant[3],1),
							lfc(info2.nquant[0],s2.nquant[0],1),lfc(info2.nquant[1],s2.nquant[1],1),lfc(info2.nquant[2],s2.nquant[2],1),lfc(info2.nquant[3],s2.nquant[3],1),
							DirichletLikelihoodRatioTest.testMultinomials(info1.quant,s1.quant),
							DirichletLikelihoodRatioTest.testMultinomials(info2.quant,s2.quant),
							s1.start,s2.start,
							info1.startQuant,info2.startQuant,s1.startQuant,s2.startQuant,
							info1.downQuant,info2.downQuant,s1.downQuant,s2.downQuant,
							lfc(s1.startQuant,info1.startQuant),lfc(s2.startQuant,info2.startQuant),
							lfc(s1.downQuant,info1.downQuant),lfc(s2.downQuant,info2.downQuant),
							absmax(lfc(s1.startQuant,info1.startQuant),lfc(s2.startQuant,info2.startQuant)),
							absmax(lfc(s1.downQuant,info1.downQuant),lfc(s2.downQuant,info2.downQuant))
							);
					
					last = i;
					id++;
				}
					
			}
			
		}
		
	}

	private Object absmax(double a, double b) {
		return Math.abs(a)>Math.abs(b)?a:b;
	}

	private double lfc(double a, double b) {
		return Math.log(a/b)/Math.log(2);
	}
	
	private double lfc(double a, double b, double p) {
		return Math.log((a+p)/(b+p))/Math.log(2);
	}


	private NtermInfo getInfo(ImmutableReferenceGenomicRegion<Orf> o, double[] codons, double[][] acodons, NumericArray sf, int p, int last)  {
		if (p<0){
			return new NtermInfo(0, 0, 0, 0,new double[]{0,0,0,0},new double[]{0,0,0,0});
		}
//		double medSf = sf!=null?NumericArrayFunction.Median.applyAsDouble(sf):Double.NaN;
		
		double start = o.getData().getStartScores()[p];
		double startQuant = codons[p];
		int l = last-p;
		double[] quant = new double[4];
		double[] nquant = new double[4];
		double downQuant = 0;
		for (int i=p+1; i<last; i++) {
			downQuant+=codons[i];
			for (int c=2; c<6; c++)
				quant[c-2]+=acodons[c][i];
		}
		downQuant = l==0?Double.NaN:downQuant/(l-1);
		for (int c=2; c<6; c++) {
			nquant[c-2]=l==0?Double.NaN:quant[c-2]/(l-1);// not necessary, fold change to cds protein! /sf.getDouble(c)*medSf;
		}
		
		return new NtermInfo(start, startQuant, l, downQuant,quant,nquant);
	}
	
	private static class NtermInfo {
		double start;
		double startQuant;
		int l;
		double downQuant;
		double[] quant;
		double[] nquant;
		
		public NtermInfo(double start, double startQuant, int l, double downQuant, double[] quant, double[] nquant) {
			this.start = start;
			this.startQuant = startQuant;
			this.l = l;
			this.downQuant = downQuant;
			this.quant = quant;
			this.nquant = nquant;
		}
		
		
	}
	
}
