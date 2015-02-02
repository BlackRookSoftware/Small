package com.blackrook.j2ee.small.dao;

import java.sql.Connection;
import java.sql.SQLException;

import com.blackrook.j2ee.small.DefaultQueryResolver;
import com.blackrook.j2ee.small.QueryResolver;
import com.blackrook.j2ee.small.exception.SmallFrameworkException;
import com.blackrook.sql.SQLResult;
import com.blackrook.sql.SQLTransaction;
import com.blackrook.sql.SQLTransaction.Level;
import com.blackrook.sql.SQLUtil;

/**
 * Base Data Access Object for submitting SQL-driven database queries.
 * @author Matthew Tropiano
 */
public abstract class SmallSQLDAO
{
	/** SQL Datasource. */
	private SQLDataSource dataSource;
	/** Query resolver class. */
	private QueryResolver queryResolver;

	/**
	 * Base constructor.
	 * @param dataSource the data source to use.
	 */
	protected SmallSQLDAO(SQLDataSource dataSource)
	{
		this(dataSource, new DefaultQueryResolver());
	}

	/**
	 * Base constructor.
	 * @param dataSource the data source to use.
	 * @param queryResolver the query resolver to use for query-by-keyword lookups.
	 */
	protected SmallSQLDAO(SQLDataSource dataSource, QueryResolver queryResolver)
	{
		this.dataSource = dataSource;
		this.queryResolver = queryResolver;
	}

	/**
	 * Gets a query by keyword, using the current {@link QueryResolver} assigned
	 * to this class.
	 * @return the associated query or null if not found. 
	 */
	protected String getQueryByName(String keyword)
	{
		return queryResolver.resolveQuery(keyword);
	}

	/**
	 * Generates a transaction for multiple queries in one set.
	 * This transaction performs all of its queries through one connection.
	 * The connection is held by this transaction until it is finished via either {@link SQLTransaction#complete()}
	 * or {@link SQLTransaction#abort()}.
	 * @param transactionLevel the isolation level of the transaction.
	 * @return a {@link SQLTransaction} object to handle a contiguous transaction.
	 * @throws SmallFrameworkException if the transaction could not be created.
	 */
	protected final SQLTransaction startTransaction(Level transactionLevel)
	{
		try {
			return dataSource.startTransaction(transactionLevel);
		} catch (InterruptedException e) {
			throw new SmallFrameworkException("Connection acquisition has been interrupted unexpectedly: "+e.getLocalizedMessage());
		}
	}
	
	/**
	 * Attempts to grab an available connection from the default 
	 * servlet connection pool and performs a query.
	 * @param query the query to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the SQLResult returned.
	 * @throws SmallFrameworkException if the query cannot be resolved or the query causes an error.
	 */
	protected final SQLResult doQuery(String query, Object ... parameters)
	{
		Connection conn = null;
		SQLResult result = null;		
		try {
			conn = dataSource.getAvailableConnection();
			result = SQLUtil.doQuery(conn, query, parameters);
		} catch (SQLException e) {
			throw new SmallFrameworkException(e);
		} catch (InterruptedException e) {
			throw new SmallFrameworkException("Connection acquisition has been interrupted unexpectedly: "+e.getLocalizedMessage());
		} finally {
			if (conn != null) try {conn.close();} catch (SQLException e) {};
		}
		return result;
	}

	/**
	 * Attempts to grab an available connection from the default servlet connection pool 
	 * and performs a query and creates objects from it, setting relevant fields.
	 * <p>
	 * Each result row is applied via the target object's public fields and setter methods.
	 * <p>
	 * For instance, if there is a column is a row called "color", its value
	 * will be applied via the public field "color" or the setter "setColor()". Public
	 * fields take precedence over setters.
	 * <p>
	 * Only certain types are converted without issue. Below is a set of source types
	 * and their valid target types:
	 * <table>
	 * <tr>
	 * 		<td><b>Boolean</b></td>
	 * 		<td>
	 * 			Boolean, all numeric primitives and their autoboxed equivalents, String. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>Number</b></td>
	 * 		<td>
	 * 			Boolean (zero is false, nonzero is true), all numeric primitives and their autoboxed equivalents, String,
	 * 			Date, Timestamp. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>Timestamp</b></td>
	 * 		<td>
	 * 			Long (both primitive and object as milliseconds since the Epoch), Timestamp, Date, String 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>Date</b></td>
	 * 		<td>
	 * 			Long (both primitive and object as milliseconds since the Epoch), Timestamp, Date, String 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>String</b></td>
	 * 		<td>
	 * 			Boolean, all numeric primitives and their autoboxed equivalents, 
	 * 			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>Clob</b></td>
	 * 		<td>
	 * 			Boolean, all numeric primitives and their autoboxed equivalents, 
	 * 			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>Blob</b></td>
	 * 		<td> 
	 * 			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>Clob</b></td>
	 * 		<td>
	 * 			Boolean, all numeric primitives and their autoboxed equivalents, 
	 * 			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>byte[]</b></td>
	 * 		<td>
	 *			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>char[]</b></td>
	 * 		<td>
	 * 			Boolean, all numeric primitives and their autoboxed equivalents, 
	 * 			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * </table>
	 * @param type the class type to instantiate.
	 * @param query the query to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the SQLResult returned.
	 * @throws SmallFrameworkException if the query cannot be resolved or the query causes an error.
	 * @throws ClassCastException if one object type cannot be converted to another.
	 */
	protected final <T> T[] doQuery(Class<T> type, String query, Object ... parameters)
	{
		Connection conn = null;
		T[] result = null;		
		try {
			conn = dataSource.getAvailableConnection();
			result = SQLUtil.doQuery(type, conn, query, parameters);
		} catch (SQLException e) {
			throw new SmallFrameworkException(e);
		} catch (InterruptedException e) {
			throw new SmallFrameworkException("Connection acquisition has been interrupted unexpectedly: "+e.getLocalizedMessage());
		} finally {
			if (conn != null) try {conn.close();} catch (SQLException e) {};
		}
		return result;
	}

	/**
	 * Attempts to grab an available connection from the default 
	 * servlet connection pool and performs an update query.
	 * @param query the query statement to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the update result returned (usually number of rows affected).
	 * @throws SmallFrameworkException if the query cannot be resolved or the query causes an error.
	 */
	protected final SQLResult doUpdateQuery(String query, Object ... parameters)
	{
		Connection conn = null;
		SQLResult result = null;
		try {
			conn = dataSource.getAvailableConnection();
			result = SQLUtil.doQueryUpdate(conn, query, parameters);
		} catch (SQLException e) {
			throw new SmallFrameworkException(e);
		} catch (InterruptedException e) {
			throw new SmallFrameworkException("Connection acquisition has been interrupted unexpectedly: "+e.getLocalizedMessage());
		} finally {
			if (conn != null) try {conn.close();} catch (SQLException e) {};
		}
		return result;
	}

}
