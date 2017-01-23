package gedi.oml;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME) 
@Target({ElementType.TYPE})
public @interface Param {

	String value();
	String parser() default "";
	String defaultValue() default "";

}
