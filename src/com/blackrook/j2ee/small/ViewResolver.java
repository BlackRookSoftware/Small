package com.blackrook.j2ee.small;

/**
 * Classes that implement this interface return a path to a view for a keyword or name.
 * @author Matthew Tropiano
 */
public interface ViewResolver
{
	/**
	 * Returns a path to a view for a particular keyword.
	 * If this returns a non-null string, it will be
	 * cached by the main view resolver as a valid view.
	 * @param viewName the view name to use as a lookup (from a controller call).
	 * @return the path to a valid view.
	 */
	public String resolveView(String viewName);
}