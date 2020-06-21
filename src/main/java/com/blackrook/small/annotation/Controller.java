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

import com.blackrook.small.annotation.controller.EntryPath;

/**
 * Annotation that is used to signify that this object is a Controller with entry points.
 * Must be on a class also annotated with {@link Component}. An {@link EntryPath} annotation on the
 * controller class specifies the base path for the endpoints.
 * <p>All HTTP calls find their way here, and these objects handle the incoming request.
 * <p>Requests are handled via {@link EntryPath}-annotated methods.
 * <p>
 * This object may have dependency singleton {@link Component} objects
 * injected into them via {@link ComponentConstructor}-annotated constructors. 
 * Only one constructor can be annotated with {@link ComponentConstructor}.
 * @author Matthew Tropiano
 * @see EntryPath
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Controller {}
