/*******************************************************************************
 * Copyright (c) 2020-2022 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.exception.request;

/**
 * Exception thrown when a bean could not be created.
 * @author Matthew Tropiano
 */
public class BeanCreationException extends RuntimeException
{
	private static final long serialVersionUID = 651186337840937597L;

	/**
	 * Creates a new exception.
	 * @param message the exception message.
	 */
	public BeanCreationException(String message)
	{
		super(message);
	}

	/**
	 * Creates a new exception with a message.
	 * @param message the exception message.
	 * @param exception the exception cause.
	 */
	public BeanCreationException(String message, Throwable exception)
	{
		super(message, exception);
	}

}
