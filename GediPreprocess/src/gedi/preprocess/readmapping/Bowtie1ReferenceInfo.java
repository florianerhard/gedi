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
