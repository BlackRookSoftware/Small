/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
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

import com.blackrook.small.annotation.controller.EntryPath;
import com.blackrook.small.annotation.filter.FilterEntry;

/**
 * Annotates a method parameter for request header value binding. Should be used in Controllers and Filters. 
 * @author Matthew Tropiano
 * @see EntryPath
 * @see FilterEntry
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Header
{
	/** @return the name of the header value. */
	String value();
}
