package gedi.core.data.annotation;


import java.io.IOException;

import gedi.app.Config;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

public class ScoreAnnotation implements BinarySerializable, ScoreProvider {
	
	private double score;
	
	public ScoreAnnotation() {
	}
	
	public ScoreAnnotation(double score) {
		this.score = score;
	}
	

	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putDouble(score);
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		score = in.getDouble();
	}
	
	@Override
	public String toString() {
		return String.format(Config.getInstance().getRealFormat(),score);
	}

	@Override
	public double getScore() {
		return score;
	}
	
	
	public void setScore(double score) {
		this.score = score;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(score);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ScoreAnnotation other = (ScoreAnnotation) obj;
		if (Double.doubleToLongBits(score) != Double
				.doubleToLongBits(other.score))
			return false;
		return true;
	}
	

}
