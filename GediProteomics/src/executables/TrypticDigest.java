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

import gedi.proteomics.digest.FullAfterAADigester;
import gedi.util.functions.EI;
import gedi.util.io.text.fasta.FastaEntry;
import gedi.util.io.text.fasta.FastaFile;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class TrypticDigest {
	
	
	public static void main(String[] args) throws IOException {
		
		if (args.length!=2 || !new File(args[1]).exists()) {
			System.err.println("TrypticDigest <missed> proteins.fasta");
			System.exit(1);
		}
		
		int missed = Integer.parseInt(args[0]);
		FullAfterAADigester digest = new FullAfterAADigester(missed, 'K','R');
		
		
		Iterator<FastaEntry> it = new FastaFile(args[1]).entryIterator(true);
		
		while (it.hasNext()) {
			FastaEntry fe = it.next();
			System.out.println(fe.getHeader());
			EI.wrap(digest.iteratePeptides(fe.getSequence())).filter(s->s.length()>=6).forEachRemaining(System.out::println);

		}
		
	}
	

}
