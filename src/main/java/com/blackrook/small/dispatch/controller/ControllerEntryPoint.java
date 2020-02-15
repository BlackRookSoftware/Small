/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.dispatch.controller;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.small.annotation.controller.Attachment;
import com.blackrook.small.annotation.controller.Content;
import com.blackrook.small.annotation.controller.ControllerEntry;
import com.blackrook.small.annotation.controller.FilterChain;
import com.blackrook.small.annotation.controller.NoCache;
import com.blackrook.small.annotation.controller.View;
import com.blackrook.small.dispatch.DispatchEntryPoint;
import com.blackrook.small.dispatch.DispatchMVCEntryPoint;
import com.blackrook.small.dispatch.controller.ControllerComponent.Output;
import com.blackrook.small.enums.RequestMethod;
import com.blackrook.small.exception.SmallFrameworkException;
import com.blackrook.small.exception.SmallFrameworkSetupException;
import com.blackrook.small.parser.JSONDriver;
import com.blackrook.small.parser.XMLDriver;
import com.blackrook.small.parser.multipart.Part;
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

		ControllerEntry controllerEntry = method.getAnnotation(ControllerEntry.class);
		
		if (Utils.isEmpty(controllerEntry.method()))
			this.requestMethods = REQUEST_METHODS_GET;
		else
			this.requestMethods = controllerEntry.method();
		
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
		
		if (method.isAnnotationPresent(ControllerEntry.class))
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
	){
		Object retval = null;
		try {
			retval = invoke(requestMethod, request, response, pathVariableMap, cookieMap, partMap);
		} catch (Exception e) {
			throw new SmallFrameworkException("An exception occurred in a Controller method.", e);
		}
		
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
					String viewKey = String.valueOf(retval);
					if (viewKey.startsWith(PREFIX_REDIRECT))
						SmallResponseUtil.sendRedirect(response, viewKey.substring(PREFIX_REDIRECT.length()));
					else
						SmallUtil.sendToView(request, response, getServiceProfile().getViewResolver().resolveView(viewKey));
					break;
				}
				case ATTACHMENT:
					fname = SmallRequestUtil.getFileName(request);
					// fall through.
				case CONTENT:
				{
					Class<?> returnType = getType();
					
					// File output.
					if (File.class.isAssignableFrom(returnType))
					{
						File outfile = (File)retval;
						if (outfile == null || !outfile.exists())
							SmallResponseUtil.sendCode(response, 404, "File not found.");
						else if (!Utils.isEmpty(mimeType))
							SmallResponseUtil.sendFileContents(response, mimeType, outfile);
						else
							SmallResponseUtil.sendFileContents(response, outfile);
					}
					// StringBuffer data output.
					else if (StringBuffer.class.isAssignableFrom(returnType))
					{
						mimeType = Utils.isEmpty(mimeType) ? "text/plain" : mimeType;
						sendStringData(response, mimeType, fname, ((StringBuffer)retval).toString());
					}
					// StringBuilder data output.
					else if (StringBuilder.class.isAssignableFrom(returnType))
					{
						mimeType = Utils.isEmpty(mimeType) ? "text/plain" : mimeType;
						sendStringData(response, mimeType, fname, ((StringBuilder)retval).toString());
					}
					// String data output.
					else if (String.class.isAssignableFrom(returnType))
					{
						mimeType = Utils.isEmpty(mimeType) ? "text/plain" : mimeType;
						sendStringData(response, mimeType, fname, (String)retval);
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
						InputStream inStream = (InputStream)retval;
						if (Utils.isEmpty(mimeType))
							SmallResponseUtil.sendData(response, "application/octet-stream", null, inStream, -1);
						else
							SmallResponseUtil.sendData(response, mimeType, null, inStream, -1);
						Utils.close(inStream);
					}
					// Object output, XML.
					else if (SmallUtil.isXML(mimeType))
					{
						XMLDriver handler;
						if ((handler = SmallUtil.getEnvironment(request.getServletContext()).getXMLHandler(returnType)) != null)
						{
							try {
								sendStringData(response, "application/xml; charset=utf-8", fname, handler.toXMLString(retval));
							} catch (IOException e) {
								SmallResponseUtil.sendCode(response, 500, "No suitable converter found for object.");
							}
						}
					}
					// Object output, JSON.
					else if (SmallUtil.isJSON(mimeType) || Utils.isEmpty(mimeType))
					{
						JSONDriver driver;
						if ((driver = SmallUtil.getEnvironment(request.getServletContext()).getJSONDriver()) != null)
						{
							try {
								sendStringData(response, "application/json; charset=utf-8", fname, driver.toJSONString(retval));
							} catch (IOException e) {
								SmallResponseUtil.sendCode(response, 500, "No suitable converter found for object.");
							}
						}
						else
						{
							SmallResponseUtil.sendCode(response, 500, "No suitable converter found for object.");
						}
					}
					else
					{
						SmallResponseUtil.sendCode(response, 500, "No suitable converter found for " + retval.getClass());
					}
					break;
				}
				default:
					// Do nothing.
					break;
			}
		}
		return null;
	}
	
	/**
	 * Writes string data to the response.
	 * @param response the response object.
	 * @param mimeType the response MIME-Type.
	 * @param fileName the file name.
	 * @param data the string data to send.
	 */
	private void sendStringData(HttpServletResponse response, String mimeType, String fileName, String data)
	{
		byte[] bytedata = getStringData(data, "UTF-8");
		if (Utils.isEmpty(mimeType))
			SmallResponseUtil.sendData(response, mimeType, fileName, new ByteArrayInputStream(bytedata), bytedata.length);
		else
			SmallResponseUtil.sendData(response, "text/plain; charset=utf-8", fileName, new ByteArrayInputStream(bytedata), bytedata.length);
	}

	/**
	 * Converts a string to byte data.
	 */
	private byte[] getStringData(String data, String encoding)
	{
		try {
			return data.getBytes(encoding);
		} catch (UnsupportedEncodingException e) {
			throw new SmallFrameworkException(e);
		}
	}

}

