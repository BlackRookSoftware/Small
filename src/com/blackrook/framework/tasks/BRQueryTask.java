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
package com.blackrook.framework.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import com.blackrook.db.QueryResult;
import com.blackrook.framework.BRFrameworkTask;

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
	protected QueryResult result;
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
				PreparedStatement st = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
				int i = 1;
				for (Object obj : parameters)
					st.setObject(i++, obj);
				result = new QueryResult(st.executeUpdate(), st.getGeneratedKeys());
				st.close();
				}
			else
			{
				PreparedStatement st = connection.prepareStatement(query);
				int i = 1;
				for (Object obj : parameters)
					st.setObject(i++, obj);
				ResultSet rs = st.executeQuery(query);
				result = new QueryResult(rs);
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
	public QueryResult getResult()
	{
		return result;
		}

}
