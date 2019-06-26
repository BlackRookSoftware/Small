package com.blackrook.j2ee.small.annotation.parameters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import com.blackrook.j2ee.small.annotation.controller.ControllerEntry;
import com.blackrook.j2ee.small.annotation.filter.FilterEntry;

/**
 * Annotates a request header for request header value binding. Should be used in Controllers and Filters. 
 * <p>
 * Turns the headers on the request into a {@link Map}.
 * @author Matthew Tropiano
 * @see ControllerEntry
 * @see FilterEntry
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface HeaderMap
{
}
