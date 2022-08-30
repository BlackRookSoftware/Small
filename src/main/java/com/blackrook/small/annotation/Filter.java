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

import com.blackrook.small.annotation.filter.FilterEntry;
import com.blackrook.small.dispatch.filter.FilterExitPoint;

/**
 * Annotation that is used to signify that this object is a Filter with entry points.
 * <p>
 * Must be on a class also annotated with {@link Component}.
 * <p>
 * Filters can contain an entry method that is called where some values are evaluated and then
 * the filter makes a decision whether to continue with the rest of the request, or stop here.
 * <p>
 * Filters can also contain an exit method that is called if the controller method gets called.
 * <p>
 * Popular uses are secure protocol redirects, attribute and session setup, and authorization gateways.   
 * <p>
 * Filter Exit points ({@link FilterExitPoint}) are executed in backwards order, starting with the last filter entry point that passed.
 * <p>
 * This object may have dependency singleton {@link Component} objects
 * injected into them via {@link ComponentConstructor}-annotated constructors. 
 * Only one constructor can be annotated with one.
 * @author Matthew Tropiano
 * @see FilterEntry
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Filter {}
