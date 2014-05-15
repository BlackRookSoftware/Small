package com.blackrook.j2ee;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.blackrook.commons.Common;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.linkedlist.Queue;
import com.blackrook.j2ee.annotation.Attribute;
import com.blackrook.j2ee.annotation.BodyAttachment;
import com.blackrook.j2ee.annotation.BodyContent;
import com.blackrook.j2ee.annotation.Controller;
import com.blackrook.j2ee.annotation.HeaderValue;
import com.blackrook.j2ee.annotation.Model;
import com.blackrook.j2ee.annotation.Parameter;
import com.blackrook.j2ee.annotation.Path;
import com.blackrook.j2ee.annotation.PathFile;
import com.blackrook.j2ee.annotation.PathQuery;
import com.blackrook.j2ee.annotation.RequestEntry;
import com.blackrook.j2ee.annotation.View;
import com.blackrook.j2ee.component.Filter;
import com.blackrook.j2ee.exception.SimpleFrameworkException;
import com.blackrook.j2ee.exception.SimpleFrameworkSetupException;

/**
 * Creates a controller profile to assist in re-calling controllers by
 * path and methods.
 * @author Matthew Tropiano
 */
class ControllerEntry
{
	/** Controller instance. */
	private Object instance;
	/** Method map. */
	private HashMap<RequestMethod, HashMap<String, MethodDescriptor>> methodMap;
	/** Filter list. */
	private Queue<Filter> filterQueue;
	
	/**
	 * Creates the controller profile for a {@link Controller} class.
	 * @param clazz the input class to profile.
	 * @throws SimpleFrameworkException if this profile cannot be created due to an initialization problem.
	 */
	ControllerEntry(Class<?> clazz)
	{
		Controller controllerAnnotation = clazz.getAnnotation(Controller.class);
		if (controllerAnnotation == null)
			throw new SimpleFrameworkSetupException("Class "+clazz.getName()+" is not annotated with @Controller.");
		
		this.filterQueue = new Queue<Filter>();
		this.methodMap = new HashMap<RequestMethod, HashMap<String, MethodDescriptor>>();
		
		String methodPrefix = controllerAnnotation.methodPrefix();
		
		// TODO: Handle "onlyEntry".
		
		for (Method m : clazz.getMethods())
		{
			if (validMethod(m, methodPrefix))
			{
				RequestEntry re = m.getAnnotation(RequestEntry.class);
				if (Common.isEmpty(re.value()))
					continue;
				
				String methodName = m.getName();
				String pagename = Character.toLowerCase(methodName.charAt(methodPrefix.length())) + 
						methodName.substring(methodPrefix.length() + 1);
				
				MethodDescriptor ed = new MethodDescriptor(clazz.getSimpleName(), m);
				for (RequestMethod rm : re.value())
				{
					HashMap<String, MethodDescriptor> map = null;
					if ((map = methodMap.get(rm)) == null)
						methodMap.put(rm, map = new HashMap<String, MethodDescriptor>(4));
					map.put(pagename, ed);
				}
			}
		}
		
		instance = (Controller)FrameworkUtil.getBean(clazz);
	}
	
	/** Checks if a method is a valid request entry. */
	private boolean validMethod(Method method, String methodPrefix)
	{
		return
			(method.getModifiers() & Modifier.PUBLIC) != 0 
			&& method.getName().startsWith(methodPrefix)
			&& method.getName().length() > methodPrefix.length()
			&& (method.getReturnType() == Void.TYPE || method.getReturnType() == Void.class)
			&& method.isAnnotationPresent(RequestEntry.class)
			;
			
	}
	
	/**
	 * Gets a method on the controller using the specified page string. 
	 * @param rm the request method.
	 * @param pageString the page name (no extension).
	 */
	public MethodDescriptor getDescriptorUsingPath(RequestMethod rm, String pageString)
	{
		HashMap<String, MethodDescriptor> map = methodMap.get(rm);
		if (map == null)
			return null;
		
		MethodDescriptor out = map.get(getPageNoExtension(pageString));
		return out;
	}
	
	/**
	 * Get the base page name parsed out of the page.
	 */
	private final String getPageNoExtension(String page)
	{
		int endIndex = page.indexOf('.');
		if (endIndex >= 0)
			return page.substring(0, endIndex);
		else
			return page; 
	}
	
	/**
	 * Adds a filter to this controller.
	 */
	public void addFilter(Filter filter)
	{
		filterQueue.add(filter);
	}

	/**
	 * Calls filters attached to a controller.
	 * @return true if all filters were passed successfully.
	 */
	public boolean callFilters(RequestMethod method, String file, HttpServletRequest request, HttpServletResponse response, Object content)
	{
		Iterator<Filter> it = filterQueue.iterator();
		boolean go = true;
		while (go && it.hasNext())
			go = it.next().onFilter(method, file, request, response, content);
		return go;
	}

	/**
	 * Returns the instantiated controller.
	 */
	public Object getInstance()
	{
		return instance;
	}
	
	/**
	 * Entry descriptor class.
	 */
	public static class MethodDescriptor
	{
		public static enum Source
		{
			PATH,
			PATH_FILE,
			PATH_QUERY,
			SERVLET_REQUEST,
			SERVLET_RESPONSE,
			SESSION,
			SERVLET_CONTEXT,
			HEADER_VALUE,
			METHOD_TYPE,
			ATTRIBUTE,
			PARAMETER,
			CONTENT,
			MODEL;
		}
		
		public static enum Output
		{
			CONTENT,
			CONTENT_ATTACHMENT,
			VIEW;
		}

		/**
		 * Parameter entry for the descriptor.
		 */
		public static class ParameterInfo
		{
			private Class<?> type;
			private Source sourceType;
			private ScopeType sourceScopeType;
			private String name;
			
			private ParameterInfo(Source sourceType, ScopeType scope, Class<?> type, String name)
			{
				this.type = type;
				this.sourceType = sourceType;
				this.sourceScopeType = scope;
				this.name = name;
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
		}

		/** Method. */
		private Method method;
		/** Return type. */
		private Class<?> type;
		/** Output content. */
		private Output outputType;
		/** Parameter entry. */
		private ParameterInfo[] parameters;
		
		private MethodDescriptor(String controllerName, Method method)
		{
			this.method = method;
			this.type = method.getReturnType();
			this.outputType = Output.VIEW;
			
			if (method.isAnnotationPresent(BodyContent.class))
				this.outputType = Output.CONTENT;
			else if (method.isAnnotationPresent(BodyAttachment.class))
				this.outputType = Output.CONTENT_ATTACHMENT;
			else if (method.isAnnotationPresent(View.class))
				this.outputType = Output.VIEW;
			
			Annotation[][] pannotations = method.getParameterAnnotations();
			Class<?>[] ptypes = method.getParameterTypes();
			
			this.parameters = new ParameterInfo[ptypes.length];
			for (int i = 0; i < ptypes.length; i++)
			{
				Source source = null;
				ScopeType scope = null;
				String name = null;
				Class<?> paramType = ptypes[i];
				
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
					if (annotation.getClass() == Path.class)
						source = Source.PATH;
					else if (annotation.getClass() == PathFile.class)
						source = Source.PATH_FILE;
					else if (annotation.getClass() == PathQuery.class)
						source = Source.PATH_QUERY;
					else if (annotation.getClass() == BodyContent.class)
						source = Source.CONTENT;
					else if (annotation.getClass() == HeaderValue.class)
					{
						source = Source.HEADER_VALUE;
						HeaderValue p = (HeaderValue)annotation;
						name = p.value();
					}
					else if (annotation.getClass() == Model.class)
					{
						source = Source.MODEL;
						Model p = (Model)annotation;
						name = p.value();
					}
					else if (annotation.getClass() == Parameter.class)
					{
						source = Source.PARAMETER;
						Parameter p = (Parameter)annotation;
						name = p.value();
					}
					else if (annotation.getClass() == Attribute.class)
					{
						source = Source.ATTRIBUTE;
						Attribute p = (Attribute)annotation;
						name = p.value();
						scope = p.scope();
					}
				}
				
				this.parameters[i] = new ParameterInfo(source, scope, paramType, name);
			}
			
		}

		public Method getMethod()
		{
			return method;
		}

		public Class<?> getType()
		{
			return type;
		}

		public Output getOutputType()
		{
			return outputType;
		}

		public ParameterInfo[] getParameters()
		{
			return parameters;
		}
		
	}
	
}
