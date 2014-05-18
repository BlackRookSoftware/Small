package com.blackrook.j2ee;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.xml.sax.SAXException;

import com.blackrook.commons.AbstractChainedHashMap;
import com.blackrook.commons.Common;
import com.blackrook.commons.Reflect;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.hash.HashedQueueMap;
import com.blackrook.commons.linkedlist.Queue;
import com.blackrook.commons.list.List;
import com.blackrook.j2ee.MethodDescriptor.ParameterDescriptor;
import com.blackrook.j2ee.component.ViewResolver;
import com.blackrook.j2ee.enums.RequestMethod;
import com.blackrook.j2ee.enums.ScopeType;
import com.blackrook.j2ee.exception.SimpleFrameworkException;
import com.blackrook.j2ee.lang.RFCParser;
import com.blackrook.j2ee.multipart.MultipartParser;
import com.blackrook.j2ee.multipart.MultipartParserException;
import com.blackrook.j2ee.multipart.Part;
import com.blackrook.lang.json.JSONConversionException;
import com.blackrook.lang.json.JSONObject;
import com.blackrook.lang.json.JSONReader;
import com.blackrook.lang.json.JSONWriter;
import com.blackrook.lang.xml.XMLStruct;
import com.blackrook.lang.xml.XMLStructFactory;
import com.blackrook.lang.xml.XMLWriter;

/**
 * The main dispatcher servlet for the controller portion of the framework.
 * @author Matthew Tropiano
 */
public final class DispatcherServlet extends HttpServlet
{
	private static final long serialVersionUID = -5986230302849170240L;
	
	private static final String PREFIX_REDIRECT = "redirect:";
	
	/** The controllers that were instantiated. */
	private HashMap<String, ControllerDescriptor> controllerCache;
	/** The filters that were instantiated. */
	private HashMap<Class<?>, FilterDescriptor> filterCache;
	/** Map of view resolvers to instantiated view resolvers. */
	private HashMap<Class<? extends ViewResolver>, ViewResolver> viewResolverMap;

	// Default constructor.
	public DispatcherServlet()
	{
		controllerCache = new HashMap<String, ControllerDescriptor>();
		filterCache = new HashMap<Class<?>, FilterDescriptor>();
		viewResolverMap = new HashMap<Class<? extends ViewResolver>, ViewResolver>();
	}
	
	@Override
	public final void doGet(HttpServletRequest request, HttpServletResponse response)
	{
		callControllerEntry(request, response, RequestMethod.GET, null);
	}

	@Override
	public final void doHead(HttpServletRequest request, HttpServletResponse response)
	{
		callControllerEntry(request, response, RequestMethod.HEAD, null);
	}

	@Override
	public final void doPut(HttpServletRequest request, HttpServletResponse response)
	{
		callControllerEntry(request, response, RequestMethod.PUT, null);
	}

	@Override
	public final void doDelete(HttpServletRequest request, HttpServletResponse response)
	{
		callControllerEntry(request, response, RequestMethod.DELETE, null);
	}

	@Override
	public final void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		callControllerEntry(request, response, RequestMethod.OPTIONS, null);
	}

	@Override
	public final void doPost(HttpServletRequest request, HttpServletResponse response)
	{
		if (isFormEncoded(request))
			callControllerEntry(request, response, RequestMethod.POST, null);
		else if (MultipartParser.isMultipart(request))
		{
			MultipartParser parser = null;
			try {
				parser = new MultipartParser(request, new File(System.getProperty("java.io.tmpdir")));
			} catch (UnsupportedEncodingException e) {
				sendError(response, 400, "The encoding type for the POST request is not supported.");
			} catch (MultipartParserException e) {
				sendError(response, 500, "The server could not parse the multiform request.");
			}
			
			List<Part> parts = parser.getPartList();
			
			HashedQueueMap<String, Part> partMap = new HashedQueueMap<String, Part>();
			for (Part part : parts)
				partMap.enqueue(part.getName(), part);
			
			try {
				callControllerEntry(request, response, RequestMethod.POST, partMap);
			} finally {
				// clean up files.
				for (Part part : parts)
					if (part.isFile())
					{
						File tempFile = part.getFile();
						tempFile.delete();
					}
			}
		}
		else
		{
			callControllerEntry(request, response, RequestMethod.POST, null);
		}
	}
	
	/**
	 * Fetches a regular controller entry and invokes the correct method.
	 */
	private void callControllerEntry(HttpServletRequest request, HttpServletResponse response, RequestMethod requestMethod, HashedQueueMap<String, Part> multiformPartMap)
	{
		String path = getPath(request);
		ControllerDescriptor entry = getControllerUsingPath(path);
		if (entry == null)
			sendError(response, 404, "The controller at path \""+path+"\" could not be resolved.");
		else
		{
			String page = getPage(request);

			// get cookies from request.
			HashMap<String, Cookie> cookieMap = new HashMap<String, Cookie>();
			Cookie[] cookies = request.getCookies();
			if (cookies != null) for (Cookie c : cookies)
				cookieMap.put(c.getName(), c);
			
			ControllerMethodDescriptor cmd = entry.getDescriptorUsingPath(requestMethod, page);
			if (cmd != null)
			{
				for (Class<?> filterClass : entry.getFilterChain())
				{
					FilterDescriptor fd = getFilter(filterClass);
					if (!handleFilterMethod(fd, requestMethod, request, response, fd.getMethodDescriptor(), fd.getInstance(), cookieMap, multiformPartMap))
						return;
				}

				for (Class<?> filterClass : cmd.getFilterChain())
				{
					FilterDescriptor fd = getFilter(filterClass);
					if (!handleFilterMethod(fd, requestMethod, request, response, fd.getMethodDescriptor(), fd.getInstance(), cookieMap, multiformPartMap))
						return;
				}

				handleControllerMethod(entry, requestMethod, request, response, cmd, entry.getInstance(), cookieMap, multiformPartMap);
			}
			else
				FrameworkUtil.sendCode(response, 405, "Entry point does not support this method.");
		}
	}

	
	/**
	 * Completes a full filter call.
	 */
	private boolean handleFilterMethod(
		FilterDescriptor entry, 
		RequestMethod requestMethod, HttpServletRequest request, HttpServletResponse response, 
		MethodDescriptor descriptor, Object instance, 
		HashMap<String, Cookie> cookieMap, HashedQueueMap<String, Part> multiformPartMap
	)
	{
		Object retval = null;
		try {
			retval = invokeEntryMethod(entry, requestMethod, request, response, descriptor, instance, cookieMap, multiformPartMap);
		} catch (Exception e) {
			throw new SimpleFrameworkException("An exception occurred in a Filter method.", e);
		}
		
		return (Boolean)retval;
	}
	
	/**
	 * Completes a full controller request call.
	 */
	private void handleControllerMethod(
		ControllerDescriptor entry, 
		RequestMethod requestMethod, HttpServletRequest request, HttpServletResponse response, 
		ControllerMethodDescriptor descriptor, Object instance, 
		HashMap<String, Cookie> cookieMap, HashedQueueMap<String, Part> multiformPartMap
	)
	{
		Object retval = null;
		try {
			retval = invokeEntryMethod(entry, requestMethod, request, response, descriptor, instance, cookieMap, multiformPartMap);
		} catch (Exception e) {
			throw new SimpleFrameworkException("An exception occurred in a Controller method.", e);
		}
		
		if (descriptor.isNoCache())
		{
			response.setHeader("Cache-Control", "no-cache");
			response.setHeader("Pragma", "no-cache");
		}
		
		if (descriptor.getOutputType() != null) switch (descriptor.getOutputType())
		{
			case VIEW:
			{
				String viewKey = String.valueOf(retval);
				if (viewKey.startsWith(PREFIX_REDIRECT))
					FrameworkUtil.sendRedirect(response, viewKey.substring(PREFIX_REDIRECT.length()));
				else
					FrameworkUtil.sendToView(request, response, getViewResolver(entry.getViewResolverClass()).resolveView(viewKey));
				break;
			}
			case CONTENT:
			{
				if (File.class.isAssignableFrom(descriptor.getType()))
				{
					File outfile = (File)retval;
					if (outfile == null || !outfile.exists())
						FrameworkUtil.sendCode(response, 404, "File not found.");
					else
						FrameworkUtil.sendFileContents(response, outfile);
				}
				else if (XMLStruct.class.isAssignableFrom(descriptor.getType()))
					FrameworkUtil.sendXML(response, (XMLStruct)retval);
				else if (JSONObject.class.isAssignableFrom(descriptor.getType()))
					FrameworkUtil.sendJSON(response, (JSONObject)retval);
				else if (String.class.isAssignableFrom(descriptor.getType()))
				{
					byte[] data =  getStringData(((String)retval));
					FrameworkUtil.sendData(response, "text/plain; charset=utf-8", null, new ByteArrayInputStream(data), data.length);
				}
				else if (byte[].class.isAssignableFrom(descriptor.getType()))
				{
					byte[] data = (byte[])retval;
					FrameworkUtil.sendData(response, "application/octet-stream", null, new ByteArrayInputStream(data), data.length);
				}
				else
					FrameworkUtil.sendJSON(response, retval);
				break;
			}
			case ATTACHMENT:
			{
				String fname = getPage(request);
				
				// File output.
				if (File.class.isAssignableFrom(descriptor.getType()))
				{
					File outfile = (File)retval;
					if (outfile == null || !outfile.exists())
						FrameworkUtil.sendCode(response, 404, "File not found.");
					else
						FrameworkUtil.sendFile(response, outfile);
				}
				// XML output.
				else if (XMLStruct.class.isAssignableFrom(descriptor.getType()))
				{
					byte[] data;
					try {
						StringWriter sw = new StringWriter();
						(new XMLWriter()).writeXML((XMLStruct)retval, sw);
						data = getStringData(sw.toString());
					} catch (IOException e) {
						throw new SimpleFrameworkException(e);
					}
					FrameworkUtil.sendData(response, "application/xml", fname, new ByteArrayInputStream(data), data.length);
				}
				// String data output.
				else if (String.class.isAssignableFrom(descriptor.getType()))
				{
					byte[] data = getStringData(((String)retval));
					FrameworkUtil.sendData(response, "text/plain; charset=utf-8", fname, new ByteArrayInputStream(data), data.length);
				}
				// JSON output.
				else if (JSONObject.class.isAssignableFrom(descriptor.getType()))
				{
					byte[] data;
					try {
						data = getStringData(JSONWriter.writeJSONString((JSONObject)retval));
					} catch (IOException e) {
						throw new SimpleFrameworkException(e);
					}
					FrameworkUtil.sendData(response, "application/json", fname, new ByteArrayInputStream(data), data.length);
				}
				// binary output.
				else if (byte[].class.isAssignableFrom(descriptor.getType()))
				{
					byte[] data = (byte[])retval;
					FrameworkUtil.sendData(response, "application/octet-stream", fname, new ByteArrayInputStream(data), data.length);
				}
				// Object JSON output.
				else
				{
					byte[] data;
					try {
						data = getStringData(JSONWriter.writeJSONString(retval));
					} catch (IOException e) {
						throw new SimpleFrameworkException(e);
					}
					FrameworkUtil.sendData(response, "application/json", fname, new ByteArrayInputStream(data), data.length);
				}
				break;
			}
			default:
				// Do nothing.
				break;
		}
	}

	/**
	 * Calls a method on a filter or controller.
	 * Returns its return value.
	 */
	private Object invokeEntryMethod(
		ComponentDescriptor entry, 
		RequestMethod requestMethod, HttpServletRequest request,
		HttpServletResponse response, MethodDescriptor descriptor, 
		Object instance, HashMap<String, Cookie> cookieMap, HashedQueueMap<String, Part> multiformPartMap
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
					path = path != null ? path : getFullPath(request);
					invokeParams[i] = Reflect.createForType("Parameter " + i, path, pinfo.getType());
					break;
				case PATH_FILE:
					pathFile = pathFile != null ? pathFile : getPage(request);
					invokeParams[i] = Reflect.createForType("Parameter " + i, pathFile, pinfo.getType());
					break;
				case PATH_QUERY:
					invokeParams[i] = Reflect.createForType("Parameter " + i, request.getQueryString(), pinfo.getType());
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
				case HEADER_VALUE:
				{
					String headerName = pinfo.getName();
					invokeParams[i] = Reflect.createForType("Parameter " + i, request.getHeader(headerName), pinfo.getType());
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
						for (Map.Entry<String, String[]> paramEntry : request.getParameterMap().entrySet())
						{
							String[] vals = paramEntry.getValue();
							map.put(paramEntry.getKey(), vals.length == 1 ? vals[0] : Arrays.copyOf(vals, vals.length));
						}
						invokeParams[i] = map;
					}
					else if (AbstractChainedHashMap.class.isAssignableFrom(pinfo.getType()))
					{
						AbstractChainedHashMap<String, Object> map = new HashMap<String, Object>();
						for (Map.Entry<String, String[]> paramEntry : request.getParameterMap().entrySet())
						{
							String[] vals = paramEntry.getValue();
							map.put(paramEntry.getKey(), vals.length == 1 ? vals[0] : Arrays.copyOf(vals, vals.length));
						}
						invokeParams[i] = map;
					}
					else
					{
						throw new SimpleFrameworkException("Parameter " + i + " is not a type that can store a key-value structure.");
					}
					break;
				}
				case PARAMETER:
				{
					String parameterName = pinfo.getName();
					Object value = null;
					if (multiformPartMap != null)
					{
						if (Reflect.isArray(pinfo.getType()))
						{
							Queue<Part> partlist = multiformPartMap.get(parameterName);
							Object[] vout = (Object[])Array.newInstance(pinfo.getType(), partlist.size());
							int x = 0;
							for (Part p : partlist)
								vout[x++] = getPartData(p, pinfo.getType());
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
					MethodDescriptor attribDescriptor = entry.getAttributeConstructor(pinfo.getName());
					if (attribDescriptor != null)
					{
						Object attrib = invokeEntryMethod(entry, requestMethod, request, response, attribDescriptor, instance, cookieMap, multiformPartMap);
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
							invokeParams[i] = FrameworkUtil.getRequestBean(request, pinfo.getType(), pinfo.getName());
							break;
						case SESSION:
							invokeParams[i] = FrameworkUtil.getSessionBean(request, pinfo.getType(), pinfo.getName());
							break;
						case APPLICATION:
							invokeParams[i] = FrameworkUtil.getApplicationBean(request.getServletContext(), pinfo.getType(), pinfo.getName());
							break;
					}
					break;
				}
				case MODEL:
				{
					MethodDescriptor modelDescriptor = entry.getModelConstructor(pinfo.getName());
					if (modelDescriptor != null)
					{
						Object model = invokeEntryMethod(entry, requestMethod, request, response, modelDescriptor, instance, cookieMap, multiformPartMap);
						FrameworkUtil.setModelFields(request, model);
						request.setAttribute(pinfo.getName(), invokeParams[i] = model);
					}
					else
						request.setAttribute(pinfo.getName(), invokeParams[i] = FrameworkUtil.setModelFields(request, pinfo.getType()));
					break;
				}
				case CONTENT:
				{
					if (requestMethod == RequestMethod.POST)
					{
						try {
							content = content != null ? content : getContentData(request, pinfo.getType());
							invokeParams[i] = content;
						} catch (UnsupportedEncodingException e) {
							sendError(response, 400, "The encoding type for the POST request is not supported.");
						} catch (JSONConversionException e) {
							sendError(response, 400, "The JSON content was malformed.");
						} catch (SAXException e) {
							sendError(response, 400, "The XML content was malformed.");
						} catch (IOException e) {
							sendError(response, 500, "Server could not read request.");
							throwException(e);
						}
					}
					else
						invokeParams[i] = null;
					break;
				}
			}
		}
		
		return Reflect.invokeBlind(descriptor.getMethod(), instance, invokeParams);
	}

	/**
	 * Get the base path (no file) parsed out of the request URI.
	 */
	private String getPath(HttpServletRequest request)
	{
		String requestURI = request.getRequestURI();
		int contextPathLen = request.getContextPath().length();
		int slashIndex = requestURI.lastIndexOf('/');
		if (slashIndex >= 0)
			return requestURI.substring(contextPathLen, slashIndex);
		else
			return requestURI.substring(contextPathLen); 
	}

	/**
	 * Get the full path parsed out of the request URI.
	 */
	private String getFullPath(HttpServletRequest request)
	{
		String requestURI = request.getRequestURI();
		int contextPathLen = request.getContextPath().length();
		int qIndex = requestURI.indexOf('?');
		if (qIndex >= 0)
			return requestURI.substring(contextPathLen, qIndex);
		else
			return requestURI.substring(contextPathLen); 
	}

	/**
	 * Get the base page parsed out of the request URI.
	 */
	private String getPage(HttpServletRequest request)
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
	 * Gets the controller to call using the requested path.
	 * @param uriPath the path to resolve, no query string.
	 * @return a controller, or null if no controller by that name. 
	 * This servlet sends a 404 back if this happens.
	 * @throws SimpleFrameworkException if a huge error occurred.
	 */
	private ControllerDescriptor getControllerUsingPath(String path)
	{
		if (controllerCache.containsKey(path))
			return controllerCache.get(path);
		
		synchronized (controllerCache)
		{
			// in case a thread already completed it.
			if (controllerCache.containsKey(path))
				return controllerCache.get(path);
			
			ControllerDescriptor out = instantiateController(path);
	
			if (out == null)
				return null;
			
			// add to cache and return.
			controllerCache.put(path, out);
			return out;
		}
	}

	/**
	 * Gets the filter to call.
	 * @throws SimpleFrameworkException if a huge error occurred.
	 */
	private FilterDescriptor getFilter(Class<?> clazz)
	{
		if (filterCache.containsKey(clazz))
			return filterCache.get(clazz);
		
		synchronized (filterCache)
		{
			// in case a thread already completed it.
			if (filterCache.containsKey(clazz))
				return filterCache.get(clazz);
			
			FilterDescriptor out = instantiateFilter(clazz);
			// add to cache and return.
			filterCache.put(clazz, out);
			return out;
		}
	}

	// Instantiates a controller via root resolver.
	private ControllerDescriptor instantiateController(String path)
	{
		String className = getClassNameForController(path);
		
		if (className == null)
			return null;
		
		Class<?> controllerClass = null;
		try {
			controllerClass = Class.forName(className);
		} catch (ClassNotFoundException e) {
			return null;
		}
		
		ControllerDescriptor out = null;
		
		try {
			out = new ControllerDescriptor(controllerClass);
		} catch (Exception e) {
			throw new SimpleFrameworkException("Controller could not be instantiated: "+className, e);
		}
		
		return out;
	}

	// Instantiates a filter via root resolver.
	private FilterDescriptor instantiateFilter(Class<?> clazz)
	{
		FilterDescriptor out = null;
		out = new FilterDescriptor(clazz);
		return out;
	}

	// Gets the classname of a path.
	private String getClassNameForController(String path)
	{
		String pkg = Toolkit.INSTANCE.getControllerRootPackage() + ".";
		String cls = "";
		
		if (Common.isEmpty(path))
		{
			if (Toolkit.INSTANCE.getControllerRootIndexClass() == null)
				return null;
			cls = Toolkit.INSTANCE.getControllerRootIndexClass();
			return cls;
		}
		else
		{
			String[] dirs = path.substring(1).split("[/]+");
			if (dirs.length > 1)
			{
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < dirs.length - 1; i++)
				{
					sb.append(dirs[i]);
					sb.append('.');
				}
				pkg += sb.toString();
	
			}
	
			cls = dirs[dirs.length - 1];
			cls = pkg + Toolkit.INSTANCE.getControllerRootPrefix() + Character.toUpperCase(cls.charAt(0)) + cls.substring(1) + Toolkit.INSTANCE.getControllerRootSuffix();
			
			// if class is index folder without using root URL, do not permit use.
			if (cls.equals(Toolkit.INSTANCE.getControllerRootIndexClass()))
				return null;
	
			return cls;
		}
	}
	
	/**
	 * Returns the instance of view resolver to use for resolving views (duh).
	 */
	private ViewResolver getViewResolver(Class<? extends ViewResolver> vclass)
	{
		if (!viewResolverMap.containsKey(vclass))
		{
			synchronized (viewResolverMap)
			{
				if (viewResolverMap.containsKey(vclass))
					return viewResolverMap.get(vclass);
				else
				{
					ViewResolver resolver = Reflect.create(vclass);
					viewResolverMap.put(vclass, resolver);
					return resolver;
				}
			}
		}
		return viewResolverMap.get(vclass);
	}

	/**
	 * Get content data.
	 */
	private <T> T getContentData(HttpServletRequest request, Class<T> type) 
		throws UnsupportedEncodingException, JSONConversionException, SAXException, IOException
	{
		if (isJSON(request))
		{
			JSONObject json = readJSON(request);
			if (type == JSONObject.class)
				return type.cast(json);
			else
				return json.newObject(type);
		}
		else if (isXML(request))
		{
			XMLStruct xmlStruct = readXML(request);
			if (type == XMLStruct.class)
				return type.cast(xmlStruct);
			else
				throw new ClassCastException("Expected XMLStruct class type for XML data.");
		}
		else
		{
			String content = readPlainText(request);
			return Reflect.createForType(content, type);
		}
	}

	/**
	 * Converts a Multipart Part.
	 */
	private <T> T getPartData(Part part, Class<T> type)
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
	 * Converts a string to byte data.
	 */
	private byte[] getStringData(String data)
	{
		try {
			return data.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new SimpleFrameworkException(e);
		}
	}

	/**
	 * Reads XML data from the request and returns an XMLStruct.
	 */
	private XMLStruct readXML(HttpServletRequest request) throws SAXException, IOException
	{
		XMLStruct xml = null;
		ServletInputStream sis = request.getInputStream();
		try {
			xml = XMLStructFactory.readXML(sis);
		} catch (IOException e) {
			throw e;
		} catch (SAXException e) {
			throw e;
		} finally {
			sis.close();
		}
		
		return xml;
	}
	
	/**
	 * Checks if the request is XML-formatted.
	 * @param request the request object.
	 * @return true if so, false if not.
	 */
	private boolean isXML(HttpServletRequest request)
	{
		String type = request.getContentType();
		return type.startsWith("application/xml") 
			|| type.startsWith("application/xop+xml")
			|| type.startsWith("application/rss+xml")
			;
	}

	/**
	 * Reads JSON data from the request and returns a JSONObject.
	 */
	private JSONObject readJSON(HttpServletRequest request) throws UnsupportedEncodingException, JSONConversionException, IOException
	{
		String contentType = request.getContentType();
		RFCParser parser = new RFCParser(contentType);
		String charset = "UTF-8";
		while (parser.hasTokens())
		{
			String nextToken = parser.nextToken();
			if (nextToken.startsWith("charset="))
				charset = nextToken.substring("charset=".length()).trim();
		}

		JSONObject jsonObject = null;
		ServletInputStream sis = request.getInputStream();
		try {
			jsonObject = JSONReader.readJSON(new InputStreamReader(sis, charset));
		} catch (UnsupportedEncodingException e) {
			throw e;
		} catch (JSONConversionException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} finally {
			sis.close();
		}
		
		return jsonObject;
	}
	
	/**
	 * Checks if the request is JSON-formatted.
	 * @param request the request object.
	 * @return true if so, false if not.
	 */
	private boolean isJSON(HttpServletRequest request)
	{
		return request.getContentType().startsWith("application/json");
	}

	/**
	 * Reads plaintext content.
	 * @param request the request object.
	 * @return the read string in native encoding (Java UTF16).
	 */
	private String readPlainText(HttpServletRequest request) throws UnsupportedEncodingException, IOException
	{
		String contentType = request.getContentType();
		RFCParser parser = new RFCParser(contentType);
		String charset = "UTF-8";
		while (parser.hasTokens())
		{
			String nextToken = parser.nextToken();
			if (nextToken.startsWith("charset="))
				charset = nextToken.substring("charset=".length()).trim();
		}

		StringBuffer sb = new StringBuffer();
		ServletInputStream sis = request.getInputStream();
		try {

			int buf = 0;
			char[] c = new char[16384];
			InputStreamReader ir = new InputStreamReader(sis, charset);
			while ((buf = ir.read(c)) >= 0)
				sb.append(c, 0, buf);
				
		} catch (UnsupportedEncodingException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} finally {
			sis.close();
		}
		
		return sb.toString();
	}
	
	/**
	 * Checks if this request is a regular form POST. 
	 * @param request the request object.
	 * @return true if so, false if not.
	 */
	private boolean isFormEncoded(HttpServletRequest request)
	{
		return request.getContentType().equals("application/x-www-form-urlencoded");
	}

	/**
	 * Sends request to the error page with a status code.
	 * @param response servlet response object.
	 * @param statusCode the status code to use.
	 * @param message the status message.
	 */
	private void sendError(HttpServletResponse response, int statusCode, String message)
	{
		try{
			response.sendError(statusCode, message);
		} catch (Exception e) {
			throwException(e);
		}
	}

	/**
	 * Forces an exception to propagate up to the dispatcher.
	 * Basically encloses the provided throwable in a {@link SimpleFrameworkException},
	 * which is a {@link RuntimeException}.
	 * @param t the {@link Throwable} to encapsulate and throw.
	 */
	private void throwException(Throwable t)
	{
		throw new SimpleFrameworkException(t);
	}

	/**
	 * Initializes a filter.
	private void initializeFilter(XMLStruct struct)
	{
		String pkg = struct.getAttribute(XML_FILTERPATH_PACKAGE);
		String classString = struct.getAttribute(XML_FILTERPATH_CLASSES);
	
		if (pkg == null)
			throw new SimpleFrameworkException("Filter in declaration does not declare a package.");
		if (classString == null)
			throw new SimpleFrameworkException("Filter for package \""+pkg+"\" does not declare a class.");
		
		String[] classes = classString.split("(\\s|\\,)+");
		
		filterEntries.put(pkg, classes);
	}
	 */
	
	
}
