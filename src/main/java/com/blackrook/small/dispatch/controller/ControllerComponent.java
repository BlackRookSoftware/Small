/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.dispatch.controller;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.blackrook.small.annotation.Controller;
import com.blackrook.small.annotation.controller.EntryPath;
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
	/** 
	 * Controller output handling types. 
	 */
	public static enum Output
	{
		CONTENT,
		ATTACHMENT,
		VIEW;
	}

	/** Base URL path. */
	private String path;
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
		EntryPath pathAnnotation = clazz.getAnnotation(EntryPath.class);
		if (pathAnnotation == null)
			throw new SmallFrameworkSetupException("Class "+clazz.getName()+" is not annotated with @Controller.");

		this.path = SmallUtil.pathify(pathAnnotation.value());
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
	 * @return the base path for this controller.
	 */
	public String getPath() 
	{
		return path;
	}
	
	/**
	 * @return the list of filter classes to call.
	 */
	public Class<?>[] getFilterChain()
	{
		return filterChain;
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
		else if (method.isAnnotationPresent(EntryPath.class))
			throw new SmallFrameworkSetupException("Method " + method.toString() + " is annotated with @ControllerEntry, but must be public.");
		super.scanMethod(method);
	}

	/** Checks if a method is a valid request entry. */
	private boolean isValidEntryMethod(Method method)
	{
		return
			method.isAnnotationPresent(EntryPath.class) 
			&& (method.getModifiers() & Modifier.PUBLIC) != 0
			;
	}

}
