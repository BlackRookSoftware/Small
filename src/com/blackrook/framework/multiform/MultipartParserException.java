package com.blackrook.framework.multiform;

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
	 */
	public MultipartParserException(String message)
	{
		super(message);
		}

	/**
	 * Creates a new exception.
	 */
	public MultipartParserException(Throwable exception)
	{
		super(exception);
		}
	
	/**
	 * Creates a new exception with a message.
	 */
	public MultipartParserException(String message, Throwable exception)
	{
		super(message, exception);
		}
	
}
