package gedi.app.config;


import gedi.app.extension.DefaultExtensionPoint;

public class ConfiguratorExtensionPoint extends DefaultExtensionPoint<String, Configurator>{

	private static ConfiguratorExtensionPoint instance;

	public static ConfiguratorExtensionPoint getInstance() {
		if (instance==null) 
			instance = new ConfiguratorExtensionPoint();
		return instance;
	}
	
	protected ConfiguratorExtensionPoint() {
		super(Configurator.class);
	}
	
	
	public void addExtension(Class<? extends Configurator> extension) {
		super.addExtension(extension, extension.getName());
	}

}

