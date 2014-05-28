package com.blackrook.j2ee.small;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Random;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.blackrook.commons.Common;
import com.blackrook.commons.ObjectPair;
import com.blackrook.commons.Reflect;
import com.blackrook.commons.TypeProfile;
import com.blackrook.commons.TypeProfile.MethodSignature;
import com.blackrook.commons.hash.Hash;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.j2ee.small.exception.SmallFrameworkException;
import com.blackrook.j2ee.small.types.MIMETypes;
import com.blackrook.lang.json.JSONObject;
import com.blackrook.lang.json.JSONWriter;
import com.blackrook.lang.util.EntityTables;
import com.blackrook.lang.xml.XMLStruct;
import com.blackrook.lang.xml.XMLWriter;

/**
 * Utility library for common or useful functions.
 * @author Matthew Tropiano
 */
public final class SmallUtil implements EntityTables
{
	/** MIME Type Map. */
	private static MIMETypes MIME_TYPE_MAP = new MIMETypes();
	/** Singleton context for beans not attached to the application context. */
	private static final HashMap<String, Object> SINGLETON_MAP = new HashMap<String, Object>();
	/** Date format parser map. */
	private static final HashMap<String, SimpleDateFormat> DATE_PATTERN_MAP = new HashMap<String, SimpleDateFormat>();
	/** MIME type for JSON */
	public static final String CONTENT_MIME_TYPE_JSON = "application/json";
	/** MIME type for XML */
	public static final String CONTENT_MIME_TYPE_XML = "application/xml";
	/** Lag simulator seed. */
	private static Random randomLagSimulator = new Random();

	private SmallUtil() {}
	
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
	 * Gets and auto-casts an object bean stored at the request level.
	 * The bean is created and stored if it doesn't exist.
	 * The name used is the fully-qualified class name prefixed with "$$".
	 * @param request the source request object.
	 * @param clazz the class type of the object that should be returned.
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
	 * @return a typecast object on the request.
	 * @throws SmallFrameworkException if the object cannot be instantiated for any reason.
	 */
	public static <T> T getRequestBean(HttpServletRequest request, Class<T> clazz, String name, boolean create)
	{
		Object obj = request.getAttribute(name);
		if (obj == null && create)
		{
			try {
				obj = clazz.newInstance();
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
	 * The name used is the fully-qualified class name prefixed with "$$".
	 * @param request the source request object.
	 * @param clazz the class type of the object that should be returned.
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
	 * The name used is the fully-qualified class name prefixed with "$$".
	 * @param context the servlet context to use.
	 * @param clazz the class type of the object that should be returned.
	 * @return a typecast object on the application scope.
	 * @throws IllegalArgumentException if the class provided in an anonymous class or array without a component type.
	 * @throws SmallFrameworkException if the object cannot be instantiated for any reason.
	 */
	public static <T> T getApplicationBean(ServletContext context, Class<T> clazz)
	{
		String className = clazz.getCanonicalName(); 
		if ((className = clazz.getCanonicalName()) == null)
			throw new IllegalArgumentException("Class provided has no type!");
		return getApplicationBean(context, clazz, "$$"+className, true);
	}

	/**
	 * Gets and auto-casts an object bean stored at the application level.
	 * The bean is created and stored if it doesn't exist.
	 * @param context the servlet context to use.
	 * @param clazz the class type of the object that should be returned.
	 * @param name the attribute name.
	 * @return a typecast object on the application scope.
	 * @throws SmallFrameworkException if the object cannot be instantiated for any reason.
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
	 * @param create if true, instantiate this class in the application's servlet context (via {@link Class#newInstance()}) if it doesn't exist.
	 * @return a typecast object on the application scope, or null if it doesn't exist and wasn't created.
	 * @throws SmallFrameworkException if the object cannot be instantiated for any reason.
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
	 * Gets and auto-casts a singleton object bean stored at the program level,
	 * accessible always, and not attached to a servlet context.
	 * The bean is created and stored if it doesn't exist.
	 * The name used is the fully-qualified class name.
	 * @param clazz the class type of the object that should be returned.
	 * @return a typecast object on the application scope.
	 * @throws SmallFrameworkException if the object cannot be instantiated for any reason.
	 */
	public static <T> T getBean(Class<T> clazz)
	{
		return getBean(clazz, "$$"+clazz.getName(), true);
	}

	/**
	 * Gets and auto-casts a singleton object bean stored at the program level,
	 * accessible always, and not attached to a servlet context.
	 * The bean is created and stored if it doesn't exist.
	 * @param clazz the class type of the object that should be returned.
	 * @param name the attribute name.
	 * @return a typecast object on the application scope.
	 * @throws SmallFrameworkException if the object cannot be instantiated for any reason.
	 */
	public static <T> T getBean(Class<T> clazz, String name)
	{
		return getBean(clazz, name, true);
	}

	/**
	 * Gets and auto-casts a singleton object bean stored at the program level,
	 * accessible always, and not attached to a servlet context.
	 * @param clazz the class type of the object that should be returned.
	 * @param name the attribute name.
	 * @param create if true, instantiate this class (via {@link Class#newInstance()}) if it doesn't exist.
	 * @return a typecast object on the application scope, or null if it doesn't exist and wasn't created.
	 * @throws SmallFrameworkException if the object cannot be instantiated for any reason.
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
							Array.set(newArray, i, Reflect.createForType(fieldName, getParameterString(request, fieldName), arrayType));
						else
							Array.set(newArray, i, getParameterString(request, fieldName));
					}
					Reflect.setField(target, fieldName, newArray);
				}
				else
				{
					if (setterType != String.class)
						Reflect.setField(target, fieldName, Reflect.createForType(fieldName, getParameterString(request, fieldName), setterType));
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
					throw new SmallFrameworkException("Model and session attribute types for field \""+fieldName+"\" do not match.");
				foundFields.put(fieldName);
			}
			else if (context != null && getAttributeExist(context, fieldName))
			{
				Object objval = context.getAttribute(fieldName);
				if (field.getType() == objval.getClass())
					Reflect.setField(target, fieldName, objval);
				else
					throw new SmallFrameworkException("Model and context attribute types for field \""+fieldName+"\" do not match.");
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
							Array.set(newArray, i, Reflect.createForType(fieldName, values[i], arrayType));
						else
							Array.set(newArray, i, getParameterString(request, fieldName));
					}
					Reflect.invokeBlind(signature.getMethod(), target, newArray);
				}
				else
				{
					if (setterType != String.class)
						Reflect.invokeBlind(signature.getMethod(), target, Reflect.createForType(fieldName, getParameterString(request, fieldName), setterType));
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
					throw new SmallFrameworkException("Model and session attribute types for field \""+fieldName+"\" do not match.");
			}
			else if (context != null && getAttributeExist(context, fieldName))
			{
				Object objval = context.getAttribute(fieldName);
				if (signature.getType() == objval.getClass())
					Reflect.invokeBlind(signature.getMethod(), target, objval);
				else
					throw new SmallFrameworkException("Model and context attribute types for field \""+fieldName+"\" do not match.");
			}
		}
		
		return target;
	}
	
	/**
	 * Forces an exception to propagate up to the dispatcher.
	 * Basically encloses the provided throwable in a {@link SmallFrameworkException},
	 * which is a {@link RuntimeException}.
	 * @param t the {@link Throwable} to encapsulate and throw.
	 */
	public static void throwException(Throwable t)
	{
		throw new SmallFrameworkException(t);
	}

	/**
	 * Includes the output of a view in the response.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param path target view path relative to the application context.
	 */
	public static void includeView(HttpServletRequest request, HttpServletResponse response, String path)
	{
		try{
			request.getRequestDispatcher(path).include(request, response);
		} catch (Exception e) {
			throwException(e);
		}
	}

	/**
	 * Surreptitiously forwards the request to a view.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param path target view path relative to the application context.
	 */
	public static void sendToView(HttpServletRequest request, HttpServletResponse response, String path)
	{
		try{
			request.getRequestDispatcher(path).forward(request, response);
		} catch (Exception e) {
			throwException(e);
		}
	}

	/**
	 * Sends back JSON to the client.
	 * The "Content-Type" portion of the header is changed to "application/json".
	 * @param response the servlet response to write to.
	 * @param jsonObject the JSON Object to write to the request.
	 */
	public static void sendJSON(HttpServletResponse response, JSONObject jsonObject)
	{
		response.setHeader("Content-Type", CONTENT_MIME_TYPE_JSON);
		try {
			JSONWriter.writeJSON(jsonObject, response.getWriter());
		} catch (IOException e) {
			throwException(e);
		}
	}

	/**
	 * Sends back a JSON-ified object to the client.
	 * Works best with POJOs and Small beans.
	 * The "Content-Type" portion of the header is changed to "application/json".
	 * @param response the servlet response to write to.
	 * @param object the Object to write to the request, which then is .
	 */
	public static void sendJSON(HttpServletResponse response, Object object)
	{
		response.setHeader("Content-Type", CONTENT_MIME_TYPE_JSON);
		try {
			JSONWriter.writeJSON(JSONObject.create(object), response.getWriter());
		} catch (IOException e) {
			throwException(e);
		}
	}

	/**
	 * Sends back XML to the client.
	 * The "Content-Type" portion of the header is changed to "application/json".
	 * @param response the servlet response to write to.
	 * @param xml the XML structure to write to the request.
	 */
	public static void sendXML(HttpServletResponse response, XMLStruct xml)
	{
		response.setHeader("Content-Type", CONTENT_MIME_TYPE_XML);
		try {
			(new XMLWriter()).writeXML(xml, response.getWriter());
		} catch (IOException e) {
			throwException(e);
		}
	}

	/**
	 * Sends a file to the client.
	 * Via this method, most browsers will be forced to download the file in
	 * question, as this adds "Content-Disposition" headers to the response.
	 * The file's MIME type is guessed by its extension.
	 * The file's name becomes the filename in the content's "disposition".
	 * @param response servlet response object.
	 * @param file the file content to send.
	 */
	public static void sendFile(HttpServletResponse response, File file)
	{
		sendFileContents(response, SmallUtil.getMIMEType(file.getName()), file, file.getName());
	}

	/**
	 * Sends a file to the client.
	 * The file's name becomes the filename in the content's "disposition".
	 * Via this method, most browsers will be forced to download the file in
	 * question separately, as this adds "Content-Disposition" headers to the response.
	 * @param response servlet response object.
	 * @param mimeType the MIME Type of the content to send.
	 * @param file the file content to send.
	 */
	public static void sendFile(HttpServletResponse response, String mimeType, File file)
	{
		sendFileContents(response, mimeType, file, file.getName());
	}

	/**
	 * Sends the contents of a file to the client.
	 * Via this method, most browsers will attempt to open the file in-browser,
	 * as this has no "Content-Disposition" attached to it.
	 * The file's MIME type is guessed by its extension.
	 * @param response servlet response object.
	 * @param file the file content to send.
	 */
	public static void sendFileContents(HttpServletResponse response, File file)
	{
		sendFileContents(response, SmallUtil.getMIMEType(file.getName()), file, null);
	}

	/**
	 * Sends contents of a file to the client.
	 * Via this method, most browsers will attempt to open the file in-browser,
	 * as this has no "Content-Disposition" attached to it.
	 * @param response servlet response object.
	 * @param mimeType the MIME Type of the content to send.
	 * @param file the file content to send.
	 */
	public static void sendFileContents(HttpServletResponse response, String mimeType, File file)
	{
		sendFileContents(response, mimeType, file, null);
	}

	/**
	 * Sends the contents of a file to the client.
	 * @param response servlet response object.
	 * @param mimeType the MIME Type of the content to send.
	 * @param file the file content to send.
	 * @param fileName the new file name of what to send. Can be null - leaving it out
	 * does not send "Content-Disposition" headers. You may want to do this if you don't 
	 * care whether the file is downloaded or opened by the browser. 
	 */
	public static void sendFileContents(HttpServletResponse response, String mimeType, File file, String fileName)
	{
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			sendData(response, mimeType, fileName, fis, file.length());
		} catch (IOException e) {
			throwException(e);
		} finally {
			Common.close(fis);
		}
	}

	/**
	 * Sends the contents of a stream out through the response.
	 * The input stream is not closed after the data is sent.
	 * The "Content-Type" portion of the header is changed to <code>mimeType</code>.
	 * The "Content-Length" portion of the header is changed to <code>length</code>, if positive.
	 * @param response servlet response object.
	 * @param mimeType the MIME-Type of the stream.
	 * @param contentName the name of the data to send (file name). Can be null - leaving it out
	 * 	does not send "Content-Disposition" headers.
	 * @param inStream the input stream to read.
	 * @param length the length of data in bytes to send.
	 */
	public static void sendData(HttpServletResponse response, String mimeType, String contentName, InputStream inStream, long length)
	{
		sendData(response, mimeType, contentName, null, inStream, length);
	}

	/**
	 * Sends the contents of a stream out through the response.
	 * The input stream is not closed after the data is sent.
	 * The "Content-Type" portion of the header is changed to <code>mimeType</code>.
	 * The "Content-Length" portion of the header is changed to <code>length</code>, if positive.
	 * @param response servlet response object.
	 * @param mimeType the MIME-Type of the stream.
	 * @param contentName the name of the data to send (file name). Can be null - leaving it out
	 * 	does not send "Content-Disposition" headers.
	 * @param encoding if not null, adds a "Content-Encoding" header.
	 * @param inStream the input stream to read.
	 * @param length the length of data in bytes to send.
	 */
	public static void sendData(HttpServletResponse response, String mimeType, String contentName, String encoding, InputStream inStream, long length)
	{
		response.setHeader("Content-Type", mimeType);
		if (!Common.isEmpty(encoding))
			response.setHeader("Content-Encoding", encoding);
		if (!Common.isEmpty(contentName))
			response.setHeader("Content-Disposition", "attachment; filename=\"" + contentName + "\"");
		if (length >= 0)
			response.setHeader("Content-Length", String.valueOf(length));
		
		try {
			while (length > 0)
			{
				length -= Common.relay(
					inStream, 
					response.getOutputStream(), 
					32768, 
					length > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)length
				);
			}
		} catch (IOException e) {
			throwException(e);
		}
	}

	/**
	 * Sends request to the error page with a status code.
	 * @param response servlet response object.
	 * @param statusCode the status code to use.
	 * @param message the status message.
	 */
	public static void sendCode(HttpServletResponse response, int statusCode, String message)
	{
		try{
			response.sendError(statusCode, message);
		} catch (Exception e) {
			throwException(e);
		}
	}

	/**
	 * Forwards the client abruptly to another document or servlet (new client request). 
	 * @param response	servlet response object.
	 * @param url		target URL.
	 */
	public static void sendRedirect(HttpServletResponse response, String url)
	{
		try{
			response.sendRedirect(url);
		} catch (Exception e) {
			throwException(e);
		}
	}

	/**
	 * Pauses the current thread for up to <code>maxMillis</code>
	 * milliseconds, used for simulating lag.
	 * For debugging and testing only!
	 */
	public static void simulateLag(long maxMillis)
	{
		Common.sleep(randomLagSimulator.nextLong() % (maxMillis <= 1 ? 1 :maxMillis));
	}
	
}
