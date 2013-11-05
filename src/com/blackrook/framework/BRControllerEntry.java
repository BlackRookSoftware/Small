package com.blackrook.framework;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.linkedlist.Queue;
import com.blackrook.framework.annotation.RequestEntry;
import com.blackrook.framework.annotation.RequestMethod;
import com.blackrook.framework.multipart.Part;
import com.blackrook.framework.util.BRUtil;
import com.blackrook.lang.json.JSONObject;
import com.blackrook.lang.xml.XMLStruct;

/**
 * Creates a controller profile to assist in re-calling controllers by
 * path and methods.
 * @author Matthew Tropiano
 */
class BRControllerEntry
{
	/** Controller class. */
	private Class<?> controllerClass;
	/** Controller instance. */
	private BRController instance;
	/** Method map. */
	private HashMap<RequestMethod, HashMap<String, Method>> methodMap;
	/** Filter list. */
	private Queue<BRFilter> filterQueue;
	
	/**
	 * Creates the controller profile for a {@link BRController} class.
	 * @param clazz the input class to profile.
	 * @param methodPrefix the method prefix.
	 * @throws BRFrameworkException if this profile cannot be created due to an initialization problem.
	 */
	BRControllerEntry(Class<?> clazz, String methodPrefix)
	{
		this.filterQueue = new Queue<BRFilter>();
		this.controllerClass = clazz;
		this.methodMap = new HashMap<RequestMethod, HashMap<String,Method>>();
		
		for (Method m : clazz.getMethods())
		{
			if (validMethod(m, methodPrefix))
			{
				RequestEntry re = m.getAnnotation(RequestEntry.class);
				String methodName = m.getName();
				String pagename = Character.toLowerCase(methodName.charAt(methodPrefix.length())) + 
						methodName.substring(methodPrefix.length() + 1);
				
				Class<?>[] paramTypes = m.getParameterTypes();
				
				int groupType = 0;
				
				for (RequestMethod rm : re.value())
				{
					if (groupType == 0)
						groupType = rm.getCompatibilityGroup();
					else if (groupType != rm.getCompatibilityGroup())
						throw new BRFrameworkException("Request Method "+rm.name()+" in not compatible with the rest of the types on controller method "+clazz.getCanonicalName()+"."+m.getName()+"()");
					
					Class<?>[] requestParamTypes = rm.getParameterTypes();

					if (requestParamTypes.length != paramTypes.length)
						throw new BRFrameworkException("Controller method "+clazz.getCanonicalName()+"."+m.getName()+"() has parameter types that do not agree with the request entry type.");
					for (int i = 0; i < requestParamTypes.length; i++)
						if (requestParamTypes[i] != paramTypes[i])
							throw new BRFrameworkException("Controller method "+clazz.getCanonicalName()+"."+m.getName()+"() has parameter types that do not agree with the request entry type.");
						
					HashMap<String, Method> map = null;
					if ((map = methodMap.get(rm)) == null)
						methodMap.put(rm, map = new HashMap<String, Method>(4));
					map.put(pagename, m);
					}
				}
			}
		
		instance = (BRController)BRUtil.getBean(controllerClass);
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
	public Method getMethodUsingPath(RequestMethod rm, String pageString)
	{
		HashMap<String, Method> map = methodMap.get(rm);
		if (map == null)
			return null;
		
		Method out = map.get(getPageNoExtension(pageString));
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
	public void addFilter(BRFilter filter)
	{
		filterQueue.add(filter);
		}

	/**
	 * Calls filters attached to a controller.
	 * @return true if all filters were passed successfully.
	 */
	public boolean callFilters(String file, HttpServletRequest request, HttpServletResponse response)
	{
		Iterator<BRFilter> it = filterQueue.iterator();
		boolean go = true;
		while (go && it.hasNext())
			go = it.next().onFilter(file, request, response);
		return go;
		}

	/**
	 * Calls filters attached to a controller.
	 * @return true if all filters were passed successfully.
	 */
	public boolean callFilters(String file, HttpServletRequest request, HttpServletResponse response, Part[] parts)
	{
		Iterator<BRFilter> it = filterQueue.iterator();
		boolean go = true;
		while (go && it.hasNext())
			go = it.next().onFilterMultipart(file, request, response, parts);
		
		return go;
		}

	/**
	 * Calls filters attached to a controller.
	 * @return true if all filters were passed successfully.
	 */
	public boolean callFilters(String file, HttpServletRequest request, HttpServletResponse response, JSONObject json)
	{
		Iterator<BRFilter> it = filterQueue.iterator();
		boolean go = true;
		while (go && it.hasNext())
			go = it.next().onFilterJSON(file, request, response, json);
		
		return go;
		}

	/**
	 * Calls filters attached to a controller.
	 * @return true if all filters were passed successfully.
	 */
	public boolean callFilters(String file, HttpServletRequest request, HttpServletResponse response, XMLStruct xml)
	{
		Iterator<BRFilter> it = filterQueue.iterator();
		boolean go = true;
		while (go && it.hasNext())
			go = it.next().onFilterXML(file, request, response, xml);
		
		return go;
		}

	/**
	 * Returns the class type of this controller.
	 */
	public Class<?> getControllerClass()
	{
		return controllerClass;
		}

	/**
	 * Returns the instantiated controller.
	 */
	public BRController getInstance()
	{
		return instance;
		}

}
