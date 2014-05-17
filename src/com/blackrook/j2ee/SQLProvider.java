package com.blackrook.j2ee;

/**
 * Class for enabling entry into the SQL pools.
 * @author Matthew Tropiano
 */
public abstract class SQLProvider
{
	/**
	 * Gets a query by keyword.
	 * @return the associated query or null if not found. 
	 */
	public String getQueryByName(String keyword)
	{
		return Toolkit.INSTANCE.getQueryByName(keyword);
	}

}
