package com.blackrook.framework.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.blackrook.framework.BRFrameworkTask;
import com.blackrook.framework.BRQueryResult;

/**
 * Query task structure for asynchronous queries.
 * The query is set up in the constructor, and started once the task is executed.
 * @author Matthew Tropiano
 */
public class BRQueryTask extends BRFrameworkTask
{
	/** The connection to use for the query. */
	protected Connection connection;
	/** The query string. */
	protected String query;
	/** Is this an update query (as opposed to a data query)? */
	protected boolean isUpdate;
	/** The ResultSet, created after a successful query. */
	protected BRQueryResult result;
	/** The list of parameters supplied. */
	protected Object[] parameters;
	
	/**
	 * Constructor for the query thread.
	 * Assumes data query, as opposed to an update query.
	 * @param connection the database connection to use.
	 * @param query the query string to use.
	 * @param parameters the set of parameters passed for parameterized queries.
	 */
	public BRQueryTask(Connection connection, String query, Object ... parameters)
	{
		this(connection,query,false);
		}

	/**
	 * Constructor for the query task.
	 * @param connection the database connection to use.
	 * @param query the query string to use.
	 * @param isUpdate is this an update query (as opposed to a data query)?
	 * @param parameters the set of parameters passed for parameterized queries.
	 */
	public BRQueryTask(Connection connection, String query, boolean isUpdate, Object ... parameters)
	{
		super();
		if (connection == null || query == null)
			throw new IllegalArgumentException("Connection and query cannot be null.");
		this.connection = connection;
		this.query = query;
		this.isUpdate = isUpdate;
		this.result = null;
		this.parameters = parameters;
		}
	
	@Override
	public void doTask() throws Exception
	{
		try {
			if (isUpdate)
			{
				PreparedStatement st = connection.prepareStatement(query);
				int i = 1;
				for (Object obj : parameters)
					st.setObject(i++, obj);
				result = new BRQueryResult(st.executeUpdate());
				st.close();
				}
			else
			{
				PreparedStatement st = connection.prepareStatement(query);
				int i = 1;
				for (Object obj : parameters)
					st.setObject(i++, obj);
				ResultSet rs = st.executeQuery(query);
				result = new BRQueryResult(rs);
				rs.close();
				st.close();
				}
		} catch (Exception e) {
			throw e;
		} finally {
			connection.close();
			}
		}
	
	/**
	 * Gets the query string used for this thread.
	 */
	public String getQuery()
	{
		return query;
		}
	
	/**
	 * Is this an update query (as opposed to a data query)?
	 */
	public boolean isUpdate()
	{
		return isUpdate;
		}

	/**
	 * Returns the result of the query.
	 * Null if this thread performed an "update" query. 
	 */
	public BRQueryResult getResult()
	{
		return result;
		}

}
