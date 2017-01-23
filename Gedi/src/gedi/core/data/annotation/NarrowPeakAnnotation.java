package gedi.core.data.annotation;


import java.io.IOException;

import gedi.app.Config;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

public class NarrowPeakAnnotation implements BinarySerializable, ScoreProvider {
	
	private String name;
	private double score;
	private double enrichment;
	private double pvalue;
	private double qvalue;
	private int summit;
	
	public NarrowPeakAnnotation() {
	}
	
	public NarrowPeakAnnotation(String name, double score, double enrichment,
			double pvalue, double qvalue, int summit) {
		this.name = name;
		this.score = score;
		this.enrichment = enrichment;
		this.pvalue = pvalue;
		this.qvalue = qvalue;
		this.summit = summit;
	}

	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putString(name);
		out.putDouble(score);
		out.putDouble(enrichment);
		out.putDouble(pvalue);
		out.putDouble(qvalue);
		out.putInt(summit);
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		name = in.getString();
		score = in.getDouble();
		enrichment = in.getDouble();
		pvalue = in.getDouble();
		qvalue = in.getDouble();
		summit = in.getInt();
	}
	
	@Override
	public String toString() {
		return String.format("%s("+Config.getInstance().getRealFormat()+")",name,score);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public double getEnrichment() {
		return enrichment;
	}

	public void setEnrichment(double enrichment) {
		this.enrichment = enrichment;
	}

	public double getPvalue() {
		return pvalue;
	}

	public void setPvalue(double pvalue) {
		this.pvalue = pvalue;
	}

	public double getQvalue() {
		return qvalue;
	}

	public void setQvalue(double qvalue) {
		this.qvalue = qvalue;
	}

	public int getSummit() {
		return summit;
	}

	public void setSummit(int summit) {
		this.summit = summit;
	}

	
}
