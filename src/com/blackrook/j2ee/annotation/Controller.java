package com.blackrook.j2ee.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.blackrook.j2ee.DefaultViewResolver;
import com.blackrook.j2ee.component.ViewResolver;

/**
 * Annotation that is used to signify that this object is a Controller with entry points.
 * <p>All HTTP calls find their way here, and these objects handle the incoming request.
 * @author Matthew Tropiano
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Controller
{
	/**
	 * The "directory" path to use for the controller.
	 * Default is "/".
	 */
	String value() default "";
	/** 
	 * The view resolver class to use.
	 * Default is {@link DefaultViewResolver}. 
	 */
	Class<? extends ViewResolver> viewResolver() default DefaultViewResolver.class;
}
