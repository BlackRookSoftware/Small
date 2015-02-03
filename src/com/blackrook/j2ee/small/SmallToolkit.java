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
import java.lang.reflect.Constructor;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import com.blackrook.commons.Common;
import com.blackrook.commons.Reflect;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.list.List;
import com.blackrook.j2ee.small.annotation.Component;
import com.blackrook.j2ee.small.annotation.ConstructorMain;
import com.blackrook.j2ee.small.annotation.Controller;
import com.blackrook.j2ee.small.annotation.Filter;
import com.blackrook.j2ee.small.exception.SmallFrameworkException;
import com.blackrook.j2ee.small.exception.SmallFrameworkSetupException;
import com.blackrook.j2ee.small.struct.PathTrie;

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
	private String contextApplicationPath;

	/** WebSocket Server container instance. Can be null if not present. */
	private ServerContainer serverContainer;

	/** Controller root. */
	private String controllerRootPackage;
	/** Tempdir root. */
	private File tempDir;

	/** The path to controller trie. */
	private PathTrie<Class<?>> pathTrie;

	/** The component that are instantiated. */
	private HashMap<Class<?>, Object> componentInstances;
	/** The controllers that were instantiated. */
	private HashMap<Class<?>, ControllerDescriptor> controllerInstances;
	/** The filters that were instantiated. */
	private HashMap<Class<?>, FilterDescriptor> filterInstances;
	/** Map of view resolvers to instantiated view resolvers. */
	private HashMap<Class<? extends ViewResolver>, ViewResolver> viewResolverMap;
	
	/**
	 * Constructs the toolkit.
	 */
	public SmallToolkit()
	{
		pathTrie = new PathTrie<Class<?>>();
		componentInstances = new HashMap<Class<?>, Object>();
		controllerInstances = new HashMap<Class<?>, ControllerDescriptor>(10);
		filterInstances = new HashMap<Class<?>, FilterDescriptor>(10);
		viewResolverMap = new HashMap<Class<? extends ViewResolver>, ViewResolver>();
	}
	
	@Override
	public void contextInitialized(ServletContextEvent sce)
	{
		servletContext = sce.getServletContext();
		serverContainer = (ServerContainer)servletContext.getAttribute("javax.websocket.server.ServerContainer");
		controllerRootPackage = servletContext.getInitParameter(INIT_PARAM_CONTROLLER_ROOT);
		
		tempDir = (File)servletContext.getAttribute("javax.servlet.context.tempdir");
		if (tempDir == null)
			tempDir = new File(System.getProperty("java.io.tmpdir"));
		
		if (!tempDir.exists() && !Common.createPath(tempDir.getPath()))
			throw new SmallFrameworkSetupException("The temp directory for uploaded files could not be created/found.");
		
		if (Common.isEmpty(controllerRootPackage))
			throw new SmallFrameworkSetupException("The root package init parameter was not specified.");
		
		contextApplicationPath = servletContext.getRealPath("/");
		initComponents(servletContext, ClassLoader.getSystemClassLoader());
		initComponents(servletContext, Thread.currentThread().getContextClassLoader());
		INSTANCE = this;
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce)
	{
		// Do nothing.
	}

	/**
	 * Returns the temporary directory to use for multipart files.
	 */
	public File getTemporaryDirectory()
	{
		return tempDir;
	}
	
	/**
	 * Gets a singleton component annotated with {@link Component} by class.
	 */
	public <T> T getComponent(Class<T> clazz)
	{
		return createOrGetComponent(clazz);
	}
	
	/**
	 * Returns the specific constructor to use for this class.
	 */
	@SuppressWarnings("unchecked")
	private <T> Constructor<T> getAnnotatedConstructor(Class<T> clazz)
	{
		for (Constructor<T> cons : (Constructor<T>[])clazz.getConstructors())
		{
			if (!cons.isAnnotationPresent(ConstructorMain.class))
				continue;
			else
				return cons;
		}
		
		return null;
	}

	/**
	 * Creates or gets an engine singleton component by class.
	 * @param clazz the class to create/retrieve.
	 */
	@SuppressWarnings("unchecked")
	<T> T createOrGetComponent(Class<T> clazz)
	{
		T instance = null;
		if ((instance = (T)componentInstances.get(clazz)) != null)
			return instance;
		else
		{
			instance = createComponent(clazz, getAnnotatedConstructor(clazz));
			componentInstances.put(clazz, instance);
			return instance;
		}
	}

	/**
	 * Creates a new component for a class and using one of its constructors.
	 * @param clazz the class to instantiate.
	 * @param constructor the constructor to call for instantiation.
	 * @return the new class instance.
	 */
	private <T> T createComponent(Class<T> clazz, Constructor<T> constructor)
	{
		T object = null;
		
		if (constructor == null)
		{
			object = Reflect.create(clazz);
			return object;
		}
		else
		{
			Class<?>[] types = constructor.getParameterTypes();
			Object[] params = new Object[types.length]; 
			for (int i = 0; i < types.length; i++)
			{
				if (types[i].equals(clazz))
					throw new SmallFrameworkSetupException("Circular dependency detected: class "+types[i].getSimpleName()+" is the same as this one: "+clazz.getSimpleName());
				else
				{
					if (!types[i].isAnnotationPresent(Component.class))
						throw new SmallFrameworkSetupException("Class "+types[i].getSimpleName()+" is not annotated with @Component.");
					
					params[i] = createOrGetComponent(types[i]);
				}
			}
			
			object = Reflect.construct(constructor, params);
			return object;
		}
	}

	/**
	 * Init visible controllers using a class loader.
	 * @param loader the {@link ClassLoader} to look in.
	 */
	private void initComponents(ServletContext servletContext, ClassLoader loader)
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
				if (controllerInstances.containsKey(componentClass))
					continue;
				
				controllerInstances.put(componentClass, new ControllerDescriptor(componentClass, this));
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
				if (filterInstances.containsKey(componentClass))
					continue;
				
				filterInstances.put(componentClass, new FilterDescriptor(componentClass, this));
			}
			else if (componentClass.isAnnotationPresent(ServerEndpoint.class))
			{
				if (serverContainer == null)
					throw new SmallFrameworkException("Could not add Endpoint class "+componentClass.getName()+"! The WebSocket server container may not be enabled or initialized.");
				
				try {
					serverContainer.addEndpoint(componentClass);
				} catch (DeploymentException e) {
					throw new SmallFrameworkException("Could not add Endpoint class "+componentClass.getName()+"!", e);
				}
			}
			else if (Endpoint.class.isAssignableFrom(componentClass))
			{
				if (serverContainer == null)
					throw new SmallFrameworkException("Could not add Endpoint class "+componentClass.getName()+"! The WebSocket server container may not be enabled or initialized.");
				
				try {
					serverContainer.addEndpoint(ServerEndpointConfig.Builder.create(componentClass, "/" + componentClass.getName()).build());
				} catch (DeploymentException e) {
					throw new SmallFrameworkException("Could not add Endpoint class "+componentClass.getName()+"!", e);
				}
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
		File inFile = new File(contextApplicationPath+"/"+path);
		return inFile.exists() ? inFile : null;
	}

	/**
	 * Gets a file path that is on the application path. 
	 * @param relativepath the relative path to the file to get.
	 * @return a file representing the specified resource or null if it couldn't be found.
	 */
	String getApplicationFilePath(String relativepath)
	{
		return contextApplicationPath + "/" + relativepath;
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
	Class<?> getControllerClassByPath(String path, List<String> remainder)
	{
		return pathTrie.getWithRemainderByKey(path, remainder, 0);
	}

	/**
	 * Gets the controller to call.
	 * @param clazz the controller class.
	 * @return a controller, or null if no controller by that class. 
	 * This servlet sends a 404 back if this happens.
	 */
	ControllerDescriptor getController(Class<?> clazz)
	{
		return controllerInstances.get(clazz);
	}

	/**
	 * Gets the filter to call.
	 * @throws SmallFrameworkException if a huge error occurred.
	 */
	FilterDescriptor getFilter(Class<?> clazz)
	{
		if (filterInstances.containsKey(clazz))
			return filterInstances.get(clazz);
		else synchronized (filterInstances)
		{
			// in case a thread already completed it.
			if (filterInstances.containsKey(clazz))
				return filterInstances.get(clazz);
			
			FilterDescriptor out = new FilterDescriptor(clazz, this);
			// add to cache and return.
			filterInstances.put(clazz, out);
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
