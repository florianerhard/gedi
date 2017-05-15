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
			if (unmapped!=null) throw new RuntimeException("STAR does not support output of unmapped reads in fastq!");
			return String.format("STAR --runMode alignReads --runThreadN %d --genomeDir %s --readFilesIn %s --outSAMmode NoQS --outSAMunmapped Within --alignEndsType EndToEnd  --outSAMattributes nM MD\n"
					+ "mv Aligned.out.sam %s",
				nthreads,info.index,input,output);
		}
	};

	public abstract String getIndex(Genomic genomic, ReferenceType t);
	public abstract boolean isInherentGenomicTranscriptomicMapper();
	public abstract String getShortReadCommand(ReadMappingReferenceInfo info, String input, String output, String unmapped, int nthreads);
}
