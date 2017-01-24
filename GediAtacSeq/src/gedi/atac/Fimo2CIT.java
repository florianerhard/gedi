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

package gedi.atac;

import java.io.IOException;
import java.util.HashMap;

import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeNode;
import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.annotation.ScoreAnnotation;
import gedi.core.data.annotation.ScoreNameAnnotation;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.StringUtils;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.userInteraction.progress.ConsoleProgress;

public class Fimo2CIT {

	public static void main(String[] args) throws IOException {
		if (args.length!=3) {
			System.err.println("Fimo2CIT <meme-file> <fimo> <cit>");
			System.exit(1);
		}
		
		HashMap<String,String> map = new HashMap<String,String>();
		new LineOrientedFile(args[0]).lineIterator().filter(l->l.startsWith("MOTIF"))
		.map(l->StringUtils.split(l, ' ')).forEachRemaining(f->map.put(f[1],f[2]));
		
		ConsoleProgress p = new ConsoleProgress();
		p.init().setCount((int) new LineOrientedFile(args[1]).lineIterator().count());
		
		MemoryIntervalTreeStorage<ScoreNameAnnotation> mem = new MemoryIntervalTreeStorage<ScoreNameAnnotation>(ScoreNameAnnotation.class);
		
		new LineOrientedFile(args[1]).lineIterator().skip(1).map(l->StringUtils.split(l, '\t')).forEachRemaining(f->{
			p.setDescription(map.getOrDefault(f[0], f[0])).incrementProgress();
			ScoreNameAnnotation d = new ScoreNameAnnotation(map.getOrDefault(f[0], f[0]), -Math.log(Double.parseDouble(f[6]))/Math.log(10));
			MutableReferenceGenomicRegion<ScoreNameAnnotation> rgr = new MutableReferenceGenomicRegion<ScoreNameAnnotation>().parse(f[1],d).toStrand(Strand.parse(f[4]));
			rgr.setRegion(rgr.getRegion().map(new ArrayGenomicRegion(Integer.parseInt(f[2])-1,Integer.parseInt(f[3]))));
			mem.add(rgr);
		});
		p.finish();
		
		
		CenteredDiskIntervalTreeStorage<ScoreNameAnnotation> cit = new CenteredDiskIntervalTreeStorage<ScoreNameAnnotation>(args[2], ScoreNameAnnotation.class);
		cit.fill(mem);
	}
	
}
