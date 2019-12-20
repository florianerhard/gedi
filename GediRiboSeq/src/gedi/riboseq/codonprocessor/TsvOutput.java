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
package gedi.riboseq.codonprocessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import gedi.util.datastructure.array.NumericArray;
import gedi.util.functions.EI;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.mutable.MutableTuple;

public class TsvOutput extends CodonProcessorOutput {

	@Override
	public void createOutput2(String[] conditions) throws IOException {
		LineWriter out = new LineOrientedFile(counter.getPrefix()+".tsv").write();
		for (int i=0; i<counter.getVariableNames().length; i++) {
			out.write(counter.getVariableNames()[i]);
			out.write("\t");
		}
		out.writeLine(EI.wrap(conditions).concat("\t"));
		
		
		ArrayList<MutableTuple> keys = new ArrayList<MutableTuple>(counter.keys());
		Collections.sort(keys);
		
		for (MutableTuple key : keys) {
			NumericArray n = counter.getCounts(key);
			for (int i=0; i<counter.getVariableNames().length; i++) {
				out.write(key.get(i).toString());
				out.write("\t");
			}
				
			for (int c=0; c<conditions.length; c++) {
				if (c>0) out.write("\t");
				out.write(n.format(c));
			}
			
			out.writeLine();
		}
		
		out.close();
	}

	
}
