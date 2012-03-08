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
import java.sql.SQLException;

import com.blackrook.framework.BRFrameworkTask;
import com.blackrook.framework.BRQueryResult;

/**
 * Query task structure for asynchronous query batches.
 * The queries are set up in the constructor, and started once the task is executed.
 * They will not be committed if one query does not succeed.
 * @author Matthew Tropiano
 */
public class BRQueryBatchTask extends BRFrameworkTask
{
	/** The connection to use for the query. */
	protected Connection connection;
	/** The query string. */
	protected String[] queries;
	/** The result sets, created after successful queries. */
	protected BRQueryResult[] results;
	/** The list of parameters supplied for each query. */
	protected Object[][] parameters;
	
	/**
	 * Constructor for the query batch task.
	 * @param connection the database connection to use.
	 * @param queries the query strings to use.
	 * @param parameters the set of parameters passed for parameterized queries.
	 */
	public BRQueryBatchTask(Connection connection, String[] queries, Object[][] parameters)
	{
		super();
		if (connection == null || queries == null)
			throw new IllegalArgumentException("Connection and query cannot be null.");
		this.connection = connection;
		this.queries = queries;
		this.results = null;
		this.parameters = parameters;
		setProgress(0f);
		setProgressMax(queries.length);
		}
	
	@Override
	public void doTask() throws Exception
	{
		connection.setAutoCommit(false);
		for (int n = 0; n < queries.length; n++)
		{
			PreparedStatement st = null;
			try {
				st = connection.prepareStatement(queries[n]);
				int i = 1;
				if (parameters != null && parameters[n] != null) 
					for (Object obj : parameters[n])
						st.setObject(i++, obj);
				int out = st.executeUpdate();
				results[n] = new BRQueryResult(out);
			} catch (SQLException e) {
				throw e;
			} finally {
				if (st != null) st.close();
				connection.rollback();
				connection.setAutoCommit(true);
				connection.close(); // should release
				}
			setProgress(n+1);
			}
		connection.commit();
		connection.setAutoCommit(true);
		connection.close(); // should release
		}
	
	/**
	 * Gets the query strings used for this thread.
	 */
	public String[] getQueries()
	{
		return queries;
		}

	/**
	 * Returns the results of the queries.
	 * Null if this thread performed an "update" query. 
	 */
	public BRQueryResult[] getResults()
	{
		return results;
		}

}
