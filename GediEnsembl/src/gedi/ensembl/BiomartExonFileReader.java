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

package gedi.ensembl;

import gedi.core.data.annotation.Transcript;
import gedi.core.reference.ReferenceSequence;
import gedi.util.FunctorUtils;
import gedi.util.datastructure.tree.redblacktree.IntervalCoordinateSystem;
import gedi.util.io.text.HeaderLine;
import gedi.util.io.text.tsv.GenomicExonsTsvFileReader;

import java.io.IOException;
import java.util.Comparator;
import java.util.function.BiFunction;

public class BiomartExonFileReader extends GenomicExonsTsvFileReader<Transcript> {

	
	private static final String GeneID = "Gene ID";
	private static final String TranscriptID = "Transcript ID";
	private static final String AlternateGeneID = "Ensembl "+GeneID;
	private static final String AlternateTranscriptID = "Ensembl "+TranscriptID;
	private static final String Chromosome = "Chromosome Name";
	private static final String Strand = "Strand";
	private static final String Start = "Exon Chr Start (bp)";
	private static final String End = "Exon Chr End (bp)";
	private static final String CodingStart = "Genomic coding start";
	private static final String CodingEnd = "Genomic coding end";
	
	
	
	public BiomartExonFileReader(String path, boolean onlyStandardChromosomes) throws IOException {
		this(path,onlyStandardChromosomes,BiomartType.Transcript);
	}

	public BiomartExonFileReader(String path, boolean onlyStandardChromosomes, BiomartType type) throws IOException {
		super(path,Chromosome,Strand,Start,End,IntervalCoordinateSystem.ENSEMBL,getData(type,onlyStandardChromosomes),(Comparator<String[]>)null,null,Transcript.class);
		
		init = h->{
			if (h.hasField(AlternateGeneID))
				h.addAlternative(AlternateGeneID, GeneID);
			if (h.hasField(AlternateTranscriptID))
				h.addAlternative(AlternateTranscriptID, TranscriptID);
			if (type==BiomartType.Transcript)
				sameRegionComparator = FunctorUtils.arrayComparator(h.get(GeneID),h.get(TranscriptID));
			else {
				sameRegionComparator = FunctorUtils.arrayComparator(h.get(GeneID));
				mergeOverlap = true;
			}
		};
		
		getRef=(h,f)->{
			ReferenceSequence re = gedi.core.reference.Chromosome.obtain(Ensembl.correctChr(f[h.get(Chromosome)]),f[h.get(Strand)]);
			if (onlyStandardChromosomes && !Ensembl.isStandardChromosome(re))
				return null;
			return re;
		};
		
		if (type==BiomartType.Gene)
			regionMapper = a->a.removeIntrons();
	}
	

	private static BiFunction<HeaderLine, String[][], Transcript> getData(BiomartType type, boolean onlyStandardChromosomes) {
		return (h,tr)->{
			int codingStart = -1;
			int codingEnd = -1;
			for (int i=0; i<tr.length; i++) {
				if (tr[i][h.get(CodingStart)].length()>0) {
					if (codingStart==-1) codingStart = Integer.parseInt(tr[i][h.get(CodingStart)])-1;
					else codingStart = Math.min(codingStart,Integer.parseInt(tr[i][h.get(CodingStart)])-1);
					if (codingEnd==-1) codingEnd = Integer.parseInt(tr[i][h.get(CodingEnd)]);
					else codingEnd = Math.max(codingEnd,Integer.parseInt(tr[i][h.get(CodingEnd)]));
				}
			}
			return new Transcript(
					tr[0][h.get(GeneID)],
					type==BiomartType.Transcript?tr[0][h.get(TranscriptID)]:tr[0][h.get(GeneID)],
					codingStart,codingEnd
					);
		};
	}

	
}
