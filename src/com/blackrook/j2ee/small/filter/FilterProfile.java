package com.blackrook.j2ee.small.filter;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.blackrook.j2ee.small.SmallServiceProfile;
import com.blackrook.j2ee.small.annotation.Filter;
import com.blackrook.j2ee.small.annotation.filter.FilterEntry;
import com.blackrook.j2ee.small.exception.SmallFrameworkException;
import com.blackrook.j2ee.small.exception.SmallFrameworkSetupException;

/**
 * Creates a controller profile to assist in re-calling filters.
 * @author Matthew Tropiano
 */
public class FilterProfile extends SmallServiceProfile
{
	/** Method descriptor for filter. */
	private FilterEntryPoint entryMethod;

	/**
	 * Creates the filter profile for a {@link Filter} class.
	 * @param instance the input class to profile.
	 * @throws SmallFrameworkException if this profile cannot be created due to an initialization problem.
	 */
	public FilterProfile(Object instance)
	{
		super(instance);

		this.entryMethod = null; 
		
		Class<?> clazz = instance.getClass();
		Filter controllerAnnotation = clazz.getAnnotation(Filter.class);
		if (controllerAnnotation == null)
			throw new SmallFrameworkSetupException("Class "+clazz.getName()+" is not annotated with @Filter.");

		scanMethods(clazz);
	}

	@Override
	protected void scanUnknownMethod(Method method)
	{
		if (!isEntryMethod(method))
			return;
		
		if (method.getReturnType() != Boolean.TYPE && method.getReturnType() != Boolean.class)
			throw new SmallFrameworkSetupException("Methods annotated with @FilterEntry must return a boolean.");
		else if (this.entryMethod != null)
			throw new SmallFrameworkSetupException("Filter already contains an entry point.");
		this.entryMethod = new FilterEntryPoint(this, method);
	}

	/** Checks if a method is a valid request entry. */
	private boolean isEntryMethod(Method method)
	{
		return
			(method.getModifiers() & Modifier.PUBLIC) != 0 
			&& method.isAnnotationPresent(FilterEntry.class)
			&& method.getReturnType() == Boolean.TYPE 
			;
	}
	
	/**
	 * Returns this filter's sole entry method.
	 * @return
	 */
	public FilterEntryPoint getEntryMethod() 
	{
		return entryMethod;
	}
	
}
