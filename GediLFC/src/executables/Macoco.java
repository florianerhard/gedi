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

import gedi.macoco.javapipeline.CountEquivalenceClasses;
import gedi.macoco.javapipeline.EquivalenceClassesEffectiveLengths;
import gedi.macoco.javapipeline.EquivalenceClassesMacoco;
import gedi.macoco.javapipeline.EquivalenceClassesMaximumLikelihood;
import gedi.macoco.javapipeline.MacocoParameterSet;
import gedi.util.program.CommandLineHandler;
import gedi.util.program.GediProgram;

public class Macoco {

	
	public static void main(String[] args) {
		
	
		MacocoParameterSet params = new MacocoParameterSet();
		GediProgram pipeline = GediProgram.create("Macoco",
				new CountEquivalenceClasses(params),
				new EquivalenceClassesMaximumLikelihood(params)
//				new EquivalenceClassesMacoco(params),
//				new EquivalenceClassesEffectiveLengths(params)
				);
		GediProgram.run(pipeline, new CommandLineHandler("Macoco","Macoco computes maximal coverage consistent estimates of transcript abundances.",args));
		
	}
}
