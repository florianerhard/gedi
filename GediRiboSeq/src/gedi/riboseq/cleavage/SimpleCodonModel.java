package gedi.riboseq.cleavage;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.riboseq.utils.RiboUtils;
import gedi.util.StringUtils;

import java.util.HashMap;

public class SimpleCodonModel {

	private HashMap<String,Integer> map = new HashMap<String, Integer>();
	private String[] spec;

	// 28->12 29-12 29L->13 30L->13
	public SimpleCodonModel(String[] li) {
		this.spec = li;
		for (String l : li) {
			String[] p = StringUtils.split(l, "->");
			if (p.length!=2) throw new RuntimeException("Not the proper format for simple model: "+l);
			map.put(p[0], Integer.parseInt(p[1]));
		}
	}
	
	public String[] getSpec() {
		return spec;
	}

	public int getPosition(
			ImmutableReferenceGenomicRegion<AlignedReadsData> read, int d) {
		String key = read.getRegion().getTotalLength()+"";
		if (RiboUtils.hasLeadingMismatch(read.getData(), d)) {
			if (RiboUtils.isLeadingMismatchInsideGenomicRegion(read.getData(), d)) 
				key+="L";
			else
				key = (read.getRegion().getTotalLength()+1)+"L";
		}
		Integer re = map.get(key);
		return re==null?-1:re;
	}

}