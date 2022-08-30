/*******************************************************************************
 * Copyright (c) 2020-2022 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.exception.views;

import javax.servlet.ServletException;

/**
 * Exception thrown when a something goes wrong in the view driver.
 * @author Matthew Tropiano
 */
public class ViewProcessingException extends ServletException
{
	private static final long serialVersionUID = 4956046544253841562L;

	/**
	 * Creates a new exception.
	 * @param message the exception message.
	 */
	public ViewProcessingException(String message)
	{
		super(message);
	}

	/**
	 * Creates a new exception with a message.
	 * @param message the exception message.
	 * @param exception the exception cause.
	 */
	public ViewProcessingException(String message, Throwable exception)
	{
		super(message, exception);
	}

}
