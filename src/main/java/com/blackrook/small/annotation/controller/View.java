/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.annotation.controller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.blackrook.small.annotation.Controller;
import com.blackrook.small.annotation.filter.FilterEntry;
import com.blackrook.small.roles.ViewDriver;

/**
 * Should be used on {@link EntryPath}-annotated methods on {@link Controller}-annotated components.
 * <p>
 * Designates that the return value is the name of a view to be handled by {@link ViewDriver}s.
 * <p> 
 * If the return type is... 
 * <ul>
 * <li>SmallModelView, it is used for both the model for the found view and view name to resolve.</li>
 * <li>Anything else, the string value is considered to be the name of the view to resolve, no model.</li>
 * </ul>
 * <p>
 * If the view name starts with "<code>redirect:</code>", then a redirect request is sent to the browser (<code>Location</code> header).
 * <p>
 * If no {@link ViewDriver} handles the view, a 501 error is sent back.
 * @author Matthew Tropiano
 * @see EntryPath
 * @see FilterEntry
 * @see Attachment
 * @see Content
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface View {}
