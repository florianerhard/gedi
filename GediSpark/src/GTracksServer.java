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
import java.io.IOException;
import java.util.LinkedHashMap;

import gedi.app.Gedi;
import gedi.core.data.annotation.CompositeReferenceSequenceLengthProvider;
import gedi.core.data.annotation.CompositeReferenceSequencesProvider;
import gedi.core.data.annotation.ReferenceSequenceLengthProvider;
import gedi.core.data.annotation.ReferenceSequencesProvider;
import gedi.core.data.mapper.GenomicRegionDataMappingJob;
import gedi.core.reference.LazyGenome;
import gedi.gui.genovis.TracksDataManager;
import gedi.util.job.Transition;
import gedi.util.oml.petrinet.Pipeline;

import static spark.Spark.*;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.StdErrLog;

public class GTracksServer {

	// should be there per user!
	// or at least some way to prevent canceling the petri net from another user
	private TracksDataManager data;
	private LinkedHashMap<String, Integer> genome;

	public GTracksServer() throws IOException {
		data = new TracksDataManager(Pipeline.fromOml("/resources/test/testseq.oml").getPetriNet());
		getGenome();
	}
	public TracksDataManager getData() {
		return data;
	}

	public LinkedHashMap<String, Integer> getGenome() {
		if (genome==null) {
			CompositeReferenceSequenceLengthProvider lengths = new CompositeReferenceSequenceLengthProvider();
			CompositeReferenceSequencesProvider refs = new CompositeReferenceSequencesProvider();
			for (Transition t : data.getDataPipeline().getTransitions()) {
				if (t.getJob() instanceof GenomicRegionDataMappingJob) {
					GenomicRegionDataMappingJob<?,?> j = (GenomicRegionDataMappingJob<?,?>) t.getJob();
					j.getMapper().applyForAll(ReferenceSequencesProvider.class, p->refs.add(p));
					j.getMapper().applyForAll(ReferenceSequenceLengthProvider.class, p->lengths.add(p));
				}
			}
			LazyGenome lg = new LazyGenome(refs,lengths);
			LinkedHashMap<String, Integer> genome = new LinkedHashMap<>();
			for (String v : lg.getReferenceSequences())
				genome.put(v, lg.getLength(v));
			this.genome = genome;
		}
		
		return genome;
	}

    public static void main(String[] args) throws IOException {
    	Gedi.startup(true);
        staticFiles.location("/resources/test"); //index.html is served at localhost:4567 (default port)
        staticFiles.expireTime(600);
        webSocket("/test", new GTracksWebSocketHandler(new GTracksServer()));
        webSocket("/test2", new TestWebSocketHandler());
        init();
    }



}
