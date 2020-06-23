/*******************************************************************************
 * Copyright (c) 2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.exception.request;

import javax.servlet.ServletException;

/**
 * Exception thrown when a converter for incoming/outgoing data cannot be read.
 * @author Matthew Tropiano
 */
public class NoConverterException extends ServletException
{
	private static final long serialVersionUID = -4936691975471716617L;

	/**
	 * Creates a new exception.
	 * @param message the exception message.
	 */
	public NoConverterException(String message)
	{
		super(message);
	}

	/**
	 * Creates a new exception with a message.
	 * @param message the exception message.
	 * @param exception the exception cause.
	 */
	public NoConverterException(String message, Throwable exception)
	{
		super(message, exception);
	}

}
