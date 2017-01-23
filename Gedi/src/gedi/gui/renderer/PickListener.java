package gedi.gui.renderer;

import java.util.EventListener;

public interface PickListener<T> extends EventListener {
	
	/**
	 * 
	 * @param e
	 * @return if pick event processed (i.e. no further listeners are fired)
	 */
    boolean picked(PickEvent<T> e);
    
}
