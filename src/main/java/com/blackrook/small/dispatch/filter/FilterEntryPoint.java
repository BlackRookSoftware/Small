package com.blackrook.small.dispatch.filter;

import java.lang.reflect.Method;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.small.dispatch.DispatchEntryPoint;
import com.blackrook.small.dispatch.DispatchMVCEntryPoint;
import com.blackrook.small.enums.RequestMethod;
import com.blackrook.small.exception.SmallFrameworkException;
import com.blackrook.small.parser.multipart.Part;
import com.blackrook.small.struct.HashDequeMap;

/**
 * Filter entry method.
 * The {@link #handleCall(RequestMethod, HttpServletRequest, HttpServletResponse, Map, Map, HashDequeMap)} method returns a boolean.
 * If true, continue to the next filter, if false, stop.
 * @author Matthew Tropiano
 */
public class FilterEntryPoint extends DispatchEntryPoint<FilterComponent> implements DispatchMVCEntryPoint<Boolean>
{
	/**
	 * Creates an entry method around a service profile instance.
	 * @param filterProfile the service instance.
	 * @param method the method invoked.
	 */
	public FilterEntryPoint(FilterComponent filterProfile, Method method)
	{
		super(filterProfile, method);
	}

	@Override
	public Boolean handleCall(
		RequestMethod requestMethod, 
		HttpServletRequest request, 
		HttpServletResponse response, 
		Map<String, String> pathVariableMap, 
		Map<String, Cookie> cookieMap, 
		HashDequeMap<String, Part> multiformPartMap
	){
		Object retval = null;
		try {
			retval = invoke(requestMethod, request, response, pathVariableMap, cookieMap, multiformPartMap);
		} catch (Exception e) {
			throw new SmallFrameworkException("An exception occurred in a Filter method.", e);
		}
		return (Boolean)retval;
	}

}
