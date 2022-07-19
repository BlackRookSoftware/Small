/*******************************************************************************
 * Copyright (c) 2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.small.annotation.Filter;
import com.blackrook.small.annotation.filter.FilterEntry;

/**
 * An optional return type for a {@link FilterEntry}-annotated method on a {@link Filter}
 * that returns either a new {@link HttpServletRequest}, a new {@link HttpServletResponse}, both, or neither.
 * @author Matthew Tropiano
 */
public class SmallFilterResult
{
	/** Filter chain passes - no changes to request or response object. */
	private static final SmallFilterResult PASS = new SmallFilterResult(true);
	/** Filter chain fails - no changes to request or response object. */
	private static final SmallFilterResult FAIL = new SmallFilterResult(false);
	
	/** The request. */
	private HttpServletRequest request;
	/** The response. */
	private HttpServletResponse response;
	/** The SmallResponse, if any. */
	private SmallResponse smallResponse;
	/** Continue flag. */
	private boolean passing;
	
	private SmallFilterResult(boolean passing)
	{
		this.request = null;
		this.response = null;
		this.smallResponse = null;
		this.passing = passing;
	}
	
	/**
	 * @return a result that signifies that this filter chain must not continue.
	 */
	public static SmallFilterResult fail()
	{
		return FAIL;
	}
	
	/**
	 * Returns a failing result with a SmallResponse.
	 * This object gets attached to the servlet request to be potentially decorated later by a filter exit.
	 * @param smallResponse the SmallResponse to attach to the request.
	 * @return a result that signifies that this filter chain must not continue.
	 */
	public static SmallFilterResult fail(SmallResponse smallResponse)
	{
		SmallFilterResult out = new SmallFilterResult(false);
		out.smallResponse = smallResponse;
		return out;
	}
	
	/**
	 * @return a result that signifies that this filter chain can continue.
	 */
	public static SmallFilterResult pass()
	{
		return PASS;
	}

	/**
	 * Returns a passing result with new request and response.
	 * @param request the request object to pass along to the next part of the chain. Can be null to preserve the same request.
	 * @return a result that signifies that this filter chain can continue, with a potentially new request or response.
	 */
	public static SmallFilterResult pass(HttpServletRequest request)
	{
		return pass(request, null);
	}

	/**
	 * Returns a passing result with new request and response.
	 * @param response the response object to pass along to the next part of the chain. Can be null to preserve the same response.
	 * @return a result that signifies that this filter chain can continue, with a potentially new request or response.
	 */
	public static SmallFilterResult pass(HttpServletResponse response)
	{
		return pass(null, response);
	}

	/**
	 * Returns a passing result with a new request and response.
	 * @param request the request object to pass along to the next part of the chain. Can be null to preserve the same request.
	 * @param response the response object to pass along to the next part of the chain. Can be null to preserve the same response.
	 * @return a result that signifies that this filter chain can continue, with a potentially new request or response.
	 */
	public static SmallFilterResult pass(HttpServletRequest request, HttpServletResponse response)
	{
		SmallFilterResult out = new SmallFilterResult(true);
		out.request = request;
		out.response = response;
		return out;
	}

	/**
	 * Returns a passing result with a SmallResponse.
	 * This object gets attached to the servlet request to be potentially decorated later by a controller or filter entry (enter or exit).
	 * @param smallResponse the SmallResponse to attach to the request.
	 * @return a result that signifies that this filter chain must not continue.
	 */
	public static SmallFilterResult pass(SmallResponse smallResponse)
	{
		SmallFilterResult out = new SmallFilterResult(true);
		out.smallResponse = smallResponse;
		return out;
	}
	
	/**
	 * Gets the replacement servlet request object.
	 * If null, the incoming request does not change.
	 * @return the replacement request object, if any.
	 */
	public HttpServletRequest getRequest()
	{
		return request;
	}
	
	/**
	 * Gets the replacement servlet response object.
	 * If null, the incoming response does not change.
	 * @return the replacement response object, if any.
	 */
	public HttpServletResponse getResponse()
	{
		return response;
	}
	
	/**
	 * Gets the SmallResponse object.
	 * @return the attached SmallResponse object, if any.
	 */
	public SmallResponse getSmallResponse()
	{
		return smallResponse;
	}
	
	/**
	 * @return if the next filter in the chain should be called.
	 */
	public boolean isPassing()
	{
		return passing;
	}
	
}
