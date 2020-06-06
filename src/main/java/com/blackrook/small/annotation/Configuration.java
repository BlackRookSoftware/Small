package com.blackrook.small.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.blackrook.small.SmallConfiguration;
import com.blackrook.small.SmallServlet;

/**
 * An annotation on a {@link SmallServlet} to describe the
 * SmallConfiguration class to instantiate and grab settings from. 
 * @author Matthew Tropiano
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Configuration
{
	/** The configuration class to use for this servlet. */
	Class<? extends SmallConfiguration> value();
}
