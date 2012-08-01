package com.blackrook.framework.filter;

import java.io.IOException;
import java.io.InputStream;

import com.blackrook.db.QueryResult;
import com.blackrook.framework.BRFrameworkTask;
import com.blackrook.framework.BRQueryTask;
import com.blackrook.framework.BRRootFilter;

/**
 * The common root filter.
 * @author Matthew Tropiano
 */
public class BRCommonFilter extends BRRootFilter
{
	/** Default Filter SQL Pool. */
	private String filterDefaultSQLPool;
	/** Default Filter Thread Pool. */
	private String filterDefaultThreadPool;
	
	/**
	 * Creates a new filter that uses the default database and thread pools.
	 */
	protected BRCommonFilter()
	{
		this(DEFAULT_POOL_NAME, DEFAULT_POOL_NAME);
		}
	
	/**
	 * Creates a new filter that uses the database and thread pools provided.
	 */
	protected BRCommonFilter(String defaultSQLPoolName, String defaultThreadPoolName)
	{
		super();
		filterDefaultSQLPool = defaultSQLPoolName;
		filterDefaultThreadPool = defaultThreadPoolName;
		}
	
	/**
	 * Gets the name of the default connection pool that this filter uses.
	 */
	public String getDefaultSQLPool()
	{
		return filterDefaultSQLPool;
		}

	/**
	 * Sets the name of the default connection pool that this filter uses.
	 */
	public void setDefaultSQLPool(String filterDefaultSQLPool)
	{
		this.filterDefaultSQLPool = filterDefaultSQLPool;
		}

	/**
	 * Gets the name of the default thread pool that this filter uses.
	 */
	public String getDefaultThreadPool()
	{
		return filterDefaultThreadPool;
		}

	/**
	 * Sets the name of the default thread pool that this filter uses.
	 */
	public void setDefaultThreadPool(String filterDefaultThreadPool)
	{
		this.filterDefaultThreadPool = filterDefaultThreadPool;
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
		return getToolkit().doQueryPooled(filterDefaultSQLPool, query, parameters);
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
		return getToolkit().doQueryPooledInline(filterDefaultSQLPool, query, parameters);
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
		return getToolkit().doUpdateQueryPooled(filterDefaultSQLPool, query, parameters);
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
		return getToolkit().doUpdateQueryPooledInline(filterDefaultSQLPool, query, parameters);
		}

	/**
	 * Attempts to grab an available thread from the servlet's default 
	 * thread pool and starts a task that can be monitored by the caller.
	 * @param task the task to run.
	 * @return a framework task encapsulation for monitoring the task.
	 */
	public final BRFrameworkTask spawnTask(BRFrameworkTask task)
	{
		return getToolkit().spawnTaskPooled(filterDefaultThreadPool, task);
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
		return getToolkit().spawnRunnablePooled(filterDefaultThreadPool, runnable);
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
		return getToolkit().spawnQueryPooled(filterDefaultSQLPool, filterDefaultThreadPool, query, parameters);
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
		return getToolkit().spawnQueryPooled(sqlPoolName, filterDefaultThreadPool, query, parameters);
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
		return getToolkit().spawnUpdateQueryPooled(filterDefaultSQLPool, filterDefaultThreadPool, query, parameters);
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
		return getToolkit().spawnUpdateQueryPooled(sqlPoolName, filterDefaultThreadPool, query, parameters);
		}

}
