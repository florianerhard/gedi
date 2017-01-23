package gedi.remote;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import gedi.app.extension.DefaultExtensionPoint;
import gedi.app.extension.ExtensionPoint;

public class ProtocolExtensionPoint extends DefaultExtensionPoint<String,Protocol> {

	protected ProtocolExtensionPoint() {
		super(Protocol.class);
	}

	private static final Logger log = Logger.getLogger( ProtocolExtensionPoint.class.getName() );

	private static ProtocolExtensionPoint instance;

	public static ProtocolExtensionPoint getInstance() {
		if (instance==null) 
			instance = new ProtocolExtensionPoint();
		return instance;
	}



}
