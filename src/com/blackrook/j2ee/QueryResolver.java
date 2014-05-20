package com.blackrook.j2ee;

/**
 * Classes that implement this interface return a full query
 * for a keyword. Multiple query resolvers can be registered.
 * @author Matthew Tropiano
 */
public interface QueryResolver
{
	/**
	 * If this method returns true for a keyword,
	 * the query is NOT cached for a particular keyword.
	 * @param keyword the keyword to use as a lookup.
	 * @return true to not cache, false otherwise.
	 */
	public boolean dontCacheQuery(String keyword);
	
	/**
	 * Returns a query string for a particular keyword.
	 * If this returns a non-null string, it will be
	 * cached by the main query resolver as a valid query.
	 * @param keyword the keyword to use as a lookup.
	 * @return the path to a valid query, or null to pass handling of
	 * this resource to the next query resolver.
	 */
	public String resolveQuery(String keyword);

}
