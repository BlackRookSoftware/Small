package com.blackrook.j2ee.small.annotation.attribs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.blackrook.j2ee.small.annotation.controller.ControllerEntry;
import com.blackrook.j2ee.small.annotation.filter.FilterEntry;
import com.blackrook.j2ee.small.enums.ScopeType;

/**
 * Annotates a controller method or method parameter for attribute binding. Should be used in Controllers and Filters.
 * <p>
 * Matched type is NOT converted. Any value created by this parameter is persisted to the scope declared.
 * Any value that does not already exist is created (via default constructor or matching annotated method in the same class).
 * @author Matthew Tropiano
 * @see ControllerEntry
 * @see FilterEntry
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Attribute
{
	/** Name of the attribute. */
	String value();
	/** The preferred scope to use to search for the matching attribute. */
	ScopeType scope();
}
