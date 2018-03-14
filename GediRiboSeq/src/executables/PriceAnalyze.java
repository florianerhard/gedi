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

import gedi.app.Gedi;
import gedi.riboseq.javapipeline.PriceParameterSet;
import gedi.riboseq.javapipeline.analyze.PriceAnalyzeMajorIsoforms;
import gedi.riboseq.javapipeline.analyze.PriceIdentifyMajorIsoform;
import gedi.util.program.CommandLineHandler;
import gedi.util.program.GediProgram;

public class PriceAnalyze {

	
	public static void main(String[] args) {
		
		Gedi.startup(true);
		
		PriceParameterSet params = new PriceParameterSet();
		GediProgram pipeline = GediProgram.create("PriceAnalyze",
				new PriceIdentifyMajorIsoform(params),
				new PriceAnalyzeMajorIsoforms(params)
				);
		GediProgram.run(pipeline, params.paramFile, new CommandLineHandler("PriceAnalyze","PriceAnalyze analyzes further aspects  in PRICE-inferred codons.",args));
		
	}
	
}
