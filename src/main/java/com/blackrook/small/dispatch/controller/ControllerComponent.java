package com.blackrook.small.dispatch.controller;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.blackrook.small.annotation.Controller;
import com.blackrook.small.annotation.controller.ControllerEntry;
import com.blackrook.small.annotation.controller.FilterChain;
import com.blackrook.small.dispatch.DispatchComponent;
import com.blackrook.small.exception.SmallFrameworkException;
import com.blackrook.small.exception.SmallFrameworkSetupException;
import com.blackrook.small.struct.Utils;
import com.blackrook.small.util.SmallUtil;

/**
 * Creates a controller profile to assist in re-calling controllers by path and methods.
 * @author Matthew Tropiano
 */
public class ControllerComponent extends DispatchComponent
{
	/** Class to instance. */
	private static final Map<Class<? extends ViewResolver>, ViewResolver> VIEW_RESOLVER_MAP = 
			new HashMap<Class<? extends ViewResolver>, ViewResolver>();

	/** Controller output handling types. */
	public static enum Output
	{
		CONTENT,
		ATTACHMENT,
		VIEW;
	}

	/** Base URL path. */
	private String path;
	/** View resolver. */
	private ViewResolver viewResolver;
	/** Filter class list. */
	private Class<?>[] filterChain;
	/** Method map. */
	private List<ControllerEntryPoint> entryMethods;
	
	/**
	 * Creates the controller profile for a {@link Controller} class.
	 * @param instance the input class to profile.
	 * @throws SmallFrameworkException if this profile cannot be created due to an initialization problem.
	 */
	public ControllerComponent(Object instance)
	{
		super(instance);

		Class<?> clazz = instance.getClass();
		Controller controllerAnnotation = clazz.getAnnotation(Controller.class);
		if (controllerAnnotation == null)
			throw new SmallFrameworkSetupException("Class "+clazz.getName()+" is not annotated with @Controller.");

		this.path = SmallUtil.pathify(controllerAnnotation.value());
		this.viewResolver = createViewResolver(controllerAnnotation.viewResolver());
		this.entryMethods = new ArrayList<>();
		
		// accumulate filter chains.
		Class<?>[] packageFilters = NO_FILTERS; 
		Class<?>[] controllerFilters = NO_FILTERS; 

		String packageName = clazz.getPackage().getName();
		do {
			try{ Class.forName(packageName); } catch (Throwable t) {}
			Package p = Package.getPackage(packageName);
			if (p != null)
			{
				if (p.isAnnotationPresent(FilterChain.class))
				{
					FilterChain fc = p.getAnnotation(FilterChain.class);
					packageFilters = Utils.joinArrays(fc.value(), packageFilters);
				}
			}
		} while (packageName.lastIndexOf('.') > 0 && (packageName = packageName.substring(0, packageName.lastIndexOf('.'))).length() > 0);

		if (clazz.isAnnotationPresent(FilterChain.class))
		{
			FilterChain fc = clazz.getAnnotation(FilterChain.class);
			controllerFilters = Arrays.copyOf(fc.value(), fc.value().length);
		}
		
		this.filterChain = Utils.joinArrays(packageFilters, controllerFilters);
	}

	/**
	 * Gets the base path for this controller.
	 * @return
	 */
	String getPath() 
	{
		return path;
	}
	
	/**
	 * Returns the list of filters to call.
	 */
	Class<?>[] getFilterChain()
	{
		return filterChain;
	}

	/**
	 * Gets this controller's view resolver.
	 * @return
	 */
	ViewResolver getViewResolver()
	{
		return viewResolver;
	}

	/**
	 * @return the entry points on this controller.
	 */
	public List<ControllerEntryPoint> getEntryMethods() 
	{
		return entryMethods;
	}
	
	@Override
	protected void scanMethod(Method method)
	{
		if (isValidEntryMethod(method))
			entryMethods.add(new ControllerEntryPoint(this, method));
		else if (method.isAnnotationPresent(ControllerEntry.class))
			throw new SmallFrameworkSetupException("Method " + method.toString() + " is annotated with @ControllerEntry, but must be public.");
		super.scanMethod(method);
	}

	/** Checks if a method is a valid request entry. */
	private boolean isValidEntryMethod(Method method)
	{
		return
			method.isAnnotationPresent(ControllerEntry.class) 
			&& (method.getModifiers() & Modifier.PUBLIC) != 0
			;
	}

	/**
	 * Returns the instance of view resolver to use for resolving views.
	 */
	private static ViewResolver createViewResolver(Class<? extends ViewResolver> vclass)
	{
		if (!VIEW_RESOLVER_MAP.containsKey(vclass))
		{
			synchronized (VIEW_RESOLVER_MAP)
			{
				if (VIEW_RESOLVER_MAP.containsKey(vclass))
					return VIEW_RESOLVER_MAP.get(vclass);
				else
				{
					ViewResolver resolver = Utils.create(vclass);
					VIEW_RESOLVER_MAP.put(vclass, resolver);
					return resolver;
				}
			}
		}
		return VIEW_RESOLVER_MAP.get(vclass);
	}
	
}
