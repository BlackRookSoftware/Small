/*******************************************************************************
 * Copyright (c) 2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
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
	 * Gets the port to use for secure connections.
	 * Can be null for no secure connections.
	 * @return the secure server port, or null for no secure sockets.
	 */
	Integer getSecureServerPort();

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

	/**
	 * Checks if the bootstrap should attempt to configure itself to support WebSockets.
	 * @return true if so, false if not.
	 */
	boolean allowWebSockets();

	/**
	 * Checks if Multipart requests are auto-parsed if detected 
	 * as "multipart" on POST, PUT, or PATCH requests.
	 * @return true if so, false if not.
	 */
	boolean autoParseMultipart();

	/**
	 * Fetches the value of an arbitrary attribute set that may be specific to the application.
	 * Returns a default value if it is not set.
	 * @param attributeName the attribute name.
	 * @param def the default value to return if it is not set.
	 * @return the corresponding attribute value, or <code>def</code> if not set.
	 */
	Object getAttribute(String attributeName, Object def);

	/**
	 * Fetches the value of an arbitrary attribute set that may be specific to the application.
	 * Returns <code>null</code> if it is not set.
	 * @param attributeName the attribute name.
	 * @return the corresponding attribute value, or <code>null</code> if not set.
	 */
	default Object getAttribute(String attributeName)
	{
		return getAttribute(attributeName, null);
	}

}
