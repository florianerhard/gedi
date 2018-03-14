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

import gedi.util.StringUtils;
import gedi.util.io.text.LineIterator;
import gedi.util.math.stat.RandomNumbers;
import gedi.util.r.RConnect;

import java.io.IOException;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REngineException;

public class PlotTest {

	public static void main(String[] args) throws REngineException, IOException {
		
		RandomNumbers rnd = new RandomNumbers();
		int s = 4;
		double[][] graphs = new double[s][];
		double[][] contexts = new double[s][];
		int[] unique = new int[s];
		String[] names = new String[s];
		for (int i=0; i<s; i++) {
			graphs[i] = rnd.getUnif(29,0,100).toDoubleArray();
			contexts[i] = rnd.getUnif(98,0,100).toDoubleArray();
			unique[i] = rnd.getUnif(0,100);
			names[i] = StringUtils.createRandomIdentifier(20, rnd);
		}
		RConnect.R().startPDF("test.pdf",14,14);
		
		
		
		RConnect.R().assign("id", new REXPInteger(0));
		RConnect.R().assign("names", names);
		RConnect.R().assign("names", names);
		RConnect.R().assign("graphs", REXP.createDoubleMatrix(graphs));
		RConnect.R().assign("contexts", REXP.createDoubleMatrix(contexts));
		RConnect.R().assign("unique", unique);
		
		RConnect.R().evalUnchecked(new LineIterator(ResolveAmbiguities.class.getResourceAsStream("plotambiguity.R")).concat("\n"));
		
		RConnect.R().finishPDF();
		
		
	}
	
}
