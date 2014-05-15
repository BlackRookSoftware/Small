package com.blackrook.j2ee.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a method parameter for request parameter binding. Should be used in Controllers.
 * <p>
 * Matched type can be array, in order to accept parameters with the same name. 
 * If the request body type is <code>multipart/**</code>, the parameter type can be {@link File},
 * which matches an uploaded file.
 * @author Matthew Tropiano
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Parameter
{
	/** Name of the request parameter. */
	String value();
}
