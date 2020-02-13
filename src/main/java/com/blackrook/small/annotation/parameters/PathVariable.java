package com.blackrook.small.annotation.parameters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.blackrook.small.annotation.controller.ControllerEntry;
import com.blackrook.small.annotation.filter.FilterEntry;

/**
 * Annotates a method parameter. Should be used in Controllers and Filters.
 * <p>
 * Value refers to a variable value on the request path, and the parameter type must be a convertable type.
 * @author Matthew Tropiano
 * @see ControllerEntry
 * @see FilterEntry
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface PathVariable
{
	/** @return the name of the path variable. */
	String value();
}
