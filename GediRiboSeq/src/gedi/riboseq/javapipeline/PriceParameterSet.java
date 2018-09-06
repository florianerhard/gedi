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
package gedi.riboseq.javapipeline;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.genomic.Genomic;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.riboseq.inference.clustering.RiboClusterInfo;
import gedi.riboseq.inference.codon.Codon;
import gedi.riboseq.inference.orf.OrfInference;
import gedi.util.FileUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.datastructure.tree.redblacktree.IntervalTreeSet;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.program.GediParameter;
import gedi.util.program.GediParameterSet;
import gedi.util.program.parametertypes.BooleanParameterType;
import gedi.util.program.parametertypes.DoubleParameterType;
import gedi.util.program.parametertypes.FileParameterType;
import gedi.util.program.parametertypes.GenomicParameterType;
import gedi.util.program.parametertypes.IntParameterType;
import gedi.util.program.parametertypes.InternalParameterType;
import gedi.util.program.parametertypes.StorageParameterType;
import gedi.util.program.parametertypes.StringParameterType;
import gedi.util.userInteraction.progress.Progress;

public class PriceParameterSet extends GediParameterSet {
	
	public GediParameter<File> paramFile = new GediParameter<File>(this,"${prefix}.param", "File containing the parameters used to call PRICE", false, new FileParameterType());
	

	public GediParameter<String> prefix = new GediParameter<String>(this,"prefix", "The prefix used for all output files", false, new StringParameterType());
	public GediParameter<File> maxPos = new GediParameter<File>(this,"${prefix}.maxPos", "The position upstream of the p site with maximal probability", false, new FileParameterType());

	public GediParameter<Integer> nthreads = new GediParameter<Integer>(this,"nthreads", "The number of threads to use for computations", false, new IntParameterType(), Runtime.getRuntime().availableProcessors());
	public GediParameter<GenomicRegionStorage<AlignedReadsData>> reads = new GediParameter<GenomicRegionStorage<AlignedReadsData>>(this,"reads", "The mapped reads from the ribo-seq experiment.", false, new StorageParameterType<AlignedReadsData>());
	public GediParameter<Genomic> genomic = new GediParameter<Genomic>(this,"genomic", "The indexed GEDI genome.", true, new GenomicParameterType());
	
	public GediParameter<String> filter = new GediParameter<String>(this,"filter", "Use only reads matching the filter (e.g. 28:30)", false, new StringParameterType(),"");
	public GediParameter<File> estimateData = new GediParameter<File>(this,"${prefix}.estimateData", "File containing the sufficient statistics to estimate model", false, new FileParameterType());
	public GediParameter<Boolean> percond = new GediParameter<Boolean>(this,"percond", "Estimate models per condition", false, new BooleanParameterType());
	public GediParameter<Boolean> skipmt = new GediParameter<Boolean>(this,"skipmt", "Skip mitochondrial chromosome for estimating the model", false, new BooleanParameterType());
	
	
	public GediParameter<Boolean> plot = new GediParameter<Boolean>(this,"plot", "Use R to produce various plots",false, new BooleanParameterType());
	public GediParameter<Integer> maxiter = new GediParameter<Integer>(this,"maxiter", "The maximal number of iterations per repeat in the EM for estimating model parameters", false, new IntParameterType(), 1000);
	public GediParameter<Integer> repeats = new GediParameter<Integer>(this,"repeats", "The number of repeats in the EM for estimating model parameters", false, new IntParameterType(), 100000);
	
	public GediParameter<File> model = new GediParameter<File>(this,"${prefix}.model", "File containing the estimated model", false, new FileParameterType());
	public GediParameter<File> clusters = new GediParameter<File>(this,"${prefix}.clusters.cit", "File containing the genomic chunks to run PRICE on", false, new FileParameterType()).setRemoveFile(true);
	
	public GediParameter<File> signaldata = new GediParameter<File>(this,"${prefix}.signal.tsv", "Signal to noise data.",false, new FileParameterType());
	
	public GediParameter<GenomicRegionStorage<Void>> introns = new GediParameter<GenomicRegionStorage<Void>>(this,"introns", "Reads or transcripts to take additional splice junctions from", false, new StorageParameterType<Void>(),true);
	public GediParameter<Boolean> novelTranscripts = new GediParameter<Boolean>(this,"novelTranscripts", "Try to infer novel transcripts based on reads.", false, new BooleanParameterType());
	public GediParameter<Boolean> keepAnno = new GediParameter<Boolean>(this,"keepAnno", "Do not apply filters to annotated ORFs.", false, new BooleanParameterType());
	
	public GediParameter<GenomicRegionStorage<Void>> checkOrfs = new GediParameter<GenomicRegionStorage<Void>>(this,"checkOrfs", "File containing ORFs to check inference against.", true, new StorageParameterType<Void>(),true);
	public GediParameter<Boolean> checkAnnotation = new GediParameter<Boolean>(this,"checkAnnotation", "Check inference against annotation.", false, new BooleanParameterType());
	
	public GediParameter<OrfInference> orfinference = new GediParameter<OrfInference>(this,"orfinference", "Internal ORF inference object", false, new InternalParameterType<>(OrfInference.class));
	
	public GediParameter<File> codons = new GediParameter<File>(this,"${prefix}.codons.bin", "Inferred codons.", false, new FileParameterType()).setRemoveFile(true);
	public GediParameter<Double> delta = new GediParameter<Double>(this,"delta", "Regularization parameter", false, new DoubleParameterType(), 0.0);
	
	public GediParameter<Boolean> inferDelta = new GediParameter<Boolean>(this,"inferDelta", "Automatically select delta (such that a 10% off-frame codon is recognized as such).", false, new BooleanParameterType());
	
	public GediParameter<File> indices = new GediParameter<File>(this,"${prefix}.codons.cit", "Codon indexs.", false, new FileParameterType());
	public GediParameter<File> rmq = new GediParameter<File>(this,"${prefix}.codons.rmq", "Viewer indices.", false, new FileParameterType());
	
	public GediParameter<Integer> trainingExamples = new GediParameter<Integer>(this,"trainingExamples", "The number of genes in one bin for start prediction training", false, new IntParameterType(), 1000);
	public GediParameter<File> startmodel = new GediParameter<File>(this,"${prefix}.start.model", "Start prediction model.", false, new FileParameterType()).setRemoveFile(true);
	
	public GediParameter<File> noisemodel = new GediParameter<File>(this,"${prefix}.noise.model", "Noise model for the generalized binomial test.", false, new FileParameterType()).setRemoveFile(true);
	
	
	public GediParameter<File> orfstsv = new GediParameter<File>(this,"${prefix}.orfs.tsv", "Table containing all ORFs.", false, new FileParameterType());
	public GediParameter<File> orfsbin = new GediParameter<File>(this,"${prefix}.orfs.bin", "Binary file containing ORF information.", false, new FileParameterType()).setRemoveFile(true);
	public GediParameter<File> pvals = new GediParameter<File>(this,"${prefix}.orfs.pvals", "Pvalues from OrfInference.", false, new FileParameterType()).setRemoveFile(true);
	
	public GediParameter<Double> fdr = new GediParameter<Double>(this,"fdr", "Desired false discovery rate", false, new DoubleParameterType(), 0.1);
	public GediParameter<File> orfstmp = new GediParameter<File>(this,"${prefix}.tmp.orfs.cit", "Temporary ORF index.", false, new FileParameterType()).setRemoveFile(true);
	
	public GediParameter<File> orfs = new GediParameter<File>(this,"${prefix}.orfs.cit", "ORF index.", false, new FileParameterType());

	public GediParameter<File> optcodons = new GediParameter<File>(this,"${prefix}.opt.codons.cit", "Optimistic codon mapping.", false, new FileParameterType());
	public GediParameter<Boolean> opt = new GediParameter<Boolean>(this,"opt", "Run optimistic codon mapping.", false, new BooleanParameterType());
	
	
	public GediParameter<File> majorIsoformCit = new GediParameter<File>(this,"${prefix}.majorisoform.cit", "Major isoform data.", false, new FileParameterType());
	
	public GediParameter<Double> minRpc = new GediParameter<Double>(this,"minRpc", "Minimal reads per codon to test an ORF", false, new DoubleParameterType(),1.0);
	
	public GediParameter<String> removeGenes = new GediParameter<String>(this,"removeGenes", "remove genes in this file from analysis", false, new StringParameterType(),true);
	
	
	public GediParameter<File> localTable = new GediParameter<File>(this,"${prefix}.localtest.tsv", "File containing the localtest output table", false, new FileParameterType());
	public GediParameter<File> localCit = new GediParameter<File>(this,"${prefix}.localtest.cit", "File containing the localtest regions", false, new FileParameterType());
	public GediParameter<String> localContrastsFile = new GediParameter<String>(this,"contrasts", "Contrasts to compute local changes", false, new StringParameterType(),true);

}
