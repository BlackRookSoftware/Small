package com.blackrook.framework;

/**
 * Classes that implement this interface return a path to a view
 * for a keyword. Multiple view resolvers can be registered.
 * @author Matthew Tropiano
 */
public interface BRViewResolver
{
	/**
	 * If this method returns true for a keyword,
	 * the view path is NOT cached for a particular keyword.
	 * @param keyword the keyword to use as a lookup.
	 * @return true to not cache, false otherwise.
	 */
	public boolean dontCacheView(String keyword);
	
	/**
	 * Returns a path to a view for a particular keyword.
	 * If this returns a non-null string, it will be
	 * cached by the main view resolver as a valid view.
	 * @param keyword the keyword to use as a lookup.
	 * @return the path to a valid view, or null to pass handling of
	 * this resource to the next view resolver.
	 */
	public String resolveView(String keyword);
}