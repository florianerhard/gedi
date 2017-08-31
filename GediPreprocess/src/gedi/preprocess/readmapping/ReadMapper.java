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

package gedi.preprocess.readmapping;

import gedi.core.genomic.Genomic;

public enum ReadMapper {

	bowtie {

		@Override
		public String getIndex(Genomic genomic, ReferenceType t) {
			return genomic.getInfos().get("bowtie-"+(t==ReferenceType.Transcriptomic?"transcriptomic":"genomic"));
		}
		
		@Override
		public boolean isInherentGenomicTranscriptomicMapper() {
			return false;
		}
		
		@Override
		public String getShortReadCommand(ReadMappingReferenceInfo info, String input, String output, String unmapped, int nthreads) {
			return String.format("bowtie -p %d -a -m 100 -v 3 --best --strata %s %s --sam %s %s %s",
				nthreads,info.norc?"--norc":"",unmapped!=null?"--un "+unmapped:"",info.index,input,output);
		}
		
		@Override
		public String getPacBioCommand(ReadMappingReferenceInfo info, String input, String output, String unmapped, int nthreads) {
			throw new RuntimeException("bowtie does not support PacBio reads!");
		}
		
	},
	
	STAR {

		@Override
		public String getIndex(Genomic genomic, ReferenceType t) {
			return genomic.getInfos().get("STAR");
		}
		
		@Override
		public boolean isInherentGenomicTranscriptomicMapper() {
			return true;
		}
		
		@Override
		public String getShortReadCommand(ReadMappingReferenceInfo info, String input, String output, String unmapped, int nthreads) {
			return String.format("STAR --runMode alignReads --runThreadN %d --genomeDir %s --readFilesIn %s --outSAMmode NoQS --outSAMunmapped Within --alignEndsType EndToEnd  --outSAMattributes nM MD  %s\n"
					+ "mv Aligned.out.sam %s %s",
				nthreads,info.index,input,unmapped!=null?"--outReadsUnmapped Fastx":"",output,unmapped!=null?"\nmv Unmapped.out.mate1 "+unmapped:"");
		}
		
		@Override
		public String getPacBioCommand(ReadMappingReferenceInfo info, String input, String output, String unmapped, int nthreads) {
			return String.format("STARlong --runMode alignReads --runThreadN %d --genomeDir %s --readFilesIn %s --outSAMmode NoQS --outSAMunmapped Within --alignEndsType Local  --outSAMattributes nM MD "
					+ "--readNameSeparator space --outFilterMultimapScoreRange 1 --outFilterMismatchNmax 2000 --scoreGapNoncan -20 --scoreGapGCAG -4 --scoreGapATAC -8 --scoreDelOpen -1 --scoreDelBase -1 "
					+ "--scoreInsOpen -1 --scoreInsBase -1 --seedSearchStartLmax 50 --seedPerReadNmax 100000 --alignSJoverhangMin 25"
					+ "--seedPerWindowNmax 1000 --alignTranscriptsPerReadNmax 100000 --alignTranscriptsPerWindowNmax 10000 %s\n"
					+ "mv Aligned.out.sam %s %s",
				nthreads,info.index,input,unmapped!=null?"--outReadsUnmapped Fastx":"",output,unmapped!=null?"\nmv Unmapped.out.mate1 "+unmapped:"");
		}
	};

	public abstract String getIndex(Genomic genomic, ReferenceType t);
	public abstract boolean isInherentGenomicTranscriptomicMapper();
	public abstract String getShortReadCommand(ReadMappingReferenceInfo info, String input, String output, String unmapped, int nthreads);
	public abstract String getPacBioCommand(ReadMappingReferenceInfo info, String input, String output, String unmapped, int nthreads);
}
