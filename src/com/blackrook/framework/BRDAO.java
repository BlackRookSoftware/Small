package com.blackrook.framework;

import com.blackrook.db.QueryResult;
import com.blackrook.framework.BRToolkit;

/**
 * Data access object for submitting database queries.
 * @author Matthew Tropiano
 */
public abstract class BRDAO
{
	/** Default SQL Pool. */
	private String defaultSQLPool;

	/**
	 * Base constructor.
	 */
	protected BRDAO()
	{
		this(BRToolkit.DEFAULT_POOL_NAME);
		}

	/**
	 * Other constructor. Sets default pools.
	 */
	protected BRDAO(String defaultThreadPoolName)
	{
		super();
		defaultSQLPool = defaultThreadPoolName;
		}

	/**
	 * Gets the Black Rook Framework Toolkit.
	 */
	public final BRToolkit getToolkit()
	{
		return BRToolkit.INSTANCE;
		}


	/**
	 * Gets the name of the default connection pool that this DAO uses.
	 */
	public String getDefaultSQLPool()
	{
		return defaultSQLPool;
		}

	/**
	 * Sets the name of the default connection pool that this DAO uses.
	 */
	public void setDefaultSQLPool(String servletDefaultSQLPool)
	{
		this.defaultSQLPool = servletDefaultSQLPool;
		}

	/**
	 * Attempts to grab an available connection from the default 
	 * servlet connection pool and performs a query.
	 * @param query the query (by key) to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the ResultSet returned.
	 */
	protected final QueryResult doQuery(String query, Object ... parameters)
	{
		return getToolkit().doQueryPooled(defaultSQLPool, query, parameters);
		}

	/**
	 * Attempts to grab an available connection from the default 
	 * servlet connection pool and performs a query. The provided query
	 * is a literal query - NOT a key that references a query.
	 * @param query the query to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the ResultSet returned.
	 */
	protected final QueryResult doQueryInline(String query, Object ... parameters)
	{
		return getToolkit().doQueryPooledInline(defaultSQLPool, query, parameters);
		}

	/**
	 * Attempts to grab an available connection from the default 
	 * servlet connection pool and performs an update query.
	 * @param query the query statement to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the update result returned (usually number of rows affected).
	 */
	protected final QueryResult doUpdateQuery(String query, Object ... parameters)
	{
		return getToolkit().doUpdateQueryPooled(defaultSQLPool, query, parameters);
		}

	/**
	 * Attempts to grab an available connection from the default 
	 * servlet connection pool and performs an update query. The provided query
	 * is a literal query - NOT a key that references a query.
	 * @param query the query to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the ResultSet returned.
	 */
	protected final QueryResult doUpdateQueryInline(String query, Object ... parameters)
	{
		return getToolkit().doUpdateQueryPooledInline(defaultSQLPool, query, parameters);
		}
	
}
