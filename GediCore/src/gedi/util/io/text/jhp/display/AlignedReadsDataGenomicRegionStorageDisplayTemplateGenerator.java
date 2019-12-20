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
import java.util.ArrayList;
import java.util.Arrays;
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
import gedi.util.FileUtils;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.EI;
import gedi.util.io.text.jhp.TemplateEngine;
import gedi.util.io.text.jhp.TemplateGenerator;
import gedi.util.io.text.tsv.formats.BedFileLoader;

public class AlignedReadsDataGenomicRegionStorageDisplayTemplateGenerator implements AutoTemplateGenerator<PreloadInfo<?, GenomicRegionStoragePreload>,AlignedReadsDataGenomicRegionStorageDisplayTemplateGenerator> {

	private static final Logger log = Logger.getLogger( AlignedReadsDataGenomicRegionStorageDisplayTemplateGenerator.class.getName() );
	
	
	/**
	 * For auto inclusion in {@link CoreStartup}
	 */
	public static final Class<?> cls = GenomicRegionStorage.class;
	
	
	private String path;
	private PreloadInfo<GenomicRegionStorage<? extends AlignedReadsData>, GenomicRegionStoragePreload<AlignedReadsData>> pre;
	
	public AlignedReadsDataGenomicRegionStorageDisplayTemplateGenerator(String path, PreloadInfo<GenomicRegionStorage<? extends AlignedReadsData>, GenomicRegionStoragePreload<AlignedReadsData>> pre) {
		this.path = path;
		this.pre = pre;
	}

	@Override
	public void accept(TemplateEngine t, AlignedReadsDataGenomicRegionStorageDisplayTemplateGenerator[] g) {
		Runnable res = t.save("paths","id","file","totals","colors","names");
		
		// write oml
		t.set("paths", EI.wrap(g).map(te->te.path).toArray(String.class));
		if (t.get("id")==null)
			t.set("id", FileUtils.getNameWithoutExtension(path).replace("/", "_"));
		
		DoubleArrayList totals = new DoubleArrayList();
		for (int i=0; i<g.length; i++) {
			DynamicObject[] tot = g[i].pre.getInfo().getMetaData().getEntry("conditions").asArray();
			if (tot.length>0) {
				double[] ttots = EI.wrap(tot).mapToDouble(d->d.getEntry("sizefactor").asDouble(Double.NaN)).toDoubleArray();
				boolean nosf = false;
				for (int c=0; c<ttots.length; c++) 
					if (Double.isNaN(ttots[c])) {
						nosf=true;
					} else ttots[c]=1E6*ttots[c];
				
				if (nosf) {
					ttots = EI.wrap(tot).mapToDouble(d->d.getEntry("total").asDouble(Double.NaN)).toDoubleArray();
					for (int c=0; c<ttots.length; c++) 
						if (Double.isNaN(ttots[c])) {
							ttots[c] = 1_000_000;
							log.warning("No total count/size factor info for "+g[i].pre.getInfo().getMetaData().get("conditions").getEntry(c).getEntry("name").asString(""+c)+" in "+g[i].path);
						}
				}
				totals.addAll(ttots);
			} else {
				int num = g[i].pre.getInfo().getExample().getNumConditions();
				for (int j=0; j<num; j++) {
					totals.add(1_000_000);
				}
				log.warning("No total count/size factor info in "+g[i].path);
			}
		}
		
		if (t.get("totals")==null) 
			t.set("totals", totals.toDoubleArray());
			
		t.template("alignedreadsdatagenomicregion");
		
		// write cps
		t.push(Display.CPS_ID);
		ArrayList<String> names = new ArrayList<>();
		ArrayList<String> colors = new ArrayList<>();
		ArrayList<String> files = new ArrayList<>();
		for (int i=0; i<g.length; i++) {
			DynamicObject[] cond = g[i].pre.getInfo().getMetaData().getEntry("conditions").asArray();
			if (cond.length>0) {
				String[] tnames = EI.wrap(cond).map(d->d.getEntry("name").asString(null)).toArray(String.class);
				for (int c=0; c<tnames.length; c++) if (tnames[c]==null) tnames[c] = (names.size()+c)+"";
				names.addAll(Arrays.asList(tnames));
				
				EI.repeat(cond.length, g[i].path).toCollection(files);
				EI.wrap(cond).map(d->d.getEntry("color").asString(null)).toCollection(colors);
			} else {
				int num = g[i].pre.getInfo().getExample().getNumConditions();
				for (int j=0; j<num; j++) {
					names.add("C"+names.size());
					colors.add(null);
				}
				EI.repeat(num, g[i].path).toCollection(files);
			}
		}
		if (EI.wrap(colors).filter(s->s!=null).count()>0) { 
			for (int c=0; c<colors.size(); c++) if (colors.get(c)==null) colors.set(c, "black");
		} else
			colors = null;
		
		if (colors!=null && t.get("colors")==null) 
			t.set("colors", colors.toArray(new String[0]));
		
		if (t.get("names")==null) {
			t.set("names", names.toArray(new String[0]));
		}
		
		t.template("alignedreadsdatagenomicregion.cps");
		t.pop();
		
		// write list
		t.push(Display.STORAGE_ID);
		DynamicObject sl = DynamicObject.arrayOfObjects("name",names)
			.cascade(DynamicObject.arrayOfObjects("total",totals))
			.cascade(DynamicObject.arrayOfObjects("file",files));
		
		if (colors==null)
			sl = sl.cascade(DynamicObject.arrayOfObjects("color",EI.repeat(names.size(), "Dark2").toArray(String.class)));
		else
			sl = sl.cascade(DynamicObject.arrayOfObjects("color",colors));
		
		t.parameter("items", sl);
		
		t.template("alignedreadsdatagenomicregion.table");
		t.pop();
		
		res.run();
	}

	@Override
	public double applyAsDouble(PreloadInfo<?, GenomicRegionStoragePreload> pre) {
		GenomicRegionStoragePreload value = pre.getInfo();
		if (AlignedReadsData.class.isAssignableFrom(value.getType())) return 1;
		return -1;
	}

}
