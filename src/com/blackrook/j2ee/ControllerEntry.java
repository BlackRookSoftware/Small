package com.blackrook.j2ee;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.commons.Common;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.linkedlist.Queue;
import com.blackrook.j2ee.annotation.Attribute;
import com.blackrook.j2ee.annotation.Controller;
import com.blackrook.j2ee.annotation.Model;
import com.blackrook.j2ee.annotation.RequestEntry;
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
	/** Model map. */
	private HashMap<String, MethodDescriptor> modelMap;
	/** Attribute map. */
	private HashMap<String, MethodDescriptor> attributeMap;
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
		this.modelMap = new HashMap<String, MethodDescriptor>();
		this.attributeMap = new HashMap<String, MethodDescriptor>();
		
		String methodPrefix = controllerAnnotation.methodPrefix();
		
		// TODO: Handle "onlyEntry".
		
		for (Method m : clazz.getMethods())
		{
			if (isEntryMethod(m, methodPrefix))
			{
				RequestEntry anno = m.getAnnotation(RequestEntry.class);
				if (Common.isEmpty(anno.value()))
					continue;
				
				String methodName = m.getName();
				String pagename = Character.toLowerCase(methodName.charAt(methodPrefix.length())) + 
						methodName.substring(methodPrefix.length() + 1);
				
				MethodDescriptor ed = new MethodDescriptor(clazz.getSimpleName(), m);
				for (RequestMethod rm : anno.value())
				{
					HashMap<String, MethodDescriptor> map = null;
					if ((map = methodMap.get(rm)) == null)
						methodMap.put(rm, map = new HashMap<String, MethodDescriptor>(4));
					map.put(pagename, ed);
				}
			}
			else if (isModelConstructorMethod(m))
			{
				Model anno = m.getAnnotation(Model.class);
				modelMap.put(anno.value(), new MethodDescriptor(clazz.getSimpleName(), m));
			}
			else if (isAttributeConstructorMethod(m))
			{
				Attribute anno = m.getAnnotation(Attribute.class);
				attributeMap.put(anno.value(), new MethodDescriptor(clazz.getSimpleName(), m));
			}
		}
		
		instance = FrameworkUtil.getBean(clazz);
	}
	
	/** Checks if a method is a valid request entry. */
	private boolean isEntryMethod(Method method, String methodPrefix)
	{
		return
			(method.getModifiers() & Modifier.PUBLIC) != 0 
			&& method.getName().startsWith(methodPrefix)
			&& method.getName().length() > methodPrefix.length()
			&& method.isAnnotationPresent(RequestEntry.class)
			;
	}
	
	/** Checks if a method is a Model constructor. */
	private boolean isModelConstructorMethod(Method method)
	{
		return
			(method.getModifiers() & Modifier.PUBLIC) != 0 
			&& method.getReturnType() != Void.TYPE 
			&& method.getReturnType() != Void.class
			&& method.isAnnotationPresent(Model.class)
			;
	}
	
	/** Checks if a method is an Attribute constructor. */
	private boolean isAttributeConstructorMethod(Method method)
	{
		return
			(method.getModifiers() & Modifier.PUBLIC) != 0 
			&& method.getReturnType() != Void.TYPE 
			&& method.getReturnType() != Void.class
			&& method.isAnnotationPresent(Attribute.class)
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
	
}
