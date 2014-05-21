package com.blackrook.j2ee.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import com.blackrook.commons.AbstractMap;

/**
 * Annotates a method parameter for request parameter binding. Should be used in Controllers.
 * <p>
 * Turns the parameters on the request into a {@link Map} or {@link AbstractMap}, depending on type.
 * @author Matthew Tropiano
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ParameterMap
{
}
