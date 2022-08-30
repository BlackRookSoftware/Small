/*******************************************************************************
 * Copyright (c) 2020-2022 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.dispatch.filter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.small.dispatch.DispatchEntryPoint;
import com.blackrook.small.dispatch.DispatchMVCEntryPoint;
import com.blackrook.small.enums.RequestMethod;
import com.blackrook.small.multipart.Part;
import com.blackrook.small.struct.HashDequeMap;

/**
 * Filter exit method.
 * The {@link #handleCall(RequestMethod, HttpServletRequest, HttpServletResponse, Map, Map, HashDequeMap)} method returns Void (null).
 * @author Matthew Tropiano
 */
public class FilterExitPoint extends DispatchEntryPoint<FilterComponent> implements DispatchMVCEntryPoint<Void>
{
	/**
	 * Creates an entry method around a service profile instance.
	 * @param filterProfile the service instance.
	 * @param method the method invoked.
	 */
	public FilterExitPoint(FilterComponent filterProfile, Method method)
	{
		super(filterProfile, method);
	}

	@Override
	public Void handleCall(
		RequestMethod requestMethod, 
		HttpServletRequest request, 
		HttpServletResponse response, 
		Map<String, String> pathVariableMap, 
		Map<String, Cookie> cookieMap, 
		HashDequeMap<String, Part> partMap
	) throws InvocationTargetException, ServletException, IOException 
	{
		invoke(requestMethod, request, response, pathVariableMap, cookieMap, partMap);
		return (Void)null;
	}

}
