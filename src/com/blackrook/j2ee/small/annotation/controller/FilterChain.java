package com.blackrook.j2ee.small.annotation.controller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.blackrook.j2ee.small.annotation.Controller;

/**
 * Annotation to specify a filter chain to call. 
 * <p>If on a package, all controllers in the package use this chain.
 * <p>If on a class with {@link Controller}, all entry points in the controller use this chain.
 * <p>If on a method in a class with {@link Controller}, this specific entry point in the controller uses this chain.
 * <p>All filters specified are additive to the whole chain, from packages down to controllers, then methods.
 * @author Matthew Tropiano
 */
@Target({ElementType.TYPE, ElementType.PACKAGE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FilterChain
{
	/** The list of filter classes. */
	Class<?>[] value();
}
