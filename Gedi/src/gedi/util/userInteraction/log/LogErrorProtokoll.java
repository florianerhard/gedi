package gedi.util.userInteraction.log;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LogErrorProtokoll extends UniqueErrorProtokoll {

	private static final Logger log = Logger.getLogger( ErrorProtokoll.class.getName() );
	private Level level = Level.WARNING;
	
	public LogErrorProtokoll() {
		super(true);
	}
	
	public void setLevel(Level level) {
		this.level = level;
	}

	@Override
	protected void report(String errorType, Object object, String message) {
		log.log(level, message);
	}
	
}
