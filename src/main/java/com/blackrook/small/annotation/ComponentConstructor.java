/*******************************************************************************
 * Copyright (c) 2020-2022 Black Rook Software
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

import com.blackrook.small.SmallConfiguration;
import com.blackrook.small.annotation.component.AfterInitialize;

/**
 * This annotation specifies that the annotated constructor contains parameters
 * that should be filled with singleton instances. At this stage, components are created
 * as they are instantiated, so you can't fill components using non-instantiable class types.
 * <p>
 * All {@link Controller}s/{@link Filter}s/{@link Component}-annotated classes that 
 * have one of their constructors annotated with this will have that constructor 
 * invoked on instantiation and its parameters set to other corresponding {@link Component} 
 * objects of the same class.
 * <p>
 * One of the parameters of the constructor can be {@link ServletContext}, which passes
 * in the application's Servlet Context. Another can be {@link SmallConfiguration}, which passes
 * in the configuration for bootstrapping/configuring the application. 
 * <p> 
 * <strong>NOTE:</strong> All other kinds of interface-based searching for components will <em>fail</em>, here.
 * You can safely add those kinds of components via a method annotated with {@link AfterInitialize}.
 *  
 * @author Matthew Tropiano
 * @see Filter
 * @see Component
 * @see Controller
 */
@Target(ElementType.CONSTRUCTOR)
@Retention(RetentionPolicy.RUNTIME)
public @interface ComponentConstructor {}
