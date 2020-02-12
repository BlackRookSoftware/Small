package com.blackrook.small;

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

import com.blackrook.small.annotation.Component;
import com.blackrook.small.annotation.ComponentConstructor;
import com.blackrook.small.annotation.Controller;
import com.blackrook.small.annotation.Filter;
import com.blackrook.small.dispatch.controller.ControllerComponent;
import com.blackrook.small.dispatch.controller.ControllerEntryPoint;
import com.blackrook.small.dispatch.filter.FilterComponent;
import com.blackrook.small.enums.RequestMethod;
import com.blackrook.small.exception.SmallFrameworkException;
import com.blackrook.small.exception.SmallFrameworkSetupException;
import com.blackrook.small.parser.JSONDriver;
import com.blackrook.small.struct.URITrie;
import com.blackrook.small.struct.Utils;
import com.blackrook.small.util.SmallUtil;

/**
 * The component application environment.
 * @author Matthew Tropiano
 */
public class SmallEnvironment implements HttpSessionAttributeListener, HttpSessionListener 
{
	/** Tempdir root. */
	private File tempDir;
	/** JSON driver. */
	private JSONDriver jsonDriver;
	
	/** Components-in-construction set. */
	private Set<Class<?>> componentsConstructing;
	
	/** The path to controller trie. */
	private Map<RequestMethod, URITrie<ControllerEntryPoint>> controllerEntries;

	/** The components that are instantiated. */
	private Map<Class<?>, SmallComponent> componentInstances;
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

		this.componentsConstructing = new HashSet<>();
		
		this.controllerEntries = new HashMap<>(16);

		this.componentInstances = new HashMap<>();
		this.controllerComponents = new HashMap<>(16);
		this.filterComponents = new HashMap<>(16);
		
		this.contextListeners = new LinkedList<>();
		this.sessionListeners = new LinkedList<>();
		this.sessionAttributeListeners = new LinkedList<>();
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
	 * @return the temporary directory to use for multipart files.
	 */
	public File getTemporaryDirectory()
	{
		return tempDir;
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
	 * Initializes the environment.
	 * @param servletContext the servler context to use.
	 */
	void init(ServerContainer container, String[] controllerRootPackages, JSONDriver jsonDriver, File tempDir)
	{
		this.jsonDriver = jsonDriver;
		this.tempDir = tempDir;
		initComponents(controllerRootPackages, container, ClassLoader.getSystemClassLoader());
		initComponents(controllerRootPackages, container, Thread.currentThread().getContextClassLoader());
		for (Map.Entry<Class<?>, ? extends SmallComponent> entry : componentInstances.entrySet())
			entry.getValue().invokeAfterInitializeMethods();
		for (Map.Entry<Class<?>, ? extends SmallComponent> entry : controllerComponents.entrySet())
			entry.getValue().invokeAfterInitializeMethods();
		for (Map.Entry<Class<?>, ? extends SmallComponent> entry : filterComponents.entrySet())
			entry.getValue().invokeAfterInitializeMethods();
	}

	/**
	 * Destroys the environment.
	 */
	void destroy()
	{
		tempDir = null;
		jsonDriver = null;
		componentsConstructing.clear();
		controllerEntries.clear();
		componentInstances.clear();
		controllerComponents.clear();
		filterComponents.clear();
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
		if (trie == null)
			return null;
		
		return trie.resolve(path);
	}

	/**
	 * Init visible controllers using a class loader.
	 * @param loader the {@link ClassLoader} to look in.
	 */
	private void initComponents(String[] packageNames, ServerContainer serverContainer, ClassLoader loader)
	{
		for (String packageName : packageNames) 
			for (String className : Utils.getClasses(packageName, loader))
			{
				Class<?> componentClass = null;
				
				try {
					componentClass = Class.forName(className);
				} catch (ClassNotFoundException e) {
					throw new SmallFrameworkSetupException("Could not load class "+className+" from classpath.");
				}
				
				if (isComponentClass(componentClass))
				{
					Object component = createOrGetComponent(componentClass);
		
					if (ServletContextListener.class.isAssignableFrom(componentClass))
						contextListeners.add((ServletContextListener)component);
					
					if (HttpSessionListener.class.isAssignableFrom(componentClass))
						sessionListeners.add((HttpSessionListener)component);
					
					if (HttpSessionAttributeListener.class.isAssignableFrom(componentClass))
						sessionAttributeListeners.add((HttpSessionAttributeListener)component);
					
					if (componentClass.isAnnotationPresent(Controller.class))
					{
						if (componentClass.isAnnotationPresent(Filter.class))
							throw new SmallFrameworkSetupException("Class " + componentClass+ " is already a Controller. Can't annotate with @Filter!");
						
						Controller controllerAnnotation = componentClass.getAnnotation(Controller.class);
		
						// check for double-include. Skip.
						if (controllerComponents.containsKey(componentClass))
							continue;
						
						ControllerComponent profile;
						controllerComponents.put(componentClass, profile = new ControllerComponent(component));
						String path = SmallUtil.trimSlashes(controllerAnnotation.value());
						for (ControllerEntryPoint entryPoint : profile.getEntryMethods())
						{
							String uri = path + '/' + SmallUtil.trimSlashes(entryPoint.getPath());
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
						// check for double-include. Skip.
						if (filterComponents.containsKey(componentClass))
							continue;
						
						filterComponents.put(componentClass, new FilterComponent(component));
					}
				}
				else if (componentClass.isAnnotationPresent(ServerEndpoint.class))
				{
					if (serverContainer == null)
						throw new SmallFrameworkException("Could not add Endpoint class "+componentClass.getName()+"! The WebSocket server container may not be enabled or initialized.");
					
					ServerEndpoint anno = componentClass.getAnnotation(ServerEndpoint.class);
					ServerEndpointConfig config = ServerEndpointConfig.Builder.create(componentClass, anno.value()).build();
					config.getUserProperties().put(SmallConstants.SMALL_APPLICATION_ENVIRONMENT_ARTTRIBUTE, this);
					try {
						serverContainer.addEndpoint(config);
					} catch (DeploymentException e) {
						throw new SmallFrameworkException("Could not add Endpoint class "+componentClass.getName()+"!", e);
					}
				}
				else if (Endpoint.class.isAssignableFrom(componentClass))
				{
					if (serverContainer == null)
						throw new SmallFrameworkException("Could not add Endpoint class "+componentClass.getName()+"! The WebSocket server container may not be enabled or initialized.");
					
					ServerEndpointConfig config = ServerEndpointConfig.Builder.create(componentClass, "/" + componentClass.getName()).build();
					try {
						serverContainer.addEndpoint(config);
					} catch (DeploymentException e) {
						throw new SmallFrameworkException("Could not add Endpoint class "+componentClass.getName()+"!", e);
					}
				}
			}
	}

	/**
	 * Checks if a class is a component class.
	 * @param componentClass
	 * @return
	 */
	private boolean isComponentClass(Class<?> componentClass)
	{
		return 
			componentClass.isAnnotationPresent(Component.class) 
			|| componentClass.isAnnotationPresent(Controller.class) 
			|| componentClass.isAnnotationPresent(Filter.class);
	}

	/**
	 * Creates or gets an engine singleton component by class.
	 * @param clazz the class to create/retrieve.
	 */
	@SuppressWarnings("unchecked")
	private <T> T createOrGetComponent(Class<T> clazz)
	{
		SmallComponent component;
		if ((component = componentInstances.get(clazz)) != null)
			return (T)component.getInstance();
		else
		{
			T instance = createComponent(clazz, getAnnotatedConstructor(clazz));
			component = new SmallComponent(instance);
			component.scanMethods();
			component.invokeAfterConstructionMethods();
			componentInstances.put(clazz, component);
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
