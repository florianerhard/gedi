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
import java.util.HashMap;

import gedi.core.genomic.Genomic;
import gedi.util.ParseUtils;
import gedi.util.dynamic.DynamicObject;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;

public class ReadMappingReferenceInfo {

	public boolean norc;
	public String index;
	public String type;
	public int priority;
	public String name;
	public ReadMapper mapper;
	
	private Genomic genomic;
	private ReferenceType refType;
	
	
	public ReadMappingReferenceInfo(ReferenceType t, Genomic genomic, ReadMapper mapper) {
		this(t,genomic,t.prio,mapper);
	}

	public ReadMappingReferenceInfo(ReferenceType t, Genomic genomic, int prio, ReadMapper mapper) {
		this.genomic = genomic;
		this.refType = t;
		this.name = genomic.getId()+ "("+refType+")";
		priority = prio;
		norc = t.norc;
		type = genomic.getId()+"."+t.name();
		index = mapper.getIndex(genomic,t);
		this.mapper = mapper;
	}


	
	@Override
	public String toString() {
		return "ReferenceInfo [norc=" + norc + ", index=" + index
				+ ", type=" + type + ", priority=" + priority + "]";
	}
	
	public static void writeTable(String path, ArrayList<ReadMappingReferenceInfo> l, boolean prio, boolean skip_prio1) throws IOException {
		try (LineWriter lw = new LineOrientedFile(path).write()) {
			lw.writeLine("File\tGenome\tTranscriptomic\tPriority");
			for (ReadMappingReferenceInfo r : l)
				if (!skip_prio1 || r.priority>1)
					lw.writef("%s.sam\t%s\t%b\t%d\n", r.type, r.genomic.getId(), r.refType==ReferenceType.Transcriptomic,prio?r.priority:2);
		}
	}
	
	@SuppressWarnings("restriction")
	public static ArrayList<ReadMappingReferenceInfo> writeTable(String path, jdk.nashorn.api.scripting.JSObject descriptor, boolean prio, boolean skip_prio1, ReadMapper mapper) throws IOException {
		return writeTable(path, DynamicObject.from(descriptor), prio, skip_prio1,mapper);
	}
	
	public static ArrayList<ReadMappingReferenceInfo> writeTable(String path, DynamicObject descriptor, boolean prio, boolean skip_prio1, ReadMapper mapper) throws IOException {
		ArrayList<ReadMappingReferenceInfo> infos = new ArrayList<>();
		for (String r : descriptor.getProperties()) {
			Genomic genomic = Genomic.get(r);
			DynamicObject val = descriptor.getEntry(r);
			if (val.isString()) {
				ReferenceType type = ParseUtils.parseEnumNameByPrefix(val.asString(), true, ReferenceType.class);
				ReadMapper cmapper = type==ReferenceType.rRNA?ReadMapper.bowtie:mapper;
				
				if (type==ReferenceType.Both && !cmapper.isInherentGenomicTranscriptomicMapper()) {
					ReadMappingReferenceInfo ri = new ReadMappingReferenceInfo(ReferenceType.Genomic,genomic,cmapper);
					if (ri.index!=null)
						infos.add(ri);
					ri = new ReadMappingReferenceInfo(ReferenceType.Transcriptomic,genomic,cmapper);
					if (ri.index!=null)
						infos.add(ri);
					
				} else {
					ReadMappingReferenceInfo ri = new ReadMappingReferenceInfo(type,genomic,cmapper);
					if (ri.index!=null)
						infos.add(ri);
				}
			}
			else if (val.isObject()){
				// object like {"genomic": 2, "transcriptomic": 3}
				for (String t : val.getProperties()) {
					ReferenceType type = ParseUtils.parseEnumNameByPrefix(t, true, ReferenceType.class);
					int pr = val.getEntry(t).asInt();
					ReadMapper cmapper = type==ReferenceType.rRNA?ReadMapper.bowtie:mapper;
					ReadMappingReferenceInfo ri = new ReadMappingReferenceInfo(type,genomic,pr,cmapper);
					if (ri.index!=null)
						infos.add(ri);
				}
				
			} else {
				throw new RuntimeException("Unknown reference descriptor: "+descriptor.toJson()+"\nExpected either a ReferenceType or an object like {\"genomic\": 2, \"transcriptomic\": 3}");
			}
			
		}

		
		try (LineWriter lw = new LineOrientedFile(path).write()) {
			lw.writeLine("File\tGenome\tTranscriptomic\tPriority");
			for (ReadMappingReferenceInfo r : infos)
				if (!skip_prio1 || r.priority>1)
					lw.writef("%s.sam\t%s\t%b\t%d\n", r.type, r.genomic.getId(), r.refType==ReferenceType.Transcriptomic,prio?r.priority:2);
		}
		return infos;
	}
}
