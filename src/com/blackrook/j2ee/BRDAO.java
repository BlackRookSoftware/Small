package com.blackrook.j2ee;

import com.blackrook.j2ee.BRToolkit;
import com.blackrook.j2ee.BRTransaction.Level;
import com.blackrook.sql.SQLResult;

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
	private BRToolkit getToolkit()
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
	 * The connection is held by this transaction until it is finished via {@link SQLTransaction#finish()}.
	 * @param transactionLevel the isolation level of the transaction.
	 * @return a {@link SQLTransaction} object to handle a contiguous transaction.
	 * @throws BRFrameworkException if the transaction could not be created.
	 */
	protected final BRTransaction startTransaction(Level transactionLevel)
	{
		return getToolkit().startTransaction(defaultSQLPool, transactionLevel);
		}
	
	/**
	 * Attempts to grab an available connection from the default 
	 * servlet connection pool and performs a query.
	 * @param queryKey the query (by key) to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the SQLResult returned.
	 * @throws BRFrameworkException if the query cannot be resolved or the query causes an error.
	 */
	protected final SQLResult doQuery(String queryKey, Object ... parameters)
	{
		return getToolkit().doQueryPooled(defaultSQLPool, queryKey, parameters);
		}

	/**
	 * Attempts to grab an available connection from the default 
	 * servlet connection pool and performs a query. The provided query
	 * is a literal query - NOT a key that references a query.
	 * @param query the query to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the SQLResult returned.
	 * @throws BRFrameworkException if the query causes an error.
	 */
	protected final SQLResult doQueryInline(String query, Object ... parameters)
	{
		return getToolkit().doQueryPooledInline(defaultSQLPool, query, parameters);
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
	 * @return the SQLResult returned.
	 * @throws BRFrameworkException if the query cannot be resolved or the query causes an error.
	 * @throws ClassCastException if one object type cannot be converted to another.
	 */
	protected final <T> T[] doQuery(Class<T> type, String queryKey, Object ... parameters)
	{
		return getToolkit().doQueryPooled(defaultSQLPool, type, queryKey, parameters);
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
	 * @return the SQLResult returned.
	 * @throws BRFrameworkException if the query causes an error.
	 * @throws ClassCastException if one object type cannot be converted to another.
	 */
	protected final <T> T[] doQueryInline(Class<T> type, String query, Object ... parameters)
	{
		return getToolkit().doQueryPooledInline(defaultSQLPool, type, query, parameters);
		}

	/**
	 * Attempts to grab an available connection from the default 
	 * servlet connection pool and performs an update query.
	 * @param queryKey the query statement to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the update result returned (usually number of rows affected).
	 * @throws BRFrameworkException if the query cannot be resolved or the query causes an error.
	 */
	protected final SQLResult doUpdateQuery(String queryKey, Object ... parameters)
	{
		return getToolkit().doUpdateQueryPooled(defaultSQLPool, queryKey, parameters);
		}

	/**
	 * Attempts to grab an available connection from the default 
	 * servlet connection pool and performs an update query. The provided query
	 * is a literal query - NOT a key that references a query.
	 * @param query the query to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the SQLResult returned.
	 * @throws BRFrameworkException if the query causes an error.
	 */
	protected final SQLResult doUpdateQueryInline(String query, Object ... parameters)
	{
		return getToolkit().doUpdateQueryPooledInline(defaultSQLPool, query, parameters);
		}
	
}
