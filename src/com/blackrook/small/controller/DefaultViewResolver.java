package com.blackrook.small.controller;


/**
 * Default resolver for views.
 * <p>Maps every view to: <code>"/WEB-INF/jsp/" + keyword + ".jsp"</code>
 * @author Matthew Tropiano
 */
public class DefaultViewResolver implements ViewResolver
{

	/** Always returns <code>"/WEB-INF/jsp/" + keyword + ".jsp"</code> for every keyword. */
	@Override
	public String resolveView(String keyword)
	{
		return "/WEB-INF/jsp/" + keyword + ".jsp";
	}

}
