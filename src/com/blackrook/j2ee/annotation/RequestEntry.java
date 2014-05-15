package com.blackrook.j2ee.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.blackrook.j2ee.RequestMethod;

/**
 * Public methods with this annotation on {@link Controller} objects are
 * considered entry points via HTTP requests.
 * @author Matthew Tropiano
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestEntry
{
	/** The request methods that this entry point accepts. */
	RequestMethod[] value() default { RequestMethod.GET };
	/** 
	 * If true, this is the ONLY ENTRY that will be called for all RequestMethods specified.
	 * False by default. 
	 */
	boolean onlyEntry() default false;
}
