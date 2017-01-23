package gedi.core.data.annotation;

public interface ScoreProvider {

	double getScore();
	
	default int getIntScore() {
		return (int)getScore();
	}
	
}
