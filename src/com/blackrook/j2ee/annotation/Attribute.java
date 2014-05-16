package com.blackrook.j2ee.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.blackrook.j2ee.ScopeType;

/**
 * Annotates a method or method parameter for attribute binding. Should be used on Controllers.
 * <p>
 * Matched type is NOT converted. Any value created by this parameter is persisted to the scope declared.
 * Any value that does not already exist is created (via default constructor or method).
 * @author Matthew Tropiano
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Attribute
{
	/** Name of the attribute. */
	String value();
	/** The preferred scope to use to search for the matching attribute. */
	ScopeType scope();
}
