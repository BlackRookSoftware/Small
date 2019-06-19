/*******************************************************************************
 * Copyright (c) 2009-2012 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  
 * Contributors:
 *     Matt Tropiano - initial API and implementation
 ******************************************************************************/
package com.blackrook.j2ee.small.exception;

/**
 * An exception that is thrown when the framework finds a problem with parsing paths for controllers.
 * @author Matthew Tropiano
 */
public class SmallFrameworkParseException extends SmallFrameworkException
{
	private static final long serialVersionUID = 293213593023790633L;

	/**
	 * Creates a new exception.
	 */
	public SmallFrameworkParseException()
	{
		super();
	}
	
	/**
	 * Creates a new exception.
	 * @param message the exception message.
	 */
	public SmallFrameworkParseException(String message)
	{
		super(message);
	}

	/**
	 * Creates a new exception.
	 * @param exception the exception cause.
	 */
	public SmallFrameworkParseException(Throwable exception)
	{
		super(exception);
	}
	
	/**
	 * Creates a new exception with a message.
	 * @param message the exception message.
	 * @param exception the exception cause.
	 */
	public SmallFrameworkParseException(String message, Throwable exception)
	{
		super(message, exception);
	}
	
}
