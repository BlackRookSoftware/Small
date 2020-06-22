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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.blackrook.small.exception.request.BeanCreationException;
import com.blackrook.small.exception.request.MethodNotAllowedException;
import com.blackrook.small.exception.request.MultipartParserException;
import com.blackrook.small.exception.request.NoConverterException;
import com.blackrook.small.exception.request.NoViewHandlerException;
import com.blackrook.small.exception.request.NotFoundException;
import com.blackrook.small.exception.request.UnsupportedMediaTypeException;
import com.blackrook.small.exception.views.ViewProcessingException;
import com.blackrook.small.multipart.MultipartFormDataParser;
import com.blackrook.small.multipart.MultipartParser;
import com.blackrook.small.multipart.Part;
import com.blackrook.small.struct.HashDequeMap;
import com.blackrook.small.struct.URITrie;
import com.blackrook.small.struct.Utils;
import com.blackrook.small.struct.URITrie.Result;
import com.blackrook.small.util.SmallRequestUtils;
import com.blackrook.small.util.SmallResponseUtils;
import com.blackrook.small.util.SmallUtils;

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
	private static final Map<String, Cookie> EMPTY_COOKIE_MAP = new HashMap<>(2);
	private static final Map<String, String> EMPTY_PATH_VAR_MAP = new HashMap<>(2);

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
		if ((environment = SmallUtils.getEnvironment(servletContext)) == null)
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

	private SmallEnvironment createEnvironment(ServletContext servletContext)
	{
		SmallConfiguration smallConfig = (SmallConfiguration)servletContext.getAttribute(SmallConstants.SMALL_APPLICATION_CONFIGURATION_ATTRIBUTE);
		
		File tempDir = null;

		if (smallConfig.getTempPath() != null)
			tempDir = new File(smallConfig.getTempPath());
		else if ((tempDir = (File)servletContext.getAttribute("javax.servlet.context.tempdir")) == null)
			tempDir = new File(System.getProperty("java.io.tmpdir"));
	
		if (!tempDir.exists() && !Utils.createPath(tempDir.getPath()))
			throw new SmallFrameworkSetupException("The temp directory for uploaded files could not be created/found.");
	
		SmallEnvironment env = new SmallEnvironment();
		env.init(servletContext, smallConfig != null ? smallConfig.getApplicationPackageRoots() : null, tempDir);			
		return env;
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
    protected void service(HttpServletRequest request, HttpServletResponse response)
    {
        try 
        {
        	try {
        		callMethod(request, response);
        	} catch (Throwable t) {
        		if (!environment.handleException(request, response, t))
        			throw t;
        	}
        }
        catch (NotFoundException e) 
        {
            SmallResponseUtils.sendError(response, 404, e.getLocalizedMessage());
        }
        catch (MethodNotAllowedException e) 
        {
            SmallResponseUtils.sendError(response, 405, e.getLocalizedMessage());
        }
        catch (BeanCreationException e) 
        {
            SmallResponseUtils.sendError(response, 500, e.getLocalizedMessage());
        }
        catch (MultipartParserException e) 
        {
            SmallResponseUtils.sendError(response, 400, e.getLocalizedMessage());
        }
        catch (NoConverterException e) 
        {
            SmallResponseUtils.sendError(response, 501, e.getLocalizedMessage());
        }
        catch (UnsupportedMediaTypeException e) 
        {
            SmallResponseUtils.sendError(response, 415, e.getLocalizedMessage());
        }
        catch (NoViewHandlerException e) 
        {
            SmallResponseUtils.sendError(response, 500, e.getLocalizedMessage());
        }
        catch (ViewProcessingException e) 
        {
            SmallResponseUtils.sendError(response, 500, e.getLocalizedMessage());
        }
        catch (IOException e) 
        {
			getServletContext().log("An exception was uncaught: ", e);
            SmallResponseUtils.sendError(response, 500, e.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
        }
        catch (Exception e) 
        {
			getServletContext().log("An exception was uncaught: ", e);
            SmallResponseUtils.sendError(response, 500, e.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
        }
    }

	private Set<RequestMethod> getMethodsForPath(String path)
	{
		Set<RequestMethod> out = new HashSet<>();
		for (RequestMethod m : RequestMethod.values())
		{
			URITrie.Result<ControllerEntryPoint> result = environment.getControllerEntryPoint(m, path);
			if (result != null && result.hasValue())
				out.add(m);
		}
		return out;
	}

	private void callMethod(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException, MethodNotAllowedException
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
		else if (SmallUtils.getConfiguration(getServletContext()).allowOptions() && method.equals(METHOD_OPTIONS))
			callOptions(request, response);
		else if (SmallUtils.getConfiguration(getServletContext()).allowTrace() && method.equals(METHOD_TRACE))
			doTrace(request, response);
		else
			throw new MethodNotAllowedException("Method " + method + " not allowed.");
	}

	private void callHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		// HEAD is a GET with no body.
		callControllerEntry(request, response, RequestMethod.GET, null);
		response.setContentLength(0);
	}

	private void callOptions(HttpServletRequest request, HttpServletResponse response)
	{
		// OPTIONS sends back a header with allowed methods.
		StringBuilder sb = new StringBuilder();
		String path = SmallUtils.trimSlashes(SmallRequestUtils.getPath(request));
		SmallConfiguration config = SmallUtils.getConfiguration(getServletContext());
		
		for (RequestMethod m : getMethodsForPath(path))
		{
			sb.append(sb.length() > 0 ? ", " : "").append(m.name());
			if (m == RequestMethod.GET)
				sb.append(sb.length() > 0 ? ", " : "").append("HEAD");
		}
		if (config.allowOptions())
			sb.append(sb.length() > 0 ? ", " : "").append("OPTIONS");
		if (config.allowTrace())
			sb.append(sb.length() > 0 ? ", " : "").append("TRACE");
			
		if (sb.length() > 0)
			response.setHeader("Allow", sb.toString());
	}
	
	private void callPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		if (MultipartFormDataParser.isMultipart(request))
			callMultipart(RequestMethod.POST, request, response);
		else if (METHOD_PATCH.equalsIgnoreCase(request.getHeader(HEADER_METHOD_OVERRIDE)))
			callControllerEntry(request, response, RequestMethod.PATCH, null);
		else
			callControllerEntry(request, response, RequestMethod.POST, null);
	}

	private void callPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		if (MultipartFormDataParser.isMultipart(request))
			callMultipart(RequestMethod.PUT, request, response);
		else if (METHOD_PATCH.equalsIgnoreCase(request.getHeader(HEADER_METHOD_OVERRIDE)))
			callControllerEntry(request, response, RequestMethod.PATCH, null);
		else
			callControllerEntry(request, response, RequestMethod.PUT, null);
	}

	private void callMultipart(RequestMethod method, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		MultipartParser parser = SmallRequestUtils.getMultipartParser(request);
		if (parser == null)
			throw new UnsupportedMediaTypeException("The " + request.getContentType() + " request type is not supported for multipart requests.");
		else
		{
			parser.parse(request, environment.getTemporaryDirectory());
			
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
	
	private void callControllerEntry(
		HttpServletRequest request, 
		HttpServletResponse response, 
		RequestMethod requestMethod, 
		HashDequeMap<String, Part> multiformPartMap
	) throws ServletException, IOException
	{
		String path = SmallRequestUtils.getPath(request);
		
		Result<ControllerEntryPoint> result = environment.getControllerEntryPoint(requestMethod, path);
		
		if (result == null || !result.hasValue())
		{
			if (getMethodsForPath(path).isEmpty())
				throw new NotFoundException("Not found. No handler for "+requestMethod.name()+ " '"+path+"'");
			else
				throw new MethodNotAllowedException("Method " + requestMethod.name() + " not allowed.");
		}
		
		// get cookies from request.
		Map<String, Cookie> cookieMap = null;
		Cookie[] cookies = request.getCookies();
		if (cookies != null)
		{
			cookieMap = new HashMap<String, Cookie>();
			for (Cookie c : cookies)
				cookieMap.put(c.getName(), c);
		}
		else
		{
			cookieMap = EMPTY_COOKIE_MAP;
		}
		
		// Get path variables.
		Map<String, String> pathVariables = result.getPathVariables() != null ? result.getPathVariables() : EMPTY_PATH_VAR_MAP;
		
		ControllerEntryPoint entryPoint = result.getValue();
		
		for (Class<?> filterClass : entryPoint.getFilterChain())
		{
			FilterComponent filterProfile = environment.getFilter(filterClass);
			if (!filterProfile.getEntryMethod().handleCall(requestMethod, request, response, pathVariables, cookieMap, multiformPartMap))
				return;
		}
	
		if (result.getRemainder() != null)
			request.setAttribute(SmallConstants.SMALL_REQUEST_ATTRIBUTE_PATH_REMAINDER, result.getRemainder());
		
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
