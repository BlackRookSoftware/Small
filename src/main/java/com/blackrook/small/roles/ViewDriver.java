/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.roles;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.small.annotation.controller.EntryPath;
import com.blackrook.small.annotation.controller.View;

/**
 * Describes a view resolution driver for Small.
 * <p>
 * When a {@link EntryPath} method returns a {@link View}, it is passed to one of these components to render.
 * <p>
 * There can be many components with this role - the {@link #handleView(HttpServletRequest, HttpServletResponse, String)}.
 * @author Matthew Tropiano
 */
public interface ViewDriver
{
	/**
	 * Called when a view needs to be rendered.
	 * Returning <code>false</code> will pass this along to the next view resolution handler, and returning
	 * <code>true</code> will stop the chain.
	 * @param request the HTTP request object.
	 * @param response the HTTP response object.
	 * @param viewName the name of the view to handle.
	 * @return true if the view was handled by this component, false if not.
	 */
	boolean handleView(HttpServletRequest request, HttpServletResponse response, String viewName);
}
