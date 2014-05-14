package com.blackrook.j2ee;

import com.blackrook.j2ee.exception.SimpleFrameworkException;
import com.blackrook.sql.SQLConnectionPool;

/**
 * Class for enabling entry into the SQL pools.
 * @author Matthew Tropiano
 */
public abstract class SQLProvider
{
	/**
	 * Returns a connection pool by name.
	 * @param poolName the name of the connection pool to retrieve.
	 * @return a valid connection pool.
	 * @throws SimpleFrameworkException if the pool could not be found.
	 */
	protected SQLConnectionPool getSQLPool(String poolName)
	{
		return Toolkit.INSTANCE.getSQLPool(poolName);
	}
	
	/**
	 * Gets a query by keyword.
	 * @return the associated query or null if not found. 
	 */
	public String getQueryByName(String keyword)
	{
		return Toolkit.INSTANCE.getQueryByName(keyword);
	}

}
