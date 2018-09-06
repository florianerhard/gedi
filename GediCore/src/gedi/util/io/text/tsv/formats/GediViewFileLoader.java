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
package gedi.util.io.text.tsv.formats;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import gedi.core.workspace.loader.WorkspaceItemLoader;
import gedi.util.PaintUtils;
import gedi.util.StringUtils;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.EI;
import gedi.util.genomic.GediViewItem;
import gedi.util.gui.ColorPalettes;

public class GediViewFileLoader implements WorkspaceItemLoader<GediViewItem[],Void> {

	public static final String[] extensions = new String[]{"gediview"};
	
	@Override
	public String[] getExtensions() {
		return extensions;
	}

	@Override
	public GediViewItem[] load(Path path)
			throws IOException {
		Comparator<PreGediViewItem> trackcomp = (a,b)->a.track.compareTo(b.track);
		PreGediViewItem[] a = new CsvReaderFactory().createReader(path)
			.iterateObjects(PreGediViewItem.class)
			.toArray(PreGediViewItem.class);
		
		LinkedHashMap<String, ArrayList<PreGediViewItem>> map = new LinkedHashMap<>();
		for (PreGediViewItem i : a) 
			map.computeIfAbsent(i.track, x->new ArrayList<>()).add(i);
		
		ArrayList<GediViewItem> re = new ArrayList<>();
		for (String track : map.keySet()) 
			EI.wrap(convert(map.get(track).toArray(new PreGediViewItem[0]))).toCollection(re);
		
		return re.toArray(new GediViewItem[0]);
		
//			.multiplex(trackcomp,PreGediViewItem.class)
//			.demultiplex(this::convert)
//			.toArray(GediViewItem.class);
	}
	
	private Iterator<GediViewItem> convert(PreGediViewItem[] pres) {
		GediViewItem[] re = new GediViewItem[pres.length];
		for (int j = 0; j < pres.length; j++) {
			String[] p = StringUtils.split(pres[j].condition, '/');
			if (p.length!=2 || !StringUtils.isInt(p[0]) || !StringUtils.isInt(p[1]))
				throw new RuntimeException("Not a valid condition: "+pres[j].condition+", should be 2/4 or similar!");
			int cond = Integer.parseInt(p[0]);
			int numCond = Integer.parseInt(p[1]);
			
			re[j] = new GediViewItem(pres[j].file, cond, numCond, pres[j].total, pres[j].label, pres[j].track, pres[j].color, DynamicObject.parseJson(pres[j].options));
		}
		return EI.wrap(re);
	}
	
	@Override
	public Void preload(Path path) throws IOException {
		return null;
	}


	@Override
	public Class<GediViewItem[]> getItemClass() {
		return GediViewItem[].class;
	}

	@Override
	public boolean hasOptions() {
		return false;
	}

	@Override
	public void updateOptions(Path path) {
	}

	
	private static class PreGediViewItem {
		private String file;
		private String condition;
		private double total;
		private String label;
		private String track;
		private String color;
		private String options;
	}
	
}
