/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
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
import javax.websocket.server.ServerContainer;

import com.blackrook.small.dispatch.controller.ControllerEntryPoint;
import com.blackrook.small.dispatch.filter.FilterComponent;
import com.blackrook.small.enums.RequestMethod;
import com.blackrook.small.exception.SmallFrameworkSetupException;
import com.blackrook.small.parser.JSONDriver;
import com.blackrook.small.parser.MultipartParser;
import com.blackrook.small.parser.multipart.MultipartFormDataParser;
import com.blackrook.small.parser.multipart.MultipartParserException;
import com.blackrook.small.parser.multipart.Part;
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
public final class SmallDispatcher extends HttpServlet
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
	
	private SmallEnvironment applicationEnvironment;
	
	public SmallDispatcher()
	{
		this.applicationEnvironment = null;
	}
	
	@Override
	public void init() throws ServletException
	{
		super.init();
		ServletContext servletContext = getServletContext();
		ServerContainer serverContainer = (ServerContainer)servletContext.getAttribute("javax.websocket.server.ServerContainer");
		
		String packages = getInitParameter(SmallConstants.INIT_PARAM_APPLICATION_PACKAGE_ROOTS);
		if (Utils.isEmpty(packages))
			throw new SmallFrameworkSetupException("The root package init parameter was not specified.");

		String[] controllerRootPackages = packages.split("\\,\\s*");

		JSONDriver jsonDriver = null;
		String jsonDriverClassName = getInitParameter(SmallConstants.INIT_PARAM_APPLICATION_JSON_DRIVER);
		if (!Utils.isEmpty(jsonDriverClassName)) try {
			Class<?> jsonDriverClass = Class.forName(jsonDriverClassName, true, ClassLoader.getSystemClassLoader());
			jsonDriver = (JSONDriver)Utils.create(jsonDriverClass);  
		} catch (ClassCastException e) {
			throw new SmallFrameworkSetupException("The provided class, "+jsonDriverClassName+" is not an implementation of JSONDriver.");
		} catch (ClassNotFoundException e) {
			throw new SmallFrameworkSetupException("The provided class, "+jsonDriverClassName+" cannot be found.");
		}

		File tempDir = (File)servletContext.getAttribute("javax.servlet.context.tempdir");
		if (tempDir == null)
			tempDir = new File(System.getProperty("java.io.tmpdir"));
	
		if (!tempDir.exists() && !Utils.createPath(tempDir.getPath()))
			throw new SmallFrameworkSetupException("The temp directory for uploaded files could not be created/found.");
	
		if ((applicationEnvironment = SmallUtil.getEnvironment(servletContext)) == null)
		{
			applicationEnvironment = new SmallEnvironment();
			applicationEnvironment.init(serverContainer, controllerRootPackages, jsonDriver, tempDir);			
			servletContext.setAttribute(SmallConstants.SMALL_APPLICATION_ENVIRONMENT_ARTTRIBUTE, applicationEnvironment);
		}
	}

	@Override
	public void destroy()
	{
		super.destroy();
		applicationEnvironment.destroy();
	}

	@Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String method = request.getMethod();
        if (method.equals(METHOD_GET))
    		callControllerEntry(request, response, RequestMethod.GET, null);
        else if (method.equals(METHOD_POST))
            doPost(request, response);
        else if (method.equals(METHOD_PUT))
    		callControllerEntry(request, response, RequestMethod.PUT, null);
        else if (method.equals(METHOD_DELETE))
    		callControllerEntry(request, response, RequestMethod.DELETE, null);
        else if (method.equals(METHOD_PATCH))
			callControllerEntry(request, response, RequestMethod.PATCH, null);
        else if (method.equals(METHOD_HEAD))
    		callControllerEntry(request, response, RequestMethod.HEAD, null);
        else if (method.equals(METHOD_OPTIONS))
    		callControllerEntry(request, response, RequestMethod.OPTIONS, null);
        else if (method.equals(METHOD_TRACE))
    		doTrace(request, response);
        else
			SmallResponseUtil.sendError(response, 501, "The server cannot process this request method.");
    }
    
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
	{
		if (MultipartFormDataParser.isMultipart(request))
		{
			MultipartParser parser = SmallRequestUtil.getMultipartParser(request);
			if (parser == null)
				SmallResponseUtil.sendError(response, 400, "The multipart POST request type is not supported.");
			else
			{
				try {
					parser.parse(request, applicationEnvironment.getTemporaryDirectory());
				} catch (UnsupportedEncodingException e) {
					SmallResponseUtil.sendError(response, 400, "The encoding type for the POST request is not supported.");
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
		{
			if (request.getHeader("X-HTTP-Method-Override").equalsIgnoreCase("PATCH"))
				callControllerEntry(request, response, RequestMethod.PATCH, null);
			else
				callControllerEntry(request, response, RequestMethod.POST, null);
		}
	}
	
	/**
	 * Fetches a regular controller entry and invokes the correct method.
	 */
	private void callControllerEntry(HttpServletRequest request, HttpServletResponse response, RequestMethod requestMethod, HashDequeMap<String, Part> multiformPartMap)
	{
		String path = SmallUtil.trimSlashes(SmallRequestUtil.getPath(request));
		Result<ControllerEntryPoint> result = applicationEnvironment.getControllerEntryPoint(requestMethod, path);
		
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
			FilterComponent filterProfile = applicationEnvironment.getFilter(filterClass);
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
