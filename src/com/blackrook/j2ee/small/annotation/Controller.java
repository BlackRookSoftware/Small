package com.blackrook.j2ee.small.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.blackrook.j2ee.small.DefaultViewResolver;
import com.blackrook.j2ee.small.ViewResolver;

/**
 * Annotation that is used to signify that this object is a Controller with entry points.
 * <p>All HTTP calls find their way here, and these objects handle the incoming request.
 * <p>Requests are handled via {@link ControllerEntry}-annotated methods.
 * <p>
 * This object may have dependency singleton {@link Component} objects
 * injected into them via {@link ComponentConstructor}-annotated constructors. 
 * Only one constructor can be annotated with one.
 * @author Matthew Tropiano
 * @see ControllerEntry
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Controller
{
	/**
	 * The "directory" path to use for the controller.
	 */
	String value() default "";
	/** 
	 * The view resolver class to use.
	 * Default is {@link DefaultViewResolver}. 
	 */
	Class<? extends ViewResolver> viewResolver() default DefaultViewResolver.class;
}
