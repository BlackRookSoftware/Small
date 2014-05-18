package com.blackrook.j2ee.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a controller method or method parameter for sets of request parameters from the
 * request, session, and application (precedence is in that order).
 * <p>
 * The model is automatically persisted to the PAGE scope for the view.
 * @author Matthew Tropiano
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Model
{
	/** 
	 * Name of the model attribute to set. 
	 * If no name is specified, the class name is used. 
	 */
	String value() default "";
}
