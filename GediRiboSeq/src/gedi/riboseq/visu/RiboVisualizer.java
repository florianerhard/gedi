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

package gedi.riboseq.visu;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import executables.RiboView;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.genomic.Genomic;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.core.workspace.loader.WorkspaceItemLoaderExtensionPoint;
import gedi.gui.genovis.SwingGenoVisViewer;
import gedi.gui.genovis.VisualizationTrack;
import gedi.util.FileUtils;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.EI;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.jhp.Jhp;
import gedi.util.nashorn.JS;
import gedi.util.oml.OmlNodeExecutor;
import gedi.util.oml.OmlReader;
import gedi.util.oml.cps.CpsReader;
import gedi.util.oml.petrinet.Pipeline;

public class RiboVisualizer {

	private static final Logger logger = Logger.getLogger(RiboVisualizer.class.getName());
	
	private Genomic g;
	private SwingGenoVisViewer viewer;


	@SuppressWarnings({ "unchecked", "rawtypes", "resource" })
	public RiboVisualizer(String genomic, String[] folders) throws IOException {
		this.g = Genomic.get(genomic);
		
		// look in folders
		ArrayList<String> prefCreate = new ArrayList<String>();
		ArrayList<String> totalCreate = new ArrayList<String>();
		for (String f : folders) {
			for (String orfFile : EI.wrap(new File(f).list()).filter(p->p.endsWith(".orfs.cit") && !p.contains(".tmp.")).loop()) {
				String pref = new File(new File(f),orfFile.substring(0,orfFile.length()-".orfs.cit".length())).getPath();
				prefCreate.add(pref);
				File meta = new File(new File(f),orfFile+".metadata.json");
				totalCreate.add(meta.getPath());
			}
		}
		String[] prefix = prefCreate.toArray(new String[0]);
		String[] totalCountFile = totalCreate.toArray(new String[0]);
		
		String[][] names = new String[prefix.length][];
		String[][] condRmq = new String[prefix.length][];
		double[][] totals = new double[prefix.length][];
		String[] experimentNames = new String[prefix.length];
		String[] models = new String[prefix.length];
		
		HashMap context = new HashMap();
		context.put("genomic", g);
		
		
		for (int e=0; e<prefix.length; e++) {
			experimentNames[e] = FileUtils.getFullNameWithoutExtension(prefix[e]);
			models[e] = prefix[e]+".model";
			
			DynamicObject json = DynamicObject.parseJson(new LineOrientedFile(totalCountFile[e]).readAllText2());
			totals[e] = EI.wrap(json.getEntry("conditions").asArray()).mapToDouble(c->Math.max(1, c.getEntry("total").asDouble())).toDoubleArray();
			names[e] = EI.wrap(json.getEntry("conditions").asArray()).map(c->c.getEntry("name").asString()).toArray(String.class);
			
			String uprefix = prefix[e];
			condRmq[e] = EI.seq(0,names[e].length).map(s->uprefix+"."+s+".codons.rmq").toArray(String.class);
			
			Path p = Paths.get(prefix[e]+".orfs.cit");
			GenomicRegionStorage<?> orfs = ((GenomicRegionStorage<?>) WorkspaceItemLoaderExtensionPoint.getInstance().get(p).load(p)).toMemory();
			context.put("orfs"+e, orfs);

			p = Paths.get(new File(p.toFile().getAbsoluteFile().getParentFile().getParentFile().getPath(),FileUtils.getNameWithoutExtension(prefix[e])+".cit").getAbsolutePath());
			
			GenomicRegionStorage<AlignedReadsData> reads;
			if (p.toFile().exists())
				reads = ((GenomicRegionStorage<AlignedReadsData>) WorkspaceItemLoaderExtensionPoint.getInstance().get(p).load(p));
			else
				reads = new MemoryIntervalTreeStorage<AlignedReadsData>(AlignedReadsData.class);
			context.put("reads"+e, reads);

		}
			
		JS js = new JS();
		js.putVariable("mergedrmq", EI.wrap(prefix).map(s->s+".codons.rmq").toArray(String.class));
		js.putVariable("tracks", new String[0]);
		js.putVariable("names", names);
		js.putVariable("totals", totals);
		js.putVariable("conditionrmq", condRmq);
		js.putVariable("experimentNames", experimentNames);
		js.putVariable("models", models);
		
		InputStream stream = RiboView.class.getResourceAsStream("pricevis2.oml.jhp");
		
		String src = new LineIterator(stream).concat("\n");
		Jhp jhp = new Jhp(js);
		src = jhp.apply(src);
		
		String cps = new LineIterator(RiboView.class.getResourceAsStream("/resources/colors.cps")).concat("\n");
		Pipeline pipeline = (Pipeline)new OmlNodeExecutor().addInterceptor(new CpsReader().parse(cps)).execute(new OmlReader().setJs(js).parse(src),context);
		
		viewer = new SwingGenoVisViewer(pipeline.getPetriNet());
		for (VisualizationTrack track : pipeline.getTracks())
			viewer.addTrack(track);
	}
	
	
	public SwingGenoVisViewer getViewer(String location) {
		viewer.setScreenshotMode(false);
		ReferenceGenomicRegion reg = ImmutableReferenceGenomicRegion.parse(g, location).toMutable().toStrandIndependent();
		viewer.setLocation(reg.getReference(),reg.getRegion());
		return viewer;
	}
	
	public BufferedImage getImage(String location, int width) {
		ImmutableReferenceGenomicRegion<Object> reg = ImmutableReferenceGenomicRegion.parse(g, location);
		
		HashSet<String> visible = new HashSet<String>();
		for (VisualizationTrack<?, ?> track : viewer.getTracks()) {
			if (!track.isHidden()) {
				visible.add(track.getId());
				if (track.getId().endsWith("Editor") || !track.getId().startsWith(reg.getReference().getStrand().toString()))
					track.setHidden(true);
			}
		}
		
		viewer.setScreenshotMode(true);
		viewer.setSize(new Dimension(width,viewer.getPreferredSize().height));
		viewer.validate();
		viewer.setLocation(reg.getReference().toStrandIndependent(),reg.getRegion(),false);
		BufferedImage re = viewer.getImage();
		
		for (String vis : visible)
			viewer.getTrack(vis).setHidden(false);
		
		return re;
	}

	
	public static class RiboViewCache {
		
		Map<List<String>,SoftReference<RiboVisualizer>> cache = Collections.synchronizedMap(new HashMap<>());

		public RiboVisualizer getOrCreate(String genomic, String[] folders) throws IOException {
			ArrayList<String> key = new ArrayList<>(Arrays.asList(folders));
			key.add(genomic);
			SoftReference<RiboVisualizer> ref = cache.get(key);
			RiboVisualizer re = null;
			if (ref==null || (re=ref.get())==null) {
				if (ref==null)
					logger.fine("First use!");
				else
					logger.fine("Was removed from cache!");
				re = new RiboVisualizer(genomic, folders);
				cache.put(key, new SoftReference<RiboVisualizer>(re));
			} else
				logger.fine("Used from cache!");
			
			// get rid of all removed entries
			Iterator<List<String>> it = cache.keySet().iterator();
			while (it.hasNext()) {
				if (cache.get(it.next()).get()==null)
					it.remove();
			}
			return re;
		}
		
	}
	
	
	public static BufferedImage getRiboImage(String genomic, String[] folders, String location, int width, RiboViewCache cache) throws IOException {
		RiboVisualizer visu = cache.getOrCreate(genomic,folders);
		return visu.getImage(location, width);
		
	}
	
	
}
