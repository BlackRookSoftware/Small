package com.blackrook.j2ee.small.dao;

import java.sql.SQLException;

import com.blackrook.sql.SQLConnectionPool;
import com.blackrook.sql.SQLConnector;

/**
 * Describes a data source or pool that is used by SQL databases.
 * @see SQLConnectionPool
 * @author Matthew Tropiano
 */
public class SQLDataSource extends SQLConnectionPool
{
	/**
	 * Creates a new data source.
	 * @param className The fully qualified class name of the JDBC driver.
	 * @param jdbcURL The JDBC URL to use.
	 * @param connections the number of connections to pool.
	 * @param userName the account user name.
	 * @param password the account password.
	 * @throws SQLException
	 */
	public SQLDataSource(String className, String jdbcURL, int connections, String userName, String password) throws SQLException
	{
		super(new SQLConnector(className, jdbcURL), connections, userName, password);
	}

}
