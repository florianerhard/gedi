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

package gedi.lfc.localtest.javapipeline;

import java.io.File;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.genomic.Genomic;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.feature.special.Downsampling;
import gedi.util.program.GediParameter;
import gedi.util.program.GediParameterSet;
import gedi.util.program.parametertypes.BooleanParameterType;
import gedi.util.program.parametertypes.DoubleParameterType;
import gedi.util.program.parametertypes.EnumParameterType;
import gedi.util.program.parametertypes.FileParameterType;
import gedi.util.program.parametertypes.GenomicParameterType;
import gedi.util.program.parametertypes.IntParameterType;
import gedi.util.program.parametertypes.StorageParameterType;
import gedi.util.program.parametertypes.StringParameterType;

public class LTestParameterSet extends GediParameterSet {

	public GediParameter<File> paramFile = new GediParameter<File>(this,"${prefix}.param", "File containing the parameters used to call PRICE", false, new FileParameterType());
	
	public GediParameter<String> prefix = new GediParameter<String>(this,"prefix", "The prefix used for all output files", false, new StringParameterType());
	
	public GediParameter<Integer> nthreads = new GediParameter<Integer>(this,"nthreads", "The number of threads to use for computations", false, new IntParameterType(), Runtime.getRuntime().availableProcessors());
	public GediParameter<Integer> minlen = new GediParameter<Integer>(this,"minlen", "Minimal read length to consider", false, new IntParameterType(), 25);
	public GediParameter<Downsampling> downsampling = new GediParameter<Downsampling>(this,"downsampling", "Downsampling mode", false, new EnumParameterType<>(Downsampling.class), Downsampling.Logsc);
	public GediParameter<GenomicRegionStorage<AlignedReadsData>> reads = new GediParameter<GenomicRegionStorage<AlignedReadsData>>(this,"reads", "The mapped reads from the ribo-seq experiment.", false, new StorageParameterType<AlignedReadsData>());
	public GediParameter<Genomic> genomic = new GediParameter<Genomic>(this,"genomic", "The indexed GEDI genome.", true, new GenomicParameterType());


	public GediParameter<File> rmqFile = new GediParameter<File>(this,"${prefix}.rmq", "File containing all local pvalues (-log10)", false, new FileParameterType());
	public GediParameter<File> outTable = new GediParameter<File>(this,"${prefix}.tsv", "File containing the output table", false, new FileParameterType());
	
	
	
}
