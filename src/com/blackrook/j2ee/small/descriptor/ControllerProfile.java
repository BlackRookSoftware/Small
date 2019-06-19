package com.blackrook.j2ee.small.descriptor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.blackrook.j2ee.small.SmallUtil;
import com.blackrook.j2ee.small.annotation.Controller;
import com.blackrook.j2ee.small.annotation.ControllerEntry;
import com.blackrook.j2ee.small.annotation.FilterChain;
import com.blackrook.j2ee.small.exception.SmallFrameworkException;
import com.blackrook.j2ee.small.exception.SmallFrameworkSetupException;
import com.blackrook.j2ee.small.resolver.ViewResolver;
import com.blackrook.j2ee.small.util.Utils;

/**
 * Creates a controller profile to assist in re-calling controllers by path and methods.
 * @author Matthew Tropiano
 */
public class ControllerProfile extends ServiceProfile
{
	private static final Map<Class<? extends ViewResolver>, ViewResolver> VIEW_RESOLVER_MAP = new HashMap<Class<? extends ViewResolver>, ViewResolver>();
	
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
	public ControllerProfile(Object instance)
	{
		super(instance);

		Class<?> clazz = instance.getClass();
		Controller controllerAnnotation = clazz.getAnnotation(Controller.class);
		if (controllerAnnotation == null)
			throw new SmallFrameworkSetupException("Class "+clazz.getName()+" is not annotated with @Controller.");
		
		this.path = SmallUtil.trimSlashes(controllerAnnotation.value());
		this.viewResolver = createViewResolver(controllerAnnotation.viewResolver());
		this.entryMethods = new ArrayList<>();
		
		// accumulate filter chains.
		Class<?>[] packageFilters = NO_FILTERS; 
		Class<?>[] controllerFilters = NO_FILTERS; 

		String packageName = clazz.getPackage().getName();
		do {
			try{ Class.forName(packageName); } catch (Throwable t) {}
			Package p = (ClassLoader.getSystemClassLoader()).getDefinedPackage(packageName);
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

		scanMethods(clazz);
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
	 * Gets the entry points on this controller.
	 * @return
	 */
	public List<ControllerEntryPoint> getEntryMethods() 
	{
		return entryMethods;
	}
	
	@Override
	protected void scanUnknownMethod(Method method)
	{
		if (!isEntryMethod(method))
			return;
				
		entryMethods.add(new ControllerEntryPoint(this, method));
	}

	/** Checks if a method is a valid request entry. */
	private boolean isEntryMethod(Method method)
	{
		return
			(method.getModifiers() & Modifier.PUBLIC) != 0 
			&& method.isAnnotationPresent(ControllerEntry.class)
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
