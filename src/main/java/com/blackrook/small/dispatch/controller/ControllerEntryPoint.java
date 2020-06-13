/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.dispatch.controller;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.small.annotation.controller.Attachment;
import com.blackrook.small.annotation.controller.Content;
import com.blackrook.small.annotation.controller.EntryPath;
import com.blackrook.small.annotation.controller.FilterChain;
import com.blackrook.small.annotation.controller.HTTPMethod;
import com.blackrook.small.annotation.controller.NoCache;
import com.blackrook.small.annotation.controller.View;
import com.blackrook.small.dispatch.DispatchEntryPoint;
import com.blackrook.small.dispatch.DispatchMVCEntryPoint;
import com.blackrook.small.dispatch.controller.ControllerComponent.Output;
import com.blackrook.small.enums.RequestMethod;
import com.blackrook.small.exception.SmallFrameworkSetupException;
import com.blackrook.small.exception.request.NoConverterException;
import com.blackrook.small.exception.request.NoViewHandlerException;
import com.blackrook.small.exception.request.NotFoundException;
import com.blackrook.small.multipart.Part;
import com.blackrook.small.roles.JSONDriver;
import com.blackrook.small.roles.XMLDriver;
import com.blackrook.small.struct.HashDequeMap;
import com.blackrook.small.struct.Utils;
import com.blackrook.small.util.SmallRequestUtil;
import com.blackrook.small.util.SmallResponseUtil;
import com.blackrook.small.util.SmallUtil;

/**
 * Method descriptor class, specifically for controllers.
 * @author Matthew Tropiano
 */
public class ControllerEntryPoint extends DispatchEntryPoint<ControllerComponent> implements DispatchMVCEntryPoint<Void>
{
	private static final String PREFIX_REDIRECT = "redirect:";
	private static final Class<?>[] NO_FILTERS = new Class<?>[0];
	private static final RequestMethod[] REQUEST_METHODS_GET = new RequestMethod[]{RequestMethod.GET};

	/** Full entry path. */
	private String path;
	/** Output content. */
	private Output outputType;
	/** Forced MIME type. */
	private String mimeType;
	/** No cache? */
	private boolean noCache;
	/** Filter class list. */
	private Class<?>[] filterChain;
	/** Entry request methods. */
	private RequestMethod[] requestMethods;

	/**
	 * Creates an entry method around a service profile instance.
	 * @param controllerProfile the service instance.
	 * @param method the method invoked.
	 */
	public ControllerEntryPoint(ControllerComponent controllerProfile, Method method)
	{
		super(controllerProfile, method);
		
		this.outputType = null;
		this.noCache = method.isAnnotationPresent(NoCache.class);
		this.filterChain = NO_FILTERS;

		EntryPath controllerEntry = method.getAnnotation(EntryPath.class);
		
		LinkedList<RequestMethod> requestMethodsFound = new LinkedList<>();

		if (method.isAnnotationPresent(HTTPMethod.GET.class))
			requestMethodsFound.add(RequestMethod.GET);
		if (method.isAnnotationPresent(HTTPMethod.POST.class))
			requestMethodsFound.add(RequestMethod.POST);
		if (method.isAnnotationPresent(HTTPMethod.PUT.class))
			requestMethodsFound.add(RequestMethod.PUT);
		if (method.isAnnotationPresent(HTTPMethod.PATCH.class))
			requestMethodsFound.add(RequestMethod.PATCH);
		if (method.isAnnotationPresent(HTTPMethod.DELETE.class))
			requestMethodsFound.add(RequestMethod.DELETE);
		
		if (requestMethodsFound.isEmpty())
			this.requestMethods = REQUEST_METHODS_GET;
		else
			requestMethodsFound.toArray(this.requestMethods = new RequestMethod[requestMethodsFound.size()]);
		
		this.path = SmallUtil.pathify(controllerEntry.value());

		if (method.isAnnotationPresent(FilterChain.class))
		{
			FilterChain fc = method.getAnnotation(FilterChain.class);
			if (Utils.isEmpty(fc.value()))
				this.filterChain = Utils.joinArrays(controllerProfile.getFilterChain(), fc.value());
			else
				this.filterChain = controllerProfile.getFilterChain();
		}
		else
		{
			this.filterChain = controllerProfile.getFilterChain();
		}
		
		if (method.isAnnotationPresent(EntryPath.class))
		{
			Class<?> type = getType();
			
			if (method.isAnnotationPresent(Content.class))
			{
				Content c = method.getAnnotation(Content.class);
				if (type == Void.class || type == Void.TYPE)
					throw new SmallFrameworkSetupException("Entry methods that are annotated @Content cannot return void.");
				this.outputType = Output.CONTENT;
				this.mimeType = Utils.isEmpty(c.value()) ? null : c.value();
			}
			else if (method.isAnnotationPresent(Attachment.class))
			{
				Attachment a = method.getAnnotation(Attachment.class);
				if (type == Void.class || type == Void.TYPE)
					throw new SmallFrameworkSetupException("Entry methods that are annotated @Attachment cannot return void.");
				this.outputType = Output.ATTACHMENT;
				this.mimeType = Utils.isEmpty(a.value()) ? null : a.value();
			}
			else if (method.isAnnotationPresent(View.class))
			{
				if (type == Void.class || type == Void.TYPE)
					throw new SmallFrameworkSetupException("Entry methods that are annotated @View cannot return void.");
				this.outputType = Output.VIEW;
			}
			else if (type != Void.class && type != Void.TYPE)
				throw new SmallFrameworkSetupException("Entry methods that don't return void must be annotated with @Content, @Attachment, or @View.");
		}
		
	}

	/**
	 * @return the request methods handled by this entry point. 
	 */
	public RequestMethod[] getRequestMethods() 
	{
		return requestMethods;
	}
	
	/**
	 * @return the full path of the entry method.
	 */
	public String getPath() 
	{
		return path;
	}
	
	/**
	 * @return the forced MIME type to use. If null, the dispatcher decides it.
	 */
	public String getMimeType()
	{
		return mimeType;
	}

	/**
	 * @return method's output type, if controller call.
	 */
	public Output getOutputType()
	{
		return outputType;
	}

	/**
	 * @return true, if a directive will be sent to the client to not cache the response data, false if not.  
	 */
	public boolean isNoCache()
	{
		return noCache;
	}

	/**
	 * @return this method's full filter chain (package to controller to this method).
	 */
	public Class<?>[] getFilterChain()
	{
		return filterChain;
	}

	@Override
	public Void handleCall(
		RequestMethod requestMethod, 
		HttpServletRequest request, 
		HttpServletResponse response, 
		Map<String, String> pathVariableMap, 
		Map<String, Cookie> cookieMap, 
		HashDequeMap<String, Part> partMap
	) throws ServletException, IOException 
	{
		Object retval = invoke(requestMethod, request, response, pathVariableMap, cookieMap, partMap);
		
		if (noCache)
		{
			response.setHeader("Cache-Control", "no-cache");
			response.setHeader("Pragma", "no-cache");
			response.setDateHeader("Expires", 0);
		}
		
		if (outputType != null)
		{
			String fname = null;
			switch (outputType)
			{
				case VIEW:
				{
					String viewName = String.valueOf(retval);
					if (viewName.startsWith(PREFIX_REDIRECT))
						SmallResponseUtil.sendRedirect(response, viewName.substring(PREFIX_REDIRECT.length()));
					else if (!SmallUtil.getEnvironment(request.getServletContext()).handleView(request, response, viewName))
						throw new NoViewHandlerException("No view handler for \"" + viewName + "\".");
					break;
				}
				case ATTACHMENT:
				{
					fname = SmallRequestUtil.getFileName(request);
					// fall through.
				}
				case CONTENT:
				{
					Class<?> returnType = getType();
					
					// File output.
					if (File.class.isAssignableFrom(returnType))
					{
						File outfile = (File)retval;
						if (outfile == null || !outfile.exists())
							throw new NotFoundException("File not found.");
						else if (!Utils.isEmpty(mimeType))
							SmallResponseUtil.sendFileContents(response, mimeType, outfile);
						else
							SmallResponseUtil.sendFileContents(response, SmallUtil.getMIMEType(request.getServletContext(), outfile.getName()), outfile);
					}
					// StringBuffer data output.
					else if (StringBuffer.class.isAssignableFrom(returnType))
					{
						mimeType = Utils.isEmpty(mimeType) ? "text/plain" : mimeType;
						SmallResponseUtil.sendStringData(response, mimeType, fname, ((StringBuffer)retval).toString());
					}
					// StringBuilder data output.
					else if (StringBuilder.class.isAssignableFrom(returnType))
					{
						mimeType = Utils.isEmpty(mimeType) ? "text/plain" : mimeType;
						SmallResponseUtil.sendStringData(response, mimeType, fname, ((StringBuilder)retval).toString());
					}
					// String data output.
					else if (String.class.isAssignableFrom(returnType))
					{
						mimeType = Utils.isEmpty(mimeType) ? "text/plain" : mimeType;
						SmallResponseUtil.sendStringData(response, mimeType, fname, (String)retval);
					}
					// binary output.
					else if (byte[].class.isAssignableFrom(returnType))
					{
						byte[] data = (byte[])retval;
						if (Utils.isEmpty(mimeType))
							SmallResponseUtil.sendData(response, "application/octet-stream", null, new ByteArrayInputStream(data), data.length);
						else
							SmallResponseUtil.sendData(response, mimeType, null, new ByteArrayInputStream(data), data.length);
					}
					// InputStream
					else if (InputStream.class.isAssignableFrom(returnType))
					{
						try (InputStream inStream = (InputStream)retval)
						{
							if (Utils.isEmpty(mimeType))
								SmallResponseUtil.sendData(response, "application/octet-stream", null, inStream, -1);
							else
								SmallResponseUtil.sendData(response, mimeType, null, inStream, -1);
						}
					}
					// Object output, XML.
					else if (SmallUtil.isXML(mimeType))
					{
						XMLDriver driver = SmallUtil.getEnvironment(request.getServletContext()).getXMLDriver();
						if (driver == null)
							throw new NoConverterException("XML encoding not supported.");
						if (fname != null)
							response.setHeader("Content-Disposition", "attachment; filename=\"" + fname + "\"");
						response.setContentType("application/xml; charset=utf-8");
						driver.toXML(response.getWriter(), retval);
					}
					// Object output, JSON.
					else if (SmallUtil.isJSON(mimeType) || Utils.isEmpty(mimeType))
					{
						JSONDriver driver = SmallUtil.getEnvironment(request.getServletContext()).getJSONDriver();
						if (driver == null)
							throw new NoConverterException("JSON encoding not supported.");
						if (fname != null)
							response.setHeader("Content-Disposition", "attachment; filename=\"" + fname + "\"");
						response.setContentType("application/json; charset=utf-8");
						driver.toJSON(response.getWriter(), retval);
					}
					else
					{
						throw new NoConverterException("No suitable converter found for " + retval.getClass());
					}
					break;
				}
				default:
				{
					// Do nothing.
					break;
				}
			}
		}
		return null;
	}
	
}

