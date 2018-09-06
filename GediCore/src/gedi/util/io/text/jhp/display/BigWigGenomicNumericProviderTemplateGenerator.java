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

import gedi.core.data.numeric.BigWigGenomicNumericProvider;
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericProvider;
import gedi.core.workspace.loader.PreloadInfo;
import gedi.startup.CoreStartup;
import gedi.util.functions.EI;
import gedi.util.io.text.jhp.TemplateEngine;

public class BigWigGenomicNumericProviderTemplateGenerator implements AutoTemplateGenerator<PreloadInfo<?, Void>,BigWigGenomicNumericProviderTemplateGenerator> {

	
	/**
	 * For auto inclusion in {@link CoreStartup}
	 */
	public static final Class<?> cls = BigWigGenomicNumericProvider.class;
	
	
	private String path;
	
	public BigWigGenomicNumericProviderTemplateGenerator(String path) {
		this.path = path;
	}

	@Override
	public void accept(TemplateEngine t, BigWigGenomicNumericProviderTemplateGenerator[] g) {
		Runnable res = t.save("paths","id");
		t.set("paths", EI.wrap(g).map(te->te.path).toArray(String.class));
		t.set("id", path);
		t.template("bigwiggenomicnumericprovider");
		res.run();
	}

	@Override
	public double applyAsDouble(PreloadInfo<?, Void> pre) {
		return 1;
	}

}
