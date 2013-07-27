package com.blackrook.framework.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.framework.BRFilter;

/**
 * The common root filter.
 * @author Matthew Tropiano
 */
public abstract class BRCommonFilter extends BRFilter
{
	@Override
	public boolean onFilter(HttpServletRequest request, HttpServletResponse response)
	{
		return true;
		}

}
