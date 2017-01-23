package gedi.preprocess.readmapping;

public enum ReferenceType {

	Genomic(3,false),Transcriptomic(2,true),Both(-1,false),rRNA(1,true);
	
	public int prio;
	public boolean norc;
	private ReferenceType(int prio, boolean norc) {
		this.prio = prio;
		this.norc = norc;
	}
	
}
