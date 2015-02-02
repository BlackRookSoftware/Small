package com.blackrook.j2ee.small.util;


import javax.servlet.http.HttpServletResponse;

import com.blackrook.j2ee.small.exception.SmallFrameworkException;

/**
 * Utility class for {@link HttpServletResponse} manipulation.
 * @author Matthew Tropiano
 */
public final class ResponseUtil
{
	private ResponseUtil() {}

	/**
	 * Sends request to the error page with a status code.
	 * @param response servlet response object.
	 * @param statusCode the status code to use.
	 * @param message the status message.
	 */
	public static void sendError(HttpServletResponse response, int statusCode, String message)
	{
		try{
			response.sendError(statusCode, message);
		} catch (Exception e) {
			throw new SmallFrameworkException(e);
		}
	}

}
