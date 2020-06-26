/*******************************************************************************
 * Copyright (c) 2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.dispatch.filter;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.blackrook.small.SmallFilterResult;
import com.blackrook.small.annotation.Filter;
import com.blackrook.small.annotation.filter.FilterEntry;
import com.blackrook.small.annotation.filter.FilterExit;
import com.blackrook.small.dispatch.DispatchComponent;
import com.blackrook.small.exception.SmallFrameworkException;
import com.blackrook.small.exception.SmallFrameworkSetupException;

/**
 * Creates a controller profile to assist in re-calling filters.
 * @author Matthew Tropiano
 */
public class FilterComponent extends DispatchComponent
{
	/** Entry method descriptor for filter. */
	private FilterEntryPoint entryMethod;
	/** Exit method descriptor for filter. */
	private FilterExitPoint exitMethod;

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
		if (method.isAnnotationPresent(FilterEntry.class))
		{
			if (isValidEntryMethod(method))
			{
				if (entryMethod != null)
					throw new SmallFrameworkSetupException("Filter already contains an entry point.");
				entryMethod = new FilterEntryPoint(this, method);
			}
			else 
			{
				throw new SmallFrameworkSetupException("Method " + method.toString() + " is annotated with @FilterEntry, but must be public and return a boolean value.");
			}
		}

		if (method.isAnnotationPresent(FilterExit.class))
		{
			if (isValidExitMethod(method))
			{
				if (exitMethod != null)
					throw new SmallFrameworkSetupException("Filter already contains an exit point.");
				exitMethod = new FilterExitPoint(this, method);
			}
			else if (method.isAnnotationPresent(FilterExit.class))
			{
				throw new SmallFrameworkSetupException("Method " + method.toString() + " is annotated with @FilterExit, but must be public and return void.");
			}
		}
		
		super.scanMethod(method);
	}

	/** 
	 * Checks if a method is a valid filter entry. 
	 */
	private boolean isValidEntryMethod(Method method)
	{
		return
			(method.getModifiers() & Modifier.PUBLIC) != 0
			&& (method.getReturnType() == SmallFilterResult.class) 
			;
	}
	
	/** 
	 * Checks if a method is a valid filter exit. 
	 */
	private boolean isValidExitMethod(Method method)
	{
		return
			(method.getModifiers() & Modifier.PUBLIC) != 0
			&& (method.getReturnType() == Void.TYPE || method.getReturnType() == Void.class) 
			;
	}
	
	/**
	 * @return this filter's entry method.
	 */
	public FilterEntryPoint getEntryMethod() 
	{
		return entryMethod;
	}
	
	/**
	 * @return this filter's exit method.
	 */
	public FilterExitPoint getExitMethod()
	{
		return exitMethod;
	}
	
}
