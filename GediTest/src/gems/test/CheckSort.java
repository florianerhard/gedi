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
package gems.test;

import java.io.IOException;

import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;

public class CheckSort {

	public static void main(String[] args) throws IOException {
		LineIterator it = new LineOrientedFile(LineOrientedFile.STDOUT).lineIterator();
		
		
		
		MutableReferenceGenomicRegion tmp;
		MutableReferenceGenomicRegion curr = new MutableReferenceGenomicRegion();
		MutableReferenceGenomicRegion last = new MutableReferenceGenomicRegion();
		while (it.hasNext()) {
			curr.parse(it.next());
			if (last.getReference()!=null) {
				if (last.compareTo(curr)>=0)
					throw new RuntimeException(last+" >= "+curr);
			}
			
			tmp = curr;
			curr = last;
			last = tmp;
			
		}
		
	}
	
}
