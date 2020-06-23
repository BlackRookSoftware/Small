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

import com.blackrook.small.annotation.Controller;

/**
 * Annotation to specify a filter chain to call. 
 * <p>If on a package, all controllers in the package use this chain.
 * <p>If on a class with {@link Controller}, all entry points in the controller use this chain.
 * <p>If on a method in a class with {@link Controller}, this specific entry point in the controller uses this chain.
 * <p>All filters specified are additive to the whole chain, from packages down to controllers, then methods.
 * @author Matthew Tropiano
 */
@Target({ElementType.TYPE, ElementType.PACKAGE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FilterChain
{
	/** @return the list of filter classes. */
	Class<?>[] value();
}
