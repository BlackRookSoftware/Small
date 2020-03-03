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

import com.blackrook.small.annotation.filter.FilterEntry;

/**
 * Annotates a method. Should be used in Controllers.
 * <p>
 * Designates that the return value is the name of a view. If not String, the returned object's {@link String#valueOf(Object)} return value is used.
 * <p>
 * If the view starts with "<code>redirect:</code>", then a redirect request is sent to the browser (<code>Location</code> header).
 * @author Matthew Tropiano
 * @see ControllerEntry
 * @see FilterEntry
 * @see Attachment
 * @see Content
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface View
{
}
