/*******************************************************************************
 * Copyright (c) 2020-2022 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.roles;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Describes a view resolution driver for Small.
 * @author Matthew Tropiano
 * @param <E> the exception type.
 */
public interface ExceptionHandler<E extends Throwable>
{
	/**
	 * Gets the exception class that this handles.
	 * This is necessary as method parameter type erasure occurs when classes are compiled without
	 * debugging information, and thus is lost when packaging applications in most cases.
	 * <p>This method is called during environment construction.
	 * @return the handled class.
	 * @since 1.5.0 
	 */
	Class<E> getHandledClass();
	
	/**
	 * Called when an exception needs handling when thrown from a controller.
	 * @param request the HTTP request object.
	 * @param response the HTTP response object.
	 * @param exception the exception caught.
	 */
	void handleException(HttpServletRequest request, HttpServletResponse response, E exception);
}
