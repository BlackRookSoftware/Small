package com.blackrook.j2ee;

/**
 * Attribute scope type.
 * @author Matthew Tropiano
 */
public enum ScopeType
{
	/** Request scope. */
	REQUEST,
	/** Session scope. */
	SESSION,
	/** Application (servlet context) scope. */
	APPLICATION;
}
