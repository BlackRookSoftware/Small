package com.blackrook.framework;

/**
 * An exception that is thrown when the framework finds a problem.
 * @author Matthew Tropiano
 */
public class BRFrameworkException extends RuntimeException
{
	private static final long serialVersionUID = 293213593023790633L;

	/**
	 * Creates a new exception.
	 */
	public BRFrameworkException()
	{
		super();
		}
	
	/**
	 * Creates a new exception.
	 */
	public BRFrameworkException(String message)
	{
		super(message);
		}

	/**
	 * Creates a new exception.
	 */
	public BRFrameworkException(Throwable exception)
	{
		super(exception);
		}
	
	/**
	 * Creates a new exception with a message.
	 */
	public BRFrameworkException(String message, Throwable exception)
	{
		super(message, exception);
		}
	
}
