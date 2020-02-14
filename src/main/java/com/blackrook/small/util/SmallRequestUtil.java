/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.blackrook.small.exception.SmallFrameworkException;
import com.blackrook.small.parser.MultipartParser;
import com.blackrook.small.parser.RFCParser;
import com.blackrook.small.parser.multipart.MultipartFormDataParser;
import com.blackrook.small.struct.Utils;
import com.blackrook.small.struct.TypeProfileFactory.Profile;
import com.blackrook.small.struct.TypeProfileFactory.Profile.FieldInfo;
import com.blackrook.small.struct.TypeProfileFactory.Profile.MethodInfo;

/**
 * Utility class for {@link HttpServletRequest} manipulation.
 * @author Matthew Tropiano
 */
public final class SmallRequestUtil
{
	/** Date format parser map. */
	private static final Map<String, SimpleDateFormat> DATE_PATTERN_MAP = new HashMap<String, SimpleDateFormat>();

	private SmallRequestUtil() {}

	/**
	 * Checks if the request is JSON-formatted.
	 * @param request the request object.
	 * @return true if so, false if not.
	 */
	public static boolean isJSON(HttpServletRequest request)
	{
		String type = request.getContentType();
		return type.startsWith("application/json");
	}

	/**
	 * Checks if the request is JSON-formatted.
	 * @param request the request object.
	 * @return true if so, false if not.
	 */
	public static boolean isXML(HttpServletRequest request)
	{
		String type = request.getContentType();
		return 
			type.startsWith("application/xml")
			|| type.startsWith("text/xml");
	}

	/**
	 * Checks if this request is a regular form POST. 
	 * @param request the request object.
	 * @return true if so, false if not.
	 */
	public static boolean isFormEncoded(HttpServletRequest request)
	{
		return "application/x-www-form-urlencoded".equals(request.getContentType());
	}

	/**
	 * Returns the appropriate multipart parser for a request.
	 * @param request the request.
	 * @return a multipart parser.
	 */
	public static MultipartParser getMultipartParser(HttpServletRequest request)
	{
		String contentType = request.getContentType();
		if (Utils.isEmpty(contentType))
			return null;
		if (contentType.startsWith("multipart/form-data"))
			return new MultipartFormDataParser();
		else
			return null;
	}

	/**
	 * Get the path parsed out of the request URI.
	 * @param request the request.
	 * @return the path string.
	 */
	public static String getPath(HttpServletRequest request)
	{
		String requestURI = request.getRequestURI();
		int extIndex = requestURI.lastIndexOf('.');
		int endIndex = requestURI.indexOf('?');
		
		if (extIndex >= 0)
			return requestURI.substring(0, extIndex);
		if (endIndex >= 0)
			return requestURI.substring(0, endIndex);
		
		return requestURI;
	}

	/**
	 * Get the base file name parsed out of the request URI.
	 * @param request the request.
	 * @return the page.
	 */
	public static String getFileName(HttpServletRequest request)
	{
		String requestURI = request.getRequestURI();
		int slashIndex = requestURI.lastIndexOf('/');
		int endIndex = requestURI.indexOf('?');
		if (endIndex >= 0)
			return requestURI.substring(slashIndex + 1, endIndex);
		else
			return requestURI.substring(slashIndex + 1); 
	}

	/**
	 * Gets the current connection's session id.
	 * Meant to be a convenience method for <code>request.getSession().getId()</code> 
	 * @param request servlet request object.
	 * @return the id attached to the session, or null if no session.
	 */
	public static String getSessionId(HttpServletRequest request)
	{
		HttpSession session = request.getSession();
		return session != null ? session.getId() : null;	
	}

	/**
	 * Gets and auto-casts an object bean stored at the request level.
	 * The bean is created and stored if it doesn't exist.
	 * The name used is the fully-qualified class name prefixed with "$$".
	 * @param request the source request object.
	 * @param clazz the class type of the object that should be returned.
	 * @param <T> the object type.
	 * @return a typecast object on the request, or <code>null</code>, if the session is null or the attribute does not exist.
	 * @throws IllegalArgumentException if the class provided in an anonymous class or array without a component type.
	 * @throws SmallFrameworkException if the object cannot be instantiated for any reason.
	 */
	public static <T> T getRequestBean(HttpServletRequest request, Class<T> clazz)
	{
		String className = clazz.getCanonicalName(); 
		if ((className = clazz.getCanonicalName()) == null)
			throw new IllegalArgumentException("Class provided has no type!");
		return getRequestBean(request, clazz, "$$"+className, true);
	}

	/**
	 * Gets and auto-casts an object bean stored at the request level.
	 * The bean is created and stored if it doesn't exist.
	 * @param request the source request object.
	 * @param name the attribute name.
	 * @param clazz the class type of the object that should be returned.
	 * @param <T> the object type.
	 * @return a typecast object on the request, or <code>null</code>, if the session is null or the attribute does not exist.
	 * @throws SmallFrameworkException if the object cannot be instantiated for any reason.
	 */
	public static <T> T getRequestBean(HttpServletRequest request, Class<T> clazz, String name)
	{
		return getRequestBean(request, clazz, name, true);
	}

	/**
	 * Gets and auto-casts an object bean stored at the request level.
	 * @param request the source request object.
	 * @param name the attribute name.
	 * @param clazz the class type of the object that should be returned.
	 * @param create if true, instantiate this class in the request (via {@link Class#newInstance()}) if it doesn't exist.
	 * @param <T> the object type.
	 * @return a typecast object on the request.
	 * @throws SmallFrameworkException if the object cannot be instantiated for any reason.
	 */
	public static <T> T getRequestBean(HttpServletRequest request, Class<T> clazz, String name, boolean create)
	{
		Object obj = request.getAttribute(name);
		if (obj == null && create)
		{
			try {
				obj = clazz.getDeclaredConstructor().newInstance();
				request.setAttribute(name, obj);
		} catch (Exception e) {
				throw new SmallFrameworkException(e);
			}
		}
	
		if (obj == null)
			return null;
		return clazz.cast(obj);
	}

	/**
	 * Gets and auto-casts an object bean stored at the session level.
	 * The bean is created and stored if it doesn't exist.
	 * The name used is the fully-qualified class name prefixed with "$$".
	 * @param request the source request object.
	 * @param clazz the class type of the object that should be returned.
	 * @param <T> the object type.
	 * @return a typecast object on the session, or <code>null</code>, if the session is null.
	 * @throws IllegalArgumentException if the class provided in an anonymous class or array without a component type.
	 * @throws SmallFrameworkException if the object cannot be instantiated for any reason.
	 */
	public static <T> T getSessionBean(HttpServletRequest request, Class<T> clazz)
	{
		String className = clazz.getCanonicalName(); 
		if ((className = clazz.getCanonicalName()) == null)
			throw new IllegalArgumentException("Class provided has no type!");
		return getSessionBean(request, clazz, "$$"+className, true);
	}

	/**
	 * Gets and auto-casts an object bean stored at the session level.
	 * The bean is created and stored if it doesn't exist.
	 * @param request the source request object.
	 * @param clazz the class type of the object that should be returned.
	 * @param name the attribute name.
	 * @param <T> the object type.
	 * @return a typecast object on the session, or <code>null</code>, if the session is null.
	 * @throws SmallFrameworkException if the object cannot be instantiated for any reason.
	 */
	public static <T> T getSessionBean(HttpServletRequest request, Class<T> clazz, String name)
	{
		return getSessionBean(request, clazz, name, true);
	}

	/**
	 * Gets and auto-casts an object bean stored at the session level.
	 * @param request the source request object.
	 * @param clazz the class type of the object that should be returned.
	 * @param name the attribute name.
	 * @param create if true, instantiate this class in the session (via {@link Class#newInstance()}) if it doesn't exist.
	 * @param <T> the object type.
	 * @return a typecast object on the session, a new instance if it doesn't exist, or null if the session is null.
	 * @throws SmallFrameworkException if the object cannot be instantiated for any reason.
	 */
	public static <T> T getSessionBean(HttpServletRequest request, Class<T> clazz, String name, boolean create)
	{
		HttpSession session = request.getSession();
		if (session == null)
			return null;
		
		Object obj = session.getAttribute(name);
		if (obj == null && create)
		{
			try {
				synchronized (session)
				{
					if ((obj = session.getAttribute(name)) == null)
					{
						obj = clazz.getDeclaredConstructor().newInstance();
						session.setAttribute(name, obj);
					}
				}
			} catch (Exception e) {
				throw new SmallFrameworkException(e);
			}
		}
	
		if (obj == null)
			return null;
		return clazz.cast(obj);
	}

	/**
	 * Gets the binary payload of a request.
	 * @param request the request.
	 * @return the resultant byte array of the data.
	 * @throws IOException if the data could not be read. 
	 */
	public static byte[] getByteData(HttpServletRequest request) throws IOException
	{
		ByteArrayOutputStream sb = new ByteArrayOutputStream();
		try (BufferedInputStream bis = new BufferedInputStream(request.getInputStream(), 16384)) {
			int buf = 0;
			byte[] c = new byte[16384];
			while ((buf = bis.read(c)) >= 0)
				sb.write(c, 0, buf);
		}
		return sb.toByteArray();
	}
	
	/**
	 * Gets the string data of a request.
	 * @param request the request.
	 * @return the resultant string.
	 * @throws UnsupportedEncodingException if the provided charset name is not a valid charset.
	 * @throws IOException if  
	 */
	public static String getStringData(HttpServletRequest request) throws UnsupportedEncodingException, IOException
	{
		StringBuffer sb = new StringBuffer();
		try (Reader ir = request.getReader()) {
			int buf = 0;
			char[] c = new char[16384];
			while ((buf = ir.read(c)) >= 0)
				sb.append(c, 0, buf);
		}
		return sb.toString();
	}
	
	/**
	 * Get content data from the request and attempts to return it as the desired type.
	 * Assumes UTF-8 if the request does not specify an encoding.
	 * @param <R> the return type.
	 * @param request the request to read from.
	 * @param type the type to convert to.
	 * @return the data returned as the requested type.
	 * @throws UnsupportedEncodingException if the incoming request is not a recognized charset.
	 * @throws IOException if a read error occurs.
	 */
	@SuppressWarnings("unchecked")
	public static <R> R getContentData(HttpServletRequest request, Class<R> type) throws UnsupportedEncodingException, IOException
	{
		String contentTypeString = request.getContentType();
		String mimeType = null;
		String charset = "UTF-8";
		if (contentTypeString != null)
		{
			RFCParser parser = new RFCParser(contentTypeString);
			while (parser.hasTokens())
			{
				String nextToken = parser.nextToken();
				if (mimeType == null)
					mimeType = nextToken;
				else if (nextToken.startsWith("charset="))
					charset = nextToken.substring("charset=".length()).trim();
			}
		}

		if (byte[].class.isAssignableFrom(type))
			return (R)getByteData(request);
		if (ByteArrayInputStream.class.isAssignableFrom(type))
			return (R)new ByteArrayInputStream(getByteData(request));
		if (ServletInputStream.class.isAssignableFrom(type))
			return (R)request.getInputStream();
		if (InputStream.class.isAssignableFrom(type))
			return (R)request.getInputStream();
		if (StringReader.class.isAssignableFrom(type))
			return (R)new StringReader(getStringData(request));
		if (BufferedReader.class.isAssignableFrom(type))
			return (R)request.getReader();
		if (InputStreamReader.class.isAssignableFrom(type))
			return (R)new InputStreamReader(request.getInputStream(), charset);
		if (Reader.class.isAssignableFrom(type))
			return (R)request.getReader();

		return Utils.createForType(getStringData(request), type);
	}

	/**
	 * Gets a group of parameters that start with a specific prefix.
	 * @param request the servlet request.
	 * @param prefix the prefix to search for.
	 * @return a HashMap containing a map of parameter to String value of parameter. 
	 * The parameters in the map are ones that match the prefix.
	 */
	public static HashMap<String, String[]> getParameters(HttpServletRequest request, String prefix)
	{
		HashMap<String, String[]> out = new HashMap<String, String[]>();
		for (Map.Entry<String, String[]> entry : ((Map<String, String[]>)request.getParameterMap()).entrySet())
			if (entry.getKey().startsWith(prefix))
				out.put(entry.getKey(), entry.getValue());
		return out;
	}

	/**
	 * Convenience method that checks if a parameter exists on a request. 
	 * @param request the servlet request.
	 * @param paramName the parameter name.
	 * @return true if it exists, false otherwise.
	 */
	public static boolean getParameterExist(HttpServletRequest request, String paramName)
	{
		return request.getParameterValues(paramName) != null && request.getParameterValues(paramName).length > 0;
	}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and returns true or false.
	 * This flavor of {@link #getParameterBoolean(HttpServletRequest, String, String)} assumes that the parameter
	 * received is a string that is either "true" or not "true".
	 * @param request the servlet request.
	 * @param paramName the parameter name.
	 * @return true if the parameter value is "true", false otherwise.
	 */
	public static boolean getParameterBoolean(HttpServletRequest request, String paramName)
	{
		return getParameterBoolean(request, paramName, "true");
	}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and returns true or false, if the string found in the request evaluates
	 * to <code>trueValue</code>, case-insensitively. The value of <code>trueValue</code> can be <code>null</code>,
	 * meaning that the parameter was not received.
	 * @param request the servlet request.
	 * @param paramName the parameter name.
	 * @param trueValue the value to equate to true.
	 * @return true if the parameter value is <code>trueValue</code>, false otherwise.
	 */
	public static boolean getParameterBoolean(HttpServletRequest request, String paramName, String trueValue)
	{
		String out = request.getParameter(paramName);
		return (out == null && trueValue == null) || (out != null && out.equalsIgnoreCase(trueValue));
	}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code>.
	 * @param request the servlet request.
	 * @param paramName the parameter name.
	 * @return the parameter as a string or the empty string if it doesn't exist.
	 */
	public static String getParameterString(HttpServletRequest request, String paramName)
	{
		return getParameterString(request, paramName, "");
	}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code>. 
	 * @param request the servlet request.
	 * @param paramName the parameter name.
	 * @param def the default value to use if the parameter is null.
	 * @return the parameter as a string or <code>def</code> if it doesn't exist.
	 */
	public static String getParameterString(HttpServletRequest request, String paramName, String def)
	{
		String out = request.getParameter(paramName);
		return out != null ? out : def;
	}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code>.
	 * @param request the servlet request.
	 * @param paramName the parameter name.
	 * @return the parameter as a byte or 0 if it doesn't exist.
	 */
	public static byte getParameterByte(HttpServletRequest request, String paramName)
	{
		return getParameterByte(request, paramName, (byte)0);
	}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code>.
	 * @param request the servlet request.
	 * @param paramName the parameter name.
	 * @param def the default value to use if the parameter is null.
	 * @return the parameter as a byte or <code>def</code> if it doesn't exist.
	 */
	public static byte getParameterByte(HttpServletRequest request, String paramName, byte def)
	{
		return parseByte(request.getParameter(paramName), def);
	}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code>.
	 * @param request the servlet request.
	 * @param paramName the parameter name.
	 * @return the parameter as a short or 0 if it doesn't exist.
	 */
	public static short getParameterShort(HttpServletRequest request, String paramName)
	{
		return getParameterShort(request, paramName, (short)0);
	}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code>.
	 * @param request the servlet request.
	 * @param paramName the parameter name.
	 * @param def the default value to use if the parameter is null.
	 * @return the parameter as a short or <code>def</code> if it doesn't exist.
	 */
	public static short getParameterShort(HttpServletRequest request, String paramName, short def)
	{
		return parseShort(request.getParameter(paramName), def);
	}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code>.
	 * @param request the servlet request.
	 * @param paramName the parameter name.
	 * @return the parameter as a char or <code>'\0'</code> if it doesn't exist.
	 */
	public static char getParameterChar(HttpServletRequest request, String paramName)
	{
		return getParameterChar(request, paramName, '\0');
	}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code>.
	 * @param request the servlet request.
	 * @param paramName the parameter name.
	 * @param def the default value to use if the parameter is null.
	 * @return the parameter as a char or <code>def</code> if it doesn't exist.
	 */
	public static char getParameterChar(HttpServletRequest request, String paramName, char def)
	{
		return parseChar(request.getParameter(paramName), def);
	}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code>.
	 * @param request the servlet request.
	 * @param paramName the parameter name.
	 * @return the parameter as an integer or 0 if it doesn't exist.
	 */
	public static int getParameterInt(HttpServletRequest request, String paramName)
	{
		return getParameterInt(request, paramName, 0);
	}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code>.
	 * @param request the servlet request.
	 * @param paramName the parameter name.
	 * @param def the default value to use if the parameter is null.
	 * @return the parameter as an integer or <code>def</code> if it doesn't exist.
	 */
	public static int getParameterInt(HttpServletRequest request, String paramName, int def)
	{
		return parseInt(request.getParameter(paramName), def);
	}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code>.
	 * @param request the servlet request.
	 * @param paramName the parameter name.
	 * @return the parameter as an integer or <code>0f</code> if it doesn't exist.
	 */
	public static float getParameterFloat(HttpServletRequest request, String paramName)
	{
		return getParameterFloat(request, paramName, 0f);
	}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code>.
	 * @param request the servlet request.
	 * @param paramName the parameter name.
	 * @param def the default value to use if the parameter is null.
	 * @return the parameter as an integer or <code>def</code> if it doesn't exist.
	 */
	public static float getParameterFloat(HttpServletRequest request, String paramName, float def)
	{
		return parseFloat(request.getParameter(paramName), def);
	}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code>.
	 * @param request the servlet request.
	 * @param paramName the parameter name.
	 * @return the parameter as a long or 0L if it doesn't exist.
	 */
	public static long getParameterLong(HttpServletRequest request, String paramName)
	{
		return getParameterLong(request, paramName, 0L);
	}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code>.
	 * @param request the servlet request.
	 * @param paramName the parameter name.
	 * @param def the default value to use if the parameter is null.
	 * @return the parameter as a long or <code>def</code> if it doesn't exist.
	 */
	public static long getParameterLong(HttpServletRequest request, String paramName, long def)
	{
		return parseLong(request.getParameter(paramName), def);
	}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code>.
	 * @param request the servlet request.
	 * @param paramName the parameter name.
	 * @return the parameter as a double or 0.0 if it doesn't exist.
	 */
	public static double getParameterDouble(HttpServletRequest request, String paramName)
	{
		return getParameterDouble(request, paramName, 0.0);
	}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code>.
	 * @param request the servlet request.
	 * @param paramName the parameter name.
	 * @param def the default value to use if the parameter is null.
	 * @return the parameter as a double or <code>def</code> if it doesn't exist.
	 */
	public static double getParameterDouble(HttpServletRequest request, String paramName, double def)
	{
		return parseDouble(request.getParameter(paramName), def);
	}

	/**
	 * Method that parses a parameter as a date. 
	 * @param request the HTTP request object.
	 * @param paramName the parameter name.
	 * @param formatString the {@link SimpleDateFormat} format string.
	 * @return a Date object representing the parsed date or null if not parsed.
	 */
	public static Date getParameterDate(HttpServletRequest request, String paramName, String formatString)
	{
		SimpleDateFormat formatter = DATE_PATTERN_MAP.get(formatString);
		if (formatter == null)
		{
			synchronized (DATE_PATTERN_MAP)
			{
				formatter = new SimpleDateFormat(formatString);
				DATE_PATTERN_MAP.put(formatString, formatter);
			}
		}
		
		Date out = null;
		try {
			synchronized(formatter)
			{
				out = formatter.parse(getParameterString(request, paramName));
			}
		} catch (ParseException e) {
			return null;
		}
		
		return out;
	}

	/**
	 * Sets the fields on a new instance of an object, using its public fields and setters, using the
	 * servlet request as a source. If this method finds fields on the request whose runtime types do not match,
	 * namely Strings to primitives or boxed primitives, an attempt is made to convert them.
	 * <p>
	 * For instance, if there is an attribute in the request called "color", its value
	 * will be applied via the public field "color" or the setter "setColor()". Public
	 * fields take precedence over setters.
	 * <p>
	 * This method does not just merely look in the request scope.
	 * If a parameter cannot be found in the Request, the Session attributes are searched next, followed by
	 * the Application. If that fails, it is ignored.
	 * @param request the servlet request.
	 * @param type the type to instantiate.
	 * @param <T> the object type.
	 * @return the new object with fields set using the model.
	 * @throws RuntimeException if an exception occurs - notably if the fields or setters on the class cannot be reached
	 * (best to use public classes in these cases), or if the object cannot be instantiated.
	 */
	public static <T extends Object> T setModelFields(HttpServletRequest request, Class<T> type)
	{
		return setModelFields(request, Utils.create(type));
	}

	/**
	 * Sets the fields on an object, using its public fields and setters, using the
	 * servlet request as a source. If this method finds fields on the request whose runtime types do not match,
	 * namely Strings to primitives or boxed primitives, an attempt is made to convert them.
	 * <p>
	 * For instance, if there is an attribute in the request called "color", its value
	 * will be applied via the public field "color" or the setter "setColor()". Public
	 * fields take precedence over setters.
	 * <p>
	 * This method does not just merely look in the request scope.
	 * If a parameter cannot be found in the Request, the Session attributes are searched next, followed by
	 * the Application. If that fails, it is ignored.
	 * @param request the servlet request.
	 * @param target the target object.
	 * @param <T> the object type.
	 * @return the passed in object.
	 * @throws RuntimeException if an exception occurs - notably if the fields or setters on the class cannot be reached
	 * (best to use public classes in these cases).
	 */
	public static <T extends Object> T setModelFields(HttpServletRequest request, T target)
	{
		HttpSession session = request.getSession();
		ServletContext context = session != null ? session.getServletContext() : null;
		
		Profile<?> profile = Utils.getProfile((Class<?>)target.getClass());
		Set<String> foundFields = new HashSet<String>();
		
		for (Map.Entry<String, FieldInfo> pair : profile.getPublicFieldsByName().entrySet())
		{
			String fieldName = pair.getKey();
			FieldInfo fieldInfo = pair.getValue();
			if (getParameterExist(request, fieldName))
			{
				Class<?> setterType = fieldInfo.getType();
				if (Utils.isArray(setterType))
				{
					String[] values = request.getParameterValues(fieldName);
					Class<?> arrayType = Utils.getArrayType(setterType);
					Object newArray = Array.newInstance(arrayType, values.length);
					
					for (int i = 0; i < values.length; i++)
					{
						if (arrayType != String.class)
							Array.set(newArray, i, Utils.createForType(fieldName, getParameterString(request, fieldName), arrayType));
						else
							Array.set(newArray, i, getParameterString(request, fieldName));
					}
					Utils.setFieldValue(target, fieldInfo.getField(), newArray);
				}
				else
				{
					if (setterType != String.class)
						Utils.setFieldValue(target, fieldInfo.getField(), Utils.createForType(fieldName, getParameterString(request, fieldName), setterType));
					else
						Utils.setFieldValue(target, fieldInfo.getField(), getParameterString(request, fieldName));
				}
				foundFields.add(fieldName);
			}
			else if (session != null && SmallUtil.getAttributeExist(session, fieldName))
			{
				Object objval = session.getAttribute(fieldName);
				if (fieldInfo.getType() == objval.getClass())
					Utils.setFieldValue(target, fieldInfo.getField(), objval);
				else
					throw new SmallFrameworkException("Model and session attribute types for field \""+fieldName+"\" do not match.");
				foundFields.add(fieldName);
			}
			else if (context != null && SmallUtil.getAttributeExist(context, fieldName))
			{
				Object objval = context.getAttribute(fieldName);
				if (fieldInfo.getType() == objval.getClass())
					Utils.setFieldValue(target, fieldInfo.getField(), objval);
				else
					throw new SmallFrameworkException("Model and context attribute types for field \""+fieldName+"\" do not match.");
				foundFields.add(fieldName);
			}
		}
		
		for (Map.Entry<String, MethodInfo> pair : profile.getSetterMethodsByName().entrySet())
		{
			String fieldName = pair.getKey();
			if (foundFields.contains(fieldName))
				continue;
			
			MethodInfo signature = pair.getValue();
			if (getParameterExist(request, fieldName))
			{
				Class<?> setterType = signature.getType();
				if (Utils.isArray(setterType))
				{
					String[] values = request.getParameterValues(fieldName);
					Class<?> arrayType = Utils.getArrayType(setterType);
					Object newArray = Array.newInstance(arrayType, values.length);
					
					for (int i = 0; i < values.length; i++)
					{
						if (arrayType != String.class)
							Array.set(newArray, i, Utils.createForType(fieldName, values[i], arrayType));
						else
							Array.set(newArray, i, getParameterString(request, fieldName));
					}
					Utils.invokeBlind(signature.getMethod(), target, newArray);
				}
				else
				{
					if (setterType != String.class)
						Utils.invokeBlind(signature.getMethod(), target, Utils.createForType(fieldName, getParameterString(request, fieldName), setterType));
					else
						Utils.invokeBlind(signature.getMethod(), target, getParameterString(request, fieldName));
				}
			}
			else if (session != null && SmallUtil.getAttributeExist(session, fieldName))
			{
				Object objval = session.getAttribute(fieldName);
				if (signature.getType() == objval.getClass())
					Utils.invokeBlind(signature.getMethod(), target, objval);
				else
					throw new SmallFrameworkException("Model and session attribute types for field \""+fieldName+"\" do not match.");
			}
			else if (context != null && SmallUtil.getAttributeExist(context, fieldName))
			{
				Object objval = context.getAttribute(fieldName);
				if (signature.getType() == objval.getClass())
					Utils.invokeBlind(signature.getMethod(), target, objval);
				else
					throw new SmallFrameworkException("Model and context attribute types for field \""+fieldName+"\" do not match.");
			}
		}
		
		return target;
	}

	/**
	 * Attempts to parse a byte from a string.
	 * If the string is null or the empty string, this returns <code>def</code>.
	 * @param s the input string.
	 * @param def the fallback value to return.
	 * @return the interpreted byte or def if the input string is blank.
	 */
	private static byte parseByte(String s, byte def)
	{
		if (isStringEmpty(s))
			return def;
		try {
			return Byte.parseByte(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/**
	 * Attempts to parse a short from a string.
	 * If the string is null or the empty string, this returns <code>def</code>.
	 * @param s the input string.
	 * @param def the fallback value to return.
	 * @return the interpreted short or def if the input string is blank.
	 */
	private static short parseShort(String s, short def)
	{
		if (isStringEmpty(s))
			return def;
		try {
			return Short.parseShort(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/**
	 * Attempts to parse a byte from a string.
	 * If the string is null or the empty string, this returns <code>def</code>.
	 * @param s the input string.
	 * @param def the fallback value to return.
	 * @return the first character in the string or def if the input string is blank.
	 */
	private static char parseChar(String s, char def)
	{
		if (isStringEmpty(s))
			return def;
		else
			return s.charAt(0);
	}

	/**
	 * Attempts to parse an int from a string.
	 * If the string is null or the empty string, this returns <code>def</code>.
	 * @param s the input string.
	 * @param def the fallback value to return.
	 * @return the interpreted integer or def if the input string is blank.
	 */
	private static int parseInt(String s, int def)
	{
		if (isStringEmpty(s))
			return def;
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/**
	 * Attempts to parse a long from a string.
	 * If the string is null or the empty string, this returns <code>def</code>.
	 * @param s the input string.
	 * @param def the fallback value to return.
	 * @return the interpreted long integer or def if the input string is blank.
	 */
	private static long parseLong(String s, long def)
	{
		if (isStringEmpty(s))
			return def;
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	/**
	 * Attempts to parse a float from a string.
	 * If the string is null or the empty string, this returns <code>def</code>.
	 * @param s the input string.
	 * @param def the fallback value to return.
	 * @return the interpreted float or def if the input string is blank.
	 */
	private static float parseFloat(String s, float def)
	{
		if (isStringEmpty(s))
			return def;
		try {
			return Float.parseFloat(s);
		} catch (NumberFormatException e) {
			return 0f;
		}
	}

	/**
	 * Attempts to parse a double from a string.
	 * If the string is null or the empty string, this returns <code>def</code>.
	 * @param s the input string.
	 * @param def the fallback value to return.
	 * @return the interpreted double or def if the input string is blank.
	 */
	private static double parseDouble(String s, double def)
	{
		if (isStringEmpty(s))
			return def;
		try {
			return Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}

	private static boolean isStringEmpty(Object obj)
	{
		if (obj == null)
			return true;
		else
			return ((String)obj).trim().length() == 0;
	}
	
}
