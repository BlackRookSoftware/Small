package com.blackrook.j2ee.small.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a method parameter. Should be used in Controllers and Filters.
 * <p>
 * Parameter type must be String, and will be set to the rest of the path not used by the controller resolver.
 * Really only useful on "default" entry points.
 * @author Matthew Tropiano
 * @see ControllerEntry
 * @see FilterEntry
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface PathRemainder
{

}
