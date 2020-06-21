/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
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

import javax.servlet.ServletContext;

/**
 * Annotation that specifies that the annotated constructor contains parameters
 * that should be filled with singleton instances.
 * <p>
 * All Controllers/Filters/Resource-annotated classes that have one of their
 * constructors annotated with this will have that constructor invoked on
 * instantiation and its parameters set to other {@link Component} objects.
 * <p>
 * One of the parameters of the constructor can be {@link ServletContext}, which passes
 * in the application's ServletContext. 
 *  
 * @author Matthew Tropiano
 * @see Filter
 * @see Component
 * @see Controller
 */
@Target(ElementType.CONSTRUCTOR)
@Retention(RetentionPolicy.RUNTIME)
public @interface ComponentConstructor {}
