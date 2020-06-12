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

import javax.servlet.http.HttpServletResponse;

import com.blackrook.small.annotation.Controller;

/**
 * Public methods with this annotation in {@link Controller}-annotated
 * classes are considered entry points via HTTP requests. 
 * Can also be on the controller class itself to declare a base path for all of the described endpoints. 
 * <p>The method return type influences what gets sent back as content. 
 * If the method returns <code>void</code>, then responding
 * must be handled some other way (via {@link HttpServletResponse}).
 * @author Matthew Tropiano
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EntryPath
{
	/** 
	 * The entry point path (after controller path resolution).
	 * <p>
	 * If blank, this is the DEFAULT entry point, if no matching method is found.
	 * You may not specify more than one DEFAULT entry in a controller. Specified path is ignored.
	 * @return the path.
	 */
	String value() default "";
	
}
