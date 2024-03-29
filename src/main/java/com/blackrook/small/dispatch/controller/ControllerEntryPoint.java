/*******************************************************************************
 * Copyright (c) 2020-2022 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.dispatch.controller;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.small.SmallModelView;
import com.blackrook.small.SmallResponse;
import com.blackrook.small.SmallResponse.GenericSmallResponse;
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
import com.blackrook.small.exception.SmallFrameworkException;
import com.blackrook.small.exception.SmallFrameworkSetupException;
import com.blackrook.small.multipart.Part;
import com.blackrook.small.struct.HashDequeMap;
import com.blackrook.small.struct.Utils;
import com.blackrook.small.util.SmallRequestUtils;
import com.blackrook.small.util.SmallUtils;

/**
 * Method descriptor class, specifically for controllers.
 * @author Matthew Tropiano
 */
public class ControllerEntryPoint extends DispatchEntryPoint<ControllerComponent> implements DispatchMVCEntryPoint<SmallResponse>
{
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
		
		this.outputType = Output.AUTO;
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
		
		this.path = SmallUtils.pathify(controllerEntry.value());

		if (method.isAnnotationPresent(FilterChain.class))
		{
			FilterChain fc = method.getAnnotation(FilterChain.class);
			if (!Utils.isEmpty(fc.value()))
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
			{
				if (!SmallResponse.class.isAssignableFrom(type))
					throw new SmallFrameworkSetupException("Entry methods that don't return void must return SmallResponse or it must be annotated with @Content, @Attachment, or @View.");
				this.outputType = Output.AUTO;
			}
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
	public SmallResponse handleCall(
		RequestMethod requestMethod, 
		HttpServletRequest request, 
		HttpServletResponse response, 
		Map<String, String> pathVariableMap, 
		Map<String, Cookie> cookieMap, 
		HashDequeMap<String, Part> partMap
	) throws InvocationTargetException, ServletException, IOException
	{
		Object retval = invoke(requestMethod, request, response, pathVariableMap, cookieMap, partMap);
		
		GenericSmallResponse smallResponse = null;
		if (outputType != null)
		{
			String fname = null;
			switch (outputType)
			{
				case VIEW:
				{
					SmallModelView modelView;
					Class<?> returnType = retval != null ? retval.getClass() : getType();
					if (SmallModelView.class.isAssignableFrom(returnType))
						modelView = (SmallModelView)retval;
					else 
						modelView = SmallModelView.create(null, String.valueOf(retval));
					
					smallResponse = SmallResponse.create(modelView);
					break;
				}
				case ATTACHMENT:
				{
					fname = SmallRequestUtils.getFileName(request);
					// fall through.
				}
				case AUTO:
				case CONTENT:
				{
					if (retval instanceof SmallResponse)
						smallResponse = SmallResponse.create((SmallResponse)retval);
					else
						smallResponse = SmallResponse.create(retval);
					
					if (fname != null)
						smallResponse.attachment(fname);
					break;
				}
				default:
				{
					throw new SmallFrameworkException("Unexpected Output Type - INTERNAL ERROR.");
				}
			}
		}
		
		if (noCache)
		{
			smallResponse.header("Cache-Control", "no-cache");
			smallResponse.header("Pragma", "no-cache");
			smallResponse.dateHeader("Expires", 0);
		}
		
		return smallResponse;
	}
	
}

