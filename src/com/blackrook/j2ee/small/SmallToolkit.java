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
package com.blackrook.j2ee.small;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.blackrook.commons.Common;
import com.blackrook.commons.Reflect;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.j2ee.small.annotation.Controller;
import com.blackrook.j2ee.small.annotation.Filter;
import com.blackrook.j2ee.small.exception.SmallFrameworkException;
import com.blackrook.j2ee.small.exception.SmallFrameworkSetupException;
import com.blackrook.j2ee.small.struct.PathTrie;
import com.blackrook.j2ee.small.struct.PathTrie.Result;

/**
 * The main manager class through which all components are pooled and instantiated. 
 * @author Matthew Tropiano
 */
public final class SmallToolkit implements ServletContextListener
{
	private static final String INIT_PARAM_CONTROLLER_ROOT = "small.application.package.root";
	
	/** Singleton toolkit instance. */
	static SmallToolkit INSTANCE = null;

	/** Servlet context. */
	private ServletContext servletContext;
	/** Application context path. */
	private String realAppPath;

	/** The path to controller trie. */
	private PathTrie<Class<?>> pathTrie;
	/** The controllers that were instantiated. */
	private HashMap<Class<?>, ControllerDescriptor> controllerCache;
	/** The filters that were instantiated. */
	private HashMap<Class<?>, FilterDescriptor> filterCache;
	/** Map of view resolvers to instantiated view resolvers. */
	private HashMap<Class<? extends ViewResolver>, ViewResolver> viewResolverMap;

	/** Controller root. */
	private String controllerRootPackage;

	/**
	 * Constructs the toolkit.
	 */
	public SmallToolkit()
	{
		pathTrie = new PathTrie<Class<?>>();
		controllerCache = new HashMap<Class<?>, ControllerDescriptor>(10);
		filterCache = new HashMap<Class<?>, FilterDescriptor>(10);
		viewResolverMap = new HashMap<Class<? extends ViewResolver>, ViewResolver>();
	}
	
	@Override
	public void contextInitialized(ServletContextEvent sce)
	{
		servletContext = sce.getServletContext();
		controllerRootPackage = servletContext.getInitParameter(INIT_PARAM_CONTROLLER_ROOT);
		
		if (Common.isEmpty(controllerRootPackage))
			throw new SmallFrameworkSetupException("The root package init parameter was not specified.");
		
		realAppPath = servletContext.getRealPath("/");
		initComponents(ClassLoader.getSystemClassLoader());
		initComponents(Thread.currentThread().getContextClassLoader());
		INSTANCE = this;
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce)
	{
		// Do nothing.
	}

	/**
	 * Init visible controllers using a class loader.
	 * @param loader the {@link ClassLoader} to look in.
	 */
	private void initComponents(ClassLoader loader)
	{
		for (String className : Reflect.getClasses(controllerRootPackage, loader))
		{
			Class<?> componentClass = null;
			try {
				componentClass = Class.forName(className);
			} catch (ClassNotFoundException e) {
				throw new SmallFrameworkSetupException("Could not load class "+className+" from classpath.");
			}
			if (componentClass.isAnnotationPresent(Controller.class))
			{
				Controller controllerAnnotation = componentClass.getAnnotation(Controller.class);

				// check for double-include. Skip.
				if (controllerCache.containsKey(componentClass))
					continue;
				
				controllerCache.put(componentClass, new ControllerDescriptor(componentClass));
				String path = controllerAnnotation.value();
				Class<?> existingClass = null;
				if ((existingClass = pathTrie.get(path)) == null)
					pathTrie.put(path, componentClass);
				else
					throw new SmallFrameworkSetupException("Path \""+ path +"\" already assigned to "+existingClass.getName());
			}
			else if (componentClass.isAnnotationPresent(Filter.class))
			{
				// check for double-include. Skip.
				if (filterCache.containsKey(componentClass))
					continue;
				
				filterCache.put(componentClass, new FilterDescriptor(componentClass));
			}
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
	 * <p>REMINDER: Close the stream when you are done!
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
	 * Gets the controller class to use using a URL path.
	 * This searches the path tree until it hits a dead end.
	 * @param path the path to use.
	 * @return a controller, or null if no controller by that class. 
	 * This servlet sends a 404 back if this happens.
	 */
	Result<Class<?>> getControllerClassByPath(String path)
	{
		return pathTrie.getPartial(path);
	}

	/**
	 * Gets the controller to call.
	 * @param clazz the controller class.
	 * @return a controller, or null if no controller by that class. 
	 * This servlet sends a 404 back if this happens.
	 */
	ControllerDescriptor getController(Class<?> clazz)
	{
		return controllerCache.get(clazz);
	}

	/**
	 * Gets the filter to call.
	 * @throws SmallFrameworkException if a huge error occurred.
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
			
			FilterDescriptor out = new FilterDescriptor(clazz);
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

}
