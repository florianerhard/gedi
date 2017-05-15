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

package gedi.util.io.text.jhp.display;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

import executables.Display;
import executables.Template;
import gedi.core.data.annotation.ScoreNameAnnotation;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.GenomicRegionStoragePreload;
import gedi.core.workspace.loader.PreloadInfo;
import gedi.core.workspace.loader.WorkspaceItemLoader;
import gedi.startup.CoreStartup;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.EI;
import gedi.util.genomic.GediViewItem;
import gedi.util.io.text.jhp.TemplateEngine;
import gedi.util.io.text.jhp.TemplateGenerator;
import gedi.util.io.text.tsv.formats.BedFileLoader;
import gedi.util.io.text.tsv.formats.GediViewFileLoader;
import gedi.util.mutable.MutableDouble;

public class GediViewItemsDisplayTemplateGenerator implements AutoTemplateGenerator<PreloadInfo<?, Void>,GediViewItemsDisplayTemplateGenerator> {

	private static final Logger log = Logger.getLogger( GediViewItemsDisplayTemplateGenerator.class.getName() );
	
	
	/**
	 * For auto inclusion in {@link CoreStartup}
	 */
	public static final Class<?> cls = GediViewItem[].class;
	
	
	private String path;
	
	public GediViewItemsDisplayTemplateGenerator(String path) {
		this.path = path;
	}

	@Override
	public void accept(TemplateEngine t, GediViewItemsDisplayTemplateGenerator[] g) {
		Runnable res = t.save("paths","ids","id","input","totals","numConditions","mapping","colors","names");

		
		GediViewItem[] items = EI.wrap(g).demultiplex(gen->{
			try {
				return EI.wrap(new GediViewFileLoader().load(Paths.get(gen.path)));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}).toArray(GediViewItem.class);
		
		// add storages
		String[] uniqFiles = EI.wrap(items).map(tr->tr.getFile()).unique(false).toArray(String.class);
		String[] ids = EI.wrap(uniqFiles).map(path->FileUtils.getFullNameWithoutExtension(path).replace("/", "_")).toArray(String.class);
		t.set("paths", uniqFiles);
		t.set("ids", ids);
		t.template("gediviewitemsstorages");
		
		// add merger,transformers and tracks
		LinkedHashMap<String, ArrayList<GediViewItem>> tracks = new LinkedHashMap<>();
		for (GediViewItem it : items)
			tracks.computeIfAbsent(it.getTrack(), tr->new ArrayList<>()).add(it);
		
		for (String trackName : tracks.keySet()) {
			ArrayList<GediViewItem> track = tracks.get(trackName);
			LinkedHashMap<String, Integer> labelIndex = new LinkedHashMap<>();
			for (GediViewItem it : track)
				labelIndex.computeIfAbsent(it.getLabel(), l->labelIndex.size());
			
			uniqFiles = EI.wrap(track).map(tr->tr.getFile()).map(path->FileUtils.getFullNameWithoutExtension(path).replace("/", "_")).unique(false).toArray(String.class);
			HashMap<String, Integer> uniqIndex = ArrayUtils.createIndexMap(uniqFiles);
			
			double[] totals = new double[track.size()];
			for (GediViewItem item : track)
				totals[labelIndex.get(item.getLabel())]+=item.getTotal();
		
			int[] numConditions = new int[uniqFiles.length];
			for (GediViewItem item : track)
				numConditions[uniqIndex.get(FileUtils.getFullNameWithoutExtension(item.getFile()).replace("/", "_"))]=item.getNumCondition();

			t.set("id", trackName);
			t.set("input", uniqFiles);
			t.set("totals", totals);
			t.set("numConditions", numConditions);
			t.set("mapping", EI.wrap(track).map(gi->new int[] {uniqIndex.get(FileUtils.getFullNameWithoutExtension(gi.getFile()).replace("/", "_")),gi.getCondition(),labelIndex.get(gi.getLabel())}).list());
			t.template("gediviewitems");
		
			t.push(Display.CPS_ID);
			
			String[] colors = new String[track.size()];
			for (int i=track.size()-1; i>=0; i--)
				colors[labelIndex.get(track.get(i).getLabel())]=track.get(i).getColor();
			t.set("colors", colors);
			t.set("names", labelIndex.keySet().toArray(new String[0]));
			t.template("alignedreadsdatagenomicregion.cps");
			t.pop();
			
		}
		res.run();
	}

	@Override
	public double applyAsDouble(PreloadInfo<?, Void> pre) {
		return 1;
	}

}
