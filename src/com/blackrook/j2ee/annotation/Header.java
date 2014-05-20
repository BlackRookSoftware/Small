package com.blackrook.j2ee.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a method parameter for request header value binding. Should be used in Controllers and Filters. 
 * @author Matthew Tropiano
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Header
{
	/** The name of the header value. */
	String value();
}
