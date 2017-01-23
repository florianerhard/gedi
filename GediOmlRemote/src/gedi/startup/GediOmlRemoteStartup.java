package gedi.startup;

import gedi.app.Startup;
import gedi.app.classpath.ClassPath;
import gedi.oml.remote.Pipeline.TrackProtocol;
import gedi.remote.ProtocolExtensionPoint;

public class GediOmlRemoteStartup implements Startup {

	@Override
	public void accept(ClassPath t) {
		ProtocolExtensionPoint.getInstance().addExtension(TrackProtocol.class,"tracks");
	}

}
