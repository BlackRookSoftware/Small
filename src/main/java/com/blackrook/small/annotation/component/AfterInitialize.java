/*******************************************************************************
 * Copyright (c) 2020-2022 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.annotation.component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.servlet.ServletContext;

import com.blackrook.small.SmallConfiguration;
import com.blackrook.small.SmallEnvironment;
import com.blackrook.small.annotation.Component;
import com.blackrook.small.annotation.controller.EntryPath;
import com.blackrook.small.annotation.filter.FilterEntry;

/**
 * Annotates a {@link Component} method to be called after all components are constructed.
 * <p>
 * The annotated method must be publicly accessible and return void.
 * If the annotated method has parameters, they will be filled with matching components. It
 * is safe to do interface-based searching this way, as all components will be constructed by the time
 * this method is called.
 * <p>
 * Other valid parameters of this method can be {@link ServletContext}, which passes
 * in the application's Servlet Context. Another can be {@link SmallConfiguration}, which passes
 * in the configuration for bootstrapping/configuring the application, as well as {@link SmallEnvironment}, 
 * which is the main component manager itself.
 * 
 * @author Matthew Tropiano
 * @see EntryPath
 * @see FilterEntry
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AfterInitialize {}
