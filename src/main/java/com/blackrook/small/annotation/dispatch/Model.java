/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.annotation.dispatch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.blackrook.small.annotation.controller.ControllerEntry;
import com.blackrook.small.annotation.filter.FilterEntry;

/**
 * Annotates a controller method or method parameter for sets of request parameters from the
 * request, session, and application (precedence is in that order).
 * <p>
 * On a method, it's the method to call to constructed the model.
 * <p>
 * On a parameter, it's the model itself.
 * <p>
 * The model is automatically persisted to the PAGE scope for the view.
 * Any model that is not constructed is created (via default constructor or matching annotated method in the same class).
 * @author Matthew Tropiano
 * @see ControllerEntry
 * @see FilterEntry
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Model
{
	/** 
	 * Name of the model attribute to set. 
	 * If no name is specified, the class name is used.
	 * @return the name.
	 */
	String value() default "";
}
