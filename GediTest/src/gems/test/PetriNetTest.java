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
package gems.test;

import gedi.core.data.mapper.GenomicRegionDataMapper;
import gedi.core.data.mapper.GenomicRegionDataMappingJob;
import gedi.core.data.mapper.SequenceSource;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.sequence.FastaIndexSequenceProvider;
import gedi.util.io.text.fasta.index.FastaIndexFile;
import gedi.util.job.ExecutionContext;
import gedi.util.job.FunctionJobAdapter;
import gedi.util.job.PetriNet;
import gedi.util.job.Place;
import gedi.util.job.Transition;
import gedi.util.job.schedule.DefaultPetriNetScheduler;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class PetriNetTest {

	
	
	@Test
	public void sequenceTest() throws IOException {
		String prov = "/mnt/biostor1/Data/Databases/GENOMES/Homo/bowtie_index/hg19.fi";
		
		PetriNet pn = new PetriNet();
		Place seq = pn.createPlace(CharSequence.class);
		Place seq2 = pn.createPlace(CharSequence.class);
		Place seq3 = pn.createPlace(CharSequence.class);
		pn.connect(pn.createTransition(new GenomicRegionDataMappingJob<Void, CharSequence>(source(prov))),
				seq);
		pn.connect(pn.createTransition(new GenomicRegionDataMappingJob<Void, CharSequence>(source(prov))),
				seq2);
		pn.connect(pn.createTransition(new GenomicRegionDataMappingJob<Void, CharSequence>(source(prov))),
				seq3);
		Transition out = pn.createTransition(new FunctionJobAdapter<Void>(CharSequence.class, Void.class, s->{assertEquals("GGGCACAGCCTCACCCAGGAAAGCAGCTGGGGGTCCACTGGGCTCAGGGAAGACCCCCTGCCAGGGAGACCCCAGGCGCCTGAATGGCCACGGGAAGGAA", s.get(0).toString());return null;}));
		pn.connect(seq,out,0);
		
		Transition concat = pn.createTransition(new FunctionJobAdapter<CharSequence>(new Class[] {CharSequence.class,CharSequence.class}, CharSequence.class, s->s.<String>get(0)+s.<String>get(1)));
		pn.connect(seq2,concat,0);
		pn.connect(seq3,concat,1);
		pn.connect(concat, pn.createPlace(CharSequence.class));
		Transition copy1 =  pn.createTransition(new FunctionJobAdapter<CharSequence>(CharSequence.class, CharSequence.class, s->s.<String>get(0)));
		pn.connect(copy1, concat.getOutput());
		pn.connect(seq, copy1, 0);
		
		pn.createMissingPlaces();
		
//		System.out.println(pn);
		
		ExecutionContext context = pn.createExecutionContext();
		context.newContext(GenomicRegionDataMappingJob.REFERENCE, ReferenceSequence.class);
		context.newContext(GenomicRegionDataMappingJob.REGION, GenomicRegion.class);
		context.setContext(GenomicRegionDataMappingJob.REFERENCE, Chromosome.obtain("chr1+"));
		context.setContext(GenomicRegionDataMappingJob.REGION, GenomicRegion.parse("1000000-1000100"));
		
		ExecutorService pool = Executors.newCachedThreadPool();
		
		
		DefaultPetriNetScheduler runner = new DefaultPetriNetScheduler(context,pool);
		runner.run();
		
		assertEquals("GGGCACAGCCTCACCCAGGAAAGCAGCTGGGGGTCCACTGGGCTCAGGGAAGACCCCCTGCCAGGGAGACCCCAGGCGCCTGAATGGCCACGGGAAGGAA", context.getToken(pn.getPlaces().get(3)));
		
		pool.shutdown();
	}

	private GenomicRegionDataMapper<Void, CharSequence> source(String prov) throws IOException {
		SequenceSource re = new SequenceSource();
		re.addFastaIndex(prov);
		return re;
	}
}
