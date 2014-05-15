package com.blackrook.j2ee.annotation;

/**
 * Annotation to signify a Controller with entry points.
 * @author Matthew Tropiano
 */
public @interface Controller
{
	/**
	 * The prefix added to the request page for the true method to invoke for the request type. 
	 * Default is "on", if unspecified.
	 */
	String methodPrefix() default "on";
}
