/*******************************************************************************
 * Copyright (c) 2009-2012 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  
 * Contributors:
 *     Matt Tropiano - initial API and implementation
 ******************************************************************************/
package com.blackrook.framework;

import java.io.IOException;
import java.io.InputStream;

import com.blackrook.db.QueryResult;
import com.blackrook.sync.Task;

/**
 * A task that runs asynchronously from the rest of the application.
 * Classes that extend this one should make
 * @author Matthew Tropiano
 */
public abstract class BRFrameworkTask extends Task
{
	/** Name for all default pools. */
	public static final String DEFAULT_POOL_NAME = "default";

	/** Default Servlet SQL Pool. */
	private String servletDefaultSQLPool;

	/**
	 * Creates a new runnable task that uses the default database and thread pools.
	 */
	protected BRFrameworkTask()
	{
		this(DEFAULT_POOL_NAME);
		}
	
	/**
	 * Creates a new runnable task that uses the database and thread pools provided.
	 */
	protected BRFrameworkTask(String defaultSQLPoolName)
	{
		super();
		servletDefaultSQLPool = defaultSQLPoolName;
		}
	
	/**
	 * Gets the Black Rook Servlet Toolkit.
	 */
	public final BRToolkit getToolkit()
	{
		return BRToolkit.getInstance();
		}

	/**
	 * Opens an input stream to a resource using a path relative to the
	 * application context path. 
	 * Outside users should not be able to access this!
	 * @param path the path to the resource to open.
	 * @return an open input stream to the specified resource or null if it couldn't be opened.
	 */
	public final InputStream getResourceAsStream(String path) throws IOException
	{
		return getToolkit().getResourceAsStream(path);
		}
	
	/**
	 * Attempts to grab an available connection from the default 
	 * servlet connection pool and performs a query.
	 * @param query the query (by key) to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the ResultSet returned.
	 */
	public final QueryResult doQuery(String query, Object ... parameters)
	{
		return getToolkit().doQueryPooled(servletDefaultSQLPool, query, parameters);
		}

	/**
	 * Attempts to grab an available connection from the default 
	 * servlet connection pool and performs a query. The provided query
	 * is a literal query - NOT a key that references a query.
	 * @param query the query to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the ResultSet returned.
	 */
	public final QueryResult doQueryInline(String query, Object ... parameters)
	{
		return getToolkit().doQueryPooledInline(servletDefaultSQLPool, query, parameters);
		}

	/**
	 * Attempts to grab an available connection from the default 
	 * servlet connection pool and performs an update query.
	 * @param query the query statement to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the update result returned (usually number of rows affected).
	 */
	public final QueryResult doUpdateQuery(String query, Object ... parameters)
	{
		return getToolkit().doUpdateQueryPooled(servletDefaultSQLPool, query, parameters);
		}

	/**
	 * Attempts to grab an available connection from the default 
	 * servlet connection pool and performs an update query. The provided query
	 * is a literal query - NOT a key that references a query.
	 * @param query the query to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the ResultSet returned.
	 */
	public final QueryResult doUpdateQueryInline(String query, Object ... parameters)
	{
		return getToolkit().doUpdateQueryPooledInline(servletDefaultSQLPool, query, parameters);
		}

}
