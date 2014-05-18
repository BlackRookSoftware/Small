package com.blackrook.j2ee.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a method parameter for Cookie binding. Should be used in Controllers and Filters.
 * <p>
 * Matched type must be {@link Cookie}, and if the Cookie name does not exist,
 * it is created for the response.
 * @author Matthew Tropiano
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CookieParameter
{
	/** Name of the cookie. */
	String value();
}
