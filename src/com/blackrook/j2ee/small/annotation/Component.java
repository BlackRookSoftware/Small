package com.blackrook.j2ee.small.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Types annotated as Singleton Components are instantiated on startup.
 * <p>
 * This object may have dependency singleton {@link Component} objects
 * injected into them via {@link ComponentConstructor}-annotated constructors. 
 * Only one constructor can be annotated with one.
 * @author Matthew Tropiano
 * @see ComponentConstructor
 */
@Target({ElementType.TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Component
{

}
