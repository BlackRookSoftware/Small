package com.blackrook.small.exception.request;

import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;

/**
 * An exception that is an encapsulation of many exceptions 
 * in the request handling chain that will either need to be logged or reported.
 * <p> {@link Exception#getCause()} here will only return the first cause. 
 * @author Matthew Tropiano
 */
public class ManyRequestExceptionsException extends ServletException
{
	private static final long serialVersionUID = 1000241881214230214L;

	private List<Throwable> causes;
	
	/**
	 * Creates a new exception.
	 * @param message the exception message.
	 */
	public ManyRequestExceptionsException(String message)
	{
		super(message);
		this.causes = new LinkedList<>();
	}

	/**
	 * Creates a new exception with a message and primary cause.
	 * @param message the exception message.
	 * @param cause the exception cause.
	 */
	public ManyRequestExceptionsException(String message, Throwable cause)
	{
		super(message, cause);
		this.causes = new LinkedList<>();
		addCause(cause);
	}

	/**
	 * Adds another cause to this exception.
	 * @param cause the cause to add.
	 */
	public synchronized void addCause(Throwable cause)
	{
		this.causes.add(cause);
	}
	
	/**
	 * @return an iterable structure of the causes that make up this exception.
	 */
	public Iterable<Throwable> getCauses()
	{
		return causes;
	}
	
}
