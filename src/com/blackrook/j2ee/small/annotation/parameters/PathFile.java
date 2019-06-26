package com.blackrook.j2ee.small.annotation.parameters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.blackrook.j2ee.small.annotation.controller.ControllerEntry;
import com.blackrook.j2ee.small.annotation.filter.FilterEntry;

/**
 * Annotates a method parameter. Should be used in Controllers and Filters.
 * <p>
 * Parameter type must be String, and will be set to the "filename" portion of the request path.
 * @author Matthew Tropiano
 * @see ControllerEntry
 * @see FilterEntry
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface PathFile
{

}
