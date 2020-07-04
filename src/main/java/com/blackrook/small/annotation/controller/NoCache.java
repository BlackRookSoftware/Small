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
import com.blackrook.small.annotation.filter.FilterEntry;

/**
 * If placed on a {@link Controller} {@link EntryPath} method, headers are written in the response for
 * hinting at the fact that the returned content should not be cached by the browser.
 * @author Matthew Tropiano
 * @see EntryPath
 * @see FilterEntry
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoCache {}
