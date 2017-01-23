package gedi.util.userInteraction.results;


public interface ResultConsumer  {

	
	/**
	 * Only called from {@link ResultProducer}s in their own worker thread!
	 * @param result
	 */
	void newResult(ResultProducer result);
	
}
