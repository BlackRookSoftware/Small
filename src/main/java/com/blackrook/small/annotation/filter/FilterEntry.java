/*******************************************************************************
 * Copyright (c) 2020-2022 Black Rook Software
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

import com.blackrook.small.SmallFilterResult;
import com.blackrook.small.annotation.Filter;

/**
 * Public methods with this annotation in {@link Filter}-annotated objects declare the filter entry point. 
 * <p>The annotated method must return a {@link SmallFilterResult}. if the result is passing, the filter chain continues.
 * If it is not, the chain is not continued. Returning null is equivalent to sending <i>failure</i>.
 * @author Matthew Tropiano
 * @see Filter
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FilterEntry {}
