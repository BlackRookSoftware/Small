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
 * An annotation used to specify a filter chain to call before an {@link EntryPath} method is invoked on a {@link Controller}. 
 * <p>If on a package, all controllers in the package use this chain.
 * <p>If on a class with {@link Controller}, all entry points in the controller use this chain, after the package chain.
 * <p>If on a method in a class with {@link Controller}, this specific entry point in the controller uses this chain, after the class chain.
 * <p>All filters specified are additive to the whole chain, from packages down to controllers, then methods, in the order specified.
 * <p>Filter entry points are called in order, then the controller entry point, and then the filter exit points are called in the opposite specified.
 * If the chain was stopped, exit points are called for each filter that was executed so far in the chain. 
 * @author Matthew Tropiano
 */
@Target({ElementType.TYPE, ElementType.PACKAGE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FilterChain
{
	/** @return the list of filter classes. */
	Class<?>[] value();
}
