package com.blackrook.framework.resolver;

import com.blackrook.framework.BRViewResolver;

/**
 * Default view resolver. 
 * Always returns <code>"/WEB-INF/jsp/" + keyword + ".jsp"</code> for every keyword.
 * @author Matthew Tropiano
 */
public class DefaultViewResolver implements BRViewResolver
{

	/** Always returns false. */
	@Override
	public boolean dontCacheView(String keyword)
	{
		return false;
		}

	/** Always returns <code>"/WEB-INF/jsp/" + keyword + ".jsp"</code> for every keyword. */
	@Override
	public String resolveView(String keyword)
	{
		return "/WEB-INF/jsp/" + keyword + ".jsp";
		}

}
