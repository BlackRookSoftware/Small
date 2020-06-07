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
	 * Gets the classpath prefix to use for fetching static pages and data.
	 * If null, this search is skipped.
	 * @return the path to use (can be null).
	 * TODO: Implement!
	 */
	String getStaticResourcePath();

	/**
	 * Gets the prefix to use for fetching static pages and data.
	 * If this path is a relative path, it is relative to the current working directory.
	 * If null, this search is skipped.
	 * @return the path to use (can be null).
	 * TODO: Implement!
	 */
	String getStaticDocumentPath();

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
