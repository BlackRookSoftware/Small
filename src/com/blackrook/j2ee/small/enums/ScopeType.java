package com.blackrook.j2ee.small.enums;

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
