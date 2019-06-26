package com.blackrook.j2ee.small.filter;

import java.lang.reflect.Method;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.j2ee.small.SmallEntryPoint;
import com.blackrook.j2ee.small.enums.RequestMethod;
import com.blackrook.j2ee.small.exception.SmallFrameworkException;
import com.blackrook.j2ee.small.struct.HashDequeMap;
import com.blackrook.j2ee.small.struct.Part;

/**
 * Filter entry method.
 * @author Matthew Tropiano
 */
public class FilterEntryPoint extends SmallEntryPoint<FilterProfile>
{
	/**
	 * Creates an entry method around a service profile instance.
	 * @param filterProfile the service instance.
	 * @param method the method invoked.
	 */
	public FilterEntryPoint(FilterProfile filterProfile, Method method)
	{
		super(filterProfile, method);
	}

	/**
	 * 
	 * @param requestMethod
	 * @param request
	 * @param response
	 * @param pathVariableMap 
	 * @param cookieMap
	 * @param multiformPartMap
	 * @return
	 */
	public boolean handleCall(
		RequestMethod requestMethod, 
		HttpServletRequest request, 
		HttpServletResponse response, 
		Map<String, String> pathVariableMap, 
		Map<String, Cookie> cookieMap, 
		HashDequeMap<String, Part> multiformPartMap
	)
	{
		Object retval = null;
		try {
			retval = invoke(requestMethod, request, response, pathVariableMap, cookieMap, multiformPartMap);
		} catch (Exception e) {
			throw new SmallFrameworkException("An exception occurred in a Filter method.", e);
		}
		
		return (Boolean)retval;
	}

}
