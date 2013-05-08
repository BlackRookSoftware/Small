package com.blackrook.framework;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.blackrook.commons.AbstractVector;
import com.blackrook.commons.Common;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.list.List;

/**
 * Utility library for common or useful functions.
 * @author Matthew Tropiano
 */
public final class BRUtil
{
	/** MIME Type Map. */
	private static BRMIMETypes MIME_TYPE_MAP = new BRMIMETypes();
	/** Singleton context for beans not attached to the application context. */
	private static final HashMap<String, Object> SINGLETON_MAP = new HashMap<String, Object>();

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
	 * Escapes a string so that it can be input safely into a URL string.
	 */
	public static String urlEscape(String inString)
	{
		StringBuffer sb = new StringBuffer();
		char[] inChars = inString.toCharArray();
		int i = 0;
		while (i < inChars.length)
		{
			char c = inChars[i];
			if (c > 255)
				sb.append(String.format("%%%02x%%%02x", ((short)c) >>> 8, ((short)c) & 0x0ff)); // big endian
			else if (!((c >= 0x30 && c <= 0x39) || (c >= 0x41 && c <= 0x5a) || (c >= 0x61 && c <= 0x7a)))
				sb.append(String.format("%%%02x", (short)c));
			else
				sb.append(c);
			i++;
			}
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
	 * Sets the fields on a bean using values in a map.
	 * The keys in the map correspond to the "setter" methods of the object,
	 * without the "set" part. Corresponding methods that are not found will be skipped.
	 * <p>
	 * For example, the object at key "color" will be set using "setColor()" 
	 * (note the change in camel case).
	 * <p>
	 * @param bean the target bean.
	 * @param map the map to lift the values from.
	 * @throws ClassCastException if one of the fields could not be cast to the proper type.
	 */
	public static void setBeanFields(Object bean, HashMap<String, Object> map)
	{
		Iterator<String> it = map.keyIterator();
		while (it.hasNext())
		{
			String s = it.next();
			Object obj = map.get(s);
			
			try {
				Method setterMethod = obj.getClass().getMethod(getSetterName(s), obj.getClass());
				setterMethod.invoke(bean, obj);
			} catch (NoSuchMethodException ex) {
				// Do nothing. Skip.
			} catch (InvocationTargetException ex) {
				// Do nothing. Skip.
			} catch (IllegalArgumentException e) {
				// Do nothing. Skip.
			} catch (IllegalAccessException e) {
				// Do nothing. Skip.
				}
			}
		}
	
	// Gets the setter name for a field.
	private static String getSetterName(String fieldName)
	{
		return "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1); 
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
		if (obj == null)
		{
			try {
				obj = create ? clazz.newInstance() : null;
				session.setAttribute(name, obj);
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
	 * @param request the source request object.
	 * @param clazz the class type of the object that should be returned.
	 * @return a typecast object on the application scope.
	 * @throws BRFrameworkException if the object cannot be instantiated for any reason.
	 */
	public static <T> T getApplicationBean(ServletContext context, Class<T> clazz)
	{
		return getApplicationBean(context, clazz, "$$"+clazz.getPackage().getName()+"."+clazz.getName(), true);
		}

	/**
	 * Gets and auto-casts an object bean stored at the application level.
	 * The bean is created and stored if it doesn't exist.
	 * @param request the source request object.
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
	 * @param request the source request object.
	 * @param clazz the class type of the object that should be returned.
	 * @param name the attribute name.
	 * @param create if true, instantiate this class in the session (via {@link Class#newInstance()}) if it doesn't exist.
	 * @return a typecast object on the application scope, or null if it doesn't exist and wasn't created.
	 * @throws BRFrameworkException if the object cannot be instantiated for any reason.
	 */
	public static <T> T getApplicationBean(ServletContext context, Class<T> clazz, String name, boolean create)
	{
		Object obj = context.getAttribute(name);
		if (obj == null)
		{
			try {
				obj = create ? clazz.newInstance() : null;
				context.setAttribute(name, obj);
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
	 * @param request the source request object.
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
	 * @param request the source request object.
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
	 * @param request the source request object.
	 * @param clazz the class type of the object that should be returned.
	 * @param name the attribute name.
	 * @param create if true, instantiate this class in the session (via {@link Class#newInstance()}) if it doesn't exist.
	 * @return a typecast object on the application scope, or null if it doesn't exist and wasn't created.
	 * @throws BRFrameworkException if the object cannot be instantiated for any reason.
	 */
	public static <T> T getBean(Class<T> clazz, String name, boolean create)
	{
		Object obj = null;
		synchronized (SINGLETON_MAP) 
		{
			obj = SINGLETON_MAP.get(name);
			if (obj == null)
			{
				try {
					obj = create ? clazz.newInstance() : null;
					SINGLETON_MAP.put(name, obj);
				} catch (Exception e) {
					throwException(e);
					}
				}
			}
	
		if (obj == null)
			return null;
		return clazz.cast(obj);
		}

	/**
	 * Parses the content of a multiform request such that files and parameters are separated.
	 * @param request the HTTP request object.
	 * @param fileOutput the output vector to place the files that it finds (form content identified as files).
	 * @param paramOutput the output map to place key/value pairs that it identifies as "not files".
	 */
	@SuppressWarnings("unchecked")
	public static void parseMultiForm(HttpServletRequest request, 
		AbstractVector<FileItem> fileOutput, HashMap<String, String> paramOutput) throws FileUploadException
	{
		FileItemFactory factory = new DiskFileItemFactory();
		ServletFileUpload upload = new ServletFileUpload(factory);
		for (FileItem fit : (List<FileItem>)upload.parseRequest(request))
		{
			if (!fit.isFormField())
				fileOutput.add(fit);
			else
				paramOutput.put(fit.getFieldName(), fit.getString());
			}
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
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and returns true if it exists, false otherwise.
	 */
	public static boolean getParameterExist(HttpServletRequest request, String paramName)
	{
		return request.getParameter(paramName) != null;
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and returns true or false.
	 * This flavor of <code>getParameterBoolean</code> assumes that the parameter
	 * received is a string that is either "true" or not "true".
	 * @see {@link Common#parseBoolean(String)}
	 */
	public static boolean getParameterBoolean(HttpServletRequest request, String paramName)
	{
		return Common.parseBoolean(request.getParameter(paramName));
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
		String out = request.getParameter(paramName);
		return out != null ? out : "";
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a byte and returns 0 if it doesn't exist.
	 */
	public static byte getParameterByte(HttpServletRequest request, String paramName)
	{
		return Common.parseByte(request.getParameter(paramName));
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a short and returns 0 if it doesn't exist.
	 */
	public static short getParameterShort(HttpServletRequest request, String paramName)
	{
		return Common.parseShort(request.getParameter(paramName));
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a char and returns '\0' if it doesn't exist.
	 */
	public static char getParameterChar(HttpServletRequest request, String paramName)
	{
		return Common.parseChar(request.getParameter(paramName));
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses an integer and returns 0 if it doesn't exist.
	 */
	public static int getParameterInt(HttpServletRequest request, String paramName)
	{
		return Common.parseChar(request.getParameter(paramName));
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a long integer and returns 0L if it doesn't exist.
	 */
	public static long getParameterLong(HttpServletRequest request, String paramName)
	{
		return Common.parseChar(request.getParameter(paramName));
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a float and returns 0.0f if it doesn't exist.
	 */
	public static float getParameterFloat(HttpServletRequest request, String paramName)
	{
		return Common.parseFloat(request.getParameter(paramName));
		}

	/**
	 * Convenience method that calls <code>request.getParameter(paramName)</code> 
	 * and parses a double and returns 0.0 if it doesn't exist.
	 */
	public static double getParameterDouble(HttpServletRequest request, String paramName)
	{
		return Common.parseDouble(request.getParameter(paramName));
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
	 * and parses a byte and returns <code>def</code> if it doesn't exist.
	 */
	public static byte getParameterByte(HttpServletRequest request, String paramName, byte def)
	{
		return Common.parseByte(request.getParameter(paramName), def);
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
	 * and parses a char and returns <code>def</code> if it doesn't exist.
	 */
	public static char getParameterChar(HttpServletRequest request, String paramName, char def)
	{
		return Common.parseChar(request.getParameter(paramName), def);
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
	 * and parses a long integer and returns <code>def</code> if it doesn't exist.
	 */
	public static long getParameterLong(HttpServletRequest request, String paramName, long def)
	{
		return Common.parseLong(request.getParameter(paramName), def);
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
	 * and parses a double and returns <code>def</code> if it doesn't exist.
	 */
	public static double getParameterDouble(HttpServletRequest request, String paramName, double def)
	{
		return Common.parseDouble(request.getParameter(paramName), def);
		}

	/**
	 * Forces an exception to propagate up to the dispatcher.
	 * Basically encloses the provided throwable in a {@link BRFrameworkException},
	 * which is a {@link RuntimeException}.
	 * @param t the {@link Throwable} to encapsulate and throw.
	 */
	public static final void throwException(Throwable t)
	{
		throw new BRFrameworkException(t);
		}

}
