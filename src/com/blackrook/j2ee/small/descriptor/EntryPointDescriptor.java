package com.blackrook.j2ee.small.descriptor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.xml.sax.SAXException;

import com.blackrook.commons.AbstractMap;
import com.blackrook.commons.Common;
import com.blackrook.commons.Reflect;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.hash.HashedQueueMap;
import com.blackrook.commons.linkedlist.Queue;
import com.blackrook.j2ee.small.SmallUtil;
import com.blackrook.j2ee.small.annotation.attribs.Attribute;
import com.blackrook.j2ee.small.annotation.attribs.Model;
import com.blackrook.j2ee.small.descriptor.MethodDescriptor.ParameterDescriptor;
import com.blackrook.j2ee.small.enums.RequestMethod;
import com.blackrook.j2ee.small.enums.ScopeType;
import com.blackrook.j2ee.small.exception.SmallFrameworkException;
import com.blackrook.j2ee.small.parser.StringParser;
import com.blackrook.j2ee.small.struct.Part;
import com.blackrook.j2ee.small.util.RequestUtil;
import com.blackrook.j2ee.small.util.ResponseUtil;
import com.blackrook.lang.json.JSONConversionException;

/**
 * Interface for descriptors for component objects. 
 * @author Matthew Tropiano
 */
public abstract class EntryPointDescriptor
{

	/** Filter instance. */
	private Object instance;
	/** Model map. */
	private HashMap<String, MethodDescriptor> modelMap;
	/** Attribute map. */
	private HashMap<String, MethodDescriptor> attributeMap;

	public EntryPointDescriptor(Object instance)
	{
		this.instance = instance; 
		this.modelMap = new HashMap<String, MethodDescriptor>(3);
		this.attributeMap = new HashMap<String, MethodDescriptor>(3);
	}

	/**
	 * Scans all methods for entry points.
	 * @param clazz the instance class.
	 */
	protected void scanMethods(Class<?> clazz)
	{
		for (Method m : clazz.getMethods())
		{
			if (isModelConstructorMethod(m))
			{
				Model anno = m.getAnnotation(Model.class);
				modelMap.put(anno.value(), new ControllerMethodDescriptor(m));
			}
			else if (isAttributeConstructorMethod(m))
			{
				Attribute anno = m.getAnnotation(Attribute.class);
				attributeMap.put(anno.value(), new ControllerMethodDescriptor(m));
			}
			else
				scanUnknownMethod(m);
		}
	}
	
	/**
	 * Called by {@link #scanMethods(Class)} to handle non-model, non-attribute creation methods.
	 */
	protected abstract void scanUnknownMethod(Method method);
	
	/**
	 * Returns the instantiated component.
	 */
	public Object getInstance()
	{
		return instance;
	}

	/**
	 * Gets a method on this object that constructs an attribute. 
	 * @param attribName the attribute name.
	 */
	public MethodDescriptor getAttributeConstructor(String attribName)
	{
		return attributeMap.get(attribName);
	}

	/**
	 * Gets a method on this object that constructs a model object. 
	 * @param modelName the model attribute name.
	 */
	public MethodDescriptor getModelConstructor(String modelName)
	{
		return modelMap.get(modelName);
	}

	/**
	 * Calls a method on a filter or controller.
	 * Returns its return value.
	 */
	protected Object invokeEntryMethod(
		RequestMethod requestMethod, 
		HttpServletRequest request,
		HttpServletResponse response, 
		MethodDescriptor descriptor, 
		Object instance, 
		String pathRemainder, 
		HashMap<String, Cookie> cookieMap, 
		HashedQueueMap<String, Part> multiformPartMap
	)
	{
		
		ParameterDescriptor[] methodParams = descriptor.getParameterDescriptors(); 
		Object[] invokeParams = new Object[methodParams.length];
	
		String path = null;
		String pathFile = null;
		Object content = null;
		
		for (int i = 0; i < methodParams.length; i++)
		{
			ParameterDescriptor pinfo = methodParams[i];
			switch (pinfo.getSourceType())
			{
				case PATH:
					path = path != null ? path : request.getRequestURI().substring(1);
					invokeParams[i] = Reflect.createForType("Parameter " + i, path, pinfo.getType());
					break;
				case PATH_FILE:
					pathFile = pathFile != null ? pathFile : RequestUtil.getPage(request);
					invokeParams[i] = Reflect.createForType("Parameter " + i, pathFile, pinfo.getType());
					break;
				case PATH_QUERY:
					invokeParams[i] = Reflect.createForType("Parameter " + i, request.getQueryString(), pinfo.getType());
					break;
				case PATH_REMAINDER:
					invokeParams[i] = Reflect.createForType("Parameter " + i, pathRemainder, pinfo.getType());
					break;
				case SERVLET_REQUEST:
					invokeParams[i] = request;
					break;
				case SERVLET_RESPONSE:
					invokeParams[i] = response;
					break;
				case SERVLET_CONTEXT:
					invokeParams[i] = request.getServletContext();
					break;
				case SESSION:
					invokeParams[i] = request.getSession();
					break;
				case METHOD_TYPE:
					invokeParams[i] = requestMethod;
					break;
				case HEADER:
				{
					String headerName = pinfo.getName();
					if (StringParser.class.isAssignableFrom(pinfo.getType()))
					{
						try {
							Constructor<?> constructor = pinfo.getType().getConstructor(String.class);
							invokeParams[i] = constructor.newInstance(request.getHeader(headerName));
						} catch (IllegalArgumentException e) {
							throw new SmallFrameworkException("Error occurred in HeaderParser constructor.", e);
						} catch (InstantiationException e) {
							throw new SmallFrameworkException("Error occurred in HeaderParser constructor.", e);
						} catch (IllegalAccessException e) {
							throw new SmallFrameworkException("Error occurred in HeaderParser constructor.", e);
						} catch (InvocationTargetException e) {
							throw new SmallFrameworkException("Error occurred in HeaderParser constructor.", e);
						} catch (SecurityException e) {
							throw new SmallFrameworkException("Target class does not have a public constructor that takes a String as its sole parameter.");
						} catch (NoSuchMethodException e) {
							throw new SmallFrameworkException("Target class does not have a public constructor that takes a String as its sole parameter.");
						}
					}
					else
						invokeParams[i] = Reflect.createForType("Parameter " + i, request.getHeader(headerName), pinfo.getType());
					break;
				}
				case HEADER_MAP:
				{
					if (Map.class.isAssignableFrom(pinfo.getType()))
					{
						Map<String, String> map = new java.util.HashMap<String, String>();
						
						Enumeration<String> strenum = request.getHeaderNames();
						while (strenum.hasMoreElements())
						{
							String header = strenum.nextElement();
							map.put(header, request.getHeader(header));
						}
						
						invokeParams[i] = map;
					}
					else if (AbstractMap.class.isAssignableFrom(pinfo.getType()))
					{
						AbstractMap<String, String> map = new HashMap<String, String>();
	
						Enumeration<String> strenum = request.getHeaderNames();
						while (strenum.hasMoreElements())
						{
							String header = strenum.nextElement();
							map.put(header, request.getHeader(header));
						}
						
						invokeParams[i] = map;
					}
					else
					{
						throw new SmallFrameworkException("Parameter " + i + " is not a type that can store a key-value structure.");
					}
					break;
				}
				case COOKIE:
				{
					String cookieName = pinfo.getName();
					Cookie c = cookieMap.containsKey(cookieName) ? cookieMap.get(cookieName) : new Cookie(cookieName, "");
					response.addCookie(c);
					invokeParams[i] = c;
					break;
				}
				case PARAMETER_MAP:
				{
					if (Map.class.isAssignableFrom(pinfo.getType()))
					{
						Map<String, Object> map = new java.util.HashMap<String, Object>();
						if (multiformPartMap != null)
						{
							Iterator<String> it = multiformPartMap.keyIterator();
							while (it.hasNext())
							{
								String pname = it.next();
								Queue<Part> partlist = multiformPartMap.get(pname);
								Part[] vout = (Part[])Array.newInstance(Part.class, partlist.size());
								int x = 0;
								for (Part p : partlist)
									vout[x++] = getPartData(p, Part.class);
								if (vout.length == 1)
									map.put(pname, vout[0]);
								else
									map.put(pname, vout);
							}
						}
						else for (Map.Entry<String, String[]> paramEntry : request.getParameterMap().entrySet())
						{
							String[] vals = paramEntry.getValue();
							map.put(paramEntry.getKey(), vals.length == 1 ? vals[0] : Arrays.copyOf(vals, vals.length));
						}
						invokeParams[i] = map;
					}
					else if (AbstractMap.class.isAssignableFrom(pinfo.getType()))
					{
						AbstractMap<String, Object> map = new HashMap<String, Object>();
						if (multiformPartMap != null)
						{
							Iterator<String> it = multiformPartMap.keyIterator();
							while (it.hasNext())
							{
								String pname = it.next();
								Queue<Part> partlist = multiformPartMap.get(pname);
								Part[] vout = (Part[])Array.newInstance(Part.class, partlist.size());
								int x = 0;
								for (Part p : partlist)
									vout[x++] = getPartData(p, Part.class);
								if (vout.length == 1)
									map.put(pname, vout[0]);
								else
									map.put(pname, vout);
							}
						}
						for (Map.Entry<String, String[]> paramEntry : request.getParameterMap().entrySet())
						{
							String[] vals = paramEntry.getValue();
							map.put(paramEntry.getKey(), vals.length == 1 ? vals[0] : Arrays.copyOf(vals, vals.length));
						}
						invokeParams[i] = map;
					}
					else
					{
						throw new SmallFrameworkException("Parameter " + i + " is not a type that can store a key-value structure.");
					}
					break;
				}
				case PARAMETER:
				{
					String parameterName = pinfo.getName();
					Object value = null;
					if (multiformPartMap != null && multiformPartMap.containsKey(parameterName))
					{
						if (Reflect.isArray(pinfo.getType()))
						{
							Class<?> actualType = Reflect.getArrayType(pinfo.getType());
							Queue<Part> partlist = multiformPartMap.get(parameterName);
							Object[] vout = (Object[])Array.newInstance(actualType, partlist.size());
							int x = 0;
							for (Part p : partlist)
								vout[x++] = getPartData(p, actualType);
							value = vout;
						}
						else
						{
							value = getPartData(multiformPartMap.get(parameterName).head(), pinfo.getType());
						}
					}
					else if (Reflect.isArray(pinfo.getType()))
						value = request.getParameterValues(parameterName);
					else
						value = request.getParameter(parameterName);
						
					invokeParams[i] = Reflect.createForType("Parameter " + i, value, pinfo.getType());
					break;
				}
				case ATTRIBUTE:
				{
					MethodDescriptor attribDescriptor = getAttributeConstructor(pinfo.getName());
					if (attribDescriptor != null)
					{
						Object attrib = invokeEntryMethod(requestMethod, request, response, attribDescriptor, instance, pathRemainder, cookieMap, multiformPartMap);
						ScopeType scope = pinfo.getSourceScopeType();
						switch (scope)
						{
							case REQUEST:
								request.setAttribute(pinfo.getName(), attrib);
								break;
							case SESSION:
								request.getSession().setAttribute(pinfo.getName(), attrib);
								break;
							case APPLICATION:
								request.getServletContext().setAttribute(pinfo.getName(), attrib);
								break;
						}
					}
					
					ScopeType scope = pinfo.getSourceScopeType();
					switch (scope)
					{
						case REQUEST:
							invokeParams[i] = SmallUtil.getRequestBean(request, pinfo.getType(), pinfo.getName());
							break;
						case SESSION:
							invokeParams[i] = SmallUtil.getSessionBean(request, pinfo.getType(), pinfo.getName());
							break;
						case APPLICATION:
							invokeParams[i] = SmallUtil.getApplicationBean(request.getServletContext(), pinfo.getType(), pinfo.getName());
							break;
					}
					break;
				}
				case MODEL:
				{
					MethodDescriptor modelDescriptor = getModelConstructor(pinfo.getName());
					if (modelDescriptor != null)
					{
						Object model = invokeEntryMethod(requestMethod, request, response, modelDescriptor, instance, pathRemainder, cookieMap, multiformPartMap);
						SmallUtil.setModelFields(request, model);
						request.setAttribute(pinfo.getName(), invokeParams[i] = model);
					}
					else
						request.setAttribute(pinfo.getName(), invokeParams[i] = SmallUtil.setModelFields(request, pinfo.getType()));
					break;
				}
				case CONTENT:
				{
					if (requestMethod == RequestMethod.POST)
					{
						try {
							content = content != null ? content : RequestUtil.getContentData(request, pinfo.getType());
							invokeParams[i] = content;
						} catch (UnsupportedEncodingException e) {
							ResponseUtil.sendError(response, 400, "The encoding type for the POST request is not supported.");
						} catch (JSONConversionException e) {
							ResponseUtil.sendError(response, 400, "The JSON content was malformed.");
						} catch (SAXException e) {
							ResponseUtil.sendError(response, 400, "The XML content was malformed.");
						} catch (IOException e) {
							ResponseUtil.sendError(response, 500, "Server could not read request.");
							throw new SmallFrameworkException(e);
						}
					}
					else
						invokeParams[i] = null;
					break;
				}
			}
			
			// check for trim-able object.
			if (pinfo.getTrim() && invokeParams[i] != null && invokeParams[i].getClass() == String.class)
				invokeParams[i] = ((String)invokeParams[i]).trim(); 
			
		}
		
		return Reflect.invokeBlind(descriptor.getMethod(), instance, invokeParams);
	}

	/**
	 * Converts a Multipart Part.
	 */
	protected <T> T getPartData(Part part, Class<T> type)
	{
		if (Part.class.isAssignableFrom(type))
			return type.cast(part);
		else if (String.class.isAssignableFrom(type))
			return type.cast(part.isFile() ? part.getFileName() : part.getValue());
		else if (File.class.isAssignableFrom(type))
			return type.cast(part.isFile() ? part.getFile() : null);
		else
			return Reflect.createForType(part.isFile() ? null : part.getValue(), type);
	}

	/**
	 * Writes string data to the response.
	 * @param response the response object.
	 * @param mimeType the response MIME-Type.
	 * @param fileName the file name.
	 * @param data the string data to send.
	 */
	protected void sendStringData(HttpServletResponse response, String mimeType, String fileName, String data)
	{
		byte[] bytedata = getStringData(data);
		if (Common.isEmpty(mimeType))
			SmallUtil.sendData(response, mimeType, fileName, new ByteArrayInputStream(bytedata), bytedata.length);
		else
			SmallUtil.sendData(response, "text/plain; charset=utf-8", fileName, new ByteArrayInputStream(bytedata), bytedata.length);
	}

	/**
	 * Converts a string to byte data.
	 */
	protected byte[] getStringData(String data)
	{
		try {
			return data.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new SmallFrameworkException(e);
		}
	}

	/** Checks if a method is a Model constructor. */
	private boolean isModelConstructorMethod(Method method)
	{
		return
			(method.getModifiers() & Modifier.PUBLIC) != 0 
			&& method.getReturnType() != Void.TYPE 
			&& method.getReturnType() != Void.class
			&& method.isAnnotationPresent(Model.class)
			;
	}

	/** Checks if a method is an Attribute constructor. */
	private boolean isAttributeConstructorMethod(Method method)
	{
		return
			(method.getModifiers() & Modifier.PUBLIC) != 0 
			&& method.getReturnType() != Void.TYPE 
			&& method.getReturnType() != Void.class
			&& method.isAnnotationPresent(Attribute.class)
			;
	}
	
	

}
