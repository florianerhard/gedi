package gedi.atac;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegionPosition;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.util.StringUtils;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.userInteraction.progress.ConsoleProgress;

public class Heatmap {
	
	
	
	public static void heatmap(GenomicRegionStorage<AlignedReadsData> reads, String fimo, String id, String outFile) throws IOException {
		
		
		MutableReferenceGenomicRegion<Void>[] motifs = new LineOrientedFile(fimo).lineIterator()
			.skip(1)
			.map(l->StringUtils.split(l,'\t'))
			.filter(f->f[0].equals(id))
			.filter(f->!f[1].startsWith("chrY"))
			.map(f->{
			MutableReferenceGenomicRegion<Void> rgr = new MutableReferenceGenomicRegion<Void>().parse(f[1]);
			return rgr.setRegion(rgr.getRegion().map(new ArrayGenomicRegion(Integer.parseInt(f[2])-1,Integer.parseInt(f[3])))).toStrand(Strand.parse(f[4]));
		}).toArray(new MutableReferenceGenomicRegion[0]);
		
		int mwidth = motifs[0].getRegion().getTotalLength();
		for (int i=0; i<motifs.length;i++)
			if (mwidth!=motifs[0].getRegion().getTotalLength())
				throw new RuntimeException("Motifs have differing width!");
		int flank = 50;
		
		
		int[][] img = new int[motifs.length][flank+mwidth+flank];
		
		ConsoleProgress pro = new ConsoleProgress();
		pro.init().setCount(motifs.length);
		
		for (int r = 0; r<motifs.length; r++) {
			
			ArrayGenomicRegion imgRegion = motifs[r].getRegion().extendAll(flank, flank);
			MutableReferenceGenomicRegion<Void> motif = motifs[r];
			int[] line = img[r];
			
			Consumer<MutableReferenceGenomicRegion<AlignedReadsData>> adder = read->{
//				if (read.getRegion().contains(motif.getRegion()))
				for (int s : new int[] {
					GenomicRegionPosition.Start.position(read.getReference(), read.getRegion(), 4),
					GenomicRegionPosition.Stop.position(read.getReference(), read.getRegion(), -4)
				}) {
					if (imgRegion.contains(s)) {
						s = imgRegion.induce(s);
						if (motif.getReference().getStrand()==Strand.Minus)
							s = imgRegion.getTotalLength()-1-s;
						
						for (int i=0; i<read.getData().getNumConditions(); i++)
							line[s]+=read.getData().getTotalCountForConditionFloor(i,ReadCountMode.Weight)>0?1:0;
//						line[s]+=read.getData().getSumTotalCount();
					}
				}
				
			};
			
			reads.iterateIntersectingMutableReferenceGenomicRegions(motifs[r].getReference().toPlusStrand(), motifs[r].getRegion().getStart()-flank, motifs[r].getRegion().getEnd()+flank).forEachRemaining(adder);
			reads.iterateIntersectingMutableReferenceGenomicRegions(motifs[r].getReference().toMinusStrand(), motifs[r].getRegion().getStart()-flank, motifs[r].getRegion().getEnd()+flank).forEachRemaining(adder);
			pro.incrementProgress();
//			System.out.println(Arrays.toString(line));
		}
		pro.finish();
		
		LineOrientedFile out = new LineOrientedFile(outFile);
		out.startWriting();
		for (int i=0; i<img.length; i++) {
			out.writef(motifs[i].toLocationString());
			for (int j=0; j<img[i].length; j++) {
				out.writef("\t%d",img[i][j]);
			}
			out.writeLine();
		}
		
		out.finishWriting();
		
	}
	
	
	
}
