package com.blackrook.j2ee.small;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.xml.sax.SAXException;

import com.blackrook.commons.AbstractMap;
import com.blackrook.commons.Common;
import com.blackrook.commons.Reflect;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.hash.HashedQueueMap;
import com.blackrook.commons.linkedlist.Queue;
import com.blackrook.commons.list.List;
import com.blackrook.j2ee.small.MethodDescriptor.ParameterDescriptor;
import com.blackrook.j2ee.small.enums.RequestMethod;
import com.blackrook.j2ee.small.enums.ScopeType;
import com.blackrook.j2ee.small.exception.SmallFrameworkException;
import com.blackrook.j2ee.small.parser.MultipartParser;
import com.blackrook.j2ee.small.parser.StringParser;
import com.blackrook.j2ee.small.parser.RFCParser;
import com.blackrook.j2ee.small.parser.multipart.MultipartFormDataParser;
import com.blackrook.j2ee.small.parser.multipart.MultipartParserException;
import com.blackrook.j2ee.small.struct.Part;
import com.blackrook.j2ee.small.struct.PathTrie.Result;
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
public final class SmallDispatcher extends HttpServlet
{
	private static final long serialVersionUID = -5986230302849170240L;
	
	private static final String PREFIX_REDIRECT = "redirect:";
	
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
		else if (MultipartFormDataParser.isMultipart(request))
		{
			MultipartParser parser = getMultipartParser(request);
			if (parser == null)
				sendError(response, 400, "The multipart POST request type is not supported.");
			else
			{
				try {
					parser.parse(request, SmallToolkit.INSTANCE.getTempDir());
				} catch (UnsupportedEncodingException e) {
					sendError(response, 400, "The encoding type for the POST request is not supported.");
				} catch (MultipartParserException e) {
					sendError(response, 500, "The server could not parse the multiform request. " + e.getMessage());
				} catch (IOException e) {
					sendError(response, 500, "The server could not read the request. " + e.getMessage());
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
		}
		else
			callControllerEntry(request, response, RequestMethod.POST, null);
	}
	
	/**
	 * Fetches a regular controller entry and invokes the correct method.
	 */
	private void callControllerEntry(HttpServletRequest request, HttpServletResponse response, RequestMethod requestMethod, HashedQueueMap<String, Part> multiformPartMap)
	{
		String path = SmallUtil.removeEndingSlash(request.getRequestURI());
		Result<Class<?>> controllerClass = SmallToolkit.INSTANCE.getControllerClassByPath(path);
		if (controllerClass.getValue() == null)
		{
			sendError(response, 404, "The controller at path \""+path+"\" could not be resolved.");
			return;
		}

		ControllerDescriptor entry = SmallToolkit.INSTANCE.getController(controllerClass.getValue());
		if (entry == null)
			sendError(response, 404, "The controller at path \""+path+"\" could not be resolved.");
		else
		{
			String pageRemainder = path.substring(controllerClass.getOffset());

			// get cookies from request.
			HashMap<String, Cookie> cookieMap = new HashMap<String, Cookie>();
			Cookie[] cookies = request.getCookies();
			if (cookies != null) for (Cookie c : cookies)
				cookieMap.put(c.getName(), c);
			
			ControllerMethodDescriptor cmd = entry.getDescriptorUsingPath(requestMethod, SmallUtil.removeBeginningSlash(pageRemainder));
			if (cmd != null)
			{
				for (Class<?> filterClass : entry.getFilterChain())
				{
					FilterDescriptor fd = SmallToolkit.INSTANCE.getFilter(filterClass);
					if (!handleFilterMethod(fd, requestMethod, request, response, fd.getMethodDescriptor(), fd.getInstance(), pageRemainder, cookieMap, multiformPartMap))
						return;
				}

				for (Class<?> filterClass : cmd.getFilterChain())
				{
					FilterDescriptor fd = SmallToolkit.INSTANCE.getFilter(filterClass);
					if (!handleFilterMethod(fd, requestMethod, request, response, fd.getMethodDescriptor(), fd.getInstance(), pageRemainder, cookieMap, multiformPartMap))
						return;
				}

				handleControllerMethod(entry, requestMethod, request, response, cmd, entry.getInstance(), pageRemainder, cookieMap, multiformPartMap);
			}
			else
				SmallUtil.sendCode(response, 404, "Not found.");
		}
	}

	
	/**
	 * Completes a full filter call.
	 */
	private boolean handleFilterMethod(
		FilterDescriptor entry, 
		RequestMethod requestMethod, HttpServletRequest request, HttpServletResponse response, 
		MethodDescriptor descriptor, Object instance, String pathRemainder,
		HashMap<String, Cookie> cookieMap, HashedQueueMap<String, Part> multiformPartMap
	)
	{
		Object retval = null;
		try {
			retval = invokeEntryMethod(entry, requestMethod, request, response, descriptor, instance, pathRemainder, cookieMap, multiformPartMap);
		} catch (Exception e) {
			throw new SmallFrameworkException("An exception occurred in a Filter method.", e);
		}
		
		return (Boolean)retval;
	}
	
	/**
	 * Completes a full controller request call.
	 */
	private void handleControllerMethod(
		ControllerDescriptor entry, 
		RequestMethod requestMethod, HttpServletRequest request, HttpServletResponse response, 
		ControllerMethodDescriptor descriptor, Object instance, String pathRemainder,
		HashMap<String, Cookie> cookieMap, HashedQueueMap<String, Part> multiformPartMap
	)
	{
		Object retval = null;
		try {
			retval = invokeEntryMethod(entry, requestMethod, request, response, descriptor, instance, pathRemainder, cookieMap, multiformPartMap);
		} catch (Exception e) {
			throw new SmallFrameworkException("An exception occurred in a Controller method.", e);
		}
		
		if (descriptor.isNoCache())
		{
			response.setHeader("Cache-Control", "no-cache");
			response.setHeader("Pragma", "no-cache");
			response.setDateHeader("Expires", 0);
		}
		
		if (descriptor.getOutputType() != null)
		{
			String fname = null;
			switch (descriptor.getOutputType())
			{
				case VIEW:
				{
					String viewKey = String.valueOf(retval);
					if (viewKey.startsWith(PREFIX_REDIRECT))
						SmallUtil.sendRedirect(response, viewKey.substring(PREFIX_REDIRECT.length()));
					else
						SmallUtil.sendToView(request, response, SmallToolkit.INSTANCE.getViewResolver(entry.getViewResolverClass()).resolveView(viewKey));
					break;
				}
				case ATTACHMENT:
					fname = getPage(request);
					// fall through.
				case CONTENT:
				{
					// File output.
					if (File.class.isAssignableFrom(descriptor.getType()))
					{
						File outfile = (File)retval;
						if (outfile == null || !outfile.exists())
							SmallUtil.sendCode(response, 404, "File not found.");
						else if (Common.isEmpty(descriptor.getMimeType()))
							SmallUtil.sendFileContents(response, descriptor.getMimeType(), outfile);
						else
							SmallUtil.sendFileContents(response, outfile);
					}
					// XML output.
					else if (XMLStruct.class.isAssignableFrom(descriptor.getType()))
					{
						byte[] data;
						try {
							StringWriter sw = new StringWriter();
							(new XMLWriter()).writeXML((XMLStruct)retval, sw);
							data = getStringData(request, sw.toString());
						} catch (IOException e) {
							throw new SmallFrameworkException(e);
						}
						SmallUtil.sendData(response, "application/xml", fname, new ByteArrayInputStream(data), data.length);
					}
					// JSON output.
					else if (JSONObject.class.isAssignableFrom(descriptor.getType()))
					{
						byte[] data;
						try {
							data = getStringData(request, JSONWriter.writeJSONString((JSONObject)retval));
						} catch (IOException e) {
							throw new SmallFrameworkException(e);
						}
						SmallUtil.sendData(response, "application/json", fname, new ByteArrayInputStream(data), data.length);
					}
					// StringBuffer data output.
					else if (StringBuffer.class.isAssignableFrom(descriptor.getType()))
					{
						sendStringData(request, response, descriptor.getMimeType(), fname, ((StringBuffer)retval).toString());
					}
					// StringBuilder data output.
					else if (StringBuilder.class.isAssignableFrom(descriptor.getType()))
					{
						sendStringData(request, response, descriptor.getMimeType(), fname, ((StringBuilder)retval).toString());
					}
					// String data output.
					else if (String.class.isAssignableFrom(descriptor.getType()))
					{
						sendStringData(request, response, descriptor.getMimeType(), fname, (String)retval);
					}
					// binary output.
					else if (byte[].class.isAssignableFrom(descriptor.getType()))
					{
						byte[] data = (byte[])retval;
						if (Common.isEmpty(descriptor.getMimeType()))
							SmallUtil.sendData(response, descriptor.getMimeType(), null, new ByteArrayInputStream(data), data.length);
						else
							SmallUtil.sendData(response, "application/octet-stream", null, new ByteArrayInputStream(data), data.length);
					}
					// Object JSON output.
					else
					{
						byte[] data;
						try {
							data = getStringData(request, JSONWriter.writeJSONString(retval));
						} catch (IOException e) {
							throw new SmallFrameworkException(e);
						}
						SmallUtil.sendData(response, "application/json", fname, new ByteArrayInputStream(data), data.length);
					}
					break;
				}
				default:
					// Do nothing.
					break;
			}
		}
	}

	/**
	 * Writes string data to the response.
	 * @param response the response object.
	 * @param descriptor the method descriptor.
	 * @param fileName the file name.
	 * @param data the string data to send.
	 */
	private void sendStringData(HttpServletRequest request, HttpServletResponse response, String mimeType, String fileName, String data)
	{
		byte[] bytedata = getStringData(request, data);
		if (Common.isEmpty(mimeType))
			SmallUtil.sendData(response, mimeType, fileName, new ByteArrayInputStream(bytedata), bytedata.length);
		else
			SmallUtil.sendData(response, "text/plain; charset=utf-8", fileName, new ByteArrayInputStream(bytedata), bytedata.length);
	}

	/**
	 * Calls a method on a filter or controller.
	 * Returns its return value.
	 */
	private Object invokeEntryMethod(
		ComponentDescriptor entry, 
		RequestMethod requestMethod, HttpServletRequest request,
		HttpServletResponse response, MethodDescriptor descriptor, 
		Object instance, String pathRemainder, HashMap<String, Cookie> cookieMap, HashedQueueMap<String, Part> multiformPartMap
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
					pathFile = pathFile != null ? pathFile : getPage(request);
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
					MethodDescriptor attribDescriptor = entry.getAttributeConstructor(pinfo.getName());
					if (attribDescriptor != null)
					{
						Object attrib = invokeEntryMethod(entry, requestMethod, request, response, attribDescriptor, instance, pathRemainder, cookieMap, multiformPartMap);
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
					MethodDescriptor modelDescriptor = entry.getModelConstructor(pinfo.getName());
					if (modelDescriptor != null)
					{
						Object model = invokeEntryMethod(entry, requestMethod, request, response, modelDescriptor, instance, pathRemainder, cookieMap, multiformPartMap);
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
			
			// check for trim-able object.
			if (pinfo.getTrim() && invokeParams[i] != null && invokeParams[i].getClass() == String.class)
				invokeParams[i] = ((String)invokeParams[i]).trim(); 
			
		}
		
		return Reflect.invokeBlind(descriptor.getMethod(), instance, invokeParams);
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
	private byte[] getStringData(HttpServletRequest request, String data)
	{
		try {
			return data.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new SmallFrameworkException(e);
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
	 * Returns the appropriate multipart parser for a request.
	 * @param request the request.
	 */
	private MultipartParser getMultipartParser(HttpServletRequest request)
	{
		String contentType = request.getContentType();
		
		if (contentType.startsWith("multipart/form-data"))
			return new MultipartFormDataParser();
		else
			return null;
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
	 * Basically encloses the provided throwable in a {@link SmallFrameworkException},
	 * which is a {@link RuntimeException}.
	 * @param t the {@link Throwable} to encapsulate and throw.
	 */
	private void throwException(Throwable t)
	{
		throw new SmallFrameworkException(t);
	}

}
