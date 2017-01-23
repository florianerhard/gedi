package gedi.util.userInteraction.progress;

public class ProgressFactory {

	private static ProgressFactory instance;
	
	public static synchronized ProgressFactory getInstance() {
		if (instance==null) instance = new ProgressFactory();
		return instance;
	}
	
	private Progress progress;
	
	private ProgressFactory() {
		progress = new ConsoleProgress();
	}
	
	
	public Progress get() {
		return progress;
	}
	
}
