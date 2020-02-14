/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.parser.multipart;

/**
 * Exception thrown when a multiform request is not parsed properly.
 * @author Matthew Tropiano
 */
public class MultipartParserException extends Exception
{
	private static final long serialVersionUID = 8446356442244138570L;

	/**
	 * Creates a new exception.
	 */
	public MultipartParserException()
	{
		super();
	}
	
	/**
	 * Creates a new exception.
	 * @param message the exception message.
	 */
	public MultipartParserException(String message)
	{
		super(message);
	}

	/**
	 * Creates a new exception.
	 * @param exception the exception cause.
	 */
	public MultipartParserException(Throwable exception)
	{
		super(exception);
	}
	
	/**
	 * Creates a new exception with a message.
	 * @param message the exception message.
	 * @param exception the exception cause.
	 */
	public MultipartParserException(String message, Throwable exception)
	{
		super(message, exception);
	}
	
}
