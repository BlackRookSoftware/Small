package com.blackrook.small;

/**
 * Small Configuration class.
 * @author Matthew Tropiano
 */
public interface SmallConfiguration
{
	/**
	 * @return the server port.
	 */
	int getServerPort();

	/**
	 * @return the context path for the application.
	 */
	String getContextPath();

	/**
	 * @return the path patterns for the application's servlet entrypoint.
	 */
	String[] getServletPaths();

	/**
	 * @return the list of root packages to scan.
	 */
	String[] getApplicationPackageRoots();

	/**
	 * Checks if the OPTIONS HTTP method is allowed for all endpoints.
	 * @return true if allowed, false if not.
	 */
	boolean allowOptions();

	/**
	 * Checks if the TRACE HTTP method is allowed for all endpoints.
	 * @return true if allowed, false if not.
	 */
	boolean allowTrace();

}
