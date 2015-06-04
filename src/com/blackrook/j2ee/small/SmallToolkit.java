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
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import com.blackrook.commons.Common;
import com.blackrook.commons.Reflect;
import com.blackrook.commons.hash.Hash;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.linkedlist.Queue;
import com.blackrook.commons.list.List;
import com.blackrook.j2ee.small.annotation.Component;
import com.blackrook.j2ee.small.annotation.ComponentConstructor;
import com.blackrook.j2ee.small.annotation.Controller;
import com.blackrook.j2ee.small.annotation.Filter;
import com.blackrook.j2ee.small.descriptor.ControllerDescriptor;
import com.blackrook.j2ee.small.descriptor.FilterDescriptor;
import com.blackrook.j2ee.small.exception.SmallFrameworkException;
import com.blackrook.j2ee.small.exception.SmallFrameworkSetupException;
import com.blackrook.j2ee.small.struct.PathTrie;

/**
 * The main manager class through which all components are pooled and instantiated. 
 * @author Matthew Tropiano
 */
public final class SmallToolkit implements ServletContextListener, HttpSessionListener, HttpSessionAttributeListener
{
	private static final String INIT_PARAM_CONTROLLER_ROOT = "small.application.package.root";
	
	/** Singleton toolkit instance. */
	static SmallToolkit INSTANCE = null;

	/** Servlet context. */
	private ServletContext servletContext;

	/** WebSocket Server container instance. Can be null if not present. */
	private ServerContainer serverContainer;

	/** Controller root. */
	private String controllerRootPackage;
	/** Tempdir root. */
	private File tempDir;

	/** The path to controller trie. */
	private PathTrie<Class<?>> pathTrie;

	/** List of components that listen for session events. */
	private Queue<HttpSessionListener> sessionListeners;
	/** List of components that listen for session attribute events. */
	private Queue<HttpSessionAttributeListener> sessionAttributeListeners;

	/** Components-in-construction set. */
	private Hash<Class<?>> componentsConstructing;
	/** The components that are instantiated. */
	private HashMap<Class<?>, Object> componentInstances;
	/** The controllers that were instantiated. */
	private HashMap<Class<?>, ControllerDescriptor> controllerComponents;
	/** The filters that were instantiated. */
	private HashMap<Class<?>, FilterDescriptor> filterComponents;

	/**
	 * Constructs the toolkit.
	 */
	public SmallToolkit()
	{
		pathTrie = new PathTrie<Class<?>>();
		
		sessionListeners = new Queue<HttpSessionListener>();
		sessionAttributeListeners = new Queue<HttpSessionAttributeListener>();
		
		componentsConstructing = new Hash<Class<?>>();
		componentInstances = new HashMap<Class<?>, Object>();
		controllerComponents = new HashMap<Class<?>, ControllerDescriptor>(10);
		filterComponents = new HashMap<Class<?>, FilterDescriptor>(10);
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
		
		componentInstances.put(ServletContext.class, servletContext);
		componentInstances.put(servletContext.getClass(), servletContext);
		
		initComponents(servletContext, ClassLoader.getSystemClassLoader());
		initComponents(servletContext, Thread.currentThread().getContextClassLoader());
		INSTANCE = this;
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce)
	{
		// Do nothing.
	}

	@Override
	public void sessionCreated(HttpSessionEvent httpse)
	{
		for (HttpSessionListener listener : sessionListeners)
			listener.sessionCreated(httpse);
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent httpse)
	{
		for (HttpSessionListener listener : sessionListeners)
			listener.sessionDestroyed(httpse);
	}

	@Override
	public void attributeAdded(HttpSessionBindingEvent httpsbe)
	{
		for (HttpSessionAttributeListener listener : sessionAttributeListeners)
			listener.attributeAdded(httpsbe);
	}

	@Override
	public void attributeRemoved(HttpSessionBindingEvent httpsbe)
	{
		for (HttpSessionAttributeListener listener : sessionAttributeListeners)
			listener.attributeRemoved(httpsbe);
	}

	@Override
	public void attributeReplaced(HttpSessionBindingEvent httpsbe)
	{
		for (HttpSessionAttributeListener listener : sessionAttributeListeners)
			listener.attributeReplaced(httpsbe);
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
		Constructor<T> out = null;
		boolean hasDefaultConstructor = false;
		for (Constructor<T> cons : (Constructor<T>[])clazz.getConstructors())
		{
			if (cons.isAnnotationPresent(ComponentConstructor.class))
			{
				if (out != null)
					throw new SmallFrameworkSetupException("Found more than one constructor annotated with @ComponentConstructor in class "+clazz.getName());
				else
					out = cons;
			}
			else if (cons.getParameterCount() == 0 && (cons.getModifiers() & Modifier.PUBLIC) != 0)
			{
				hasDefaultConstructor = true;
			}	
		}

		if (out == null && !hasDefaultConstructor)
		{
			throw new SmallFrameworkSetupException("Class "+clazz.getName()+" has no viable constructors.");
		}
		
		return out;
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
			componentsConstructing.put(clazz);

			Class<?>[] types = constructor.getParameterTypes();
			Object[] params = new Object[types.length]; 
			for (int i = 0; i < types.length; i++)
			{
				if (componentsConstructing.contains(types[i]))
					throw new SmallFrameworkSetupException("Circular dependency detected in class "+clazz.getSimpleName()+": "+types[i].getSimpleName()+" has not finished constructing.");
				else
				{
					if (ServletContext.class.isAssignableFrom(types[i])) // should already exist
						createOrGetComponent(types[i]);
					else if (!types[i].isAnnotationPresent(Component.class))
						throw new SmallFrameworkSetupException("Class "+types[i].getSimpleName()+" is not annotated with @Component.");
					
					params[i] = createOrGetComponent(types[i]);
				}
			}
			
			object = Reflect.construct(constructor, params);
			componentsConstructing.remove(clazz);
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
			
			if (componentClass.isAnnotationPresent(Component.class))
			{
				Object component = createOrGetComponent(componentClass);

				if (componentClass.isAnnotationPresent(Controller.class))
				{
					if (componentClass.isAnnotationPresent(Filter.class))
						throw new SmallFrameworkSetupException("Class " + componentClass+ " is already a Controller. Can't annotate with @Filter!");
					
					Controller controllerAnnotation = componentClass.getAnnotation(Controller.class);

					// check for double-include. Skip.
					if (controllerComponents.containsKey(componentClass))
						continue;
					
					controllerComponents.put(componentClass, new ControllerDescriptor(component));
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
					if (filterComponents.containsKey(componentClass))
						continue;
					
					filterComponents.put(componentClass, new FilterDescriptor(component));
				}
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

			if (HttpSessionListener.class.isAssignableFrom(clazz))
				sessionListeners.add((HttpSessionListener)instance);
			if (HttpSessionAttributeListener.class.isAssignableFrom(clazz))
				sessionAttributeListeners.add((HttpSessionAttributeListener)instance);

			componentInstances.put(clazz, instance);
			return instance;
		}
	}

	/**
	 * Returns servlet context that constructed this.
	 */
	ServletContext getServletContext()
	{
		return servletContext;
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
		return controllerComponents.get(clazz);
	}

	/**
	 * Gets the filter to call.
	 * @param clazz the filter class.
	 */
	FilterDescriptor getFilter(Class<?> clazz)
	{
		return filterComponents.get(clazz);
	}

}
