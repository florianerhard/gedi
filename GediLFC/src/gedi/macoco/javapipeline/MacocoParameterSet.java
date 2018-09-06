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
package gedi.macoco.javapipeline;

import java.io.File;

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.genomic.Genomic;
import gedi.core.reference.Strandness;
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

public class MacocoParameterSet extends GediParameterSet {

	
	public GediParameter<String> prefix = new GediParameter<String>(this,"prefix", "The prefix used for all output files", false, new StringParameterType());
	
	public GediParameter<Integer> nthreads = new GediParameter<Integer>(this,"nthreads", "The number of threads to use for computations", false, new IntParameterType(), Runtime.getRuntime().availableProcessors());
	
	public GediParameter<GenomicRegionStorage<AlignedReadsData>> reads = new GediParameter<GenomicRegionStorage<AlignedReadsData>>(this,"reads", "The mapped reads from the ribo-seq experiment.", false, new StorageParameterType<AlignedReadsData>());
	public GediParameter<Genomic> genomic = new GediParameter<Genomic>(this,"genomic", "The indexed GEDI genome.", true, new GenomicParameterType());
	public GediParameter<GenomicRegionStorage<NameAnnotation>> mrnas = new GediParameter<GenomicRegionStorage<NameAnnotation>>(this,"mRNA", "Use other mRNAs than specified by the genomic object.", false, new StorageParameterType<NameAnnotation>(),true);
	public GediParameter<Strandness> strandness = new GediParameter<Strandness>(this,"strandness", "Which strandness.", false, new EnumParameterType<>(Strandness.class));
	
	public GediParameter<File> countTable = new GediParameter<File>(this,"${prefix}.counts.tsv", "File containing the counts table", false, new FileParameterType());
	public GediParameter<File> lenDistTable = new GediParameter<File>(this,"${prefix}.lengths.tsv", "File containing the length distribution", false, new FileParameterType());
	public GediParameter<File> outTable = new GediParameter<File>(this,"${prefix}.tsv", "File containing the output table", false, new FileParameterType());
	public GediParameter<File> emMLTable = new GediParameter<File>(this,"${prefix}.ML.tsv", "File containing the tpms estimated by the EM", false, new FileParameterType());
	public GediParameter<File> lassoTable = new GediParameter<File>(this,"${prefix}.lasso.tsv", "File containing the tpms estimated by the penalized EM", false, new FileParameterType());
	public GediParameter<File> macocoTable = new GediParameter<File>(this,"${prefix}.macoco.tsv", "File containing the tpms estimated by the MACOCO", false, new FileParameterType());
	public GediParameter<File> elTable = new GediParameter<File>(this,"${prefix}.effLen.tsv", "File containing the effective lengths of all equivalence classes", false, new FileParameterType());
	
	
	
}
