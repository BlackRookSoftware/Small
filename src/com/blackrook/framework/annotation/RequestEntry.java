package com.blackrook.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.blackrook.framework.BRController;

/**
 * Public methods with this annotation on {@link BRController} objects are
 * considered entry points via HTTP requests.
 * @author Matthew Tropiano
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestEntry
{
	RequestMethod[] value() default { RequestMethod.GET };
}
