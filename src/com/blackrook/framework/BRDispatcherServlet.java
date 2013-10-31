package com.blackrook.framework;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.xml.sax.SAXException;

import com.blackrook.commons.Reflect;
import com.blackrook.framework.annotation.RequestMethod;
import com.blackrook.framework.multipart.MultipartParser;
import com.blackrook.framework.multipart.MultipartParserException;
import com.blackrook.framework.multipart.Part;
import com.blackrook.framework.util.RFCParser;
import com.blackrook.lang.json.JSONConversionException;
import com.blackrook.lang.json.JSONObject;
import com.blackrook.lang.json.JSONReader;
import com.blackrook.lang.xml.XMLStruct;
import com.blackrook.lang.xml.XMLStructFactory;

/**
 * The main dispatcher servlet for the controller portion of the framework.
 * @author Matthew Tropiano
 */
public final class BRDispatcherServlet extends HttpServlet
{
	private static final long serialVersionUID = 4733160851384500294L;

	@Override
	public final void doGet(HttpServletRequest request, HttpServletResponse response)
	{
		BRToolkit.createToolkit(getServletContext());
		String path = getPath(request);
		BRControllerEntry entry = getControllerUsingPath(path);
		if (entry == null)
			sendCode(response, 404, "The controller at path \""+path+"\" could not be resolved.");
		else
		{
			String page = getPage(request);
			if (!entry.callFilters(page, request, response))
				return;
			
			Method m = entry.getMethodUsingPath(RequestMethod.GET, page);
			if (m != null)
				Reflect.invokeBlind(m, entry.getInstance(), page, request, response);
			else
				entry.getInstance().onGet(page, request, response);
			}
		}

	@Override
	public final void doPost(HttpServletRequest request, HttpServletResponse response)
	{
		BRToolkit.createToolkit(getServletContext());
		String path = getPath(request);
		BRControllerEntry entry = getControllerUsingPath(path);
		if (entry == null)
			sendCode(response, 404, "The controller at path \""+path+"\" could not be resolved.");
		else if (isJSON(request))
		{
			JSONObject json = null;
			try {
				json = readJSON(request);
			} catch (UnsupportedEncodingException e) {
				sendCode(response, 400, "The encoding type for the POST request is not supported.");
				return;
			} catch (JSONConversionException e) {
				sendCode(response, 400, "JSON request was malformed.");
				return;
			} catch (IOException e) {
				sendCode(response, 500, "Could not read from request.");
				return;
				}

			String page = getPage(request);
			if (!entry.callFilters(page, request, response, json))
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
				sendCode(response, 400, "XML request was malformed.");
				return;
			} catch (IOException e) {
				sendCode(response, 500, "Could not read from request.");
				return;
				}

			String page = getPage(request);
			if (!entry.callFilters(page, request, response, xmlStruct))
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
				sendCode(response, 400, "The encoding type for the POST request is not supported.");
			} catch (MultipartParserException e) {
				sendCode(response, 500, "The server could not parse the multiform request.");
				}
			
			Part[] parts = new Part[parser.getPartList().size()];
			parser.getPartList().toArray(parts);
			
			try {
				String page = getPage(request);
				if (!entry.callFilters(page, request, response, parts))
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
		else
		{
			String page = getPage(request);
			if (!entry.callFilters(page, request, response))
				return;
			
			Method m = entry.getMethodUsingPath(RequestMethod.POST, page);
			if (m != null)
				Reflect.invokeBlind(m, entry.getInstance(), page, request, response);
			else
				entry.getInstance().onPost(page, request, response);
			}
		}
	
	@Override
	public final void doHead(HttpServletRequest request, HttpServletResponse response)
	{
		BRToolkit.createToolkit(getServletContext());
		String path = getPath(request);
		BRControllerEntry entry = getControllerUsingPath(path);
		if (entry == null)
			sendCode(response, 404, "The controller at path \""+path+"\" could not be resolved.");
		else
		{
			String page = getPage(request);
			if (!entry.callFilters(page, request, response))
				return;
			
			Method m = entry.getMethodUsingPath(RequestMethod.HEAD, page);
			if (m != null)
				Reflect.invokeBlind(m, entry.getInstance(), page, request, response);
			else
				entry.getInstance().onHead(page, request, response);
			}
		}
	
	@Override
	public final void doPut(HttpServletRequest request, HttpServletResponse response)
	{
		BRToolkit.createToolkit(getServletContext());
		String path = getPath(request);
		BRControllerEntry entry = getControllerUsingPath(path);
		if (entry == null)
			sendCode(response, 404, "The controller at path \""+path+"\" could not be resolved.");
		else
		{
			String page = getPage(request);
			if (!entry.callFilters(page, request, response))
				return;
			
			Method m = entry.getMethodUsingPath(RequestMethod.PUT, page);
			if (m != null)
				Reflect.invokeBlind(m, entry.getInstance(), page, request, response);
			else
				entry.getInstance().onPut(page, request, response);
			}
		}
	
	@Override
	public final void doDelete(HttpServletRequest request, HttpServletResponse response)
	{
		BRToolkit.createToolkit(getServletContext());
		String path = getPath(request);
		BRControllerEntry entry = getControllerUsingPath(path);
		if (entry == null)
			sendCode(response, 404, "The controller at path \""+path+"\" could not be resolved.");
		else
		{
			String page = getPage(request);
			if (!entry.callFilters(page, request, response))
				return;
			
			Method m = entry.getMethodUsingPath(RequestMethod.DELETE, page);
			if (m != null)
				Reflect.invokeBlind(m, entry.getInstance(), page, request, response);
			else
				entry.getInstance().onDelete(page, request, response);
			}
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
	 * Gets the controller to call using the requested path.
	 * @param uriPath the path to resolve, no query string.
	 * @return a controller, or null if no controller by that name. 
	 * This servlet sends a 404 back if this happens.
	 * @throws BRFrameworkException if a huge error occurred.
	 */
	private final BRControllerEntry getControllerUsingPath(String uriPath)
	{
		return BRToolkit.INSTANCE.getController(uriPath);
		}

	/**
	 * Sends request to the error page with a status code.
	 * @param response servlet response object.
	 * @param statusCode the status code to use.
	 * @param message the status message.
	 */
	private final void sendCode(HttpServletResponse response, int statusCode, String message)
	{
		try{
			response.sendError(statusCode, message);
		} catch (Exception e) {
			throwException(e);
			}
		}

	/**
	 * Forces an exception to propagate up to the dispatcher.
	 * Basically encloses the provided throwable in a {@link BRFrameworkException},
	 * which is a {@link RuntimeException}.
	 * @param t the {@link Throwable} to encapsulate and throw.
	 */
	private final void throwException(Throwable t)
	{
		throw new BRFrameworkException(t);
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

}
