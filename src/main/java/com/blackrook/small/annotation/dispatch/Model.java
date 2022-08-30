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
import com.blackrook.small.annotation.controller.EntryPath;
import com.blackrook.small.annotation.filter.FilterEntry;
import com.blackrook.small.util.SmallRequestUtils;

/**
 * Annotates a {@link Controller} method or method parameter for constructing a model for 
 * passing to a view or processing in a method.
 * <p>
 * On a method, it's the method to call to construct the model of a matching name.
 * <p>
 * On a parameter, it's the model itself.
 * <p>
 * If the model is not constructed via a matching method, it will be constructed via {@link SmallRequestUtils#setModelFields(javax.servlet.http.HttpServletRequest, Class)}
 * <p>
 * Any model that is not constructed is created (via default constructor or matching annotated method in the same class).
 * @author Matthew Tropiano
 * @see EntryPath
 * @see FilterEntry
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Model 
{
	/** 
	 * Name of the model. 
	 * If no name is specified, "model" is used.
	 * @return the name.
	 */
	String value() default "model";
}
