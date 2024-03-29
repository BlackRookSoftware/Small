/*******************************************************************************
 * Copyright (c) 2020-2022 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.annotation.parameters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.servlet.http.HttpServletRequest;

import com.blackrook.small.annotation.Controller;
import com.blackrook.small.annotation.Filter;
import com.blackrook.small.annotation.controller.EntryPath;
import com.blackrook.small.annotation.filter.FilterEntry;

/**
 * Annotates a method parameter. Should be used in {@link Controller}s and {@link Filter}s.
 * <p>
 * Parameter type must be String, and will be set to the "query" portion of the request path.
 * @author Matthew Tropiano
 * @see EntryPath
 * @see FilterEntry
 * @see HttpServletRequest#getQueryString()
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface PathQuery {}
