package gedi.util.userInteraction.results;

public interface ResultProducer {

	Result getCurrentResult();
	boolean isFinalResult();
	String getName();
	String getDescription();
	
	void registerConsumer(ResultConsumer consumer);
	void unregisterConsumer(ResultConsumer consumer);
	
}
