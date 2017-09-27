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
import java.util.Locale;

import org.broad.igv.bbfile.BBFileReader;
import org.broad.igv.bbfile.BigWigIterator;
import org.broad.igv.bbfile.RPChromosomeRegion;
import org.broad.igv.bbfile.WigItem;

import gedi.core.data.numeric.GenomicNumericProvider.SpecialAggregators;
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericProvider;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;

public class ViewBW {

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
		
		BBFileReader wig = new BBFileReader(args[0]);
				
		if (args.length==1) {
			for (String ref : wig.getChromosomeNames()) {
				System.out.println(">"+ref);
				int id = wig.getChromosomeID(ref);
				BigWigIterator it = wig.getBigWigIterator(ref, wig.getChromosomeBounds(id, id).getStartBase(), ref, wig.getChromosomeBounds(id, id).getEndBase(), false);
				while (it.hasNext()) {
					WigItem item = it.next();
					System.out.printf(Locale.US,"%d\t%d\t%.4f\n",item.getStartBase(),item.getEndBase(),item.getWigValue());
				}
			}
		} 
		else if (args.length==2) {
			
			if (args[1].equals("-l")) {
				RPChromosomeRegion start = null;
				int last = -1;
				for (RPChromosomeRegion r : wig.getChromosomeRegions()) {
					if (start!=null) {
						if (r.getStartChromID()==start.getEndChromID() && r.getStartBase()==last) {
							last = r.getEndBase();
						} else {
							System.out.printf("%s\t%d\t%d\n",
									wig.getChromosomeName(start.getStartChromID()),
									start.getStartBase(),
									last
									);
							start = r;
							last = r.getEndBase();
						}
					}
					else {
						start = r;
						last = r.getEndBase();
					}
					
				}
				if (start!=null)
					System.out.printf("%s\t%d\t%d\n",
							wig.getChromosomeName(start.getStartChromID()),
							start.getStartBase(),
							last
							);
				return;
			}
			
			String p = args[1];
			int sep = p.indexOf(':');
			if (sep==-1) {
				int id = wig.getChromosomeID(p);
				BigWigIterator it = wig.getBigWigIterator(p, wig.getChromosomeBounds(id, id).getStartBase(), p, wig.getChromosomeBounds(id, id).getEndBase(), false);
				while (it.hasNext()) {
					WigItem item = it.next();
					System.out.printf(Locale.US,"%d\t%d\t%.4f\n",item.getStartBase(),item.getEndBase(),item.getWigValue());
				}
			}
			else {
				String ref = p.substring(0,sep);
				
				ArrayGenomicRegion reg = GenomicRegion.parse(p.substring(sep+1));
				for (int part=0; part<reg.getNumParts(); part++) {
					System.out.println(">"+ref+":"+reg.getStart(part)+"-"+reg.getEnd(part));
					BigWigIterator it = wig.getBigWigIterator(ref, reg.getStart(part), ref, reg.getEnd(part), false);
					while (it.hasNext()) {
						WigItem item = it.next();
						System.out.printf(Locale.US,"%d\t%d\t%.4f\n",item.getStartBase(),item.getEndBase(),item.getWigValue());
					}
				}
			}
				
		} else {
			usage();
			System.exit(1);
		}
		
	}

	private static void usage() {
		System.out.println("ViewBW <file> [<position>]");
	}
	
}
