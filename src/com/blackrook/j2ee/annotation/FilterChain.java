package com.blackrook.j2ee.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify a filter chain to call. 
 * <p>If on a package, all controllers in the package use this chain.
 * <p>If on a class with {@link Controller}, all entry points in the controller use this chain.
 * <p>If on a method in a class with {@link Controller}, this specific entry point in the controller uses this chain.
 * @author Matthew Tropiano
 */
@Target({ElementType.TYPE, ElementType.PACKAGE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FilterChain
{
	Class<?>[] value();
}
