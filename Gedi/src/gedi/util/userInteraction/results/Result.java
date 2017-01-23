package gedi.util.userInteraction.results;

public interface Result {

	default <T extends Result> boolean is(Class<T> cls) {
		return cls.isInstance(this);
	};
	
	default <T extends Result> T as(Class<T> cls) {
		if (is(cls)) return cls.cast(this);
		return null;
	}
	
	
	@SuppressWarnings("unchecked")
	public static final Class<? extends Result>[] ASPECTS = new Class[] {ImageResult.class};
	
}
