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
	 * Provides the path to the temporary directory.
	 * If this returns null, it will defer to the context attribute <code>"javax.servlet.context.tempdir"</code>,
	 * and if that isn't set, the <code>"java.io.tmpdir"</code> system property is used.
	 * @return the path for temporary files (usually multipart form files), or null.
	 */
	String getTempPath();

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
