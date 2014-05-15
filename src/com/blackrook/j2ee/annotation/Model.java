package com.blackrook.j2ee.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a method parameter for sets of request parameters from the
 * request, session, and application (precedence is in that order).
 * @author Matthew Tropiano
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Model
{
	/** 
	 * Name of the model attribute to set. 
	 * If no name is specified, the class name is used. 
	 */
	String value() default "";
}
