package com.blackrook.small.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

/**
 * Types annotated as Singleton Components are instantiated on startup.
 * <p>
 * This object may have dependency singleton {@link Component} objects
 * injected into them via {@link ComponentConstructor}-annotated constructors. 
 * Only one constructor can be annotated with one.
 * <p>
 * This object may implement {@link HttpSessionListener} or {@link HttpSessionAttributeListener} and it
 * will receive session events.
 * <p>
 * This object can also implement {@link ServletContextListener}, but it will ONLY receive 
 * {@link ServletContextListener#contextDestroyed(ServletContextEvent)} calls.
 * 
 * @author Matthew Tropiano
 * @see ComponentConstructor
 */
@Target({ElementType.TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Component
{

}
