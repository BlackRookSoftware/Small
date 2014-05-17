package com.blackrook.j2ee.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.blackrook.j2ee.DefaultViewResolver;
import com.blackrook.j2ee.component.ViewResolver;

/**
 * Annotation to signify a Controller with entry points.
 * @author Matthew Tropiano
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Controller
{
	/**
	 * The prefix added to the request page for the true method to invoke for the request type. 
	 * Default is "on", if unspecified.
	 */
	String methodPrefix() default "on";
	
	/** The view resolver class to use. */
	Class<? extends ViewResolver> viewResolver() default DefaultViewResolver.class;
	
}
