/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.blackrook.small.dispatch.controller.ControllerEntryPoint;
import com.blackrook.small.dispatch.filter.FilterComponent;
import com.blackrook.small.enums.RequestMethod;
import com.blackrook.small.exception.SmallFrameworkSetupException;
import com.blackrook.small.exception.request.MultipartParserException;
import com.blackrook.small.multipart.MultipartFormDataParser;
import com.blackrook.small.multipart.MultipartParser;
import com.blackrook.small.multipart.Part;
import com.blackrook.small.struct.HashDequeMap;
import com.blackrook.small.struct.Utils;
import com.blackrook.small.struct.URITrie.Result;
import com.blackrook.small.util.SmallRequestUtil;
import com.blackrook.small.util.SmallResponseUtil;
import com.blackrook.small.util.SmallUtil;

/**
 * The main dispatcher servlet for the controller portion of the framework.
 * Attaches an attribute to the application scope for the component system.
 * @author Matthew Tropiano
 */
public final class SmallServlet extends HttpServlet implements HttpSessionAttributeListener, HttpSessionListener
{
	private static final long serialVersionUID = 438331119650683748L;
	
	private static final String METHOD_DELETE = "DELETE";
    private static final String METHOD_HEAD = "HEAD";
    private static final String METHOD_GET = "GET";
    private static final String METHOD_OPTIONS = "OPTIONS";
    private static final String METHOD_POST = "POST";
    private static final String METHOD_PUT = "PUT";
    private static final String METHOD_PATCH = "PATCH";
    private static final String METHOD_TRACE = "TRACE";
    private static final String HEADER_METHOD_OVERRIDE = "X-HTTP-Method-Override";

	/** The application environment. */
	private SmallEnvironment environment;
	
	/**
	 * Creates the dispatcher servlet. 
	 */
	public SmallServlet()
	{
		this.environment = null;
	}
	
	@Override
	public void init() throws ServletException
	{
		super.init();
		ServletContext servletContext = getServletContext();
		if ((environment = SmallUtil.getEnvironment(servletContext)) == null)
		{
			environment = createEnvironment(servletContext);
			servletContext.setAttribute(SmallConstants.SMALL_APPLICATION_ENVIRONMENT_ATTRIBUTE, environment);
		}
	}
	
	@Override
	public void destroy()
	{
		super.destroy();
		environment.destroy();
	}

	@Override
	public void sessionCreated(HttpSessionEvent se)
	{
		environment.sessionCreated(se);
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent se)
	{
		environment.sessionDestroyed(se);
	}

	@Override
	public void attributeAdded(HttpSessionBindingEvent event)
	{
		environment.attributeAdded(event);
	}

	@Override
	public void attributeRemoved(HttpSessionBindingEvent event)
	{
		environment.attributeRemoved(event);
	}

	@Override
	public void attributeReplaced(HttpSessionBindingEvent event)
	{
		environment.attributeReplaced(event);
	}

	@Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String method = request.getMethod();
        if (method.equals(METHOD_GET))
    		callControllerEntry(request, response, RequestMethod.GET, null);
        else if (method.equals(METHOD_POST))
        	callPost(request, response);
        else if (method.equals(METHOD_PUT))
        	callPut(request, response);
        else if (method.equals(METHOD_DELETE))
    		callControllerEntry(request, response, RequestMethod.DELETE, null);
        else if (method.equals(METHOD_PATCH))
			callControllerEntry(request, response, RequestMethod.PATCH, null);
        else if (method.equals(METHOD_HEAD))
        	callHead(request, response);
        else if (SmallUtil.getConfiguration(getServletContext()).allowOptions() && method.equals(METHOD_OPTIONS))
        	callOptions(request, response);
        else if (SmallUtil.getConfiguration(getServletContext()).allowTrace() && method.equals(METHOD_TRACE))
    		doTrace(request, response);
        else
			SmallResponseUtil.sendError(response, 405, "Method is not allowed.");
    }

	private void callHead(HttpServletRequest request, HttpServletResponse response)
	{
		// HEAD is a GET with no body.
		callControllerEntry(request, response, RequestMethod.GET, null);
		response.setContentLength(0);
	}

	private void callOptions(HttpServletRequest request, HttpServletResponse response)
	{
		// OPTIONS sends back a header with allowed methods.
		StringBuilder sb = new StringBuilder();
		String path = SmallUtil.trimSlashes(SmallRequestUtil.getPath(request));
		SmallConfiguration config = SmallUtil.getConfiguration(getServletContext());
		
		Result<ControllerEntryPoint> result;
		
		if ((result = environment.getControllerEntryPoint(RequestMethod.GET, path)) != null && result.hasValue())
		{
			sb.append(sb.length() > 0 ? ", " : "").append("GET");
			sb.append(sb.length() > 0 ? ", " : "").append("HEAD");
		}
		if ((result = environment.getControllerEntryPoint(RequestMethod.POST, path)) != null && result.hasValue())
			sb.append(sb.length() > 0 ? ", " : "").append("POST");
		if ((result = environment.getControllerEntryPoint(RequestMethod.PUT, path)) != null && result.hasValue())
			sb.append(sb.length() > 0 ? ", " : "").append("PUT");
		if ((result = environment.getControllerEntryPoint(RequestMethod.PATCH, path)) != null && result.hasValue())
			sb.append(sb.length() > 0 ? ", " : "").append("PATCH");
		if ((result = environment.getControllerEntryPoint(RequestMethod.DELETE, path)) != null && result.hasValue())
			sb.append(sb.length() > 0 ? ", " : "").append("DELETE");
		
		if (config.allowOptions())
			sb.append(sb.length() > 0 ? ", " : "").append("OPTIONS");
		if (config.allowTrace())
			sb.append(sb.length() > 0 ? ", " : "").append("TRACE");
			
		if (sb.length() > 0)
			response.setHeader("Allow", sb.toString());
	}
    
	private SmallEnvironment createEnvironment(ServletContext servletContext)
	{
		SmallConfiguration smallConfig = (SmallConfiguration)servletContext.getAttribute(SmallConstants.SMALL_APPLICATION_CONFIGURATION_ATTRIBUTE);
		
		File tempDir = (File)servletContext.getAttribute("javax.servlet.context.tempdir");
		if (tempDir == null)
			tempDir = new File(System.getProperty("java.io.tmpdir"));
	
		if (!tempDir.exists() && !Utils.createPath(tempDir.getPath()))
			throw new SmallFrameworkSetupException("The temp directory for uploaded files could not be created/found.");
	
		SmallEnvironment env = new SmallEnvironment();
		env.init(servletContext, smallConfig != null ? smallConfig.getApplicationPackageRoots() : null, tempDir);			
		return env;
	}

	private void callPost(HttpServletRequest request, HttpServletResponse response)
	{
		if (MultipartFormDataParser.isMultipart(request))
			callMultipart(RequestMethod.POST, request, response);
		else if ("PATCH".equalsIgnoreCase(request.getHeader(HEADER_METHOD_OVERRIDE)))
			callControllerEntry(request, response, RequestMethod.PATCH, null);
		else
			callControllerEntry(request, response, RequestMethod.POST, null);
	}

	private void callPut(HttpServletRequest request, HttpServletResponse response)
	{
		if (MultipartFormDataParser.isMultipart(request))
			callMultipart(RequestMethod.PUT, request, response);
		else if ("PATCH".equalsIgnoreCase(request.getHeader(HEADER_METHOD_OVERRIDE)))
			callControllerEntry(request, response, RequestMethod.PATCH, null);
		else
			callControllerEntry(request, response, RequestMethod.PUT, null);
	}

	private void callMultipart(RequestMethod method, HttpServletRequest request, HttpServletResponse response)
	{
		MultipartParser parser = SmallRequestUtil.getMultipartParser(request);
		if (parser == null)
			SmallResponseUtil.sendError(response, 400, "The multipart request type is not supported.");
		else
		{
			try {
				parser.parse(request, environment.getTemporaryDirectory());
			} catch (UnsupportedEncodingException e) {
				SmallResponseUtil.sendError(response, 400, "The encoding type for the request is not supported.");
			} catch (MultipartParserException e) {
				SmallResponseUtil.sendError(response, 500, "The server could not parse the multiform request. " + e.getMessage());
			} catch (IOException e) {
				SmallResponseUtil.sendError(response, 500, "The server could not read the request. " + e.getMessage());
			}
			
			List<Part> parts = parser.getPartList();
			
			HashDequeMap<String, Part> partMap = new HashDequeMap<>();
			for (Part part : parts)
				partMap.addLast(part.getName(), part);
			
			try {
				callControllerEntry(request, response, method, partMap);
			} finally {
				// clean up files.
				for (Part part : parts) if (part.isFile())
				{
					File tempFile = part.getFile();
					tempFile.delete();
				}
			}
		}
	}
	
	private void callControllerEntry(HttpServletRequest request, HttpServletResponse response, RequestMethod requestMethod, HashDequeMap<String, Part> multiformPartMap)
	{
		String path = SmallRequestUtil.getPath(request);
		
		Result<ControllerEntryPoint> result = environment.getControllerEntryPoint(requestMethod, path);
		
		if (result == null || !result.hasValue())
		{
			SmallResponseUtil.sendError(response, 404, "Not found. No handler for "+requestMethod.name()+ " '"+path+"'");
			return;
		}
		
		// get cookies from request.
		Map<String, Cookie> cookieMap = new HashMap<String, Cookie>();
		Cookie[] cookies = request.getCookies();
		if (cookies != null) for (Cookie c : cookies)
			cookieMap.put(c.getName(), c);
		
		// Get path variables.
		Map<String, String> pathVariables = result.getPathVariables() != null ? result.getPathVariables() : (new HashMap<String, String>());
		
		ControllerEntryPoint entryPoint = result.getValue();
		
		for (Class<?> filterClass : entryPoint.getFilterChain())
		{
			FilterComponent filterProfile = environment.getFilter(filterClass);
			if (!filterProfile.getEntryMethod().handleCall(requestMethod, request, response, pathVariables, cookieMap, multiformPartMap))
				return;
		}
	
		// Call Entry
		entryPoint.handleCall(
			requestMethod, 
			request, 
			response, 
			pathVariables, 
			cookieMap, 
			multiformPartMap
		);
	}
	
}
