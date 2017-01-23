package gedi.core.data.mapper;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME) 
@Target({ElementType.TYPE})
/**
 * Only GenomicRegionDataMapper may declare this annotation; FROM must be fromType (if fromType has a single element), Void if fromType is empty or 
 * MutableTuple/one of the Mutables otherwise. Same for toType and TO.
 * @author erhard
 *
 */
public @interface GenomicRegionDataMapping {
	
	String description() default "";
	Class<?>[] fromType() default {};
	Class<?> toType() default Boolean.class;
	
	
}
