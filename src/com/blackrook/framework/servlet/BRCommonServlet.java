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
package com.blackrook.framework.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;

import com.blackrook.commons.hash.HashMap;
import com.blackrook.db.QueryResult;
import com.blackrook.framework.BRFrameworkTask;
import com.blackrook.framework.BRQueryTask;
import com.blackrook.framework.BRRootServlet;

/**
 * Base servlet for all entry points into Black Rook framework servlets.
 * All servlets that use the framework should extend this one.
 * The methods {@link #onGet(HttpServletRequest, HttpServletResponse)}, 
 * {@link #onPost(HttpServletRequest, HttpServletResponse)}, and
 * {@link #onMultiformPost(HttpServletRequest, HttpServletResponse, FileItem[], HashMap)}
 * all send HTTP 405 status codes by default.
 * @author Matthew Tropiano
 */
public abstract class BRCommonServlet extends BRRootServlet
{
	private static final long serialVersionUID = -5794345293732460631L;

	/** Default Servlet SQL Pool. */
	private String servletDefaultSQLPool;
	/** Default Servlet Thread Pool. */
	private String servletDefaultThreadPool;
	
	/**
	 * Base constructor. Sets default pools to default names.
	 * @see {@link BRRootServlet#DEFAULT_POOL_NAME}
	 */
	protected BRCommonServlet()
	{
		this(DEFAULT_POOL_NAME, DEFAULT_POOL_NAME);
		}

	/**
	 * Other constructor. Sets default pools.
	 */
	protected BRCommonServlet(String defaultSQLPoolName, String defaultThreadPoolName)
	{
		super();
		servletDefaultSQLPool = defaultSQLPoolName;
		servletDefaultThreadPool = defaultThreadPoolName;
		}

	/**
	 * Gets the name of the default connection pool that this servlet uses.
	 */
	public String getDefaultSQLPool()
	{
		return servletDefaultSQLPool;
		}

	/**
	 * Sets the name of the default connection pool that this servlet uses.
	 */
	public void setDefaultSQLPool(String servletDefaultSQLPool)
	{
		this.servletDefaultSQLPool = servletDefaultSQLPool;
		}

	/**
	 * Gets the name of the default thread pool that this servlet uses.
	 */
	public String getDefaultThreadPool()
	{
		return servletDefaultThreadPool;
		}

	/**
	 * Sets the name of the default thread pool that this servlet uses.
	 */
	public void setDefaultThreadPool(String servletDefaultThreadPool)
	{
		this.servletDefaultThreadPool = servletDefaultThreadPool;
		}

	@Override
	public void onGet(HttpServletRequest request, HttpServletResponse response)
	{
		sendError(response, 405, "Servlet does not support this method.");
		}

	@Override
	public void onPost(HttpServletRequest request, HttpServletResponse response)
	{
		sendError(response, 405, "Servlet does not support this method.");
		}

	@Override
	public void onMultiformPost(HttpServletRequest request, HttpServletResponse response, FileItem[] fileItems, HashMap<String, String> paramMap)
	{
		sendError(response, 405, "Servlet does not support this method.");
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

	/**
	 * Attempts to grab an available thread from the servlet's default 
	 * thread pool and starts a task that can be monitored by the caller.
	 * @param task the task to run.
	 * @return a framework task encapsulation for monitoring the task.
	 */
	public final BRFrameworkTask spawnTask(BRFrameworkTask task)
	{
		return getToolkit().spawnTaskPooled(servletDefaultThreadPool, task);
		}

	/**
	 * Attempts to grab an available thread from the servlet's default 
	 * thread pool and starts a runnable encapsulated as a 
	 * BRFrameworkTask that can be monitored by the caller.
	 * @param runnable the runnable to run.
	 * @return a framework task encapsulation for monitoring the task.
	 */
	public final BRFrameworkTask spawnRunnable(Runnable runnable)
	{
		return getToolkit().spawnRunnablePooled(servletDefaultThreadPool, runnable);
		}

	/**
	 * Attempts to grab an available connection from the default connection pool and starts a query task
	 * that can be monitored by the caller.
	 * @param query the query (by key) to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return an already-executing query thread, or null if connection acquisition died somehow.
	 */
	public final BRQueryTask spawnQuery(String query, Object ... parameters)
	{
		return getToolkit().spawnQueryPooled(servletDefaultSQLPool, servletDefaultThreadPool, query, parameters);
		}

	/**
	 * Attempts to grab an available connection from the default connection pool 
	 * and starts an update query task that can be monitored by the caller.
	 * @param query the query (by key) to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return an already-executing update query thread, or null if connection acquisition died somehow.
	 */
	public final BRQueryTask spawnUpdateQuery(String query, Object ... parameters)
	{
		return getToolkit().spawnUpdateQueryPooled(servletDefaultSQLPool, servletDefaultThreadPool, query, parameters);
		}

}
