package com.blackrook.framework.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.blackrook.commons.Common;
import com.blackrook.commons.ObjectPair;
import com.blackrook.commons.Reflect;
import com.blackrook.commons.hash.Hash;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.framework.BRFrameworkException;
import com.blackrook.framework.BRMIMETypes;
import com.blackrook.lang.reflect.TypeProfile;
import com.blackrook.lang.reflect.TypeProfile.MethodSignature;
import com.blackrook.lang.util.EntityTables;

/**
 * Utility library for common or useful functions.
 * @author Matthew Tropiano
 */
public final class BRUtil implements EntityTables
{
	/** MIME Type Map. */
	private static BRMIMETypes MIME_TYPE_MAP = new BRMIMETypes();
	/** Singleton context for beans not attached to the application context. */
	private static final HashMap<String, Object> SINGLETON_MAP = new HashMap<String, Object>();
	/** Date format parser map. */
	private static final HashMap<String, SimpleDateFormat> DATE_PATTERN_MAP = new HashMap<String, SimpleDateFormat>();

	private BRUtil() {}
	
	/**
	 * Gets the MIME type of a file (uses a database that is more complete than javax.activation).
	 * @param filename the file name to use to figure out a MIME type.
	 * @return the MIME type, or <code>application/octet-stream</code>.
	 */
	public static String getMIMEType(String filename)
	{
		return MIME_TYPE_MAP.getType(Common.getFileExtension(filename));
		}
	
	/**
	 * Encodes a string so that it can be input safely into a URL string.
	 */
	public static String urlEncode(String inString)
	{
		StringBuffer sb = new StringBuffer();
		char[] inChars = inString.toCharArray();
		int i = 0;
		while (i < inChars.length)
		{
			char c = inChars[i];
			if (!((c >= 0x30 && c <= 0x39) || (c >= 0x41 && c <= 0x5a) || (c >= 0x61 && c <= 0x7a)))
				sb.append(String.format("%%%02x", (short)c));
			else
				sb.append(c);
			i++;
			}
		return sb.toString();
		}
	
	/**
	 * Decodes a URL-encoded string.
	 */
	public static String urlDecode(String inString)
	{
		StringBuffer sb = new StringBuffer();
		char[] inChars = inString.toCharArray();
		char[] chars = new char[2];
		int x = 0;
		
		final int STATE_START = 0;
		final int STATE_DECODE = 1;
		int state = STATE_START;
		
		int i = 0;
		while (i < inChars.length)
		{
			char c = inChars[i];
			
			switch (state)
			{
				case STATE_START:
					if (c == '%')
					{
						x = 0;
						state = STATE_DECODE;
						}
					else
						sb.append(c);
					break;
				case STATE_DECODE:
					chars[x++] = c;
					if (x == 2)
					{
						int v = 0;
						try {
							v = Integer.parseInt(new String(chars), 16);
							sb.append((char)(v & 0x0ff));
						} catch (NumberFormatException e) {
							sb.append('%').append(chars[0]).append(chars[1]);
							}
						state = STATE_START;
						}
					break;
				}
			
			i++;
			}
		
		if (state == STATE_DECODE)
		{
			sb.append('%');
			for (int n = 0; n < x; n++)
				sb.append(chars[n]);
			}
		
		return sb.toString();
		}
	
	/**
	 * Converts a String to an HTML-safe string.
	 * @param input the input string to convert.
	 * @return the converted string.
	 */
	public static String convertToHTMLEntities(String input)
	{
		StringBuilder sb = new StringBuilder();
		char[] chars = input.toCharArray();
		
		for (int i = 0; i < chars.length; i++)
		{
			char c = chars[i];
			
			if (c < 0x0020 || c >= 0x007f)
				sb.append(String.format("&#x%04x;", c));
			else if (c == '&')
				sb.append("&amp;");
			else if (c == '<')
				sb.append("&lt;");
			else if (c == '>')
				sb.append("&gt;");
			else if (c == '"')
				sb.append("&quot;");
			else if (c == '\'')
				sb.append("&apos;");
			else
				sb.append(c);
			}
		
		return sb.toString();
		}
	
	/**
	 * Converts a String with HTML entities in it to one without.
	 * @param input the input string to convert.
	 * @return the converted string.
	 */
	public static String convertFromHTMLEntities(String input)
	{
		StringBuilder sb = new StringBuilder();
		StringBuilder entity = new StringBuilder();
		char[] chars = input.toCharArray();
		
		final int STATE_STRING = 0;
		final int STATE_ENTITY = 1;
		int state = STATE_STRING;
		
		for (int i = 0; i < chars.length; i++)
		{
			char c = chars[i];
			
			switch (state)
			{
				case STATE_STRING:
					if (c == '&')
					{
						entity.delete(0, entity.length());
						state = STATE_ENTITY;
						}
					else
						sb.append(c);
					break;
				case STATE_ENTITY:
					if (c == ';')
					{
						String e = entity.toString();
						if (e.startsWith("#x"))
						{
							int n = Integer.parseInt(e.substring(2), 16);
							sb.append((char)n);
							}
						else if (e.startsWith("#"))
						{
							int n = Integer.parseInt(e.substring(1), 10);
							sb.append((char)n);
							}
						else if (ENTITY_NAME_MAP.containsKey(e))
							sb.append(ENTITY_NAME_MAP.get(e));
						else
							sb.append(e);
						
						state = STATE_STRING;
						}
					else
						entity.append(c);
					break;
				}
			}
		
		if (state == STATE_ENTITY)
			sb.append('&').append(entity.toString());
		
		return sb.toString();
		}

	/**
	 * Gets the current connection's session id.
	 * Meant to be a convenience method. 
	 * @param request servlet request object.
	 */
	public static String getSessionId(HttpServletRequest request)
	{
		return request.getSession().getId();
		}

	/**
	 * Gets and auto-casts an object bean stored at the session level.
	 * The bean is created and stored if it doesn't exist.
	 * @param request the source request object.
	 * @param name the attribute name.
	 * @param clazz the class type of the object that should be returned.
	 * @return a typecast object on the request, or <code>null</code>, if the session is null or the attribute does not exist.
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
	 * @return a typecast object on the request.
	 * @throws BRFrameworkException if the object cannot be instantiated for any reason.
	 */
	public static <T> T getRequestBean(HttpServletRequest request, Class<T> clazz, String name, boolean create)
	{
		Object obj = request.getAttribute(name);
		if (obj == null)
		{
			try {
				obj = create ? clazz.newInstance() : null;
				request.setAttribute(name, obj);
			} catch (Exception e) {
				throwException(e);
				}
			}
	
		if (obj == null)
			return null;
		return clazz.cast(obj);
		}

	/**
	 * Gets and auto-casts an object bean stored at the session level.
	 * The bean is created and stored if it doesn't exist.
	 * @param request the source request object.
	 * @param clazz the class type of the object that should be returned.
	 * @param name the attribute name.
	 * @return a typecast object on the session, or <code>null</code>, if the session is null.
	 * @throws BRFrameworkException if the object cannot be instantiated for any reason.
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
	 * @return a typecast object on the session, a new instance if it doesn't exist, or null if the session is null.
	 * @throws BRFrameworkException if the object cannot be instantiated for any reason.
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
						obj = clazz.newInstance();
						session.setAttribute(name, obj);
						}
					}
			} catch (Exception e) {
				throwException(e);
				}
			}
	
		if (obj == null)
			return null;
		return clazz.cast(obj);
		}

	/**
	 * Gets and auto-casts an object bean stored at the application level.
	 * The bean is created and stored if it doesn't exist.
	 * The name used is the fully-qualified class name.
	 * @param context the servlet context to use.
	 * @param clazz the class type of the object that should be returned.
	 * @return a typecast object on the application scope.
	 * @throws BRFrameworkException if the object cannot be instantiated for any reason.
	 */
	public static <T> T getApplicationBean(ServletContext context, Class<T> clazz)
	{
		return getApplicationBean(context, clazz, "$$"+clazz.getCanonicalName(), true);
		}

	/**
	 * Gets and auto-casts an object bean stored at the application level.
	 * The bean is created and stored if it doesn't exist.
	 * @param context the servlet context to use.
	 * @param clazz the class type of the object that should be returned.
	 * @param name the attribute name.
	 * @return a typecast object on the application scope.
	 * @throws BRFrameworkException if the object cannot be instantiated for any reason.
	 */
	public static <T> T getApplicationBean(ServletContext context, Class<T> clazz, String name)
	{
		return getApplicationBean(context, clazz, name, true);
		}

	/**
	 * Gets and auto-casts an object bean stored at the application level.
	 * @param context the servlet context to use.
	 * @param clazz the class type of the object that should be returned.
	 * @param name the attribute name.
	 * @param create if true, instantiate this class in the session (via {@link Class#newInstance()}) if it doesn't exist.
	 * @return a typecast object on the application scope, or null if it doesn't exist and wasn't created.
	 * @throws BRFrameworkException if the object cannot be instantiated for any reason.
	 */
	public static <T> T getApplicationBean(ServletContext context, Class<T> clazz, String name, boolean create)
	{
		Object obj = context.getAttribute(name);
		if (obj == null && create)
		{
			try {
				synchronized (context)
				{
					if ((obj = context.getAttribute(name)) == null)
					{
						obj = clazz.newInstance();
						context.setAttribute(name, obj);
						}
					}
			} catch (Exception e) {
				throwException(e);
				}
			}
	
		if (obj == null)
			return null;
		return clazz.cast(obj);
		}

	/**
	 * Gets and auto-casts an object bean stored at the program level,
	 * accessible always, and not attached to a servlet context.
	 * The bean is created and stored if it doesn't exist.
	 * The name used is the fully-qualified class name.
	 * @param clazz the class type of the object that should be returned.
	 * @return a typecast object on the application scope.
	 * @throws BRFrameworkException if the object cannot be instantiated for any reason.
	 */
	public static <T> T getBean(Class<T> clazz)
	{
		return getBean(clazz, "$$"+clazz.getPackage().getName()+"."+clazz.getName(), true);
		}

	/**
	 * Gets and auto-casts an object bean stored at the program level,
	 * accessible always, and not attached to a servlet context.
	 * @param clazz the class type of the object that should be returned.
	 * @param name the attribute name.
	 * @return a typecast object on the application scope.
	 * @throws BRFrameworkException if the object cannot be instantiated for any reason.
	 */
	public static <T> T getBean(Class<T> clazz, String name)
	{
		return getBean(clazz, name, true);
		}

	/**
	 * Gets and auto-casts an object bean stored at the program level,
	 * accessible always, and not attached to a servlet context.
	 * @param clazz the class type of the object that should be returned.
	 * @param name the attribute name.
	 * @param create if true, instantiate this class in the session (via {@link Class#newInstance()}) if it doesn't exist.
	 * @return a typecast object on the application scope, or null if it doesn't exist and wasn't created.
	 * @throws BRFrameworkException if the object cannot be instantiated for any reason.
	 */
	public static <T> T getBean(Class<T> clazz, String name, boolean create)
	{
		Object obj = SINGLETON_MAP.get(name);
		if (obj == null && create)
		{
			synchronized (SINGLETON_MAP) 
			{
				obj = SINGLETON_MAP.get(name);
				if (obj == null)
				{
					try {
						obj = clazz.newInstance();
						SINGLETON_MAP.put(name, obj);
					} catch (Exception e) {
						throwException(e);
						}
					}
				}
			}
	
		if (obj == null)
			return null;
		else
			return clazz.cast(obj);
		}

	/**
	 * Gets a group of parameters that start with a specific prefix.
	 * @param prefix the prefix to search for.
	 * @return a HashMap containing a map of parameter to String value of parameter. 
	 * The parameters in the map are ones that match the prefix.
	 */
	@SuppressWarnings("unchecked")
	public static HashMap<String, String> getParameters(HttpServletRequest request, String prefix)
	{
		HashMap<String, String> out = new HashMap<String, String>();
		for (Map.Entry<String, String> entry : ((Map<String, String>)request.getParameterMap()).entrySet())
			if (entry.getKey().startsWith(prefix))
				out.put(entry.getKey(), entry.getValue());
		return out;
		}
	
	/**
	 * Convenience method that checks if a parameter exists on a request. 
	 * Returns true if it exists, false otherwise.
	 */
	public static boolean getParameterExist(HttpServletRequest request, String paramName)
	{
		return request.getParameterValues(paramName) != null && request.getParameterValues(paramName).length > 0;
		}

	/**
	 * Convenience method that calls <code>session.getAttribute(attribName)</code> 
	 * and returns true if it exists, false otherwise.
	 */
	public static boolean getAttributeExist(HttpSession session, String attribName)
	{
		return session.getAttribute(attribName) != null;
		}

	/**
	 * Convenience method that calls <code>context.getAttribute(attribName)</code> 
	 * and returns true if it exists, false otherwise.
	 */
	public static boolean getAttributeExist(ServletContext context, String attribName)
	{
		return context.getAttribute(attribName) != null;
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and returns true or false.
	 * This flavor of <code>getParameterBoolean</code> assumes that the parameter
	 * received is a string that is either "true" or not "true".
	 * @see Common#parseBoolean(String)
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
	 */
	public static boolean getParameterBoolean(HttpServletRequest request, String paramName, String trueValue)
	{
		String out = request.getParameter(paramName);
		return (out == null && trueValue == null) || (out != null && out.equalsIgnoreCase(trueValue));
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and returns the empty string if it doesn't exist.
	 */
	public static String getParameterString(HttpServletRequest request, String paramName)
	{
		return getParameterString(request, paramName, "");
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and returns <code>def</code> if it doesn't exist.
	 */
	public static String getParameterString(HttpServletRequest request, String paramName, String def)
	{
		String out = request.getParameter(paramName);
		return out != null ? out : def;
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a byte and returns 0 if it doesn't exist.
	 */
	public static byte getParameterByte(HttpServletRequest request, String paramName)
	{
		return getParameterByte(request, paramName, (byte)0);
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a byte and returns <code>def</code> if it doesn't exist.
	 */
	public static byte getParameterByte(HttpServletRequest request, String paramName, byte def)
	{
		return Common.parseByte(request.getParameter(paramName), def);
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a short and returns 0 if it doesn't exist.
	 */
	public static short getParameterShort(HttpServletRequest request, String paramName)
	{
		return getParameterShort(request, paramName, (short)0);
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a short and returns <code>def</code> if it doesn't exist.
	 */
	public static short getParameterShort(HttpServletRequest request, String paramName, short def)
	{
		return Common.parseShort(request.getParameter(paramName), def);
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a char and returns '\0' if it doesn't exist.
	 */
	public static char getParameterChar(HttpServletRequest request, String paramName)
	{
		return getParameterChar(request, paramName, '\0');
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a char and returns <code>def</code> if it doesn't exist.
	 */
	public static char getParameterChar(HttpServletRequest request, String paramName, char def)
	{
		return Common.parseChar(request.getParameter(paramName), def);
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses an integer and returns 0 if it doesn't exist.
	 */
	public static int getParameterInt(HttpServletRequest request, String paramName)
	{
		return getParameterInt(request, paramName, 0);
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses an integer and returns <code>def</code> if it doesn't exist.
	 */
	public static int getParameterInt(HttpServletRequest request, String paramName, int def)
	{
		return Common.parseInt(request.getParameter(paramName), def);
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a float and returns 0.0f if it doesn't exist.
	 */
	public static float getParameterFloat(HttpServletRequest request, String paramName)
	{
		return getParameterFloat(request, paramName, 0f);
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a float and returns <code>def</code> if it doesn't exist.
	 */
	public static float getParameterFloat(HttpServletRequest request, String paramName, float def)
	{
		return Common.parseFloat(request.getParameter(paramName), def);
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a long integer and returns 0L if it doesn't exist.
	 */
	public static long getParameterLong(HttpServletRequest request, String paramName)
	{
		return getParameterLong(request, paramName, 0L);
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a long integer and returns <code>def</code> if it doesn't exist.
	 */
	public static long getParameterLong(HttpServletRequest request, String paramName, long def)
	{
		return Common.parseLong(request.getParameter(paramName), def);
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a double and returns 0.0 if it doesn't exist.
	 */
	public static double getParameterDouble(HttpServletRequest request, String paramName)
	{
		return getParameterDouble(request, paramName, 0.0);
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a double and returns <code>def</code> if it doesn't exist.
	 */
	public static double getParameterDouble(HttpServletRequest request, String paramName, double def)
	{
		return Common.parseDouble(request.getParameter(paramName), def);
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
			out = formatter.parse(getParameterString(request, paramName));
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
	 * @return the new object with fields set using the model.
	 * @throws RuntimeException if an exception occurs - notably if the fields or setters on the class cannot be reached
	 * (best to use public classes in these cases), or if the object cannot be instantiated.
	 */
	public static <T extends Object> T setModelFields(HttpServletRequest request, Class<T> type)
	{
		return setModelFields(request, Reflect.create(type));
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
	 * @return the passed in object.
	 * @throws RuntimeException if an exception occurs - notably if the fields or setters on the class cannot be reached
	 * (best to use public classes in these cases).
	 */
	public static <T extends Object> T setModelFields(HttpServletRequest request, T target)
	{
		HttpSession session = request.getSession();
		ServletContext context = session != null ? session.getServletContext() : null;
		
		TypeProfile<?> profile = TypeProfile.getTypeProfile((Class<?>)target.getClass());
		Hash<String> foundFields = new Hash<String>();
		
		for (ObjectPair<String, Field> pair : profile.getPublicFields())
		{
			String fieldName = pair.getKey();
			Field field = pair.getValue();
			if (getParameterExist(request, fieldName))
			{
				Class<?> setterType = field.getType();
				if (Reflect.isArray(setterType))
				{
					String[] values = request.getParameterValues(fieldName);
					Class<?> arrayType = Reflect.getArrayType(setterType);
					Object newArray = Array.newInstance(arrayType, values.length);
					
					for (int i = 0; i < values.length; i++)
					{
						if (arrayType != String.class)
							Array.set(newArray, i, getConvertedModelObject(fieldName, getParameterString(request, fieldName), arrayType));
						else
							Array.set(newArray, i, getParameterString(request, fieldName));
						}
					Reflect.setField(target, fieldName, newArray);
					}
				else
				{
					if (setterType != String.class)
						Reflect.setField(target, fieldName, getConvertedModelObject(fieldName, getParameterString(request, fieldName), setterType));
					else
						Reflect.setField(target, fieldName, getParameterString(request, fieldName));
					}
				foundFields.put(fieldName);
				}
			else if (session != null && getAttributeExist(session, fieldName))
			{
				Object objval = session.getAttribute(fieldName);
				if (field.getType() == objval.getClass())
					Reflect.setField(target, fieldName, objval);
				else
					throw new BRFrameworkException("Model and session attribute types for field \""+fieldName+"\" do not match.");
				foundFields.put(fieldName);
				}
			else if (context != null && getAttributeExist(context, fieldName))
			{
				Object objval = context.getAttribute(fieldName);
				if (field.getType() == objval.getClass())
					Reflect.setField(target, fieldName, objval);
				else
					throw new BRFrameworkException("Model and context attribute types for field \""+fieldName+"\" do not match.");
				foundFields.put(fieldName);
				}
			}
		
		for (ObjectPair<String, MethodSignature> pair : profile.getSetterMethods())
		{
			String fieldName = pair.getKey();
			if (foundFields.contains(fieldName))
				continue;
			
			MethodSignature signature = pair.getValue();
			if (getParameterExist(request, fieldName))
			{
				Class<?> setterType = signature.getType();
				if (Reflect.isArray(setterType))
				{
					String[] values = request.getParameterValues(fieldName);
					Class<?> arrayType = Reflect.getArrayType(setterType);
					Object newArray = Array.newInstance(arrayType, values.length);
					
					for (int i = 0; i < values.length; i++)
					{
						if (arrayType != String.class)
							Array.set(newArray, i, getConvertedModelObject(fieldName, values[i], arrayType));
						else
							Array.set(newArray, i, getParameterString(request, fieldName));
						}
					Reflect.invokeBlind(signature.getMethod(), target, newArray);
					}
				else
				{
					if (setterType != String.class)
						Reflect.invokeBlind(signature.getMethod(), target, getConvertedModelObject(fieldName, getParameterString(request, fieldName), setterType));
					else
						Reflect.invokeBlind(signature.getMethod(), target, getParameterString(request, fieldName));
					}
				}
			else if (session != null && getAttributeExist(session, fieldName))
			{
				Object objval = session.getAttribute(fieldName);
				if (signature.getType() == objval.getClass())
					Reflect.invokeBlind(signature.getMethod(), target, objval);
				else
					throw new BRFrameworkException("Model and session attribute types for field \""+fieldName+"\" do not match.");
				}
			else if (context != null && getAttributeExist(context, fieldName))
			{
				Object objval = context.getAttribute(fieldName);
				if (signature.getType() == objval.getClass())
					Reflect.invokeBlind(signature.getMethod(), target, objval);
				else
					throw new BRFrameworkException("Model and context attribute types for field \""+fieldName+"\" do not match.");
				}
			}
		
		return target;
		}
	
	// Converts values.
	private static Object getConvertedModelObject(String name, Object source, Class<?> type)
	{
		if (type == String.class)
			return source != null ? String.valueOf(source) : null;
		
		if (source instanceof Boolean)
		{
			boolean b = (Boolean)source;
			if (type == Boolean.TYPE)
				return b;
			else if (type == Boolean.class)
				return b;
			else if (type == Byte.TYPE)
				return (byte)(b ? 1 : 0);
			else if (type == Byte.class)
				return (byte)(b ? 1 : 0);
			else if (type == Short.TYPE)
				return (short)(b ? 1 : 0);
			else if (type == Short.class)
				return (short)(b ? 1 : 0);
			else if (type == Integer.TYPE)
				return (b ? 1 : 0);
			else if (type == Integer.class)
				return (b ? 1 : 0);
			else if (type == Float.TYPE)
				return (b ? 1f : 0f);
			else if (type == Float.class)
				return (b ? 1f : 0f);
			else if (type == Long.TYPE)
				return (b ? 1L : 0L);
			else if (type == Long.class)
				return (b ? 1L : 0L);
			else if (type == Double.TYPE)
				return (b ? 1.0 : 0.0);
			else if (type == Double.class)
				return (b ? 1.0 : 0.0);
			else
				throw new BRFrameworkException("Member "+name+" is boolean typed; target is not boolean typed.");
			}
		else if (source instanceof Number)
		{
			Number n = (Number)source;
			
			if (type == Boolean.TYPE)
				return (n.doubleValue() != 0.0);
			else if (type == Boolean.class)
				return (!Double.isNaN(n.doubleValue()) && n.doubleValue() != 0.0);
			else if (type == Byte.TYPE)
				return n.byteValue();
			else if (type == Byte.class)
				return n.byteValue();
			else if (type == Short.TYPE)
				return n.shortValue();
			else if (type == Short.class)
				return n.shortValue();
			else if (type == Integer.TYPE)
				return n.intValue();
			else if (type == Integer.class)
				return n.intValue();
			else if (type == Float.TYPE)
				return n.floatValue();
			else if (type == Float.class)
				return n.floatValue();
			else if (type == Long.TYPE)
				return n.longValue();
			else if (type == Long.class)
				return n.longValue();
			else if (type == Double.TYPE)
				return n.doubleValue();
			else if (type == Double.class)
				return n.doubleValue();
			else if (type == String.class)
				return n.doubleValue();
			else
				throw new BRFrameworkException("Cannot convert attribute "+name+".");
			}
		else if (source instanceof String)
		{
			String s = (String)source;
			if (type == Boolean.TYPE)
				return Common.parseBoolean(s);
			else if (type == Boolean.class)
				return Common.parseBoolean(s);
			else if (type == Byte.TYPE)
				return Common.parseByte(s);
			else if (type == Byte.class)
				return Common.parseByte(s);
			else if (type == Short.TYPE)
				return Common.parseShort(s);
			else if (type == Short.class)
				return Common.parseShort(s);
			else if (type == Integer.TYPE)
				return Common.parseInt(s);
			else if (type == Integer.class)
				return Common.parseInt(s);
			else if (type == Float.TYPE)
				return Common.parseFloat(s);
			else if (type == Float.class)
				return Common.parseFloat(s);
			else if (type == Long.TYPE)
				return Common.parseLong(s);
			else if (type == Long.class)
				return Common.parseLong(s);
			else if (type == Double.TYPE)
				return Common.parseDouble(s);
			else if (type == Double.class)
				return Common.parseDouble(s);
			else if (type == String.class)
				return s;
			else
				throw new BRFrameworkException("Cannot convert attribute "+name+".");
			}
		
		throw new BRFrameworkException("Cannot convert attribute "+name+".");
		}
	
	/**
	 * Forces an exception to propagate up to the dispatcher.
	 * Basically encloses the provided throwable in a {@link BRFrameworkException},
	 * which is a {@link RuntimeException}.
	 * @param t the {@link Throwable} to encapsulate and throw.
	 */
	public static void throwException(Throwable t)
	{
		throw new BRFrameworkException(t);
		}
	
}
