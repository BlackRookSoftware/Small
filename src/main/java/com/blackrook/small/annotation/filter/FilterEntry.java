/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.annotation.filter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.blackrook.small.annotation.Filter;

/**
 * Public methods with this annotation in {@link Filter}-annotated objects declare the filter entry point. 
 * <p>The annotated method must return a boolean. if the method returns true, the filter chain continues.
 * If it returns false, the chain is not continued.
 * @author Matthew Tropiano
 * @see Filter
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FilterEntry
{
}
