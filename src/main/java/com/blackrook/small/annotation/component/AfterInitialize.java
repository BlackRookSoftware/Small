/*******************************************************************************
 * Copyright (c) 2020 Black Rook Software
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

import com.blackrook.small.annotation.Component;
import com.blackrook.small.annotation.controller.EntryPath;
import com.blackrook.small.annotation.filter.FilterEntry;

/**
 * Annotates a {@link Component} method to be called after all components are constructed.
 * <p>
 * The annotated method must be publicly accessible, return void, and take no parameters.
 * @author Matthew Tropiano
 * @see EntryPath
 * @see FilterEntry
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AfterInitialize {}
