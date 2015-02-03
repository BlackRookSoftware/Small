package com.blackrook.j2ee.small.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that is used to signify that this object is a Filter with entry points.
 * <p>Filters contain a single method that is called where some values are evaluated and then
 * the filter makes a decision whether to continue with the rest of the request, or stop here.
 * <p>Popular uses are secure protocol redirects, attribute and session setup, and authorization gateways.   
 * <p>
 * This object may have dependency singleton {@link Component} objects
 * injected into them via {@link ConstructorMain}-annotated constructors. 
 * Only one constructor can be annotated with one.
 * @author Matthew Tropiano
 * @see FilterEntry
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Filter
{
}
