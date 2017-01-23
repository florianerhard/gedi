package gedi.app.extension;


public interface ExtensionPoint<C,T> {

	Class<T> getExtensionPointClass();
	void addExtension(Class<? extends T> extension, C context);
	
}
