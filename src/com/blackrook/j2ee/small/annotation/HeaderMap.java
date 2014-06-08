package com.blackrook.j2ee.small.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import com.blackrook.commons.AbstractMap;

/**
 * Annotates a request header for request header value binding. Should be used in Controllers and Filters. 
 * <p>
 * Turns the headers on the request into a {@link Map} or {@link AbstractMap}, depending on type.
 * @author Matthew Tropiano
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface HeaderMap
{
}
