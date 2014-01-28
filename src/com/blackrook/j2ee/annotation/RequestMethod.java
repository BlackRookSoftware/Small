package com.blackrook.j2ee.annotation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.j2ee.multipart.Part;
import com.blackrook.lang.json.JSONObject;
import com.blackrook.lang.xml.XMLStruct;

/**
 * Enumeration of request methods for controller method invocation. 
 * @author Matthew Tropiano
 */
public enum RequestMethod
{
	/** Standard GET request. */
	GET(1, String.class, HttpServletRequest.class, HttpServletResponse.class),
	/** Standard POST request (form encoding). */
	POST(1, String.class, HttpServletRequest.class, HttpServletResponse.class),
	/** POST request that consists of many parts (mulipart form), usually contains files. */
	POST_MULTIPART(2, String.class, HttpServletRequest.class, HttpServletResponse.class, Part[].class),
	/** POST request that contains JSON content (Content-Type is application/json). */
	POST_JSON(3, String.class, HttpServletRequest.class, HttpServletResponse.class, JSONObject.class),
	/** POST request that contains XML content (Content-Type is application/xml). */
	POST_XML(4, String.class, HttpServletRequest.class, HttpServletResponse.class, XMLStruct.class),
	/** Standard HEAD request. */
	HEAD(1, String.class, HttpServletRequest.class, HttpServletResponse.class),
	/** Standard PUT request. */
	PUT(1, String.class, HttpServletRequest.class, HttpServletResponse.class),
	/** Standard DELETE request. */
	DELETE(1, String.class, HttpServletRequest.class, HttpServletResponse.class),
	;
	
	/** Quick way of determining if certain types have compatible parameters (and could theoretically be used together. */
	private final int compatibilityGroup;
	/** Required parameter types for entry points. */
	private final Class<?>[] parameterTypes;
	
	// Constructor
	private RequestMethod(int compatibilityGroup, Class<?> ... parameterTypes)
	{
		this.compatibilityGroup = compatibilityGroup;
		this.parameterTypes = parameterTypes;
	}
	
	/**
	 * Returns a quick way of determining if certain types have compatible parameters (and could theoretically be used together.
	 */
	public int getCompatibilityGroup()
	{
		return compatibilityGroup;
	}
	
	/**
	 * Returns a the required parameter types for entry points.
	 */
	public Class<?>[] getParameterTypes()
	{
		return parameterTypes;
	}
	
}
