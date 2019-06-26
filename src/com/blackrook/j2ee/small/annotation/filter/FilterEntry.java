package com.blackrook.j2ee.small.annotation.filter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.blackrook.j2ee.small.annotation.Filter;

/**
 * Public methods with this annotation in {@link Filter}-annotated objects declare the filter entry point. 
 * <p>The annotated method must return a boolean. if the method returns true, the filter chain continues.
 * If it returns false, the chain is not continued.
 * @author Matthew Tropiano
 * @see Filter
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FilterEntry
{
}
