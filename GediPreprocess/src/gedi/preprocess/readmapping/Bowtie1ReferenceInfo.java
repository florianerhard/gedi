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

package gedi.preprocess.readmapping;

import java.io.IOException;
import java.util.ArrayList;

import gedi.core.genomic.Genomic;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;

public class Bowtie1ReferenceInfo {

	public boolean norc;
	public String index;
	public String type;
	public int priority;
	
	private Genomic genomic;
	private ReferenceType refType;
	
	
	public Bowtie1ReferenceInfo(ReferenceType t, Genomic genomic) {
		this.genomic = genomic;
		this.refType = t;
		priority = t.prio;
		norc = t.norc;
		type = genomic.getId()+"."+t.name();
		index = genomic.getInfos().get("bowtie-"+(t==ReferenceType.Transcriptomic?"transcriptomic":"genomic"));
	}


	@Override
	public String toString() {
		return "ReferenceInfo [norc=" + norc + ", index=" + index
				+ ", type=" + type + ", priority=" + priority + "]";
	}
	
	public static void writeTable(String path, ArrayList<Bowtie1ReferenceInfo> l, boolean prio, boolean skip_prio1) throws IOException {
		try (LineWriter lw = new LineOrientedFile(path).write()) {
			lw.writeLine("File\tGenome\tTranscriptomic\tPriority");
			for (Bowtie1ReferenceInfo r : l)
				if (!skip_prio1 || r.priority>1)
					lw.writef("%s.sam\t%s\t%b\t%d\n", r.type, r.genomic.getId(), r.refType==ReferenceType.Transcriptomic,prio?r.priority:2);
		}
	}
}
