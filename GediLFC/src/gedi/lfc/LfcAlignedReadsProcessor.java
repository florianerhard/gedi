package gedi.lfc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.math3.distribution.BetaDistribution;

import gedi.core.data.annotation.Transcript;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ContrastMapping;
import gedi.core.processing.GenomicRegionProcessor;
import gedi.core.processing.OverlapMode;
import gedi.core.processing.ProcessorContext;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MissingInformationIntronInformation;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.util.ArrayUtils;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.datastructure.tree.redblacktree.SimpleInterval;
import gedi.util.io.text.LineOrientedFile;

public class LfcAlignedReadsProcessor implements GenomicRegionProcessor {

	
	// TODO: once table framework is there, do not produce an output file but write into a table of the context! (a table output processor will then do the trick)
	
	private ContrastMapping before;
	private ContrastMapping after;
	private Downsampling downsampling;
	private double credi = 0.05;
	
	private boolean allreads = false;
	
	private LineOrientedFile out;
	
	double[] total;
	double[] buff;
	private int minCond = -1;
	private GenomicRegionStorage<Transcript> transcripts;
	
	public LfcAlignedReadsProcessor(ContrastMapping contrast,
			Downsampling downsampling, LineOrientedFile out) {
		this.before = contrast;
		this.downsampling = downsampling;
		this.out = out;
		
//		if (contrast.getNumMergedConditions()!=2) 
//			throw new RuntimeException("Must be binary contrast!");
		
	}
	
	public LfcAlignedReadsProcessor(ContrastMapping before,ContrastMapping after,
			Downsampling downsampling, LineOrientedFile out) {
		this.before = before;
		this.after = after;
		this.downsampling = downsampling;
		this.out = out;
	}

	public LfcAlignedReadsProcessor setAllreads(boolean allreads) {
		this.allreads = allreads;
		return this;
	}
	
	@Override
	public void begin(ProcessorContext context) throws IOException {
		out.startWriting();
		if (allreads) {
			out.writef("Gene\tLocation\tMode");
			if (minCond>-1)
				out.writef("\twith reads");
			ContrastMapping contr = after==null?before:after;
			for (int i=0; i<contr.getNumMergedConditions(); i++)
				out.writef("\t%s",contr.getMappedName(i));
			out.writeLine(transcripts!=null?"\tTranscripts":"");
			
			total = new double[contr.getNumMergedConditions()];
			buff = new double[contr.getNumMergedConditions()];
		}
		else if (multimode()){
			out.writef("Gene");
			ContrastMapping contr = after==null?before:after;
			for (int i=0; i<contr.getNumMergedConditions(); i++)
				out.writef("\t%s",contr.getMappedName(i));
			out.writeLine();
			
			total = new double[contr.getNumMergedConditions()];
			buff = new double[contr.getNumMergedConditions()];
			
		}
		else{
			out.writef("Gene\talpha\tbeta\t%.3g credible\tlog2 fold change\t%.3g credible\n",0.5*credi,1-0.5*credi);
		
			total = new double[2];
			buff = new double[2];
		}
	}
	
	public LfcAlignedReadsProcessor forceMultiMode() {
		setCredible(Double.NaN);
		return this;
	}

	public LfcAlignedReadsProcessor setCredible(double credi) {
		this.credi = credi;
		return this;
	}
	
	public LfcAlignedReadsProcessor setTranscripts(
			GenomicRegionStorage<Transcript> transcripts) {
		this.transcripts = transcripts;
		return this;
	}

	public GenomicRegionProcessor setMinConditionsWithReads(int minCond) {
		this.minCond = minCond;
		return this;
	}
	
	private boolean multimode() {
		return Double.isNaN(credi) || (after==null && before.getNumMergedConditions()!=2)||(after!=null && after.getNumMergedConditions()!=2);
	}

	@Override
	public void beginRegion(MutableReferenceGenomicRegion<?> region, ProcessorContext context) {
		Arrays.fill(total, 0);
		Arrays.fill(buff, 0);
	}
	
	public void setBuffer(double[] buff) {
		this.total = buff;
	}


	@Override
	public void read(MutableReferenceGenomicRegion<?> region,
			MutableReferenceGenomicRegion<AlignedReadsData> read, ProcessorContext context) throws IOException {
		
//		if (region.getRegion().contains(read.getRegion())){
			// compute downsampled and add
		
//		if (read.getRegion().getTotalLength()>100) return;
		
			if (after==null)
				downsampling.getDownsampled(read.getData(), before, buff);
			else 
				downsampling.getDownsampled(read.getData(), before, after, buff);
			
//			if (read.getData().getNumConditions()==16) 
//				System.out.println(read);
//			else
//			for (int i=0; i<read.getData().getNumConditions(); i++) {
//				if (i/8==1 || i/8==3) {
//					if (read.getData().getTotalCount(i)>0) {
//						System.out.print(read.getReference()+":"+read.getRegion()+" [");
//						for (int c=0; c<read.getData().getNumConditions(); c++) {
//							if (c/8==1 || c/8==3) {
//								if (c>8) System.out.print(", ");
//								System.out.print(read.getData().getTotalCount(c));
//							}
//						}
//						System.out.println("] "+read.getData().getMultiplicity(0));
//						break;
//					}
//				}
//			}

			int wr = 0;
			if (minCond>0) {
				for (int i=0; i<buff.length; i++)
					if (buff[i]>0) wr++;
			}
				
			if (wr>=minCond)
				ArrayUtils.add(total, buff);
			
			if (allreads && wr>=minCond) {
				
				String mode = "";
				for (OverlapMode m : OverlapMode.values())
					if (m.test(region.getRegion(), context.get(ProcessorContext.EXON_TREE), read.getRegion())) {
						mode = m.name();
						break;
					}
				
				out.writef("%s\t%s:%s\t%s",region.getData(),read.getReference().toString(),read.getRegion().toRegionString(),mode);
				if (minCond>-1)
					out.writef("\t%d",wr);
				for (int i=0; i<buff.length; i++)
					out.writef("\t%.1f",buff[i]);
				
				
				// find all compatible transcripts
				if (transcripts!=null) {
					out.writef("\t");
					int n = 0;
					for (ImmutableReferenceGenomicRegion<Transcript> tr : transcripts.getReferenceRegionsIntersecting(read.getReference(), read.getRegion())) {
						if (compatible(tr.getRegion(),read.getRegion() instanceof MissingInformationIntronInformation?((MissingInformationIntronInformation)read.getRegion()).getInformationGenomicRegions():new GenomicRegion[] {read.getRegion()}))
							out.writef(n++>0?",%s":"%s", tr.getData().getTranscriptId());
					}
				}
				
				out.writeLine();
			}
//			System.out.println(Arrays.toString(buff)+"\t"+read.getReference()+":"+read.getRegion()+"\t"+read.getData());
			
//		}
	}
	

	private boolean compatible(GenomicRegion region, GenomicRegion[] info) {
		for (GenomicRegion i : info)
			if (!region.containsUnspliced(i))
				return false;
		return true;
	}

	@Override
	public void endRegion(MutableReferenceGenomicRegion<?> region, ProcessorContext context) throws IOException {
		if (allreads) {
		}else if (multimode()){
			out.writef("%s",region.getData());
			for (int i=0; i<total.length; i++)
				out.writef("\t%.1f",total[i]);
			out.writeLine();
		} else {
			BetaDistribution beta = new BetaDistribution(total[0]+1, total[1]+1);
			out.writef("%s\t%.1f\t%.1f\t%.4f\t%.4f\t%.4f\n",region.getData(),total[0]+1,total[1]+1,
					pToLog2Fc(beta.inverseCumulativeProbability(0.5*credi)),
					pToLog2Fc((beta.getAlpha()-1)/(beta.getAlpha()+beta.getBeta()-2)),
					pToLog2Fc(beta.inverseCumulativeProbability(1-0.5*credi))
					);
		}
		
	}
	
	
	@Override
	public void end(ProcessorContext context) throws IOException {
		out.finishWriting();
	}
	
	
	
	private static double pToLog2Fc(double p) {
		if (Double.isNaN(p)) return 0;
		return Math.log(p/(1-p))/Math.log(2);
	}

	

	
}
