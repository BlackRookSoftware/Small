/*******************************************************************************
 * Copyright (c) 2020 Black Rook Software
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
 * <p>
 * The method return type influences what gets sent back as content. 
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
	 * The entry path is a URI path that can contain statically-named tokens, path variables, 
	 * path variables qualified by a RegEx to match, or be terminated with an asterisk for
	 * a path folder to mean that anything after the path is accepted.
	 * <p>
	 * Valid paths can look like:
	 * <ul>
	 * <li><code>/pages/home</code> - a static URI.</li>
	 * <li><code>/pages/@page-name</code> - a URI with a path variable called <code>page-name</code>.</li>
	 * <li><code>/pages/@page-name/@paragraph-id</code> - a URI with two path variables called <code>page-name</code> and <code>paragraph-id</code>.</li>
	 * <li><code>/document/@id:[a-zA-Z]{4}/data</code> - a URI with a path variable called <code>id</code> that must a RegEx (matches 4 letters).</li>
	 * <li><code>/file/*</code> - a URI with an open-ended path after <code>/file/</code>.</li>
	 * <li><code>/*</code> - all URIs.</li>
	 * </ul>
	 * URI path folder search is prioritized from most specific to least - static pattern, variable with RegEx, variable without, then default paths.
	 * <p>
	 * If unchanged, this is the DEFAULT entry point ("*"), if no matching method is found.
	 * You may not specify more than one DEFAULT entry in a controller. Specified path is ignored.
	 * @return the path.
	 */
	String value() default "*";
	
}
