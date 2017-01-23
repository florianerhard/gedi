package gedi.util.orm;

public interface OrmObject {

	default void preOrmAction() {}
	default void postOrmAction()  {}
	
}
