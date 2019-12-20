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
package gedi.centeredDiskIntervalTree;

import gedi.core.region.GenomicRegion;
import gedi.util.FileUtils;
import gedi.util.dynamic.DynamicObject;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileWriter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Supplier;

public class InternalCenteredDiskIntervalTreeBuilder<D> extends CenteredDiskIntervalTreeBuilder<D> {

	public static final String MAGIC = "CITI";
	
	private PageFileWriter data;
	
	private ArrayList<String> tmps = new ArrayList<String>();
	private Supplier<PageFileWriter> dataer;
	private long offset = 0;
	
	public InternalCenteredDiskIntervalTreeBuilder(String prefix, DynamicObject globalInfo) throws IOException {
		this(System.getProperty("java.io.tmpdir"),prefix, globalInfo);
	}
	
	
	public InternalCenteredDiskIntervalTreeBuilder(String tmpFolder, String prefix, DynamicObject globalInfo) throws IOException {
		super(true,MAGIC,prefix,tmpFolder);
		dataer = ()->{
			try {
				PageFileWriter re = new PageFileWriter(File.createTempFile(prefix+"."+MAGIC, ".data", new File(tmpFolder)).getPath());
				re.getContext().setGlobalInfo(globalInfo);
				tmps.add(re.getPath());
				return re;
			} catch (IOException e) {
				throw new RuntimeException("Could not create tmp file!",e);
			}
		};
	}
	
	@Override
	public void toDisk() throws IOException {
		super.toDisk();
		if (data!=null) {
			offset+=data.position();
			data.close();
			data = null;
		}
	}
	
	public void add(GenomicRegion region, D data) throws IOException {
		if (this.data==null)
			this.data = dataer.get();
		
		long ptr = this.data.position()+offset;
		if (region.getBoundary(0)>Integer.MAX_VALUE/2 || region.getBoundary(0)<0)
			return;
		
		this.data.putCInt(region.getNumParts());
		int start = region.getBoundary(0);
		this.data.putCInt(start);
		for (int i=1; i<region.getNumBoundaries(); i++)
			this.data.putCInt(region.getBoundary(i)-start);
		FileUtils.serialize(data,this.data);
		
//		data.serialize(this.data);
		add(region,ptr);
	}
	
	public InternalCenteredDiskIntervalTreeBuilder<D> build(PageFileWriter out) throws IOException {
		if (data!=null) {
			data.close();
			data = null;
		}
		
		super.build(out);
		
		for (String path : tmps) {
			PageFile datain = new PageFile(path);
			while (!datain.eof()) {
				out.put(datain.get());
			}
			datain.close();
//		System.err.println("deleting "+data.getPath());
			new File(path).delete();
		}
		
		return this;
	}
	
	
}
