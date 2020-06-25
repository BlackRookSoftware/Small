/*******************************************************************************
 * Copyright (c) 2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import com.blackrook.small.roles.ExceptionHandler;
import com.blackrook.small.roles.JSONDriver;
import com.blackrook.small.roles.MIMETypeDriver;
import com.blackrook.small.roles.ViewDriver;
import com.blackrook.small.roles.XMLDriver;

/**
 * A class annotated with this annotation is instantiated as a singleton object shared by the application context.
 * <p>
 * These annotated types are instantiated on application startup.
 * <p>
 * This object may have other singleton {@link Component} objects injected into them 
 * via {@link ComponentConstructor}-annotated constructors. 
 * Only one constructor on the Component can be annotated with one.
 * <p>
 * This object may implement {@link HttpSessionListener} or {@link HttpSessionAttributeListener} and it
 * will receive session events.
 * <p>
 * This object can also implement {@link ServletContextListener}, but it will ONLY receive 
 * {@link ServletContextListener#contextDestroyed(ServletContextEvent)} calls.
 * <p>
 * If a component implements {@link JSONDriver}, it is used as the JSON serializer.
 * <p>
 * If a component implements {@link XMLDriver}, it is used as the XML serializer.
 * <p>
 * If a component implements {@link ViewDriver}, it is used as a view handler in the potential view handler chain.
 * <p>
 * If a component implements {@link ExceptionHandler}, it is used as an exception handler, 
 * called when an uncaught exception occurs that is the matching type.
 * <p>
 * If a component implements {@link MIMETypeDriver}, it is used has a MIME-Type lookup.
 * 
 * @author Matthew Tropiano
 * @see ComponentConstructor
 */
@Target({ElementType.TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Component {}
