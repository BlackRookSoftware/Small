package com.blackrook.j2ee;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.xml.sax.SAXException;

import com.blackrook.commons.Reflect;
import com.blackrook.j2ee.ControllerEntry.MethodDescriptor;
import com.blackrook.j2ee.ControllerEntry.MethodDescriptor.Output;
import com.blackrook.j2ee.ControllerEntry.MethodDescriptor.ParameterInfo;
import com.blackrook.j2ee.annotation.Parameter;
import com.blackrook.j2ee.exception.SimpleFrameworkException;
import com.blackrook.j2ee.multipart.MultipartParser;
import com.blackrook.j2ee.multipart.MultipartParserException;
import com.blackrook.j2ee.multipart.Part;
import com.blackrook.lang.json.JSONConversionException;
import com.blackrook.lang.json.JSONObject;
import com.blackrook.lang.json.JSONReader;
import com.blackrook.lang.xml.XMLStruct;
import com.blackrook.lang.xml.XMLStructFactory;

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
		callControllerEntry(request, response, RequestMethod.GET);
	}

	@Override
	public final void doHead(HttpServletRequest request, HttpServletResponse response)
	{
		callControllerEntry(request, response, RequestMethod.HEAD);
	}

	@Override
	public final void doPut(HttpServletRequest request, HttpServletResponse response)
	{
		callControllerEntry(request, response, RequestMethod.PUT);
	}

	@Override
	public final void doDelete(HttpServletRequest request, HttpServletResponse response)
	{
		callControllerEntry(request, response, RequestMethod.DELETE);
	}

	@Override
	public final void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		callControllerEntry(request, response, RequestMethod.OPTIONS);
	}

	@Override
	public final void doPost(HttpServletRequest request, HttpServletResponse response)
	{
		String path = getPath(request);
		ControllerEntry entry = getControllerUsingPath(path);
		if (entry == null)
			sendError(response, 404, "The controller at path \""+path+"\" could not be resolved.");
		else if (isFormEncoded(request))
		{
			String page = getPage(request);
			if (!entry.callFilters(RequestMethod.POST, page, request, response, null))
				return;
			
			Method m = entry.getMethodUsingPath(RequestMethod.POST, page);
			if (m != null)
				Reflect.invokeBlind(m, entry.getInstance(), page, request, response);
			else
				entry.getInstance().onPost(page, request, response);
		}
		else if (isJSON(request))
		{
			JSONObject json = null;
			try {
				json = readJSON(request);
			} catch (UnsupportedEncodingException e) {
				sendError(response, 400, "The encoding type for the POST request is not supported.");
				return;
			} catch (JSONConversionException e) {
				sendError(response, 400, "JSON request was malformed.");
				return;
			} catch (IOException e) {
				sendError(response, 500, "Could not read from request.");
				return;
			}

			String page = getPage(request);
			if (!entry.callFilters(RequestMethod.POST_JSON, page, request, response, json))
				return;
			
			Method m = entry.getMethodUsingPath(RequestMethod.POST_JSON, page);
			if (m != null)
				Reflect.invokeBlind(m, entry.getInstance(), page, request, response, json);
			else
				entry.getInstance().onJSON(page, request, response, json);
		}
		else if (isXML(request))
		{
			XMLStruct xmlStruct = null;
			
			try {
				xmlStruct = readXML(request);
			} catch (SAXException e) {
				sendError(response, 400, "XML request was malformed.");
				return;
			} catch (IOException e) {
				sendError(response, 500, "Could not read from request.");
				return;
			}

			String page = getPage(request);
			if (!entry.callFilters(RequestMethod.POST_XML, page, request, response, xmlStruct))
				return;
			
			Method m = entry.getMethodUsingPath(RequestMethod.POST_XML, page);
			if (m != null)
				Reflect.invokeBlind(m, entry.getInstance(), page, request, response, xmlStruct);
			else
				entry.getInstance().onXML(page, request, response, xmlStruct);
		}
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
			
			Part[] parts = new Part[parser.getPartList().size()];
			parser.getPartList().toArray(parts);
			
			try {
				String page = getPage(request);
				if (!entry.callFilters(RequestMethod.POST_MULTIPART, page, request, response, parts))
					return;
				
				Method m = entry.getMethodUsingPath(RequestMethod.POST_MULTIPART, page);
				if (m != null)
					Reflect.invokeBlind(m, entry.getInstance(), page, request, response, parts);
				else
					entry.getInstance().onMultipart(page, request, response, parts);
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
		else if (isPlainText(request))
		{
			String content = null;
			try {
				content = readPlainText(request);
			} catch (UnsupportedEncodingException e) {
				sendError(response, 400, "The encoding type for the POST request is not supported.");
				return;
			} catch (IOException e) {
				sendError(response, 500, "Could not read from request.");
				return;
			}

			String page = getPage(request);
			if (!entry.callFilters(RequestMethod.POST_TEXT, page, request, response, content))
				return;
			
			Method m = entry.getMethodUsingPath(RequestMethod.POST_TEXT, page);
			if (m != null)
				Reflect.invokeBlind(m, entry.getInstance(), page, request, response, content);
			else
				entry.getInstance().onText(page, request, response, content);
		}
		else
		{
		}
	}
	
	/**
	 * Fetches a regular controller entry and invokes the correct method.
	 */
	private final void callControllerEntry(HttpServletRequest request, HttpServletResponse response, RequestMethod requestMethod)
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
				callControllerMethod(requestMethod, request, response, md, entry.getInstance());
			else
				FrameworkUtil.sendCode(response, 405, "Entry point does not support this method.");
		}
	}

	/**
	 * Invokes a controller method.
	 */
	private final void callControllerMethod(RequestMethod requestMethod, HttpServletRequest request, HttpServletResponse response, MethodDescriptor descriptor, Object instance)
	{
		ParameterInfo[] methodParams = descriptor.getParameters(); 
		Object[] invokeParams = new Object[methodParams.length];

		String path = null;
		String pathFile = null;
		String pathQuery = null;
		
		for (int i = 0; i < methodParams.length; i++)
		{
			ParameterInfo pinfo = methodParams[i];
			switch (pinfo.getSourceType())
			{
				case PATH:
					path = path != null ? path : getPath(request);
					invokeParams[i] = Reflect.createForType("Parameter " + i, path, pinfo.getType());
					break;
				case PATH_FILE:
					pathFile = pathFile != null ? pathFile : getPage(request);
					invokeParams[i] = Reflect.createForType("Parameter " + i, pathFile, pinfo.getType());
					break;
				case PATH_QUERY:
					pathQuery = pathQuery != null ? pathQuery : getQueryString(request);
					invokeParams[i] = Reflect.createForType("Parameter " + i, pathQuery, pinfo.getType());
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
					invokeParams[i] = Reflect.createForType("Parameter " + i, request.getHeader(parameterName), pinfo.getType());
					break;
				}
				case ATTRIBUTE:
				{
					// TODO: Finish this!
					break;
				}
				case MODEL:
				{
					// TODO: Finish this!
					break;
				}
				case CONTENT:
				{
					// TODO: Finish this!
					break;
				}
			}
		}
		
		Object retval = Reflect.invokeBlind(descriptor.getMethod(), instance, invokeParams);
		if (descriptor.getOutputType() == Output.VIEW)
			FrameworkUtil.sendToView(request, response, String.valueOf(retval));
		else if (descriptor.getOutputType() == Output.CONTENT)
		{
			FrameworkUtil.sendToView(request, response, String.valueOf(retval));
		}
	}
	
	/**
	 * Get the base path parsed out of the request URI.
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
	 * Get the query string parsed out of the request URI.
	 */
	private final String getQueryString(HttpServletRequest request)
	{
		String requestURI = request.getRequestURI();
		int qIndex = requestURI.indexOf('?');
		if (qIndex >= 0)
			return requestURI.substring(qIndex + 1);
		else
			return ""; 
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
	 * Checks if this request is straight-up plaintext or equivalent. 
	 * @param request the request object.
	 * @return true if so, false if not.
	 */
	private boolean isPlainText(HttpServletRequest request)
	{
		return request.getContentType().startsWith("text/");
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
