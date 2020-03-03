/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.exception;

/**
 * An exception that is thrown when the framework finds a problem with constructing controllers or filters.
 * @author Matthew Tropiano
 */
public class SmallFrameworkSetupException extends SmallFrameworkException
{
	private static final long serialVersionUID = 293213593023790633L;

	/**
	 * Creates a new exception.
	 */
	public SmallFrameworkSetupException()
	{
		super();
	}
	
	/**
	 * Creates a new exception.
	 * @param message the exception message.
	 */
	public SmallFrameworkSetupException(String message)
	{
		super(message);
	}

	/**
	 * Creates a new exception.
	 * @param exception the exception cause.
	 */
	public SmallFrameworkSetupException(Throwable exception)
	{
		super(exception);
	}
	
	/**
	 * Creates a new exception with a message.
	 * @param message the exception message.
	 * @param exception the exception cause.
	 */
	public SmallFrameworkSetupException(String message, Throwable exception)
	{
		super(message, exception);
	}
	
}
