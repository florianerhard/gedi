package gedi.lfc;

import gedi.core.data.annotation.Transcript;
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericProvider;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ContrastMapping;
import gedi.core.reference.ReferenceSequenceConversion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.util.ArrayUtils;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.io.text.LineOrientedFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.math3.distribution.BetaDistribution;

@Deprecated
public class LfcComputer {


	public void compute(LineOrientedFile out, GenomicRegionStorage<AlignedReadsData> reads, ReferenceSequenceConversion readConversion, GenomicRegionStorage<Transcript> transcripts, ContrastMapping contrast, Downsampling downsampling, Set<String> restrictToGenes) {

		if (contrast.getNumMergedConditions()!=2) 
			throw new RuntimeException("Must be binary contrast!");
		
		// mapping to genes
		HashMap<String,MutableReferenceGenomicRegion<String>> genesToRegon = new HashMap<String, MutableReferenceGenomicRegion<String>>();
		transcripts.iterateReferenceGenomicRegions().forEachRemaining(rgr->{
			if (restrictToGenes!=null && !restrictToGenes.contains(rgr.getData().getGeneId()))
				return;
			
			MutableReferenceGenomicRegion<String> r = genesToRegon.get(rgr.getData().getGeneId());
			
			if (r==null) genesToRegon.put(rgr.getData().getGeneId(), 
					new MutableReferenceGenomicRegion<String>()
					.setReference(rgr.getReference())
					.setRegion(rgr.getRegion())
					.setData(rgr.getData().getGeneId()));
			else {
				if (!r.getReference().equals(rgr.getReference()))
					throw new RuntimeException(rgr.getData().getGeneId()+" is located on multiple chromosomes: "+r.getReference()+", "+rgr.getReference());
				r.setRegion(r.getRegion().union(rgr.getRegion()));
			}
			
		});
		
//		MemoryIntervalTreeStorage<String> genes = new MemoryIntervalTreeStorage<String>();
//		for (MutableReferenceGenomicRegion<String> rgr : genesToRegon.values())
//			genes.add(rgr.getReference(), rgr.getRegion(), rgr.getData());

		double credi = 0.05;
		try {
		
			out.startWriting();
		
			out.writef("Gene\talpha\tbeta\t%.3g credibility\tlog2 fold change\t%.3g credibility\n",0.5*credi,1-0.5*credi);
		
			for (MutableReferenceGenomicRegion<String> gene : genesToRegon.values()) {
				double[] total = new double[contrast.getNumMergedConditions()];
				double[] buff = new double[contrast.getNumMergedConditions()];
				
				reads.iterateIntersectingMutableReferenceGenomicRegions(readConversion.apply(gene.getReference()), gene.getRegion().getStart(), gene.getRegion().getEnd()).forEachRemaining(rgr->{
					
					// check if there is a matching transcript
					if (gene.getRegion().contains(rgr.getRegion())){
						// compute downsampled and add
						downsampling.getDownsampled(rgr.getData(), contrast, buff);
						ArrayUtils.add(total, buff);
						
//						System.out.println(Arrays.toString(buff)+"\t"+rgr.getReference()+":"+rgr.getRegion()+"\t"+rgr.getData());
						
					}
					
				});
				
				
//				System.err.println(gene);
				BetaDistribution beta = new BetaDistribution(total[0]+1, total[1]+1);
				out.writef("%s\t%.1f\t%.1f\t%.4f\t%.4f\t%.4f\n",gene.getData(),total[0]+1,total[1]+1,
						pToLog2Fc(beta.inverseCumulativeProbability(0.5*credi)),
						pToLog2Fc((beta.getAlpha()-1)/(beta.getAlpha()+beta.getBeta()-2)),
						pToLog2Fc(beta.inverseCumulativeProbability(1-0.5*credi))
						);
				
			}
			out.finishWriting();
			
		} catch (IOException e) {
		}
		
		
		
	}

	public void compute(LineOrientedFile out, DiskGenomicNumericProvider coverage, GenomicRegionStorage<Transcript> transcripts, Set<String> restrictToGenes, int readLength) {

		if (coverage.getNumDataRows()!=2) 
			throw new RuntimeException("Must be binary contrast!");
		
		// mapping to genes
		HashMap<String,MutableReferenceGenomicRegion<String>> genesToRegon = new HashMap<String, MutableReferenceGenomicRegion<String>>();
		transcripts.iterateReferenceGenomicRegions().forEachRemaining(rgr->{
			if (restrictToGenes!=null && !restrictToGenes.contains(rgr.getData().getGeneId()))
				return;
			
			MutableReferenceGenomicRegion<String> r = genesToRegon.get(rgr.getData().getGeneId());
			
			if (r==null) genesToRegon.put(rgr.getData().getGeneId(), 
					new MutableReferenceGenomicRegion<String>()
					.setReference(rgr.getReference())
					.setRegion(rgr.getRegion())
					.setData(rgr.getData().getGeneId()));
			else {
				if (!r.getReference().equals(rgr.getReference()))
					throw new RuntimeException(rgr.getData().getGeneId()+" is located on multiple chromosomes: "+r.getReference()+", "+rgr.getReference());
				r.setRegion(r.getRegion().union(rgr.getRegion()));
			}
			
		});
		
//		MemoryIntervalTreeStorage<String> genes = new MemoryIntervalTreeStorage<String>();
//		for (MutableReferenceGenomicRegion<String> rgr : genesToRegon.values())
//			genes.add(rgr.getReference(), rgr.getRegion(), rgr.getData());

		double credi = 0.05;
		try {
		
			out.startWriting();
		
			out.writef("Gene\talpha\tbeta\t%.3g credibility\tlog2 fold change\t%.3g credibility\n",0.5*credi,1-0.5*credi);
		
			for (MutableReferenceGenomicRegion<String> gene : genesToRegon.values()) {
				DoubleArrayList a = new DoubleArrayList();
				DoubleArrayList b = new DoubleArrayList();
				DoubleArrayList sum = new DoubleArrayList();
				
				for (int i=0; i<gene.getRegion().getTotalLength(); i++) {
					int p = gene.getRegion().map(i);
					double ca = coverage.getValue(gene.getReference(), p, 0);
					double cb = coverage.getValue(gene.getReference(), p, 1);
					a.add(ca);
					b.add(cb);
					sum.add(ca+cb);
				}
				
				
				sum.sort();
				double threshold = sum.getDouble(sum.size()/2);
				
				double asum = 0;
				double bsum = 0;
				
				DoubleArrayList fc = new DoubleArrayList();
				for (int i=0; i<a.size(); i++) {
					double ca = a.getDouble(i);
					double cb = b.getDouble(i);
					double cm = Math.max(ca,cb);
					if (ca+cb>threshold && cm>0) {
						fc.add(ca/cb);
						ca/=cm;
						cb/=cm;
						asum+=ca;
						bsum+=cb;
					}
				}
				
				fc.sort();
				double m = Math.log(fc.getDouble(fc.size()/2))/Math.log(2);
				
				
//				System.err.println(gene);
				BetaDistribution beta = new BetaDistribution(asum+1, bsum+1);
				out.writef("%s\t%.1f\t%.1f\t%.4f\t%.4f\t%.4f\n",gene.getData(),asum+1,bsum+1,
						0.0,//pToLog2Fc(beta.inverseCumulativeProbability(0.5*credi)),
						m,//pToLog2Fc((beta.getAlpha()-1)/(beta.getAlpha()+beta.getBeta()-2)),
						0.0//pToLog2Fc(beta.inverseCumulativeProbability(1-0.5*credi))
						);
				
			}
			out.finishWriting();
			
		} catch (IOException e) {
		}
		
		
		
	}
	
	
	

	private static double pToLog2Fc(double p) {
		return Math.log(p/(1-p))/Math.log(2);
	}
	
}
