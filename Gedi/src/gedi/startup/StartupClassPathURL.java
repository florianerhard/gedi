package gedi.startup;

import gedi.app.Startup;
import gedi.app.classpath.ClassPath;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;

public class StartupClassPathURL implements Startup {

	@Override
	public void accept(ClassPath t) {
		URL.setURLStreamHandlerFactory(new ClassPathHandlerFactory());
	}
	

	
	private static class ClassPathHandlerFactory implements URLStreamHandlerFactory {
	    private final HashMap<String, URLStreamHandler> protocolHandlers = new HashMap<String, URLStreamHandler>();

	    public ClassPathHandlerFactory() {
	    	addHandler("classpath", new URLStreamHandler() {
				@Override
				protected URLConnection openConnection(URL u) throws IOException {
					final URL resourceUrl = getClass().getResource(u.getPath());
					if (resourceUrl==null)
						throw new IOException("Not found in classpath: "+u.getPath());
			        return resourceUrl.openConnection();
				}
			});
	    }

	    public void addHandler(String protocol, URLStreamHandler urlHandler) {
	        protocolHandlers.put(protocol, urlHandler);
	    }

	    public URLStreamHandler createURLStreamHandler(String protocol) {
	        return protocolHandlers.get(protocol);
	    }
	}
	
}
