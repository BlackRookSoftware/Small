package com.blackrook.j2ee.small.parser.multipart;

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
