/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.dispatch;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.small.enums.RequestMethod;
import com.blackrook.small.exception.request.BeanCreationException;
import com.blackrook.small.exception.request.NoConverterException;
import com.blackrook.small.exception.request.NotFoundException;
import com.blackrook.small.exception.request.UnsupportedMediaTypeException;
import com.blackrook.small.multipart.Part;
import com.blackrook.small.struct.HashDequeMap;

/**
 * Describes an entry point that is meant to handle part of the MVC call stack.
 * @author Matthew Tropiano
 * @param <R> Return type for the entry point call.
 */
public interface DispatchMVCEntryPoint<R>
{
	/**
	 * Handles an MVC call.
	 * @param requestMethod the incoming request method.
	 * @param request the HTTP request.
	 * @param response the HTTP response.
	 * @param pathVariableMap the path variable map.
	 * @param cookieMap the cookie map.
	 * @param partMap the map of name to multipart parts.
	 * @return the return value.
	 * @throws NotFoundException if a file that was going to be sent does not exist.
	 * @throws UnsupportedMediaTypeException if an incoming or outgoing type is unsupported.
	 * @throws NoConverterException if an object could not be converted to a serializable format for transmission.
	 * @throws ClassCastException if a value could not be converted to another type.
	 * @throws BeanCreationException if a bean cannot be instantiated for any reason on the application, session, or request scope.
	 * @throws UnsupportedEncodingException if an encoding type is not supported by this server.
	 * @throws ServletException if any other servlet exception happens. 
	 * @throws IOException if an IO Error occurs.
	 */
	R handleCall(
		RequestMethod requestMethod, 
		HttpServletRequest request, 
		HttpServletResponse response, 
		Map<String, String> pathVariableMap, 
		Map<String, Cookie> cookieMap, 
		HashDequeMap<String, Part> partMap
	) throws ServletException, IOException;
}
