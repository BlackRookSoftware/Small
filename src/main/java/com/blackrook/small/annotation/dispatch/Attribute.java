/*******************************************************************************
 * Copyright (c) 2020-2022 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.annotation.dispatch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.blackrook.small.annotation.Controller;
import com.blackrook.small.annotation.Filter;
import com.blackrook.small.annotation.controller.EntryPath;
import com.blackrook.small.annotation.filter.FilterEntry;
import com.blackrook.small.enums.ScopeType;

/**
 * Annotates a controller method or method parameter for scope attribute binding. Should be used in {@link Controller}s and {@link Filter}s.
 * <p>
 * Matched type is NOT converted. Any value created by this parameter is persisted to the scope declared. Default is {@link ScopeType#REQUEST}.
 * Any value that does not already exist is created (via default constructor or matching annotated method in the same class).
 * @author Matthew Tropiano
 * @see EntryPath
 * @see FilterEntry
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Attribute
{
	/** @return the name of the attribute. */
	String value();
	/** @return the preferred scope to use to search for the matching attribute. */
	ScopeType scope() default ScopeType.REQUEST;
}
