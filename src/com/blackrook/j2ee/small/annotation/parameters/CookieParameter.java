package com.blackrook.j2ee.small.annotation.parameters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.servlet.http.Cookie;

import com.blackrook.j2ee.small.annotation.controller.ControllerEntry;
import com.blackrook.j2ee.small.annotation.filter.FilterEntry;

/**
 * Annotates a method parameter for Cookie binding. Should be used in Controllers and Filters.
 * <p>
 * Matched type must be {@link Cookie}, and if the Cookie name does not exist,
 * it is created for the response.
 * @author Matthew Tropiano
 * @see ControllerEntry
 * @see FilterEntry
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CookieParameter
{
	/** Name of the cookie. */
	String value();
}
