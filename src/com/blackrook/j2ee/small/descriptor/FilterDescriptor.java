package com.blackrook.j2ee.small.descriptor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.hash.HashedQueueMap;
import com.blackrook.j2ee.small.annotation.Filter;
import com.blackrook.j2ee.small.annotation.FilterEntry;
import com.blackrook.j2ee.small.enums.RequestMethod;
import com.blackrook.j2ee.small.exception.SmallFrameworkException;
import com.blackrook.j2ee.small.exception.SmallFrameworkSetupException;
import com.blackrook.j2ee.small.struct.Part;

/**
 * Creates a controller profile to assist in re-calling filters.
 * @author Matthew Tropiano
 */
public class FilterDescriptor extends EntryPointDescriptor
{
	/** Method descriptor for filter. */
	private MethodDescriptor filterEntryMethodDescriptor;

	/**
	 * Creates the filter profile for a {@link Filter} class.
	 * @param instance the input class to profile.
	 * @throws SmallFrameworkException if this profile cannot be created due to an initialization problem.
	 */
	public FilterDescriptor(Object instance)
	{
		super(instance);

		this.filterEntryMethodDescriptor = null; 
		
		Class<?> clazz = instance.getClass();
		Filter controllerAnnotation = clazz.getAnnotation(Filter.class);
		if (controllerAnnotation == null)
			throw new SmallFrameworkSetupException("Class "+clazz.getName()+" is not annotated with @Filter.");

		scanMethods(clazz);
	}

	@Override
	protected void scanUnknownMethod(Method method)
	{
		if (isEntryMethod(method))
		{
			if (method.getReturnType() != Boolean.TYPE && method.getReturnType() != Boolean.class)
				throw new SmallFrameworkSetupException("Methods annotated with @FilterEntry must return a boolean.");
			else if (this.filterEntryMethodDescriptor != null)
				throw new SmallFrameworkSetupException("Filter already contains an entry point.");
			this.filterEntryMethodDescriptor = new MethodDescriptor(method);
		}
	}

	/** Checks if a method is a valid request entry. */
	private boolean isEntryMethod(Method method)
	{
		return
			(method.getModifiers() & Modifier.PUBLIC) != 0 
			&& method.isAnnotationPresent(FilterEntry.class)
			;
	}

	/**
	 * Returns the method descriptor. 
	 */
	public MethodDescriptor getFilterEntryMethodDescriptor()
	{
		return filterEntryMethodDescriptor;
	}

	/**
	 * Completes a full filter call.
	 */
	public boolean handleCall(
		RequestMethod requestMethod, 
		HttpServletRequest request, 
		HttpServletResponse response, 
		MethodDescriptor descriptor, 
		Object instance, 
		String pathRemainder,
		HashMap<String, Cookie> cookieMap, 
		HashedQueueMap<String, Part> multiformPartMap
	)
	{
		Object retval = null;
		try {
			retval = invokeEntryMethod(requestMethod, request, response, descriptor, instance, pathRemainder, cookieMap, multiformPartMap);
		} catch (Exception e) {
			throw new SmallFrameworkException("An exception occurred in a Filter method.", e);
		}
		
		return (Boolean)retval;
	}
	
	
	

}
