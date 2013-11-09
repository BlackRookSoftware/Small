package com.blackrook.j2ee;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

import com.blackrook.db.DBUtil;
import com.blackrook.db.QueryResult;

/**
 * A transaction object that holds a connection that guarantees an isolation level
 * of some kind. Queries can be made through this object until it has been released. 
 * <p>
 * This object's {@link #finalize()} method attempts to roll back the transaction if it hasn't already
 * been finished.
 * @author Matthew Tropiano
 */
public final class BRTransaction
{
	/** Enumeration of transaction levels. */
	public static enum Level
	{
		/**
		 * From {@link Connection}: A constant indicating that dirty reads are 
		 * prevented; non-repeatable reads and phantom reads can occur. This 
		 * level only prohibits a transaction from reading a row with uncommitted 
		 * changes in it.
		 */
		READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),
		/**
		 * From {@link Connection}: A constant indicating that dirty reads, 
		 * non-repeatable reads and phantom reads can occur. This level allows 
		 * a row changed by one transaction to be read by another transaction 
		 * before any changes in that row have been committed (a "dirty read"). 
		 * If any of the changes are rolled back, the second transaction will 
		 * have retrieved an invalid row.
		 */
		READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),
		/**
		 * From {@link Connection}: A constant indicating that dirty reads and 
		 * non-repeatable reads are prevented; phantom reads can occur. 
		 * This level prohibits a transaction from reading a row with 
		 * uncommitted changes in it, and it also prohibits the situation 
		 * where one transaction reads a row, a second transaction alters 
		 * the row, and the first transaction rereads the row, getting different 
		 * values the second time (a "non-repeatable read").
		 */
		REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),
		/**
		 * From {@link Connection}: A constant indicating that dirty reads, 
		 * non-repeatable reads and phantom reads are prevented. This level 
		 * includes the prohibitions in TRANSACTION_REPEATABLE_READ and further 
		 * prohibits the situation where one transaction reads all rows that 
		 * satisfy a WHERE condition, a second transaction inserts a row that 
		 * satisfies that WHERE condition, and the first transaction rereads for 
		 * the same condition, retrieving the additional "phantom" row in the 
		 * second read.
		 */
		SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE),
		;
		
		private final int id;
		private Level(int id)
		{
			this.id = id;
			}
	}
	
	/** The toolkit for this framework. */
	private BRToolkit toolkit;
	/** The encapsulated connection. */
	private Connection connection;
	
	/** Previous level state on the incoming connection. */
	private int levelState;
	
	/**
	 * Wraps a connection in a transaction.
	 * The connection gets {@link Connection#setAutoCommit(boolean)} called on it with a FALSE parameter,
	 * and sets the transaction isolation level. These 
	 * @param toolkit the {@link BRToolkit}.
	 * @param connection the connection to the database to use for this transaction.
	 * @param transactionLevel the transaction level to set on this transaction.
	 * @throws BRFrameworkException if the transaction could not be created.
	 */
	BRTransaction(BRToolkit toolkit, Connection connection, Level transactionLevel)
	{
		this.toolkit = toolkit;
		this.connection = connection;
		
		try {
			this.levelState = connection.getTransactionIsolation();
			connection.setAutoCommit(false);
			connection.setTransactionIsolation(transactionLevel.id);
		} catch (SQLException e) {
			throw new BRFrameworkException(e);
			}
		}

	/**
	 * Returns true if this transaction has been completed or false if
	 * no more methods can be invoked on it.
	 */
	public boolean isFinished()
	{
		return connection == null; 
		}	
	
	/**
	 * Completes this transaction and prevents further calls on it.
	 * This calls {@link Connection#commit()} and {@link Connection#close()} 
	 * on the encapsulated connection and resets its previous state plus its auto-commit state to true.
	 * @throws IllegalStateException if this transaction was already finished.
	 * @throws BRFrameworkException if this causes a database error.
	 */
	public void finish()
	{
		if (isFinished())
			throw new IllegalStateException("This transaction is already finished.");
		
		commit();
		try {
			connection.setTransactionIsolation(levelState);
			connection.setAutoCommit(true);
			connection.close();
		} catch (SQLException e) {
			throw new BRFrameworkException(e);
			}
		connection = null;
		}
	
	/**
	 * Commits the actions completed so far in this transaction.
	 * This is also called during {@link #finish()}.
	 * @throws IllegalStateException if this transaction was already finished.
	 * @throws BRFrameworkException if this causes a database error.
	 */
	public void commit()
	{
		if (isFinished())
			throw new IllegalStateException("This transaction is already finished.");
		try {
			connection.commit();
		} catch (SQLException e) {
			throw new BRFrameworkException(e);
			}
		}
	
	/**
	 * Rolls back this entire transaction.
	 * @throws IllegalStateException if this transaction was already finished.
	 * @throws SQLException if this causes a database error.
	 */
	public void rollback()
	{
		if (isFinished())
			throw new IllegalStateException("This transaction is already finished.");
		try {
			connection.rollback();
		} catch (SQLException e) {
			throw new BRFrameworkException(e);
			}
		}
	
	/**
	 * Rolls back this transaction to a {@link Savepoint}. Everything executed
	 * after the {@link Savepoint} passed into this method will be rolled back.
	 * @param savepoint the {@link Savepoint} to roll back to.
	 * @throws IllegalStateException if this transaction was already finished.
	 * @throws BRFrameworkException if this causes a database error.
	 */
	public void rollback(Savepoint savepoint)
	{
		if (isFinished())
			throw new IllegalStateException("This transaction is already finished.");
		try {
			connection.rollback(savepoint);
		} catch (SQLException e) {
			throw new BRFrameworkException(e);
			}
		}
	
	/**
	 * Calls {@link Connection#setSavepoint()} on the encapsulated connection.
	 * @return a generated {@link Savepoint} of this transaction.
	 * @throws IllegalStateException if this transaction was already finished.
	 * @throws BRFrameworkException if this causes a database error.
	 */
	public Savepoint setSavepoint()
	{
		if (isFinished())
			throw new IllegalStateException("This transaction is already finished.");
		Savepoint out = null;
		try {
			out = connection.setSavepoint();
		} catch (SQLException e) {
			throw new BRFrameworkException(e);
			}
		
		return out;
		}
	
	/**
	 * Calls {@link Connection#setSavepoint()} on the encapsulated connection.
	 * @param name the name of the savepoint.
	 * @return a generated {@link Savepoint} of this transaction.
	 * @throws IllegalStateException if this transaction was already finished.
	 * @throws BRFrameworkException if this causes a database error.
	 */
	public Savepoint setSavepoint(String name)
	{
		if (isFinished())
			throw new IllegalStateException("This transaction is already finished.");
		Savepoint out = null;
		try {
			out = connection.setSavepoint(name);
		} catch (SQLException e) {
			throw new BRFrameworkException(e);
			}
		
		return out;
		}
	
	/**
	 * Performs a query on this transaction.
	 * @param queryKey the query (by key) to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the QueryResult returned.
	 * @throws BRFrameworkException if the query cannot be resolved or the query causes an error.
	 */
	public QueryResult doQuery(String queryKey, Object ... parameters)
	{
		String query = toolkit.getQueryByName(queryKey);
		if (query == null)
			throw new BRFrameworkException("Query could not be loaded/cached - "+queryKey);

		return doQueryInline(query, parameters);
		}

	/**
	 * Performs a query on this transaction. The provided query
	 * is a literal query - NOT a key that references a query.
	 * @param query the query to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the QueryResult returned.
	 * @throws BRFrameworkException if the query causes an error.
	 */
	public QueryResult doQueryInline(String query, Object ... parameters)
	{
		QueryResult result = null;
		try {
			result = DBUtil.doQuery(connection, query, parameters); 
		} catch (SQLException e) {
			throw new BRFrameworkException(e);
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
	 * @param queryKey the query (by key) to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the QueryResult returned.
	 * @throws BRFrameworkException if the query cannot be resolved or the query causes an error.
	 * @throws ClassCastException if one object type cannot be converted to another.
	 */
	public <T> T[] doQuery(Class<T> type, String queryKey, Object ... parameters)
	{
		String query = toolkit.getQueryByName(queryKey);
		if (query == null)
			throw new BRFrameworkException("Query could not be loaded/cached - "+queryKey);

		return doQueryInline(type, query, parameters);
		}

	/**
	 * Attempts to grab an available connection from the default 
	 * servlet connection pool and performs a query and creates objects from it, setting relevant fields.
	 * The provided query is a literal query - NOT a key that references a query.
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
	 * @return the QueryResult returned.
	 * @throws BRFrameworkException if the query causes an error.
	 * @throws ClassCastException if one object type cannot be converted to another.
	 */
	public <T> T[] doQueryInline(Class<T> type, String query, Object ... parameters)
	{
		T[] result = null;
		try {
			result = DBUtil.doQuery(type, connection, query, parameters); 
		} catch (SQLException e) {
			throw new BRFrameworkException(e);
			}
		return result;
		}

	/**
	 * Performs an update query on this transaction.
	 * @param queryKey the query (by key) to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the update result returned (usually number of rows affected).
	 * @throws BRFrameworkException if the query cannot be resolved or the query causes an error.
	 */
	public QueryResult doUpdateQuery(String queryKey, Object ... parameters)
	{
		String query = toolkit.getQueryByName(queryKey);
		if (query == null)
			throw new BRFrameworkException("Query could not be loaded/cached - "+queryKey);

		return doUpdateQueryInline(query, parameters);
		}

	/**
	 * Performs an update query on this transaction. The provided query
	 * is a literal query - NOT a key that references a query.
	 * @param query the query to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the QueryResult returned.
	 * @throws BRFrameworkException if the query causes an error.
	 */
	public QueryResult doUpdateQueryInline(String query, Object ... parameters)
	{
		QueryResult result = null;
		try {
			result = DBUtil.doQueryUpdate(connection, query, parameters); 
		} catch (SQLException e) {
			throw new BRFrameworkException(e);
			}
		return result;
		}

	@Override
	protected void finalize() throws Throwable
	{
		if (!isFinished())
			rollback();
		super.finalize();
		}
	
}
