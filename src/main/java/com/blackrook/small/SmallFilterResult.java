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
	/** Continue flag. */
	private boolean passing;
	
	private SmallFilterResult(boolean passing)
	{
		this.request = null;
		this.response = null;
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
	 * @return if the next filter in the chain should be called.
	 */
	public boolean isPassing()
	{
		return passing;
	}
	
}
