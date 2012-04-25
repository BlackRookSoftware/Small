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
import com.blackrook.framework.tasks.BRQueryTask;
import com.blackrook.sync.Task;

/**
 * A task that runs asynchronously from the rest of the application.
 * Classes that extend this one should make
 * @author Matthew Tropiano
 */
public abstract class BRFrameworkTask extends Task
{

	/** Progress value. */
	private float progress;
	/** Max Progress value. */
	private float progressMax;

	/** Default Servlet SQL Pool. */
	private String servletDefaultSQLPool;
	/** Default Servlet Thread Pool. */
	private String servletDefaultThreadPool;

	/**
	 * Creates a new runnable task that uses the default database and thread pools.
	 */
	protected BRFrameworkTask()
	{
		this("default","default");
		}
	
	/**
	 * Creates a new runnable task that uses the database and thread pools provided.
	 */
	protected BRFrameworkTask(String defaultSQLPoolName, String defaultThreadPoolName)
	{
		super();
		servletDefaultSQLPool = defaultSQLPoolName;
		servletDefaultThreadPool = defaultThreadPoolName;
		}
	
	/**
	 * Sets the current progress of this task.
	 */
	protected void setProgress(float progress)
	{
		this.progress = progress;
		}

	/**
	 * Sets the max progress value of this task.
	 */
	protected void setProgressMax(float progressMax)
	{
		this.progressMax = progressMax;
		}

	/**
	 * Gets the current progress of this task.
	 */
	public final float getProgress()
	{
		return progress;
		}

	/**
	 * Gets the max progress value of this task.
	 */
	public final float getProgressMax()
	{
		return progressMax;
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
		return BRRootManager.getResourceAsStream(path);
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
		return BRRootManager.doQueryPooled(servletDefaultSQLPool, query, parameters);
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
		return BRRootManager.doQueryPooledInline(servletDefaultSQLPool, query, parameters);
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
		return BRRootManager.doUpdateQueryPooled(servletDefaultSQLPool, query, parameters);
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
		return BRRootManager.doUpdateQueryPooledInline(servletDefaultSQLPool, query, parameters);
		}

	/**
	 * Attempts to grab an available thread from the servlet's default 
	 * thread pool and starts a task that can be monitored by the caller.
	 * @param task the task to run.
	 * @return a framework task encapsulation for monitoring the task.
	 */
	public final BRFrameworkTask spawnTask(BRFrameworkTask task)
	{
		return BRRootManager.spawnTaskPooled(servletDefaultThreadPool, task);
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
		return BRRootManager.spawnRunnablePooled(servletDefaultThreadPool, runnable);
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
		return BRRootManager.spawnQueryPooled(servletDefaultSQLPool, servletDefaultThreadPool, query, parameters);
		}

	/**
	 * Attempts to grab an available connection from a connection pool and starts a query task
	 * that can be monitored by the caller.
	 * @param sqlPoolName the SQL connection pool name to use.
	 * @param query the query (by key) to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return an already-executing query thread, or null if connection acquisition died somehow.
	 */
	public final BRQueryTask spawnQuery(String sqlPoolName, String query, Object ... parameters)
	{
		return BRRootManager.spawnQueryPooled(sqlPoolName, servletDefaultThreadPool, query, parameters);
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
		return BRRootManager.spawnUpdateQueryPooled(servletDefaultSQLPool, servletDefaultThreadPool, query, parameters);
		}

	/**
	 * Attempts to grab an available connection from a connection pool 
	 * and starts an update query task that can be monitored by the caller.
	 * @param sqlPoolName the SQL connection pool name to use.
	 * @param query the query (by key) to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return an already-executing update query thread, or null if connection acquisition died somehow.
	 */
	public final BRQueryTask spawnUpdateQuery(String sqlPoolName, String query, Object ... parameters)
	{
		return BRRootManager.spawnUpdateQueryPooled(sqlPoolName, servletDefaultThreadPool, query, parameters);
		}

}
