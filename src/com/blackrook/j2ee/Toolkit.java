/*******************************************************************************
 * Copyright (c) 2009-2012 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  
 * Contributors:
 *     Matt Tropiano - initial API and implementation
 ******************************************************************************/
package com.blackrook.j2ee;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.blackrook.commons.Common;
import com.blackrook.commons.Reflect;
import com.blackrook.commons.hash.CaseInsensitiveHashMap;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.list.List;
import com.blackrook.j2ee.annotation.Controller;
import com.blackrook.j2ee.component.QueryResolver;
import com.blackrook.j2ee.component.ViewResolver;
import com.blackrook.j2ee.exception.SimpleFrameworkException;
import com.blackrook.j2ee.exception.SimpleFrameworkSetupException;
import com.blackrook.lang.xml.XMLStruct;
import com.blackrook.lang.xml.XMLStructFactory;

/**
 * The main manager class through which all things are
 * pooled and lent out to controllers and other things that request it. 
 * @author Matthew Tropiano
 */
public final class Toolkit implements ServletContextListener
{
	/** Name for all default pools. */
	public static final String DEFAULT_POOL_NAME = "default";
	/** Application settings XML file. */
	public static final String MAPPING_XML = "/WEB-INF/simpleframework-config.xml";
	
	private static final String XML_QUERY = "queryresolver";
	private static final String XML_QUERY_CLASS = "class";
	private static final String XML_CONTROLLERROOT = "controllerroot";
	private static final String XML_CONTROLLERROOT_PACKAGE = "package";
	private static final String XML_CONTROLLERROOT_PREFIX = "prefix";
	private static final String XML_CONTROLLERROOT_SUFFIX = "suffix";
	private static final String XML_CONTROLLERROOT_INDEXCONTROLLERCLASS = "indexclass";
	
	/** Singleton toolkit instance. */
	static Toolkit INSTANCE = null;

	/** Servlet context. */
	private ServletContext servletContext;
	/** Application context path. */
	private String realAppPath;

	/** The path to controller. */
	private HashMap<String, Class<?>> pathCache;
	/** The controllers that were instantiated. */
	private HashMap<Class<?>, ControllerDescriptor> controllerCache;
	/** The filters that were instantiated. */
	private HashMap<Class<?>, FilterDescriptor> filterCache;
	/** Map of view resolvers to instantiated view resolvers. */
	private HashMap<Class<? extends ViewResolver>, ViewResolver> viewResolverMap;
	/** The cache for queries. */
	private HashMap<String, String> queryCache;
	/** List of query resolvers. */
	private List<QueryResolver> queryResolvers;

	/** Controller root package. */
	private String controllerRootPackage;
	/** Controller root prefix. */
	private String controllerRootPrefix;
	/** Controller root suffix. */
	private String controllerRootSuffix;
	/** Controller root index controller. */
	private String controllerRootIndexClass;

	/**
	 * Constructs the toolkit.
	 */
	public Toolkit()
	{
		pathCache = new HashMap<String, Class<?>>();
		controllerCache = new HashMap<Class<?>, ControllerDescriptor>();
		filterCache = new HashMap<Class<?>, FilterDescriptor>();
		viewResolverMap = new HashMap<Class<? extends ViewResolver>, ViewResolver>();
		queryCache = new CaseInsensitiveHashMap<String>(25);
		queryResolvers = new List<QueryResolver>(25);
	}
	
	@Override
	public void contextInitialized(ServletContextEvent sce)
	{
		servletContext = sce.getServletContext();
		realAppPath = servletContext.getRealPath("/");
		
		XMLStruct xml = null;
		InputStream in = null;
		
		try {
			in = getResourceAsStream(MAPPING_XML);
			if (in == null)
				throw new SimpleFrameworkException("RootManager not initialized! Missing required resource: "+MAPPING_XML);
			xml = XMLStructFactory.readXML(in);
			for (XMLStruct root : xml)
			{
				if (root.isName("config")) for (XMLStruct struct : root)
				{
					if (struct.isName(XML_QUERY))
						initializeQueryResolver(struct);
					else if (struct.isName(XML_CONTROLLERROOT))
						initializeControllerRoot(struct);
				}
			}
		} catch (Exception e) {
			throw new SimpleFrameworkException(e);
		} finally {
			Common.close(in);
		}
		initVisibleControllers(ClassLoader.getSystemClassLoader());
		initVisibleControllers(Thread.currentThread().getContextClassLoader());
		INSTANCE = this;
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce)
	{
		// Do nothing.
	}

	private void initVisibleControllers(ClassLoader loader)
	{
		for (String className : Reflect.getClasses(controllerRootPackage, loader))
		{
			Class<?> controllerClass = null;
			try {
				controllerClass = Class.forName(className);
			} catch (ClassNotFoundException e) {
				throw new SimpleFrameworkSetupException("Could not load class "+className+" from classpath.");
			}
			if (controllerClass.isAnnotationPresent(Controller.class))
				controllerCache.put(controllerClass, instantiateController(controllerClass));
		}
	}

	/**
	 * Gets a file that is on the application path. 
	 * @param path the path to the file to get.
	 * @return a file representing the specified resource or null if it couldn't be found.
	 */
	File getApplicationFile(String path)
	{
		File inFile = new File(realAppPath+"/"+path);
		return inFile.exists() ? inFile : null;
	}

	/**
	 * Gets a file path that is on the application path. 
	 * @param relativepath the relative path to the file to get.
	 * @return a file representing the specified resource or null if it couldn't be found.
	 */
	String getApplicationFilePath(String relativepath)
	{
		return realAppPath + "/" + relativepath;
	}

	/**
	 * Returns servlet context that constructed this.
	 */
	ServletContext getServletContext()
	{
		return servletContext;
	}

	/**
	 * Opens an input stream to a resource using a path relative to the
	 * application context path. 
	 * Outside users should not be able to access this!
	 * @param path the path to the resource to open.
	 * @return an open input stream to the specified resource or null if it couldn't be opened.
	 */
	@SuppressWarnings("resource")
	InputStream getResourceAsStream(String path) throws IOException
	{
		File inFile = getApplicationFile(path);
		return inFile != null ? new FileInputStream(inFile) : null;
	}

	/**
	 * Gets a query by keyword.
	 * @return the associated query or null if not found. 
	 */
	String getQueryByName(String keyword)
	{
		String out = queryCache.get(keyword);
		if  (out == null)
		{
			for (QueryResolver resolver : queryResolvers)
				if ((out = resolver.resolveQuery(keyword)) != null)
				{
					if (!resolver.dontCacheQuery(keyword))
						queryCache.put(keyword, out);
					break;					
				}
		}
		
		return out;
	}

	/**
	 * Returns a list of cached query keyword names.
	 */
	String[] getCachedQueryKeywordNames()
	{
		return getKeys(queryCache);
	}
	
	/**
	 * Gets the controller to call using the requested path.
	 * @param uriPath the path to resolve, no query string.
	 * @return a controller, or null if no controller by that name. 
	 * This servlet sends a 404 back if this happens.
	 * @throws SimpleFrameworkException if a huge error occurred.
	 */
	ControllerDescriptor getControllerUsingPath(String path)
	{
		Class<?> controllerClass = null;
		
		if (pathCache.containsKey(path))
			controllerClass = pathCache.get(path);
		else synchronized (pathCache)
		{
			// in case a thread already completed it.
			if (pathCache.containsKey(path))
				controllerClass = pathCache.get(path);
			else
			{
				String className = getClassNameForController(path);
				try {
					controllerClass = Class.forName(className);
				} catch (ClassNotFoundException e) {
					return null;
				}
			}
		}
		
		if (controllerClass == null)
			return null;
		
		if (controllerCache.containsKey(controllerClass))
			return controllerCache.get(controllerClass);
		else synchronized (controllerCache)
		{
			// in case a thread already completed it.
			if (controllerCache.containsKey(controllerClass))
				return controllerCache.get(controllerClass);
			else
			{
				ControllerDescriptor out = instantiateController(controllerClass);
				// add to cache and return.
				controllerCache.put(controllerClass, out);
				return out;
			}
		}
		
	}

	/**
	 * Gets the filter to call.
	 * @throws SimpleFrameworkException if a huge error occurred.
	 */
	FilterDescriptor getFilter(Class<?> clazz)
	{
		if (filterCache.containsKey(clazz))
			return filterCache.get(clazz);
		else synchronized (filterCache)
		{
			// in case a thread already completed it.
			if (filterCache.containsKey(clazz))
				return filterCache.get(clazz);
			
			FilterDescriptor out = instantiateFilter(clazz);
			// add to cache and return.
			filterCache.put(clazz, out);
			return out;
		}
	}

	/**
	 * Returns the instance of view resolver to use for resolving views (duh).
	 */
	ViewResolver getViewResolver(Class<? extends ViewResolver> vclass)
	{
		if (!viewResolverMap.containsKey(vclass))
		{
			synchronized (viewResolverMap)
			{
				if (viewResolverMap.containsKey(vclass))
					return viewResolverMap.get(vclass);
				else
				{
					ViewResolver resolver = Reflect.create(vclass);
					viewResolverMap.put(vclass, resolver);
					return resolver;
				}
			}
		}
		return viewResolverMap.get(vclass);
	}

	// gets String keys from a map.
	private String[] getKeys(HashMap<String, ?> map)
	{
		List<String> outList = new List<String>();
		
		Iterator<String> it = map.keyIterator();
		while(it.hasNext())
			outList.add(it.next());
		
		String[] out = new String[outList.size()];
		outList.toArray(out);
		return out;
	}
	
	/**
	 * Initializes a query resolver.
	 */
	private void initializeQueryResolver(XMLStruct struct)
	{
		String clazz = struct.getAttribute(XML_QUERY_CLASS).trim();
		if (clazz.length() == 0)
			throw new SimpleFrameworkException("Missing class in query resolver delaration.");
		
		Class<?> clz = null;
		QueryResolver resolver = null;
		
		try {
			clz = Class.forName(clazz);
		} catch (Exception e) {
			throw new SimpleFrameworkException("Class in query resolver could not be found: "+clazz);
		}
		
		try {
			resolver = (QueryResolver)FrameworkUtil.getBean(clz);
		} catch (ClassCastException e) {
			throw new SimpleFrameworkException("Class in query resolver is not an instance of BRQueryResolver: "+clz.getName());
		} catch (Exception e) {
			throw new SimpleFrameworkException("Class in query resolver could not be instantiated: "+clz.getName());
		}

		if (resolver != null)
			queryResolvers.add(resolver);
	}

	/**
	 * Initializes the controller root resolver.
	 */
	private void initializeControllerRoot(XMLStruct struct)
	{
		String pkg = struct.getAttribute(XML_CONTROLLERROOT_PACKAGE).trim();
		controllerRootPrefix = struct.getAttribute(XML_CONTROLLERROOT_PREFIX, "").trim();
		controllerRootSuffix = struct.getAttribute(XML_CONTROLLERROOT_SUFFIX, "Controller").trim();
		controllerRootIndexClass = struct.getAttribute(XML_CONTROLLERROOT_INDEXCONTROLLERCLASS).trim();
		
		if (pkg == null)
			throw new SimpleFrameworkException("Controller root declaration must specify a root package.");
		
		controllerRootPackage = pkg;
	}

	// Gets the classname of a path.
	private String getClassNameForController(String path)
	{
		String pkg = controllerRootPackage + ".";
		String cls = "";
		
		if (Common.isEmpty(path))
		{
			if (controllerRootIndexClass == null)
				return null;
			cls = controllerRootIndexClass;
			return cls;
		}
		else
		{
			String[] dirs = path.substring(1).split("[/]+");
			if (dirs.length > 1)
			{
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < dirs.length - 1; i++)
				{
					sb.append(dirs[i]);
					sb.append('.');
				}
				pkg += sb.toString();
	
			}
	
			cls = dirs[dirs.length - 1];
			cls = pkg + controllerRootPrefix + Character.toUpperCase(cls.charAt(0)) + cls.substring(1) + controllerRootSuffix;
			
			// if class is index folder without using root URL, do not permit use.
			if (cls.equals(controllerRootIndexClass))
				return null;
	
			return cls;
		}
	}

	// Instantiates a controller via root resolver.
	private ControllerDescriptor instantiateController(Class<?> clazz)
	{
		ControllerDescriptor out = null;
		out = new ControllerDescriptor(clazz);
		return out;
	}
	
	// Instantiates a filter via root resolver.
	private FilterDescriptor instantiateFilter(Class<?> clazz)
	{
		FilterDescriptor out = null;
		out = new FilterDescriptor(clazz);
		return out;
	}

}
