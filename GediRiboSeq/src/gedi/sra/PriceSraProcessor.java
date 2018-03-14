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
package gedi.sra;

import java.util.function.Consumer;
import java.util.function.Function;

import gedi.sra.processing.SraProcessor;
import gedi.sra.processing.SraTopLevel;
import gedi.sra.schema.experiment.ExperimentSetType;
import gedi.sra.schema.sample.SampleSetType;
import gedi.sra.schema.study.StudySetType;
import gedi.sra.schema.study.StudyType;
import gedi.util.datastructure.tree.Trie;
import gedi.util.datastructure.tree.Trie.AhoCorasickResult;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.text.LineWriter;

public class PriceSraProcessor implements SraProcessor {

	private static final String[] keywords = {
			"Ribosomal profil",
			"Ribosomal-profil",
			"Ribosomal foot",
			"Ribosomal-foot",
			"Ribosome profil",
			"Ribosome-profil",
			"Ribosome foot",
			"Ribosome-foot",
			"Ribosome protec",
			"Ribosome-protec",
			"Ribo-seq",
			"Ribo seq",
			"Riboseq",
			"RPF-seq",
			"RPFseq",
			" RPF"
			};
	
	private Trie<String> checker;
	
	public PriceSraProcessor() {
		checker = new Trie<>();
		
		for (String k : keywords)
			checker.put(k.toLowerCase(), k);
	}
	
	private static String lc(String s) {
		if (s==null) return null;
		return s.toLowerCase();
	}
	

	@Override
	public boolean process(String sra, Function<SraTopLevel, Object> parser, Function<SraTopLevel, String> src,
			LineWriter out) {
		String srca = src.apply(SraTopLevel.Study);
		ExtendedIterator<AhoCorasickResult<String>> it = checker.iterateAhoCorasick(lc(srca));
		if (!it.hasNext()) {
			
			srca = src.apply(SraTopLevel.Experiment);
			it = checker.iterateAhoCorasick(lc(srca));
			if (it.hasNext()) {
				out.write2("Exp");
				return true;
			}
			return false;
		}
		
		SampleSetType sst = (SampleSetType) parser.apply(SraTopLevel.Sample);
		String org = sst==null?"?":EI.wrap(sst.getSAMPLE()).map(s->s.getSAMPLENAME().getSCIENTIFICNAME()).unique(false).concat(",");
		
		StudySetType ss = (StudySetType) parser.apply(SraTopLevel.Study);
		if (ss!=null)
		for (StudyType s : ss.getSTUDY()) {
			
			org=org+"\t"+s.getAccession();
			
			it = checker.iterateAhoCorasick(lc(s.getDESCRIPTOR().getSTUDYTITLE()));
			if (it.hasNext()) {
				out.write2("Title "+it.next().getValue()+"\t"+org);
				return true;
			}
			
			it = checker.iterateAhoCorasick(lc(s.getDESCRIPTOR().getSTUDYABSTRACT()));
			if (it.hasNext()){
				out.write2("Abstract "+it.next().getValue()+"\t"+org);
				return true;
			}
			
			it = checker.iterateAhoCorasick(lc(s.getDESCRIPTOR().getSTUDYDESCRIPTION()));
			if (it.hasNext()){
				out.write2("Description "+it.next().getValue()+"\t"+org);
				return true;
			}
			
		}
		return false;
	}

}
