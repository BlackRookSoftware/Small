/*******************************************************************************
 * Copyright (c) 2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import com.blackrook.small.annotation.Component;
import com.blackrook.small.annotation.ComponentConstructor;
import com.blackrook.small.annotation.Controller;
import com.blackrook.small.annotation.Filter;
import com.blackrook.small.annotation.controller.EntryPath;
import com.blackrook.small.dispatch.controller.ControllerComponent;
import com.blackrook.small.dispatch.controller.ControllerEntryPoint;
import com.blackrook.small.dispatch.filter.FilterComponent;
import com.blackrook.small.enums.RequestMethod;
import com.blackrook.small.exception.SmallFrameworkException;
import com.blackrook.small.exception.SmallFrameworkSetupException;
import com.blackrook.small.exception.views.ViewProcessingException;
import com.blackrook.small.roles.DefaultMIMETypeDriver;
import com.blackrook.small.roles.ExceptionHandler;
import com.blackrook.small.roles.JSONDriver;
import com.blackrook.small.roles.MIMETypeDriver;
import com.blackrook.small.roles.ViewDriver;
import com.blackrook.small.roles.XMLDriver;
import com.blackrook.small.struct.HashDequeMap;
import com.blackrook.small.struct.URITrie;
import com.blackrook.small.struct.Utils;
import com.blackrook.small.util.SmallUtils;

/**
 * The component application environment.
 * @author Matthew Tropiano
 */
public class SmallEnvironment implements HttpSessionAttributeListener, HttpSessionListener 
{
	/**
	 * Default MIME-Type driver.
	 */
	private static final MIMETypeDriver DEFAULT_MIME = new DefaultMIMETypeDriver();

	/** Tempdir root. */
	private File tempDir;
	/** JSON driver. */
	private JSONDriver jsonDriver;
	/** XML driver. */
	private XMLDriver xmlDriver;
	/** MIME-Type driver. */
	private MIMETypeDriver mimeTypeDriver;

	/** View driver list. */
	private List<ViewDriver> viewDriverList;
	/** Exception handler map. */
	private Map<Class<?>, Object> exceptionHandlerMap;
	
	/** Components-in-construction set. */
	private Set<Class<?>> componentsConstructing;
	
	/** The method to path to controller trie. */
	private Map<RequestMethod, URITrie<ControllerEntryPoint>> controllerEntries;

	/** The components that are instantiated, class set. */
	private Set<Class<?>> componentSet;
	/** The components that are instantiated. */
	private List<SmallComponent> componentList;
	/** The components that are instantiated mapped by type. */
	private HashDequeMap<Class<?>, SmallComponent> componentTypeMapping;
	/** The controllers that were instantiated. */
	private Map<Class<?>, ControllerComponent> controllerComponents;
	/** The filters that were instantiated. */
	private Map<Class<?>, FilterComponent> filterComponents;
	
	/** List of components that listen for session events. */
	private Queue<ServletContextListener> contextListeners;
	/** List of components that listen for session events. */
	private Queue<HttpSessionListener> sessionListeners;
	/** List of components that listen for session attribute events. */
	private Queue<HttpSessionAttributeListener> sessionAttributeListeners;

	/**
	 * Creates the application environment.
	 */
	SmallEnvironment()
	{
		this.tempDir = null;
		this.jsonDriver = null;
		this.xmlDriver = null;
		this.mimeTypeDriver = null;

		this.viewDriverList = new ArrayList<>();
		this.exceptionHandlerMap = new HashMap<>();

		this.componentsConstructing = new HashSet<>();
		
		this.componentSet = new HashSet<>();
		this.componentList = new LinkedList<>();
		this.componentTypeMapping = new HashDequeMap<>();
		this.controllerEntries = new HashMap<>(8);
		this.controllerComponents = new HashMap<>(16);
		this.filterComponents = new HashMap<>(16);
		
		this.contextListeners = new LinkedList<>();
		this.sessionListeners = new LinkedList<>();
		this.sessionAttributeListeners = new LinkedList<>();
	}
	
	/**
	 * Initializes the environment.
	 * @param context the servlet context to use.
	 * @param controllerRootPackages the list of controller root packages.
	 * @param tempDir the temporary directory.
	 */
	void init(ServletContext context, String[] controllerRootPackages, File tempDir)
	{
		this.tempDir = tempDir;
		this.jsonDriver = null;
		this.xmlDriver = null;
		this.mimeTypeDriver = DEFAULT_MIME;
		
		registerComponent(new SmallComponent(context));
		registerComponent(new SmallComponent(SmallUtils.getConfiguration(context)));
		registerComponent(new SmallComponent(this));

		if (!Utils.isEmpty(controllerRootPackages))
			initComponents(context, controllerRootPackages);
		for (SmallComponent sc : componentList)
			sc.invokeAfterInitializeMethods(this);
	}

	/**
	 * Destroys the environment.
	 */
	void destroy(ServletContext context)
	{
		// Destroy all components.
		for (SmallComponent sc : componentList)
		{
			try {
				sc.invokeBeforeDestructionMethods();
			} catch (Exception e) {
				context.log("Exception on destroy: ", e);
			}
		}

		tempDir = null;
		jsonDriver = null;
		xmlDriver = null;
		mimeTypeDriver = null;
		viewDriverList.clear();
		exceptionHandlerMap.clear();
		componentsConstructing.clear();
		controllerEntries.clear();
		componentList.clear();
		componentTypeMapping.clear();
		controllerComponents.clear();
		filterComponents.clear();
		contextListeners.clear();
		sessionListeners.clear();
		sessionAttributeListeners.clear();
	}

	/**
	 * Retrieves a filter singleton by class.
	 * @param filterClass the class to search for.
	 * @return the corresponding filter, or null if not found.
	 */
	FilterComponent getFilter(Class<?> filterClass)
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
	URITrie.Result<ControllerEntryPoint> getControllerEntryPoint(RequestMethod requestMethod, String path)
	{
		URITrie<ControllerEntryPoint> trie = controllerEntries.get(requestMethod);
		return trie != null ? trie.resolve(SmallUtils.trimSlashes(path)) : null;
	}

	/**
	 * Init visible controllers using a class loader.
	 * @param loader the {@link ClassLoader} to look in.
	 */
	private void initComponents(ServletContext context, String[] packageNames)
	{
		boolean allowWebSockets = SmallUtils.getConfiguration(context).allowWebSockets();
		for (String packageName : packageNames)
		{
			String[] availableClasses = Utils.joinArrays(
				Utils.getClasses(packageName, ClassLoader.getSystemClassLoader()), 
				Utils.getClassesFromClasspath(packageName)
			);
			for (String className : availableClasses)
			{
				Class<?> componentClass = null;
				
				try {
					componentClass = Class.forName(className);
				} catch (ClassNotFoundException e) {
					throw new SmallFrameworkSetupException("Could not load class "+className+" from classpath.");
				}
				
				if (componentClass.isAnnotationPresent(Component.class))
				{
					// check for double-include. Skip.
					if (componentSet.contains(componentClass))
						continue;
					
					Object componentInstance = createComponent(componentClass);
		
					if (ServletContextListener.class.isAssignableFrom(componentClass))
						contextListeners.add((ServletContextListener)componentInstance);
					
					if (HttpSessionListener.class.isAssignableFrom(componentClass))
						sessionListeners.add((HttpSessionListener)componentInstance);
					
					if (HttpSessionAttributeListener.class.isAssignableFrom(componentClass))
						sessionAttributeListeners.add((HttpSessionAttributeListener)componentInstance);

					if (JSONDriver.class.isAssignableFrom(componentClass))
						jsonDriver = (JSONDriver)componentInstance;

					if (XMLDriver.class.isAssignableFrom(componentClass))
						xmlDriver = (XMLDriver)componentInstance;

					if (MIMETypeDriver.class.isAssignableFrom(componentClass))
						mimeTypeDriver = (MIMETypeDriver)componentInstance;

					if (ViewDriver.class.isAssignableFrom(componentClass))
						viewDriverList.add((ViewDriver)componentInstance);
					
					if (ExceptionHandler.class.isAssignableFrom(componentClass))
					{
						Class<?> throwableType = null;
						// search for a specific implemented method on this interface.
						for (Method m : componentClass.getMethods())
						{
							if (m.getName().equals("handleException"))
							{
								// third parameter is generic, but the implementation has a reified type.
								throwableType = (Class<?>)m.getParameters()[2].getType();
								break;
							}
						}
						if (throwableType == null)
							throw new SmallFrameworkSetupException("Class " + componentClass + " is a child of ExceptionHandler, but it does not implement method handleException()!");
						
						exceptionHandlerMap.put(throwableType, componentInstance);
					}
					
					SmallComponent component;
					if (componentClass.isAnnotationPresent(Controller.class))
					{
						if (componentClass.isAnnotationPresent(Filter.class))
							throw new SmallFrameworkSetupException("Class " + componentClass+ " is already a Controller. Can't annotate with @Filter!");
						
						component = new ControllerComponent(componentInstance);
						controllerComponents.put(componentClass, (ControllerComponent)component);
						registerComponent(component);
						component.scanMethods();
						component.invokeAfterConstructionMethods();
						
						EntryPath entryPathAnno = componentClass.getAnnotation(EntryPath.class);
						
						String path = SmallUtils.trimSlashes(entryPathAnno != null ? entryPathAnno.value() + '/' : "");
						for (ControllerEntryPoint entryPoint : ((ControllerComponent)component).getEntryMethods())
						{
							String uri = path + '/' + SmallUtils.trimSlashes(entryPoint.getPath());
							for (RequestMethod rm : entryPoint.getRequestMethods())
							{
								URITrie<ControllerEntryPoint> trie;
								if ((trie = controllerEntries.get(rm)) == null)
									controllerEntries.put(rm, trie = new URITrie<>());
								
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
						component = new FilterComponent(componentInstance);
						filterComponents.put(componentClass, (FilterComponent)component);
						registerComponent(component);
						component.scanMethods();
						component.invokeAfterConstructionMethods();
					}
					else
					{
						component = new SmallComponent(componentInstance);
						registerComponent(component);
						component.scanMethods();
						component.invokeAfterConstructionMethods();
					}
				}
				else if (allowWebSockets)
				{
					if (SmallEndpoint.class.isAssignableFrom(componentClass))
					{
						ServerContainer websocketServerContainer = SmallUtils.getWebsocketServerContainer(context);
						if (websocketServerContainer == null)
							throw new SmallFrameworkException("Could not add ServerEndpoint class "+componentClass.getName()+"! The WebSocket server container may not be enabled or initialized.");
						
						ServerEndpoint anno = componentClass.getAnnotation(ServerEndpoint.class);
						if (anno == null)
							throw new SmallFrameworkException("Could not add ServerEndpoint class "+componentClass.getName()+"! No @ServerEndpoint path annotation.");
							
						ServerEndpointConfig config = ServerEndpointConfig.Builder.create(componentClass, anno.value()).build();
						config.getUserProperties().put(SmallConstants.SMALL_APPLICATION_ENVIRONMENT_ATTRIBUTE, this);
						try {
							websocketServerContainer.addEndpoint(config);
						} catch (DeploymentException e) {
							throw new SmallFrameworkException("Could not add ServerEndpoint class "+componentClass.getName()+"!", e);
						}
					}
					else if (Endpoint.class.isAssignableFrom(componentClass))
					{
						ServerContainer websocketServerContainer = SmallUtils.getWebsocketServerContainer(context);
						if (websocketServerContainer == null)
							throw new SmallFrameworkException("Could not add Endpoint class "+componentClass.getName()+"! The WebSocket server container may not be enabled or initialized.");
						
						ServerEndpoint anno = componentClass.getAnnotation(ServerEndpoint.class);
						ServerEndpointConfig config = ServerEndpointConfig.Builder.create(componentClass, anno.value()).build();
						config.getUserProperties().put(SmallConstants.SMALL_APPLICATION_ENVIRONMENT_ATTRIBUTE, this);
						try {
							websocketServerContainer.addEndpoint(config);
						} catch (DeploymentException e) {
							throw new SmallFrameworkException("Could not add Endpoint class "+componentClass.getName()+"!", e);
						}
					}				
				}
			}
		}
	}

	/**
	 * Creates or gets an engine singleton component by class.
	 * @param clazz the class to create/retrieve.
	 */
	private <T> T createOrGetComponent(Class<T> clazz)
	{
		T out;
		if ((out = getComponent(clazz)) != null)
			return (T)out;
		else
		{
			return createComponent(clazz);
		}
	}

	/**
	 * Creates a new component for a class and using one of its constructors.
	 * @param clazz the class to instantiate.
	 * @return the new class instance.
	 */
	private <T> T createComponent(Class<T> clazz)
	{
		T object = null;
		
		if ((clazz.getModifiers() & Modifier.ABSTRACT) != 0)
			throw new SmallFrameworkSetupException("Class "+clazz.getName()+" is abstract. Not instantiable.");
		
		Constructor<T> constructor = getAnnotatedConstructor(clazz);
		
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
					params[i] = createOrGetComponent(types[i]);
				}
			}
			
			object = Utils.construct(constructor, params);
			componentsConstructing.remove(clazz);
			return object;
		}
	}

	private void registerComponent(SmallComponent component)
	{
		componentList.add(component);
		Class<?> componentClass = component.getInstance().getClass();
		componentSet.add(componentClass);
		registerComponentTree(componentClass, component);
	}

	private void registerComponentTree(Class<?> type, SmallComponent instance)
	{
		if (type == null)
			return;
		componentTypeMapping.add(type, instance);
		for (Class<?> iface : type.getInterfaces())
			registerComponentTree(iface, instance);
		registerComponentTree(type.getSuperclass(), instance);
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
	 * Gets this application's JSON converter driver.
	 * @return the supplied driver, or null if none found.
	 */
	public JSONDriver getJSONDriver()
	{
		return jsonDriver;
	}

	/**
	 * Gets this application's XML converter driver.
	 * @return the supplied driver, or null if none found.
	 */
	public XMLDriver getXMLDriver()
	{
		return xmlDriver;
	}

	/**
	 * Gets this application's MIME-Type resolver driver.
	 * @return the instantiated driver.
	 */
	public MIMETypeDriver getMIMETypeDriver()
	{
		return mimeTypeDriver;
	}

	/**
	 * @return the temporary directory to use for multipart files.
	 */
	public File getTemporaryDirectory()
	{
		return tempDir;
	}

	/**
	 * Returns a singleton component instantiated by Small of a particular type or subtype.
	 * @param clazz the class to fetch or instantiate.
	 * @param <T> object type.
	 * @return a singleton component annotated with {@link Component} by class, or null if not found.
	 * @throws SmallFrameworkException if more than one component would be returned using this method.
	 * @since 1.3.0, this does not instantiate components, only retrieves.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getComponent(Class<T> clazz)
	{
		Deque<SmallComponent> componentDeque;
		if ((componentDeque = componentTypeMapping.get(clazz)) != null)
		{
			if (componentDeque.size() > 1)
				throw new SmallFrameworkException("Too many components match class: " + clazz.getName());
			else
				return (T)(componentDeque.getFirst().getInstance());
		}
		else
		{
			return null;
		}
	}

	/**
	 * Returns all singleton components instantiated by Small of a particular type or subtype.
	 * @param clazz the class type search for.
	 * @param <T> object type.
	 * @return all singleton component annotated with {@link Component} by class, or an empty list if not found.
	 * @since 1.3.0
	 */
	@SuppressWarnings("unchecked")
	public <T> Iterable<T> getComponentList(Class<T> clazz)
	{
		Deque<SmallComponent> components;
		if ((components = componentTypeMapping.get(clazz)) != null)
		{
			return (Iterable<T>)components.stream()
				.map((oo)->oo.getInstance())
				.collect(Collectors.toList());
		}
		else
		{
			return new LinkedList<T>();
		}
	}
	
	/**
	 * Returns a new array of objects that match the provided types in the order the types are provided.
	 * If the component can't be found, <code>null</code> is set in that index.
	 * @param types the requested types.
	 * @return an array of matching singletons.
	 * @throws SmallFrameworkException if more than one component would be returned for any one type using this method.
	 * @since 1.3.0
	 * @see #getComponent(Class)
	 */
	public Object[] getComponents(Class<?> ... types)
	{
		Object[] out = new Object[types.length];
		for (int i = 0; i < out.length; i++)
			out[i] = getComponent(types[i]);
		return out;
	}

	/**
	 * Iterates through the list of views attempting to 
	 * find a view handler suitable for rendering the provided view.
	 * @param request the HTTP request object.
	 * @param response the HTTP response object.
	 * @param model the model to render using the view.
	 * @param viewName the name of the view to handle.
	 * @return true if the view was handled by this component, false if not.
	 * @throws ViewProcessingException if an error occurs on view processing of any kind.
	 */
	public boolean handleView(HttpServletRequest request, HttpServletResponse response, Object model, String viewName) throws ViewProcessingException
	{
		for (int i = 0; i < viewDriverList.size(); i++)
			if (viewDriverList.get(i).handleView(request, response, model, viewName))
				return true;
		return false;
	}

	/**
	 * Attempts to find a handler for an uncaught exception.
	 * @param <T> the exception type.
	 * @param request the HTTP request object.
	 * @param response the HTTP response object.
	 * @param throwable the throwable to handle.
	 * @return true if the exception was handled by this method, false if not.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Throwable> boolean handleException(HttpServletRequest request, HttpServletResponse response, T throwable)
	{
		ExceptionHandler<T> handler;
		if ((handler = (ExceptionHandler<T>)exceptionHandlerMap.get(throwable.getClass())) != null)
		{
			handler.handleException(request, response, throwable);
			return true;
		}
		return false;
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

}
