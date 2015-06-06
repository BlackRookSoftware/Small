package com.blackrook.j2ee.small.descriptor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.commons.Common;
import com.blackrook.commons.Reflect;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.hash.HashedQueueMap;
import com.blackrook.j2ee.small.SmallUtil;
import com.blackrook.j2ee.small.annotation.Controller;
import com.blackrook.j2ee.small.annotation.ControllerEntry;
import com.blackrook.j2ee.small.annotation.FilterChain;
import com.blackrook.j2ee.small.enums.RequestMethod;
import com.blackrook.j2ee.small.exception.SmallFrameworkException;
import com.blackrook.j2ee.small.exception.SmallFrameworkSetupException;
import com.blackrook.j2ee.small.resolver.ViewResolver;
import com.blackrook.j2ee.small.struct.Part;
import com.blackrook.j2ee.small.util.RequestUtil;
import com.blackrook.lang.json.JSONObject;
import com.blackrook.lang.json.JSONWriter;
import com.blackrook.lang.xml.XMLStruct;
import com.blackrook.lang.xml.XMLWriter;

/**
 * Creates a controller profile to assist in re-calling controllers by path and methods.
 * @author Matthew Tropiano
 */
public class ControllerDescriptor extends EntryPointDescriptor
{
	private static final String PREFIX_REDIRECT = "redirect:";
	private static final RequestMethod[] REQUEST_METHODS_GET = new RequestMethod[]{RequestMethod.GET};
	private static final Class<?>[] NO_FILTERS = new Class<?>[0];
	
	/** Map of view resolvers to instantiated view resolvers. */
	private static final HashMap<Class<? extends ViewResolver>, ViewResolver> VIEW_RESOLVER_MAP = new HashMap<Class<? extends ViewResolver>, ViewResolver>();

	/** Controller annotation. */
	private Class<? extends ViewResolver> viewResolverClass;
	/** Singular method map. */
	private HashMap<RequestMethod, ControllerMethodDescriptor> defaultMethodMap;
	/** Method map. */
	private HashMap<RequestMethod, HashMap<String, ControllerMethodDescriptor>> methodMap;
	
	/** Filter class list. */
	private Class<?>[] filterChain;
	
	/**
	 * Creates the controller profile for a {@link Controller} class.
	 * @param instance the input class to profile.
	 * @throws SmallFrameworkException if this profile cannot be created due to an initialization problem.
	 */
	public ControllerDescriptor(Object instance)
	{
		super(instance);

		this.defaultMethodMap = new HashMap<RequestMethod, ControllerMethodDescriptor>();
		this.methodMap = new HashMap<RequestMethod, HashMap<String, ControllerMethodDescriptor>>(4);
		
		Class<?> clazz = instance.getClass();
		Controller controllerAnnotation = clazz.getAnnotation(Controller.class);
		if (controllerAnnotation == null)
			throw new SmallFrameworkSetupException("Class "+clazz.getName()+" is not annotated with @Controller.");
		
		this.viewResolverClass = controllerAnnotation.viewResolver();
		
		scanMethods(clazz);
		
		// accumulate filter chains.
		Class<?>[] packageFilters = NO_FILTERS; 
		Class<?>[] controllerFilters = NO_FILTERS; 

		String packageName = clazz.getPackage().getName();
		do {
			try{ Class.forName(packageName); } catch (Throwable t) {}
			Package p = Package.getPackage(packageName);
			if (p != null)
			{
				if (p.isAnnotationPresent(FilterChain.class))
				{
					FilterChain fc = p.getAnnotation(FilterChain.class);
					packageFilters = Common.joinArrays(fc.value(), packageFilters);
				}
			}
		} while (packageName.lastIndexOf('.') > 0 && (packageName = packageName.substring(0, packageName.lastIndexOf('.'))).length() > 0);

		if (clazz.isAnnotationPresent(FilterChain.class))
		{
			FilterChain fc = clazz.getAnnotation(FilterChain.class);
			controllerFilters = Arrays.copyOf(fc.value(), fc.value().length);
		}
		this.filterChain = Common.joinArrays(packageFilters, controllerFilters);
	}
	
	/** Checks if a method is a valid request entry. */
	private boolean isEntryMethod(Method method)
	{
		return
			(method.getModifiers() & Modifier.PUBLIC) != 0 
			&& method.isAnnotationPresent(ControllerEntry.class)
			;
	}
	
	@Override
	protected void scanUnknownMethod(Method method)
	{
		if (isEntryMethod(method))
		{
			ControllerEntry anno = method.getAnnotation(ControllerEntry.class);
			RequestMethod[] requestMethods = null; 
			if (Common.isEmpty(anno.method()))
				requestMethods = REQUEST_METHODS_GET;
			else
				requestMethods = anno.method();
			
			String pagename = SmallUtil.trimSlashes(anno.value());
			
			ControllerMethodDescriptor md = new ControllerMethodDescriptor(method);
			
			for (RequestMethod rm : requestMethods)
			{
				if (Common.isEmpty(anno.value()))
				{
					if (defaultMethodMap.containsKey(rm))
						throw new SmallFrameworkSetupException("Controller already contains a default entry for this request method.");
					else
						defaultMethodMap.put(rm, md);
				}
				else
				{
					HashMap<String, ControllerMethodDescriptor> map = null;
					if ((map = methodMap.get(rm)) == null)
						methodMap.put(rm, map = new HashMap<String, ControllerMethodDescriptor>(4));
					map.put(pagename, md);
				}
			}
		}
	}

	/**
	 * Gets a method on the controller using the specified page string. 
	 * @param rm the request method.
	 * @param pageString the page name (no extension).
	 */
	public ControllerMethodDescriptor getDescriptorUsingPath(RequestMethod rm, String pageString)
	{
		HashMap<String, ControllerMethodDescriptor> map = methodMap.get(rm);
		if (map == null)
			return defaultMethodMap.get(rm);
		
		ControllerMethodDescriptor out = map.get(SmallUtil.removeExtension(pageString));
		if (out == null)
			return defaultMethodMap.get(rm);
		
		return out;
	}

	/**
	 * Returns the list of filters to call.
	 */
	public Class<?>[] getFilterChain()
	{
		return filterChain;
	}

	/**
	 * Completes a full controller request call.
	 */
	public void handleCall(
		RequestMethod requestMethod, 
		HttpServletRequest request, 
		HttpServletResponse response, 
		ControllerMethodDescriptor descriptor, 
		String pathRemainder,
		HashMap<String, Cookie> cookieMap, 
		HashedQueueMap<String, Part> multiformPartMap
	)
	{
		Object retval = null;
		try {
			retval = invokeEntryMethod(requestMethod, request, response, descriptor, pathRemainder, cookieMap, multiformPartMap);
		} catch (Exception e) {
			throw new SmallFrameworkException("An exception occurred in a Controller method.", e);
		}
		
		if (descriptor.isNoCache())
		{
			response.setHeader("Cache-Control", "no-cache");
			response.setHeader("Pragma", "no-cache");
			response.setDateHeader("Expires", 0);
		}
		
		if (descriptor.getOutputType() != null)
		{
			String fname = null;
			switch (descriptor.getOutputType())
			{
				case VIEW:
				{
					String viewKey = String.valueOf(retval);
					if (viewKey.startsWith(PREFIX_REDIRECT))
						SmallUtil.sendRedirect(response, viewKey.substring(PREFIX_REDIRECT.length()));
					else
						SmallUtil.sendToView(request, response, getViewResolver(viewResolverClass).resolveView(viewKey));
					break;
				}
				case ATTACHMENT:
					fname = RequestUtil.getPage(request);
					// fall through.
				case CONTENT:
				{
					// File output.
					if (File.class.isAssignableFrom(descriptor.getType()))
					{
						File outfile = (File)retval;
						if (outfile == null || !outfile.exists())
							SmallUtil.sendCode(response, 404, "File not found.");
						else if (Common.isEmpty(descriptor.getMimeType()))
							SmallUtil.sendFileContents(response, descriptor.getMimeType(), outfile);
						else
							SmallUtil.sendFileContents(response, outfile);
					}
					// XML output.
					else if (XMLStruct.class.isAssignableFrom(descriptor.getType()))
					{
						byte[] data;
						try {
							StringWriter sw = new StringWriter();
							(new XMLWriter()).writeXML((XMLStruct)retval, sw);
							data = getStringData(sw.toString());
						} catch (IOException e) {
							throw new SmallFrameworkException(e);
						}
						SmallUtil.sendData(response, "application/xml", fname, new ByteArrayInputStream(data), data.length);
					}
					// JSON output.
					else if (JSONObject.class.isAssignableFrom(descriptor.getType()))
					{
						byte[] data;
						try {
							data = getStringData(JSONWriter.writeJSONString((JSONObject)retval));
						} catch (IOException e) {
							throw new SmallFrameworkException(e);
						}
						SmallUtil.sendData(response, "application/json", fname, new ByteArrayInputStream(data), data.length);
					}
					// StringBuffer data output.
					else if (StringBuffer.class.isAssignableFrom(descriptor.getType()))
					{
						sendStringData(response, descriptor.getMimeType(), fname, ((StringBuffer)retval).toString());
					}
					// StringBuilder data output.
					else if (StringBuilder.class.isAssignableFrom(descriptor.getType()))
					{
						sendStringData(response, descriptor.getMimeType(), fname, ((StringBuilder)retval).toString());
					}
					// String data output.
					else if (String.class.isAssignableFrom(descriptor.getType()))
					{
						sendStringData(response, descriptor.getMimeType(), fname, (String)retval);
					}
					// binary output.
					else if (byte[].class.isAssignableFrom(descriptor.getType()))
					{
						byte[] data = (byte[])retval;
						if (Common.isEmpty(descriptor.getMimeType()))
							SmallUtil.sendData(response, descriptor.getMimeType(), null, new ByteArrayInputStream(data), data.length);
						else
							SmallUtil.sendData(response, "application/octet-stream", null, new ByteArrayInputStream(data), data.length);
					}
					// Object JSON output.
					else
					{
						byte[] data;
						try {
							data = getStringData(JSONWriter.writeJSONString(retval));
						} catch (IOException e) {
							throw new SmallFrameworkException(e);
						}
						SmallUtil.sendData(response, "application/json", fname, new ByteArrayInputStream(data), data.length);
					}
					break;
				}
				default:
					// Do nothing.
					break;
			}
		}
	}

	/**
	 * Returns the instance of view resolver to use for resolving views (duh).
	 */
	private static ViewResolver getViewResolver(Class<? extends ViewResolver> vclass)
	{
		if (!VIEW_RESOLVER_MAP.containsKey(vclass))
		{
			synchronized (VIEW_RESOLVER_MAP)
			{
				if (VIEW_RESOLVER_MAP.containsKey(vclass))
					return VIEW_RESOLVER_MAP.get(vclass);
				else
				{
					ViewResolver resolver = Reflect.create(vclass);
					VIEW_RESOLVER_MAP.put(vclass, resolver);
					return resolver;
				}
			}
		}
		return VIEW_RESOLVER_MAP.get(vclass);
	}
	
	
	
}
