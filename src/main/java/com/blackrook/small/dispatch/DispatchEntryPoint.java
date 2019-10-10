package com.blackrook.small.dispatch;

import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.blackrook.small.SmallConstants;
import com.blackrook.small.SmallEnvironment;
import com.blackrook.small.annotation.controller.Content;
import com.blackrook.small.annotation.dispatch.Attribute;
import com.blackrook.small.annotation.dispatch.Model;
import com.blackrook.small.annotation.parameters.AutoTrim;
import com.blackrook.small.annotation.parameters.CookieParameter;
import com.blackrook.small.annotation.parameters.Header;
import com.blackrook.small.annotation.parameters.HeaderMap;
import com.blackrook.small.annotation.parameters.Parameter;
import com.blackrook.small.annotation.parameters.ParameterMap;
import com.blackrook.small.annotation.parameters.Path;
import com.blackrook.small.annotation.parameters.PathFile;
import com.blackrook.small.annotation.parameters.PathQuery;
import com.blackrook.small.annotation.parameters.PathVariable;
import com.blackrook.small.enums.RequestMethod;
import com.blackrook.small.enums.ScopeType;
import com.blackrook.small.exception.SmallFrameworkException;
import com.blackrook.small.parser.JSONDriver;
import com.blackrook.small.parser.StringParser;
import com.blackrook.small.struct.HashDequeMap;
import com.blackrook.small.struct.Part;
import com.blackrook.small.struct.Utils;
import com.blackrook.small.util.SmallRequestUtil;
import com.blackrook.small.util.SmallResponseUtil;
import com.blackrook.small.util.SmallUtil;

/**
 * Entry method descriptor class.
 * Parses a method's characteristics using reflection, yielding a digest of its important contents.
 * @author Matthew Tropiano 
 */
public class DispatchEntryPoint<S extends DispatchComponent>
{
	/** Parameter source types. */
	public static enum Source
	{
		PATH,
		PATH_FILE,
		PATH_QUERY,
		PATH_VARIABLE,
		SERVLET_REQUEST,
		SERVLET_RESPONSE,
		SESSION,
		SERVLET_CONTEXT,
		HEADER,
		HEADER_MAP,
		METHOD_TYPE,
		COOKIE,
		ATTRIBUTE,
		PARAMETER,
		PARAMETER_MAP,
		CONTENT,
		MODEL;
	}
	
	/**
	 * Parameter entry for the descriptor.
	 */
	private static class ParameterDescriptor
	{
		private Class<?> type;
		private Source sourceType;
		private ScopeType sourceScopeType;
		private String name;
		private boolean trim;
		
		protected ParameterDescriptor(Source sourceType, ScopeType scope, Class<?> type, String name, boolean trim)
		{
			this.type = type;
			this.sourceType = sourceType;
			this.sourceScopeType = scope;
			this.name = name;
			this.trim = trim;
		}

		public String getName()
		{
			return name;
		}

		public Class<?> getType()
		{
			return type;
		}

		public Source getSourceType()
		{
			return sourceType;
		}

		public ScopeType getSourceScopeType()
		{
			return sourceScopeType;
		}

		public boolean getTrim()
		{
			return trim;
		}
	}

	/** Method. */
	private Method method;
	/** Return type. */
	private Class<?> type;
	/** Parameter entry. */
	private ParameterDescriptor[] parameters;
	/** Component instance. */
	private S componentInstance;

	/**
	 * Creates an entry method around a service profile instance.
	 * @param componentInstance the service instance.
	 * @param method the method invoked.
	 */
	public DispatchEntryPoint(S componentInstance, Method method)
	{
		this.componentInstance = componentInstance;
		this.method = method;
		this.type = method.getReturnType();

		Annotation[][] pannotations = method.getParameterAnnotations();
		Class<?>[] ptypes = method.getParameterTypes();
		
		this.parameters = new ParameterDescriptor[ptypes.length];
		for (int i = 0; i < ptypes.length; i++)
		{
			Source source = null;
			ScopeType scope = null;
			String name = null;
			Class<?> paramType = ptypes[i];
			boolean trim = false;
			
			if (paramType == RequestMethod.class)
				source = Source.METHOD_TYPE;
			else if (paramType == ServletContext.class)
				source = Source.SERVLET_CONTEXT;
			else if (paramType == HttpSession.class)
				source = Source.SESSION;
			else if (paramType == HttpServletRequest.class)
				source = Source.SERVLET_REQUEST;
			else if (paramType == HttpServletResponse.class)
				source = Source.SERVLET_RESPONSE;
			else for (int a = 0; a < pannotations[i].length; a++)
			{
				Annotation annotation = pannotations[i][a];
				
				if (annotation.annotationType() == AutoTrim.class)
					trim = true;
				
				if (annotation.annotationType() == Path.class)
					source = Source.PATH;
				else if (annotation.annotationType() == PathFile.class)
					source = Source.PATH_FILE;
				else if (annotation.annotationType() == PathQuery.class)
					source = Source.PATH_QUERY;
				else if (annotation.annotationType() == PathVariable.class)
				{
					source = Source.PATH_VARIABLE;
					name = ((PathVariable)annotation).value();
				}
				else if (annotation.annotationType() == Content.class)
					source = Source.CONTENT;
				else if (annotation.annotationType() == ParameterMap.class)
					source = Source.PARAMETER_MAP;
				else if (annotation.annotationType() == HeaderMap.class)
					source = Source.HEADER_MAP;
				else if (annotation.annotationType() == Header.class)
				{
					source = Source.HEADER;
					Header p = (Header)annotation;
					name = p.value();
				}
				else if (annotation.annotationType() == Model.class)
				{
					source = Source.MODEL;
					Model p = (Model)annotation;
					name = p.value();
				}
				else if (annotation.annotationType() == Parameter.class)
				{
					source = Source.PARAMETER;
					Parameter p = (Parameter)annotation;
					name = p.value();
				}
				else if (annotation.annotationType() == Attribute.class)
				{
					source = Source.ATTRIBUTE;
					Attribute p = (Attribute)annotation;
					name = p.value();
					scope = p.scope();
				}
				else if (annotation.annotationType() == CookieParameter.class)
				{
					source = Source.COOKIE;
					CookieParameter p = (CookieParameter)annotation;
					name = p.value();
				}
			}
			
			this.parameters[i] = new ParameterDescriptor(source, scope, paramType, name, trim);
		}
		
	}

	/**
	 * @return actual method that this describes.
	 */
	public Method getMethod()
	{
		return method;
	}

	/**
	 * @return method's return type.
	 */
	public Class<?> getType()
	{
		return type;
	}

	/**
	 * @return method's parameter info.
	 */
	public ParameterDescriptor[] getParameterDescriptors()
	{
		return parameters;
	}

	/**
	 * Gets the service profile that this belongs to.
	 * @return the service profile.
	 */
	public S getServiceProfile()
	{
		return componentInstance;
	}
	
	/**
	 * Calls a method on a filter or controller.
	 * Returns its return value.
	 */
	protected Object invoke(
		RequestMethod requestMethod, 
		HttpServletRequest request,
		HttpServletResponse response, 
		Map<String, String> pathVariableMap, 
		Map<String, Cookie> cookieMap, 
		HashDequeMap<String, Part> multiformPartMap
	)
	{
		Object[] invokeParams = new Object[parameters.length];
	
		String path = null;
		String pathFile = null;
		Object content = null;
		
		for (int i = 0; i < parameters.length; i++)
		{
			ParameterDescriptor pinfo = parameters[i];
			switch (pinfo.getSourceType())
			{
				case PATH:
					path = path != null ? path : request.getRequestURI().substring(1);
					invokeParams[i] = Utils.createForType("Parameter " + i, path, pinfo.getType());
					break;
				case PATH_FILE:
					pathFile = pathFile != null ? pathFile : SmallRequestUtil.getPage(request);
					invokeParams[i] = Utils.createForType("Parameter " + i, pathFile, pinfo.getType());
					break;
				case PATH_QUERY:
					invokeParams[i] = Utils.createForType("Parameter " + i, request.getQueryString(), pinfo.getType());
					break;
				case PATH_VARIABLE:
					invokeParams[i] = Utils.createForType("Parameter " + i, pathVariableMap.get(pinfo.name), pinfo.getType());
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
				case HEADER:
				{
					String headerName = pinfo.getName();
					if (StringParser.class.isAssignableFrom(pinfo.getType()))
					{
						try {
							Constructor<?> constructor = pinfo.getType().getConstructor(String.class);
							invokeParams[i] = constructor.newInstance(request.getHeader(headerName));
						} catch (IllegalArgumentException e) {
							throw new SmallFrameworkException("Error occurred in HeaderParser constructor.", e);
						} catch (InstantiationException e) {
							throw new SmallFrameworkException("Error occurred in HeaderParser constructor.", e);
						} catch (IllegalAccessException e) {
							throw new SmallFrameworkException("Error occurred in HeaderParser constructor.", e);
						} catch (InvocationTargetException e) {
							throw new SmallFrameworkException("Error occurred in HeaderParser constructor.", e);
						} catch (SecurityException e) {
							throw new SmallFrameworkException("Target class does not have a public constructor that takes a String as its sole parameter.");
						} catch (NoSuchMethodException e) {
							throw new SmallFrameworkException("Target class does not have a public constructor that takes a String as its sole parameter.");
						}
					}
					else
						invokeParams[i] = Utils.createForType("Parameter " + i, request.getHeader(headerName), pinfo.getType());
					break;
				}
				case HEADER_MAP:
				{
					if (Map.class.isAssignableFrom(pinfo.getType()))
					{
						Map<String, String> map = new java.util.HashMap<String, String>();
						
						Enumeration<String> strenum = request.getHeaderNames();
						while (strenum.hasMoreElements())
						{
							String header = strenum.nextElement();
							map.put(header, request.getHeader(header));
						}
						
						invokeParams[i] = map;
					}
					else
					{
						throw new SmallFrameworkException("Parameter " + i + " is not a type that can store a key-value structure.");
					}
					break;
				}
				case COOKIE:
				{
					String cookieName = pinfo.getName();
					Cookie c = cookieMap.containsKey(cookieName) ? cookieMap.get(cookieName) : new Cookie(cookieName, "");
					response.addCookie(c);
					invokeParams[i] = c;
					break;
				}
				case PARAMETER_MAP:
				{
					if (Map.class.isAssignableFrom(pinfo.getType()))
					{
						Map<String, Object> map = new HashMap<String, Object>();
						if (multiformPartMap != null)
						{
							for (Map.Entry<String, Deque<Part>> entry : multiformPartMap.entrySet())
							{
								String pname = entry.getKey();
								Deque<Part> partlist = entry.getValue();
								Part[] vout = (Part[])Array.newInstance(Part.class, partlist.size());
								int x = 0;
								for (Part p : partlist)
									vout[x++] = componentInstance.getPartData(p, Part.class);
								if (vout.length == 1)
									map.put(pname, vout[0]);
								else
									map.put(pname, vout);
							}
						}
						else for (Map.Entry<String, String[]> paramEntry : request.getParameterMap().entrySet())
						{
							String[] vals = paramEntry.getValue();
							map.put(paramEntry.getKey(), vals.length == 1 ? vals[0] : Arrays.copyOf(vals, vals.length));
						}
						invokeParams[i] = map;
					}
					else
					{
						throw new SmallFrameworkException("Parameter " + i + " is not a type that can store a key-value structure.");
					}
					break;
				}
				case PARAMETER:
				{
					String parameterName = pinfo.getName();
					Object value = null;
					if (multiformPartMap != null && multiformPartMap.containsKey(parameterName))
					{
						if (Utils.isArray(pinfo.getType()))
						{
							Class<?> actualType = Utils.getArrayType(pinfo.getType());
							Queue<Part> partlist = multiformPartMap.get(parameterName);
							Object[] vout = (Object[])Array.newInstance(actualType, partlist.size());
							int x = 0;
							for (Part p : partlist)
								vout[x++] = componentInstance.getPartData(p, actualType);
							value = vout;
						}
						else
						{
							value = componentInstance.getPartData(multiformPartMap.get(parameterName).getFirst(), pinfo.getType());
						}
					}
					else if (Utils.isArray(pinfo.getType()))
						value = request.getParameterValues(parameterName);
					else
						value = request.getParameter(parameterName);
						
					invokeParams[i] = Utils.createForType("Parameter " + i, value, pinfo.getType());
					break;
				}
				case ATTRIBUTE:
				{
					DispatchEntryPoint<?> attribDescriptor = componentInstance.getAttributeConstructor(pinfo.getName());
					if (attribDescriptor != null)
					{
						Object attrib = attribDescriptor.invoke(requestMethod, request, response, pathVariableMap, cookieMap, multiformPartMap);
						ScopeType scope = pinfo.getSourceScopeType();
						switch (scope)
						{
							case REQUEST:
								request.setAttribute(pinfo.getName(), attrib);
								break;
							case SESSION:
								request.getSession().setAttribute(pinfo.getName(), attrib);
								break;
							case APPLICATION:
								request.getServletContext().setAttribute(pinfo.getName(), attrib);
								break;
						}
					}
					
					ScopeType scope = pinfo.getSourceScopeType();
					switch (scope)
					{
						case REQUEST:
							invokeParams[i] = SmallRequestUtil.getRequestBean(request, pinfo.getType(), pinfo.getName());
							break;
						case SESSION:
							invokeParams[i] = SmallRequestUtil.getSessionBean(request, pinfo.getType(), pinfo.getName());
							break;
						case APPLICATION:
							invokeParams[i] = SmallUtil.getApplicationBean(request.getServletContext(), pinfo.getType(), pinfo.getName());
							break;
					}
					break;
				}
				case MODEL:
				{
					DispatchEntryPoint<?> modelDescriptor = componentInstance.getModelConstructor(pinfo.getName());
					if (modelDescriptor != null)
					{
						Object model = modelDescriptor.invoke(requestMethod, request, response, pathVariableMap, cookieMap, multiformPartMap);
						SmallRequestUtil.setModelFields(request, model);
						request.setAttribute(pinfo.getName(), invokeParams[i] = model);
					}
					else
						request.setAttribute(pinfo.getName(), invokeParams[i] = SmallRequestUtil.setModelFields(request, pinfo.getType()));
					break;
				}
				case CONTENT:
				{
					if (requestMethod == RequestMethod.POST || requestMethod == RequestMethod.PUT || requestMethod == RequestMethod.PATCH)
					{
						JSONDriver json = SmallUtil.getApplicationBean(
							request.getServletContext(), 
							SmallEnvironment.class, 
							SmallConstants.SMALL_APPLICATION_ENVIRONMENT_ARTTRIBUTE
						).getJSONDriver();
						
						if (SmallRequestUtil.isJSON(request) && json != null) try (Reader r = request.getReader()) {
							invokeParams[i] = json.fromJSON(request.getReader(), type);
						} catch (IOException e) {
							SmallResponseUtil.sendError(response, 500, "Server could not read request.");
							throw new SmallFrameworkException(e);
						} else try {
							content = content != null ? content : SmallRequestUtil.getContentData(request, pinfo.getType());
							invokeParams[i] = content;
						} catch (UnsupportedEncodingException e) {
							SmallResponseUtil.sendError(response, 400, "The encoding type for the POST request is not supported.");
						} catch (IOException e) {
							SmallResponseUtil.sendError(response, 500, "Server could not read request.");
							throw new SmallFrameworkException(e);
						}
					}
					else
						invokeParams[i] = null;
					break;
				}
			}
			
			// check for trim-able object.
			if (pinfo.getTrim() && invokeParams[i] != null && invokeParams[i].getClass() == String.class)
				invokeParams[i] = ((String)invokeParams[i]).trim(); 
			
		}
		
		return Utils.invokeBlind(method, componentInstance.getInstance(), invokeParams);
	}
	
	@Override
	public String toString() 
	{
		return componentInstance.getClass().getSimpleName() + ":" + method.toGenericString();
	}

}

