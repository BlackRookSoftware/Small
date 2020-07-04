/*******************************************************************************
 * Copyright (c) 2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
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
import com.blackrook.small.dispatch.filter.FilterEntryPoint;
import com.blackrook.small.dispatch.filter.FilterExitPoint;
import com.blackrook.small.enums.RequestMethod;
import com.blackrook.small.exception.SmallFrameworkSetupException;
import com.blackrook.small.exception.request.BeanCreationException;
import com.blackrook.small.exception.request.ManyRequestExceptionsException;
import com.blackrook.small.exception.request.MethodNotAllowedException;
import com.blackrook.small.exception.request.MultipartParserException;
import com.blackrook.small.exception.request.NoConverterException;
import com.blackrook.small.exception.request.NoViewDriverException;
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
	private static final Map<String, Cookie> EMPTY_COOKIE_MAP = Collections.unmodifiableMap(new HashMap<>(2));
	private static final Map<String, String> EMPTY_PATH_VAR_MAP = Collections.unmodifiableMap(new HashMap<>(2));

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

	private SmallEnvironment createEnvironment(ServletContext servletContext)
	{
		SmallConfiguration smallConfig = SmallUtils.getConfiguration(servletContext);
		
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
    protected void service(HttpServletRequest request, HttpServletResponse response)
    {
    	SmallResponse smallResponse = null;
        try
        {
    		if ((smallResponse = callMethod(request, response)) != null)
    			SmallUtils.sendContent(request, response, null, smallResponse);
    		// if null, nothing is written to the response (in this method).
        }
        // Servlet Exceptions
        catch (NotFoundException e) 
        {
			getServletContext().log("An exception was uncaught: ", e);
            SmallResponseUtils.sendError(response, 404, e.getLocalizedMessage());
        }
        catch (MethodNotAllowedException e) 
        {
			getServletContext().log("An exception was uncaught: ", e);
            SmallResponseUtils.sendError(response, 405, e.getLocalizedMessage());
        }
        catch (BeanCreationException e) 
        {
			getServletContext().log("An exception was uncaught: ", e);
            SmallResponseUtils.sendError(response, 500, e.getLocalizedMessage());
        }
        catch (MultipartParserException e) 
        {
			getServletContext().log("An exception was uncaught: ", e);
            SmallResponseUtils.sendError(response, 400, e.getLocalizedMessage());
        }
        catch (NoConverterException e) 
        {
			getServletContext().log("An exception was uncaught: ", e);
            SmallResponseUtils.sendError(response, 501, e.getLocalizedMessage());
        }
        catch (UnsupportedMediaTypeException e) 
        {
			getServletContext().log("An exception was uncaught: ", e);
            SmallResponseUtils.sendError(response, 415, e.getLocalizedMessage());
        }
        catch (NoViewDriverException e) 
        {
			getServletContext().log("An exception was uncaught: ", e);
            SmallResponseUtils.sendError(response, 501, e.getLocalizedMessage());
        }
        catch (ViewProcessingException e) 
        {
			getServletContext().log("An exception was uncaught: ", e);
            SmallResponseUtils.sendError(response, 500, e.getLocalizedMessage());
        }
        catch (ManyRequestExceptionsException e) 
        {
			getServletContext().log("Many exceptions were uncaught:");
			for (Throwable t : e.getCauses())
				getServletContext().log("From ManyRequestExceptionsException:", t);
            SmallResponseUtils.sendError(response, 500, e.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
        }
        // I/O Exceptions
        catch (IOException e) 
        {
			getServletContext().log("An exception was uncaught: ", e);
            SmallResponseUtils.sendError(response, 500, e.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
        }
        // Other Exceptions
        catch (Throwable e) 
        {
			getServletContext().log("An exception was uncaught: ", e);
            SmallResponseUtils.sendError(response, 500, e.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
		} 
        finally 
        {
        	// Close anything still open that the response may encapsulate.
        	Utils.close(smallResponse);
        	
			// clean up files read in multipart parts.
			@SuppressWarnings("unchecked")
			List<Part> parts = (List<Part>)request.getAttribute(SmallConstants.SMALL_REQUEST_ATTRIBUTE_MULTIPART_LIST);
			if (parts != null) for (Part part : parts) if (part.isFile())
			{
				part.getFile().delete();
			}
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

	private SmallResponse callMethod(HttpServletRequest request, HttpServletResponse response) throws Throwable
	{
		try {
			String method = request.getMethod();
			switch (method)
			{
				case METHOD_GET:
					return callControllerEntry(request, response, RequestMethod.GET, null);
				case METHOD_DELETE:
					return callControllerEntry(request, response, RequestMethod.DELETE, null);
				case METHOD_POST:
					return callPost(request, response);
				case METHOD_PUT:
					return callPut(request, response);
				case METHOD_PATCH:
					return callPatch(request, response);
				case METHOD_HEAD:
					callHead(request, response);
					return null;
				case METHOD_OPTIONS:
					if (!SmallUtils.getConfiguration(getServletContext()).allowOptions())
						throw new MethodNotAllowedException("HTTP method OPTIONS not allowed.");
					else
						callOptions(request, response);
					return null;
				case METHOD_TRACE:
					if (!SmallUtils.getConfiguration(getServletContext()).allowTrace())
						throw new MethodNotAllowedException("HTTP method TRACE not allowed.");
					else
						super.doTrace(request, response);
					return null;
				default:
					throw new MethodNotAllowedException("HTTP method " + method + " not allowed.");
			}
		} catch (Throwable t) {
			if (!environment.handleException(request, response, t))
				throw t;
			else
				return null;
		}

	}

	private void callHead(HttpServletRequest request, HttpServletResponse response) throws Throwable
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
	
	private SmallResponse callPost(HttpServletRequest request, HttpServletResponse response) throws Throwable
	{
		if (SmallUtils.getConfiguration(request.getServletContext()).autoParseMultipart() && MultipartFormDataParser.isMultipart(request))
			return callMultipart(RequestMethod.POST, request, response);
		else if (METHOD_PATCH.equalsIgnoreCase(request.getHeader(HEADER_METHOD_OVERRIDE)))
			return callControllerEntry(request, response, RequestMethod.PATCH, null);
		else
			return callControllerEntry(request, response, RequestMethod.POST, null);
	}

	private SmallResponse callPut(HttpServletRequest request, HttpServletResponse response) throws Throwable
	{
		if (SmallUtils.getConfiguration(request.getServletContext()).autoParseMultipart() && MultipartFormDataParser.isMultipart(request))
			return callMultipart(RequestMethod.PUT, request, response);
		else if (METHOD_PATCH.equalsIgnoreCase(request.getHeader(HEADER_METHOD_OVERRIDE)))
			return callControllerEntry(request, response, RequestMethod.PATCH, null);
		else
			return callControllerEntry(request, response, RequestMethod.PUT, null);
	}

	private SmallResponse callPatch(HttpServletRequest request, HttpServletResponse response) throws Throwable
	{
		if (SmallUtils.getConfiguration(request.getServletContext()).autoParseMultipart() && MultipartFormDataParser.isMultipart(request))
			return callMultipart(RequestMethod.PATCH, request, response);
		else
			return callControllerEntry(request, response, RequestMethod.PATCH, null);
	}

	private SmallResponse callMultipart(RequestMethod method, HttpServletRequest request, HttpServletResponse response) throws Throwable
	{
		MultipartParser parser = SmallRequestUtils.getMultipartParser(request);
		if (parser == null)
			throw new UnsupportedMediaTypeException("The " + request.getContentType() + " request type is not supported for multipart requests.");
		else
		{
			parser.parse(request, environment.getTemporaryDirectory());
			
			List<Part> parts = parser.getPartList();
			request.setAttribute(SmallConstants.SMALL_REQUEST_ATTRIBUTE_MULTIPART_LIST, parts);
			
			HashDequeMap<String, Part> partMap = new HashDequeMap<>();
			for (Part part : parts)
				partMap.addLast(part.getName(), part);
			return callControllerEntry(request, response, method, partMap);
		}
	}
	
	private SmallResponse callControllerEntry(
		HttpServletRequest request, 
		HttpServletResponse response, 
		RequestMethod requestMethod, 
		HashDequeMap<String, Part> multiformPartMap
	) throws Throwable {
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
		Map<String, String> pathVariables = result.getPathVariables();
		if (pathVariables == null)
			pathVariables = EMPTY_PATH_VAR_MAP;
		
		ControllerEntryPoint entryPoint = result.getValue();
		Class<?>[] filterChain = entryPoint.getFilterChain();

		if (result.getRemainder() != null)
			request.setAttribute(SmallConstants.SMALL_REQUEST_ATTRIBUTE_PATH_REMAINDER, result.getRemainder() + SmallRequestUtils.getPathExtension(request));

		int f = 0;
		Throwable exception = null;
		SmallResponse smallResponse = null;
		
		// Forward filter chain.
		try {
			for (; f < filterChain.length; f++)
			{
				FilterComponent filterProfile = environment.getFilter(filterChain[f]);
				FilterEntryPoint entry = filterProfile.getEntryMethod();
				if (entry != null)
				{
					SmallFilterResult filterResult = entry.handleCall(requestMethod, request, response, pathVariables, cookieMap, multiformPartMap);
					if (filterResult == null || !filterResult.isPassing())
						break;
					HttpServletRequest newRequest = filterResult.getRequest();
					if (newRequest != null)
						request = newRequest;
					HttpServletResponse newResponse = filterResult.getResponse();
					if (newResponse != null)
						response = newResponse;
				}
			}
		} catch (InvocationTargetException e) {
			exception = accumExceptions(exception, e.getCause());
		} catch (ServletException e) {
			exception = accumExceptions(exception, e);
		} catch (IOException e) {
			exception = accumExceptions(exception, e);
		}
	
		// Call Entry if all filters passed.
		if (f == filterChain.length)
		{
			try {
				smallResponse = entryPoint.handleCall(requestMethod, request, response, pathVariables, cookieMap, multiformPartMap);
			} catch (InvocationTargetException e) {
				exception = accumExceptions(exception, e.getCause());
			} catch (ServletException e) {
				exception = accumExceptions(exception, e);
			} catch (IOException e) {
				exception = accumExceptions(exception, e);
			}
		}
		
		// Backward filter chain.
		for (f = f - 1; f >= 0; f--)
		{
			FilterComponent filterProfile = environment.getFilter(filterChain[f]);
			FilterExitPoint exit = filterProfile.getExitMethod();
			if (exit != null)
			{
				try {
					exit.handleCall(requestMethod, request, response, pathVariables, cookieMap, multiformPartMap);
				} catch (InvocationTargetException e) {
					exception = accumExceptions(exception, e.getCause());
				}
			}
		}

		if (exception != null)
			throw exception;
		else
			return smallResponse;
	}
	
	private Throwable accumExceptions(Throwable source, Throwable t)
	{
		if (source == null)
		{
			return t;
		}
		else if (source instanceof ManyRequestExceptionsException)
		{
			ManyRequestExceptionsException e = (ManyRequestExceptionsException)source;
			e.addCause(t);
			return e;
		}
		else
		{
			ManyRequestExceptionsException e = new ManyRequestExceptionsException("Many exceptions happened.", source);
			e.addCause(t);
			return e;
		}
	}
	
}
