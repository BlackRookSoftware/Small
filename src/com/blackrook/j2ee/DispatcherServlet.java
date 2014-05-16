package com.blackrook.j2ee;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.xml.sax.SAXException;

import com.blackrook.commons.Reflect;
import com.blackrook.commons.hash.HashedQueueMap;
import com.blackrook.commons.linkedlist.Queue;
import com.blackrook.commons.list.List;
import com.blackrook.j2ee.MethodDescriptor.Output;
import com.blackrook.j2ee.MethodDescriptor.ParameterInfo;
import com.blackrook.j2ee.exception.SimpleFrameworkException;
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
	private static final long serialVersionUID = 4733160851384500294L;
	
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
				callControllerEntry(request, response, RequestMethod.POST, null);
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
	private final void callControllerEntry(HttpServletRequest request, HttpServletResponse response, RequestMethod requestMethod, HashedQueueMap<String, Part> multiformPartMap)
	{
		String path = getPath(request);
		ControllerEntry entry = getControllerUsingPath(path);
		if (entry == null)
			sendError(response, 404, "The controller at path \""+path+"\" could not be resolved.");
		else
		{
			String page = getPage(request);
			if (!entry.callFilters(requestMethod, page, request, response, null))
				return;
			
			MethodDescriptor md = entry.getDescriptorUsingPath(requestMethod, page);
			if (md != null)
				callControllerMethod(requestMethod, request, response, md, entry.getInstance(), null);
			else
				FrameworkUtil.sendCode(response, 405, "Entry point does not support this method.");
		}
	}

	/**
	 * Invokes a controller method.
	 */
	private final void callControllerMethod(RequestMethod requestMethod, HttpServletRequest request, 
			HttpServletResponse response, MethodDescriptor descriptor, Object instance, HashedQueueMap<String, Part> multiformPartMap)
	{
		ParameterInfo[] methodParams = descriptor.getParameters(); 
		Object[] invokeParams = new Object[methodParams.length];

		String path = null;
		String pathFile = null;
		Object content = null;
		
		for (int i = 0; i < methodParams.length; i++)
		{
			ParameterInfo pinfo = methodParams[i];
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
					// TODO: Handle attribute constructors.
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
					// TODO: Handle model constructors.
					request.setAttribute(pinfo.getName(), invokeParams[i] = Reflect.create(pinfo.getType()));
					break;
				}
				case CONTENT:
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
					break;
				}
			}
		}
		
		Object retval = Reflect.invokeBlind(descriptor.getMethod(), instance, invokeParams);
		if (descriptor.getOutputType() == Output.VIEW)
			FrameworkUtil.sendToView(request, response, String.valueOf(retval));
		else if (descriptor.getOutputType() == Output.CONTENT)
		{
			if (descriptor.getType() == File.class)
				FrameworkUtil.sendFileContents(response, (File)retval);
			else if (descriptor.getType() == XMLStruct.class)
				FrameworkUtil.sendXML(response, (XMLStruct)retval);
			else if (descriptor.getType() == JSONObject.class)
				FrameworkUtil.sendJSON(response, (JSONObject)retval);
			else if (descriptor.getType() == String.class)
			{
				byte[] data =  getStringData(((String)retval));
				FrameworkUtil.sendData(response, "text/plain", null, new ByteArrayInputStream(data), data.length);
			}
			else
				FrameworkUtil.sendJSON(response, retval);
		}
		else if (descriptor.getOutputType() == Output.CONTENT_ATTACHMENT)
		{
			pathFile = pathFile != null ? pathFile : getPage(request);
			
			// File output.
			if (descriptor.getType() == File.class)
				FrameworkUtil.sendFile(response, (File)retval);
			// XML output.
			else if (descriptor.getType() == XMLStruct.class)
			{
				byte[] data;
				try {
					StringWriter sw = new StringWriter();
					(new XMLWriter()).writeXML((XMLStruct)retval, sw);
					data = getStringData(sw.toString());
				} catch (IOException e) {
					throw new SimpleFrameworkException(e);
				}
				FrameworkUtil.sendData(response, "application/xml", getPageNoExtension(pathFile)+".xml", new ByteArrayInputStream(data), data.length);
			}
			// String data output.
			else if (descriptor.getType() == String.class)
			{
				byte[] data = getStringData(((String)retval));
				FrameworkUtil.sendData(response, "text/plain", pathFile, new ByteArrayInputStream(data), data.length);
			}
			// JSON output.
			else if (descriptor.getType() == JSONObject.class)
			{
				byte[] data;
				try {
					data = getStringData(JSONWriter.writeJSONString((JSONObject)retval));
				} catch (IOException e) {
					throw new SimpleFrameworkException(e);
				}
				FrameworkUtil.sendData(response, "application/json", getPageNoExtension(pathFile)+".json", new ByteArrayInputStream(data), data.length);
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
				FrameworkUtil.sendData(response, "application/json", getPageNoExtension(pathFile)+".json", new ByteArrayInputStream(data), data.length);
			}
		}
	}

	/**
	 * Get the base path (no file) parsed out of the request URI.
	 */
	private final String getPath(HttpServletRequest request)
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
	private final String getFullPath(HttpServletRequest request)
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
	private final String getPage(HttpServletRequest request)
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
	 * Get the base page name parsed out of the page.
	 */
	private final String getPageNoExtension(String page)
	{
		int endIndex = page.indexOf('.');
		if (endIndex >= 0)
			return page.substring(0, endIndex);
		else
			return page; 
	}
	
	/**
	 * Gets the controller to call using the requested path.
	 * @param uriPath the path to resolve, no query string.
	 * @return a controller, or null if no controller by that name. 
	 * This servlet sends a 404 back if this happens.
	 * @throws SimpleFrameworkException if a huge error occurred.
	 */
	private final ControllerEntry getControllerUsingPath(String uriPath)
	{
		return Toolkit.INSTANCE.getController(uriPath);
	}

	/**
	 * Sends request to the error page with a status code.
	 * @param response servlet response object.
	 * @param statusCode the status code to use.
	 * @param message the status message.
	 */
	private final void sendError(HttpServletResponse response, int statusCode, String message)
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
	private final void throwException(Throwable t)
	{
		throw new SimpleFrameworkException(t);
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
		if (type == Part.class)
			return type.cast(part);
		else if (type.isInstance(String.class))
			return type.cast(part.isFile() ? part.getFileName() : part.getValue());
		else if (type.isInstance(File.class))
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
	
}
