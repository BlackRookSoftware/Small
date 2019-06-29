package com.blackrook.small.annotation.parameters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import com.blackrook.small.annotation.controller.ControllerEntry;
import com.blackrook.small.annotation.filter.FilterEntry;

/**
 * Annotates a method parameter for request parameter binding. Should be used in Controllers.
 * <p>
 * Turns the parameters on the request into a {@link Map}.
 * @author Matthew Tropiano
 * @see ControllerEntry
 * @see FilterEntry
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ParameterMap
{
}
