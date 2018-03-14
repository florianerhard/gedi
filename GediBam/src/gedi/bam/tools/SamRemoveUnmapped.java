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
package gedi.bam.tools;

import gedi.util.FunctorUtils;
import gedi.util.io.text.LineOrientedFile;

import java.util.Iterator;
import java.util.function.UnaryOperator;

import htsjdk.samtools.SAMRecord;

public class SamRemoveUnmapped implements UnaryOperator<Iterator<SAMRecord>>, AutoCloseable {

	private LineOrientedFile out;
	
	public SamRemoveUnmapped() {
	}

	public SamRemoveUnmapped(LineOrientedFile out) {
		this.out = out;
	}

	public SamRemoveUnmapped(String path) {
		this.out = new LineOrientedFile(path);
	}

	public Iterator<SAMRecord> apply(Iterator<SAMRecord> it) {
		return FunctorUtils.filteredIterator(it,r->{
			if (!r.getReadUnmappedFlag())
				return true;
			
			if (out!=null) {
				try {
					if (!out.isWriting()) out.startWriting();
					out.writef(">%s\n%s\n",r.getReadName(),r.getReadString());
				} catch (Exception e) {
					throw new RuntimeException("Could not write unmapped reads!",e);
				}
				
			}
			return !r.getReadUnmappedFlag();
		});
	}

	@Override
	public void close() throws Exception {
		if (out!=null && out.isWriting())
			out.finishWriting();
	}
	
	
}