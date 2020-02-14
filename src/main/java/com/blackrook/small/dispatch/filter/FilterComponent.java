/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.dispatch.filter;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.blackrook.small.annotation.Filter;
import com.blackrook.small.annotation.filter.FilterEntry;
import com.blackrook.small.dispatch.DispatchComponent;
import com.blackrook.small.exception.SmallFrameworkException;
import com.blackrook.small.exception.SmallFrameworkSetupException;

/**
 * Creates a controller profile to assist in re-calling filters.
 * @author Matthew Tropiano
 */
public class FilterComponent extends DispatchComponent
{
	/** Method descriptor for filter. */
	private FilterEntryPoint entryMethod;

	/**
	 * Creates the filter profile for a {@link Filter} class.
	 * @param instance the input class to profile.
	 * @throws SmallFrameworkException if this profile cannot be created due to an initialization problem.
	 */
	public FilterComponent(Object instance)
	{
		super(instance);

		this.entryMethod = null; 
		
		Class<?> clazz = instance.getClass();
		Filter controllerAnnotation = clazz.getAnnotation(Filter.class);
		if (controllerAnnotation == null)
			throw new SmallFrameworkSetupException("Class "+clazz.getName()+" is not annotated with @Filter.");
	}

	@Override
	protected void scanMethod(Method method)
	{
		if (isValidEntryMethod(method))
		{
			if (this.entryMethod != null)
				throw new SmallFrameworkSetupException("Filter already contains an entry point.");
			entryMethod = new FilterEntryPoint(this, method);
		}
		else if (method.isAnnotationPresent(FilterEntry.class))
		{
			throw new SmallFrameworkSetupException("Method " + method.toString() + " is annotated with @FilterEntry, but must be public and return a boolean value.");
		}
		super.scanMethod(method);
	}

	/** 
	 * Checks if a method is a valid request entry. 
	 */
	private boolean isValidEntryMethod(Method method)
	{
		return
			method.isAnnotationPresent(FilterEntry.class) 
			&& (method.getModifiers() & Modifier.PUBLIC) != 0
			&& (method.getReturnType() == Boolean.TYPE || method.getReturnType() == Boolean.class) 
			;
	}
	
	/**
	 * @return this filter's sole entry method.
	 */
	public FilterEntryPoint getEntryMethod() 
	{
		return entryMethod;
	}
	
}
