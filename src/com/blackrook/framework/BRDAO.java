package com.blackrook.framework;

import com.blackrook.db.QueryResult;
import com.blackrook.framework.BRToolkit;
import com.blackrook.framework.BRTransaction.Level;

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
	 * Generates a transaction for multiple queries in one set.
	 * This transaction performs all of its queries through one connection.
	 * The connection is held by this transaction until it is finished via {@link BRTransaction#finish()}.
	 * @param transactionLevel the isolation level of the transaction.
	 * @return a {@link BRTransaction} object to handle a contiguous transaction.
	 * @throws BRFrameworkException if the transaction could not be created.
	 */
	protected BRTransaction startTransaction(Level transactionLevel)
	{
		return getToolkit().startTransaction(defaultSQLPool, transactionLevel);
		}
	
	/**
	 * Attempts to grab an available connection from the default 
	 * servlet connection pool and performs a query.
	 * @param queryKey the query (by key) to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the QueryResult returned.
	 * @throws BRFrameworkException if the query cannot be resolved or the query causes an error.
	 */
	protected final QueryResult doQuery(String queryKey, Object ... parameters)
	{
		return getToolkit().doQueryPooled(defaultSQLPool, queryKey, parameters);
		}

	/**
	 * Attempts to grab an available connection from the default 
	 * servlet connection pool and performs a query. The provided query
	 * is a literal query - NOT a key that references a query.
	 * @param query the query to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the QueryResult returned.
	 * @throws BRFrameworkException if the query causes an error.
	 */
	protected final QueryResult doQueryInline(String query, Object ... parameters)
	{
		return getToolkit().doQueryPooledInline(defaultSQLPool, query, parameters);
		}

	/**
	 * Attempts to grab an available connection from the default 
	 * servlet connection pool and performs an update query.
	 * @param queryKey the query statement to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the update result returned (usually number of rows affected).
	 * @throws BRFrameworkException if the query cannot be resolved or the query causes an error.
	 */
	protected final QueryResult doUpdateQuery(String queryKey, Object ... parameters)
	{
		return getToolkit().doUpdateQueryPooled(defaultSQLPool, queryKey, parameters);
		}

	/**
	 * Attempts to grab an available connection from the default 
	 * servlet connection pool and performs an update query. The provided query
	 * is a literal query - NOT a key that references a query.
	 * @param query the query to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the QueryResult returned.
	 * @throws BRFrameworkException if the query causes an error.
	 */
	protected final QueryResult doUpdateQueryInline(String query, Object ... parameters)
	{
		return getToolkit().doUpdateQueryPooledInline(defaultSQLPool, query, parameters);
		}
	
}
