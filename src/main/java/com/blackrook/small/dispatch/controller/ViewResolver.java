/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.dispatch.controller;

/**
 * Classes that implement this interface return a path to a view for a keyword or name.
 * @author Matthew Tropiano
 */
public interface ViewResolver
{
	/**
	 * Returns a path to a view for a particular keyword.
	 * If this returns a non-null string, it will be
	 * cached by the main view resolver as a valid view.
	 * @param viewName the view name to use as a lookup (from a controller call).
	 * @return the path to a valid view.
	 */
	public String resolveView(String viewName);
}