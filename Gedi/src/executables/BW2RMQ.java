package executables;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.script.ScriptException;

import org.broad.igv.bbfile.BBFileReader;
import org.broad.igv.bbfile.BigWigIterator;
import org.broad.igv.bbfile.RPChromosomeRegion;
import org.broad.igv.bbfile.WigItem;

import gedi.core.data.numeric.GenomicNumericProvider.SpecialAggregators;
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericBuilder;
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericProvider;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.util.nashorn.JS;
import gedi.util.nashorn.JSFunction;
import gedi.util.userInteraction.progress.ConsoleProgress;

public class BW2RMQ {

	public static void main(String[] args) throws IOException, ScriptException {
		if (args.length<2) {
			usage();
			System.exit(1);
		}
		
		if (!new File(args[0]).exists()) {
			System.out.println("File "+args[0]+" does not exist!");
			usage();
			System.exit(1);
		}
		
		BBFileReader wig = new BBFileReader(args[0]);
		JSFunction fun = null;
		
		if (args.length==3) 
			fun = new JSFunction(args[1]);
		
		DiskGenomicNumericBuilder rmq = new DiskGenomicNumericBuilder(args[args.length-1],false);
		rmq.setReferenceSorted(true);
		
		ConsoleProgress pr = new ConsoleProgress();
		pr.init();

		for (String ref : wig.getChromosomeNames()) {
			pr.setDescription(ref);
			Chromosome rs = Chromosome.obtain(ref);
			int id = wig.getChromosomeID(ref);
			BigWigIterator it = wig.getBigWigIterator(ref, wig.getChromosomeBounds(id, id).getStartBase(), ref, wig.getChromosomeBounds(id, id).getEndBase(), false);
			while (it.hasNext()) {
				WigItem item = it.next();
				for (int p=item.getStartBase(); p<item.getEndBase(); p++)
					if (fun==null)
						rmq.addValue(rs, p, item.getWigValue());
					else
						rmq.addValue(rs, p, (Number)fun.apply(item.getWigValue()));
				
				pr.incrementProgress();
			}
		}
		
		pr.finish();		
		
		rmq.build(false, true);
		
	}

	private static void usage() {
		System.out.println("BW2RMQ <bw> <js-function> <rmq>");
	}
	
}
