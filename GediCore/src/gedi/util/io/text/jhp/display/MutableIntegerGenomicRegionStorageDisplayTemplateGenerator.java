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

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.data.annotation.NameProvider;
import gedi.core.data.annotation.ScoreNameAnnotation;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.GenomicRegionStoragePreload;
import gedi.core.workspace.loader.PreloadInfo;
import gedi.core.workspace.loader.WorkspaceItemLoader;
import gedi.startup.CoreStartup;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.functions.EI;
import gedi.util.io.text.jhp.TemplateEngine;
import gedi.util.io.text.jhp.TemplateGenerator;
import gedi.util.io.text.tsv.formats.BedFileLoader;
import gedi.util.mutable.MutableInteger;

public class MutableIntegerGenomicRegionStorageDisplayTemplateGenerator implements AutoTemplateGenerator<PreloadInfo<?, GenomicRegionStoragePreload>,MutableIntegerGenomicRegionStorageDisplayTemplateGenerator> {

	
	/**
	 * For auto inclusion in {@link CoreStartup}
	 */
	public static final Class<?> cls = GenomicRegionStorage.class;
	
	
	private String path;
	
	public MutableIntegerGenomicRegionStorageDisplayTemplateGenerator(String path) {
		this.path = path;
	}

	@Override
	public void accept(TemplateEngine t, MutableIntegerGenomicRegionStorageDisplayTemplateGenerator[] g) {
		Runnable res = t.save("paths","id");
		t.set("paths", EI.wrap(g).map(te->te.path).toArray(String.class));
		t.set("id", path);
		t.template("numericarraygenomicregion");
		res.run();
	}

	@Override
	public double applyAsDouble(PreloadInfo<?, GenomicRegionStoragePreload> pre) {
		GenomicRegionStoragePreload value = pre.getInfo();
		if (Number.class.isAssignableFrom(value.getType())) return 1;
		return -1;
	}

}
