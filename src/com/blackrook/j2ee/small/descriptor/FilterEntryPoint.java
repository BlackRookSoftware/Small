package com.blackrook.j2ee.small.descriptor;

import java.lang.reflect.Method;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.hash.HashedQueueMap;
import com.blackrook.j2ee.small.enums.RequestMethod;
import com.blackrook.j2ee.small.exception.SmallFrameworkException;
import com.blackrook.j2ee.small.struct.Part;

/**
 * Filter entry method.
 * @author Matthew Tropiano
 */
public class FilterEntryPoint extends EntryPoint<FilterProfile>
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
	 * @param pathRemainder
	 * @param cookieMap
	 * @param multiformPartMap
	 * @return
	 */
	public boolean handleCall(
		RequestMethod requestMethod, 
		HttpServletRequest request, 
		HttpServletResponse response, 
		String pathRemainder,
		HashMap<String, String> pathVariableMap, 
		HashMap<String, Cookie> cookieMap, 
		HashedQueueMap<String, Part> multiformPartMap
	)
	{
		Object retval = null;
		try {
			retval = invoke(requestMethod, request, response, pathRemainder, pathVariableMap, cookieMap, multiformPartMap);
		} catch (Exception e) {
			throw new SmallFrameworkException("An exception occurred in a Filter method.", e);
		}
		
		return (Boolean)retval;
	}

}
