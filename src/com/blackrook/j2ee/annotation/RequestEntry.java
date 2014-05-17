package com.blackrook.j2ee.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.servlet.http.HttpServletResponse;

import com.blackrook.j2ee.enums.RequestMethod;

/**
 * Public methods with this annotation in {@link Controller}-annotated objects are
 * considered entry points via HTTP requests. 
 * <p>The method return type influences what gets sent back as content. 
 * If the method returns <code>void</code>, then responding
 * must be handled some other way (via {@link HttpServletResponse}).
 * @author Matthew Tropiano
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestEntry
{
	/** The request methods that this entry point accepts. */
	RequestMethod[] value() default { RequestMethod.GET };
	/** 
	 * If true, this is the ONLY ENTRY that will be called for all RequestMethods specified.
	 * False by default. 
	 */
	boolean onlyEntry() default false;
}
