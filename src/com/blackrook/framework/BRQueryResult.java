package com.blackrook.framework;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import com.blackrook.commons.Common;
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
	 * Gets the first row, or only row in this result,
	 * or null if no rows.
	 */
	public Row getRow()
	{
		return rows.size() > 0 ? rows.getByIndex(0) : null;
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
		
		/**
		 * Returns if this column's value is null.
		 */
		public boolean getNull(String columnName)
		{
			return get(columnName) != null;
			}
		
		/**
		 * Returns the boolean value of this object.
		 */
		public boolean getBoolean(String columnName)
		{
			Object obj = get(columnName);
			if (obj instanceof Boolean)
				return (Boolean)obj;
			else if (obj instanceof Number)
				return ((Number)obj).doubleValue() != 0.0f;
			else if (obj instanceof String)
				return Common.parseBoolean((String)obj);
			return false;
			}
		
		/**
		 * Returns the byte value of this object.
		 */
		public byte getByte(String columnName)
		{
			Object obj = get(columnName);
			if (obj instanceof Boolean)
				return ((Boolean)obj) ? (byte)1 : (byte)0;
			else if (obj instanceof Number)
				return ((Number)obj).byteValue();
			else if (obj instanceof String)
				return Common.parseByte((String)obj);
			return (byte)0;
			}
		
		/**
		 * Returns the short value of this object.
		 */
		public short getShort(String columnName)
		{
			Object obj = get(columnName);
			if (obj instanceof Boolean)
				return ((Boolean)obj) ? (short)1 : (short)0;
			else if (obj instanceof Number)
				return ((Number)obj).shortValue();
			else if (obj instanceof String)
				return Common.parseShort((String)obj);
			return (short)0;
			}
		
		/**
		 * Returns the integer value of this object.
		 */
		public int getInt(String columnName)
		{
			Object obj = get(columnName);
			if (obj instanceof Boolean)
				return ((Boolean)obj) ? 1 : 0;
			else if (obj instanceof Number)
				return ((Number)obj).intValue();
			else if (obj instanceof String)
				return Common.parseInt((String)obj);
			return 0;
			}
		
		/**
		 * Returns the float value of this object.
		 */
		public float getFloat(String columnName)
		{
			Object obj = get(columnName);
			if (obj instanceof Boolean)
				return ((Boolean)obj) ? 1f : 0f;
			else if (obj instanceof Number)
				return ((Number)obj).floatValue();
			else if (obj instanceof String)
				return Common.parseFloat((String)obj);
			return 0f;
			}
		
		/**
		 * Returns the long value of this object.
		 */
		public long getLong(String columnName)
		{
			Object obj = get(columnName);
			if (obj instanceof Boolean)
				return ((Boolean)obj) ? 1L : 0L;
			else if (obj instanceof Number)
				return ((Number)obj).longValue();
			else if (obj instanceof String)
				return Common.parseLong((String)obj);
			return 0L;
			}
		
		/**
		 * Returns the double value of this object.
		 */
		public double getDouble(String columnName)
		{
			Object obj = get(columnName);
			if (obj instanceof Boolean)
				return ((Boolean)obj) ? 1d : 0d;
			else if (obj instanceof Number)
				return ((Number)obj).doubleValue();
			else if (obj instanceof String)
				return Common.parseDouble((String)obj);
			return 0d;
			}
		
		/**
		 * Returns the string value of this object.
		 */
		public String getString(String columnName)
		{
			return String.valueOf(get(columnName));
			}
		
		}

}
