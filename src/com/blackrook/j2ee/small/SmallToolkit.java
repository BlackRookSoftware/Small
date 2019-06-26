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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

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

import com.blackrook.j2ee.small.annotation.Component;
import com.blackrook.j2ee.small.annotation.ComponentConstructor;
import com.blackrook.j2ee.small.annotation.Controller;
import com.blackrook.j2ee.small.annotation.Filter;
import com.blackrook.j2ee.small.controller.ControllerEntryPoint;
import com.blackrook.j2ee.small.controller.ControllerProfile;
import com.blackrook.j2ee.small.enums.RequestMethod;
import com.blackrook.j2ee.small.exception.SmallFrameworkException;
import com.blackrook.j2ee.small.exception.SmallFrameworkSetupException;
import com.blackrook.j2ee.small.filter.FilterProfile;
import com.blackrook.j2ee.small.parser.JSONDriver;
import com.blackrook.j2ee.small.struct.URITrie;
import com.blackrook.j2ee.small.struct.Utils;
import com.blackrook.j2ee.small.util.SmallUtil;

/**
 * The main manager class through which all components are pooled and instantiated. 
 * @author Matthew Tropiano
 */
public final class SmallToolkit implements ServletContextListener, HttpSessionListener, HttpSessionAttributeListener
{
	private static final String INIT_PARAM_APPLICATION_PACKAGE_ROOTS = "small.application.package.root";
	private static final String INIT_PARAM_APPLICATION_JSON_DRIVER = "small.application.driver.json.class";
	
	/** Singleton toolkit instance. */
	static SmallToolkit INSTANCE = null;

	/** Servlet context. */
	private ServletContext servletContext;

	/** WebSocket Server container instance. Can be null if not present. */
	private ServerContainer serverContainer;

	/** Controller root. */
	private String[] controllerRootPackages;
	/** Tempdir root. */
	private File tempDir;
	/** JSON driver. */
	private JSONDriver jsonDriver;

	/** List of components that listen for session events. */
	private Queue<ServletContextListener> contextListeners;
	/** List of components that listen for session events. */
	private Queue<HttpSessionListener> sessionListeners;
	/** List of components that listen for session attribute events. */
	private Queue<HttpSessionAttributeListener> sessionAttributeListeners;

	/** Components-in-construction set. */
	private Set<Class<?>> componentsConstructing;
	/** The path to controller trie. */
	private Map<RequestMethod, URITrie> controllerEntries;
	/** The components that are instantiated. */
	private Map<Class<?>, Object> componentInstances;
	/** The controllers that were instantiated. */
	private Map<Class<?>, ControllerProfile> controllerComponents;
	/** The filters that were instantiated. */
	private Map<Class<?>, FilterProfile> filterComponents;

	/**
	 * Constructs the toolkit.
	 */
	public SmallToolkit()
	{
		this.servletContext = null;
		this.serverContainer = null;
		this.controllerRootPackages = null;
		this.tempDir = null;
		this.jsonDriver = null;

		contextListeners = new LinkedList<>();
		sessionListeners = new LinkedList<>();
		sessionAttributeListeners = new LinkedList<>();
		
		componentsConstructing = new HashSet<>();
		controllerEntries = new HashMap<>();
		componentInstances = new HashMap<>();
		controllerComponents = new HashMap<>(10);
		filterComponents = new HashMap<>(10);
	}
	
	@Override
	public void contextInitialized(ServletContextEvent sce)
	{
		servletContext = sce.getServletContext();
		serverContainer = (ServerContainer)servletContext.getAttribute("javax.websocket.server.ServerContainer");
		
		String packages = servletContext.getInitParameter(INIT_PARAM_APPLICATION_PACKAGE_ROOTS);
		if (Utils.isEmpty(packages))
			throw new SmallFrameworkSetupException("The root package init parameter was not specified.");
			
		controllerRootPackages = packages.split("\\,\\s*");
		
		tempDir = (File)servletContext.getAttribute("javax.servlet.context.tempdir");
		if (tempDir == null)
			tempDir = new File(System.getProperty("java.io.tmpdir"));

		if (!tempDir.exists() && !Utils.createPath(tempDir.getPath()))
			throw new SmallFrameworkSetupException("The temp directory for uploaded files could not be created/found.");

		String jsonDriverClassName = servletContext.getInitParameter(INIT_PARAM_APPLICATION_JSON_DRIVER);
		if (!Utils.isEmpty(jsonDriverClassName)) try {
			Class<?> jsonDriverClass = Class.forName(jsonDriverClassName, true, ClassLoader.getSystemClassLoader());
			jsonDriver = (JSONDriver)Utils.create(jsonDriverClass);  
		} catch (ClassCastException e) {
			throw new SmallFrameworkSetupException("The provided class, "+jsonDriverClassName+" is not an implementation of JSONDriver.");
		} catch (ClassNotFoundException e) {
			throw new SmallFrameworkSetupException("The provided class, "+jsonDriverClassName+" cannot be found.");
		}
		
		componentInstances.put(ServletContext.class, servletContext);
		componentInstances.put(servletContext.getClass(), servletContext);
		
		initComponents(servletContext, ClassLoader.getSystemClassLoader());
		initComponents(servletContext, Thread.currentThread().getContextClassLoader());
		INSTANCE = this;
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce)
	{
		for (ServletContextListener listener : contextListeners)
			listener.contextDestroyed(sce);
		
		contextListeners.clear();
		sessionListeners.clear();
		sessionAttributeListeners.clear();
		
		componentsConstructing.clear();
		controllerEntries.clear();
		componentInstances.clear();
		controllerComponents.clear();
		filterComponents.clear();
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
	 * @return the temporary directory to use for multipart files.
	 */
	public File getTemporaryDirectory()
	{
		return tempDir;
	}
	
	/**
	 * Gets this application's JSON converter driver.
	 * @return the instantiated driver.
	 */
	public JSONDriver getJSONDriver()
	{
		return jsonDriver;
	}
	
	/**
	 * Returns a singleton component instantiated by Small or instantiates it and returns it.
	 * @param clazz the class to fetch or instantiate.
	 * @param <T> object type.
	 * @return a singleton component annotated with {@link Component} by class.
	 */
	public <T> T getComponent(Class<T> clazz)
	{
		return createOrGetComponent(clazz);
	}
	
	/**
	 * @param <T> object type.
	 * @return the specific constructor to use for this class.
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
			else if (cons.getParameterTypes().length == 0 && (cons.getModifiers() & Modifier.PUBLIC) != 0)
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
			object = Utils.create(clazz);
			return object;
		}
		else
		{
			componentsConstructing.add(clazz);

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
			
			object = Utils.construct(constructor, params);
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
		for (String packageName : controllerRootPackages) for (String className : Utils.getClasses(packageName, loader))
		{
			Class<?> componentClass = null;
			
			try {
				componentClass = Class.forName(className);
			} catch (ClassNotFoundException e) {
				throw new SmallFrameworkSetupException("Could not load class "+className+" from classpath.");
			}
			
			if (componentClass.isAnnotationPresent(Component.class) || componentClass.isAnnotationPresent(Controller.class))
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
					
					ControllerProfile profile;
					controllerComponents.put(componentClass, profile = new ControllerProfile(component));
					String path = SmallUtil.trimSlashes(controllerAnnotation.value());
					for (ControllerEntryPoint entryPoint : profile.getEntryMethods())
					{
						String uri = path + '/' + SmallUtil.trimSlashes(entryPoint.getPath());
						for (RequestMethod rm : entryPoint.getRequestMethods())
						{
							URITrie trie;
							if ((trie = controllerEntries.get(rm)) == null)
								controllerEntries.put(rm, trie = new URITrie());
							
							try {
								trie.add(uri, entryPoint);
							} catch (PatternSyntaxException e) {
								throw new SmallFrameworkSetupException("Could not set up controller "+componentClass+", method "+entryPoint.getMethod(), e);
							}
						}
					}
					
				}
				else if (componentClass.isAnnotationPresent(Filter.class))
				{
					// check for double-include. Skip.
					if (filterComponents.containsKey(componentClass))
						continue;
					
					filterComponents.put(componentClass, new FilterProfile(component));
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
			
			if (ServletContextListener.class.isAssignableFrom(clazz))
				contextListeners.add((ServletContextListener)instance);
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
	 * Retrieves a filter singleton by class.
	 * @param filterClass
	 * @return
	 */
	FilterProfile getFilter(Class<?> filterClass)
	{
		return filterComponents.get(filterClass);
	}
	
	/**
	 * Gets the controller entry point to use using a URL path.
	 * This searches the path tree until it hits a dead end.
	 * @param requestMethod the request method to use.
	 * @param path the path to use.
	 * @return a URI resolution result or null if the method is never used.
	 */
	URITrie.Result getControllerEntryPointByURI(RequestMethod requestMethod, String path)
	{
		URITrie trie = controllerEntries.get(requestMethod);
		if (trie == null)
			return null;
		
		return trie.resolve(path);
	}

}
