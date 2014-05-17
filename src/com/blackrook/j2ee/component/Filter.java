package com.blackrook.j2ee.component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.j2ee.enums.RequestMethod;

/**
 * All filters are extensions of this class.
 * @author Matthew Tropiano
 */
public abstract class Filter
{
	/**
	 * Called to figure out if an HTTP Request should pass or be rejected.
	 * Usually, this filter checks if some conditions are satisfied before continuing on to the next
	 * filter. If conditions are met, this should perform its necessary functions and return true. If not, return false.
	 * Does nothing and returns true unless overridden.
	 * @param method the request method used.
	 * @param file the requested file (portion of URL - can be blank).
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param content the content to pass along as body content. Class will vary depending on {@link RequestMethod}. Can be null.
	 * @return true to continue the filter chain, false otherwise.
	 */
	public boolean onFilter(RequestMethod method, String file, HttpServletRequest request, HttpServletResponse response, Object content)
	{
		return true;
	}

}
