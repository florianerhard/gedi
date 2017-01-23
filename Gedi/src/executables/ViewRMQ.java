package executables;

import java.io.File;
import java.io.IOException;

import gedi.core.data.numeric.GenomicNumericProvider.SpecialAggregators;
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericProvider;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;

public class ViewRMQ {

	public static void main(String[] args) throws IOException {
		if (args.length<1) {
			usage();
			System.exit(1);
		}
		
		if (!new File(args[0]).exists()) {
			System.out.println("File "+args[0]+" does not exist!");
			usage();
			System.exit(1);
		}
		
		DiskGenomicNumericProvider prov = new DiskGenomicNumericProvider(args[0]);
		
		if (args.length==1) {
			for (ReferenceSequence ref : prov.getRefereneSequences()) {
				System.out.println(">"+ref);
				prov.dump(ref, s->System.out.print(s));
			}
		} 
		else if (args.length==2) {
			String p = args[1];
			int sep = p.indexOf(':');
			if (sep==-1) 
				prov.dump(Chromosome.obtain(p), s->System.out.print(s));
			else
				prov.dump(Chromosome.obtain(p.substring(0,sep)),GenomicRegion.parse(p.substring(sep+1)), s->System.out.print(s));
		} else if (args.length==4) {
			String p = args[1];
			int sep = p.indexOf(':');
			ReferenceSequence ref = Chromosome.obtain(p.substring(0,sep));
			GenomicRegion region = GenomicRegion.parse(p.substring(sep+1));
			int interval = Integer.parseInt(args[3]);
			SpecialAggregators agg = SpecialAggregators.valueOf(args[2]);
			
			for (int s = 0; s<region.getTotalLength(); s+=interval) {
				System.out.printf("%s\t%d",ref,region.induce(s));
				for (int i=0; i<prov.getNumDataRows(); i++)
					System.out.printf("\t"+agg.getAggregatedValue(prov, ref, new ArrayGenomicRegion(s,s+interval), i));
				System.out.println();
			}
			
		} else {
			usage();
			System.exit(1);
		}
		
	}

	private static void usage() {
		System.out.println("ViewRMQ <file> [<position>] [<aggregation> <interval>]");
	}
	
}
