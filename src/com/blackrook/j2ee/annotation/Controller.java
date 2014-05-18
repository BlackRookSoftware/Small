package com.blackrook.j2ee.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.blackrook.j2ee.DefaultViewResolver;
import com.blackrook.j2ee.component.ViewResolver;

/**
 * Annotation that is used to signify that this object is a Controller with entry points.
 * <p>All HTTP calls find their way here, and these objects handle the incoming request.
 * @author Matthew Tropiano
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Controller
{
	/**
	 * The prefix added to the requested page for the true method to invoke for the request type. 
	 * Default is "<code>on</code>", if unspecified.
	 * <p> Example: "<code>data</code>" would call "<code>onData(...)</code>". Case is very important on the method name.
	 */
	String methodPrefix() default "on";
	/** 
	 * The view resolver class to use.
	 * Default is {@link DefaultViewResolver}. 
	 */
	Class<? extends ViewResolver> viewResolver() default DefaultViewResolver.class;
}
