package com.blackrook.framework;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import com.blackrook.commons.hash.CaseInsensitiveHashMap;
import com.blackrook.commons.vector.VolatileVector;

/**
 * The data encapsulation of the result of a query.
 * @author Matthew Tropiano
 */
public class BRQueryResult
{
	private static final String[] EMPTY_ARRAY = new String[0];
	
	/** Query Columns. */
	protected String[] columnNames;
	/** Rows affected or returned in the query. */
	protected int rowCount;
	/** Was this an update query? */
	protected boolean update;
	/** Set of hash maps of associative data. */
	protected VolatileVector<Row> rows;
	
	/**
	 * Creates a new query result from an update query. 
	 */
	public BRQueryResult(int rowsAffected)
	{
		columnNames = EMPTY_ARRAY;
		update = true;
		rowCount = rowsAffected;
		rows = null;
		}

	/**
	 * Creates a new query result from a result set. 
	 */
	public BRQueryResult(ResultSet rs) throws SQLException
	{
		update = false;
		rowCount = 0;

		ResultSetMetaData rsmd = rs.getMetaData();
		columnNames = new String[rsmd.getColumnCount()];
		for (int n = 0; n < columnNames.length; n++)
			columnNames[n] = rsmd.getColumnName(n+1);

		rows = new VolatileVector<Row>();
		while (rs.next())
		{
			rows.add(new Row(rs, columnNames.length));
			rowCount++;
			}
		}
	
	/**
	 * Gets the names of the columns.
	 */
	public String[] getColumnNames()
	{
		return columnNames;
		}

	/**
	 * Gets the amount of affected/returned rows from this query. 
	 */
	public int getRowCount()
	{
		return rowCount;
		}

	/**
	 * Returns true if this came from an update.
	 */
	public boolean isUpdate()
	{
		return update;
		}
	
	/**
	 * Retrieves the rows from the query result.
	 */
	public VolatileVector<Row> getRows()
	{
		return rows;
		}

	/**
	 * Row object.
	 */
	public class Row extends CaseInsensitiveHashMap<Object>
	{
		Row(ResultSet rs, int c) throws SQLException
		{
			super(columnNames.length);
			for (int i = 0; i < c; i++)
				put(columnNames[i], rs.getObject(i+1));
			}
		}

}
