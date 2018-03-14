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

import java.util.logging.Logger;

import gedi.app.Gedi;
import gedi.riboseq.javapipeline.PriceClusterReads;
import gedi.riboseq.javapipeline.PriceCodonInference;
import gedi.riboseq.javapipeline.PriceCodonViewerIndices;
import gedi.riboseq.javapipeline.PriceCollectSufficientStatistics;
import gedi.riboseq.javapipeline.PriceDetermineDelta;
import gedi.riboseq.javapipeline.PriceEstimateModel;
import gedi.riboseq.javapipeline.PriceIdentifyMaxPos;
import gedi.riboseq.javapipeline.PriceMultipleTestingCorrection;
import gedi.riboseq.javapipeline.PriceNoiseTraining;
import gedi.riboseq.javapipeline.PriceOptimisticCodonMapping;
import gedi.riboseq.javapipeline.PriceOrfInference;
import gedi.riboseq.javapipeline.PriceParameterSet;
import gedi.riboseq.javapipeline.PriceReassignCodons;
import gedi.riboseq.javapipeline.PriceSetupOrfInference;
import gedi.riboseq.javapipeline.PriceSignalToNoise;
import gedi.riboseq.javapipeline.PriceStartPredictionTraining;
import gedi.riboseq.javapipeline.PriceWriteCodons;
import gedi.riboseq.javapipeline.analyze.PriceLocalChanges;
import gedi.util.program.CommandLineHandler;
import gedi.util.program.GediProgram;

public class Price {

	
	public static void main(String[] args) {
		
		PriceParameterSet params = new PriceParameterSet();
		GediProgram pipeline = GediProgram.create("PRICE",
				new PriceIdentifyMaxPos(params),
				new PriceCollectSufficientStatistics(params),
				new PriceEstimateModel(params),
				new PriceClusterReads(params),
				new PriceSetupOrfInference(params),
				new PriceCodonInference(params),
				new PriceDetermineDelta(params),
				new PriceWriteCodons(params),
				new PriceCodonViewerIndices(params),
				new PriceStartPredictionTraining(params),
				new PriceNoiseTraining(params),
				new PriceOrfInference(params),
				new PriceMultipleTestingCorrection(params),
				new PriceReassignCodons(params),
				new PriceSignalToNoise(params),
				new PriceOptimisticCodonMapping(params)
				);
		GediProgram.run(pipeline, params.paramFile, new CommandLineHandler("PRICE","PRICE is an analysis method for Ribo-seq data.",args));
		
	}
	
}
