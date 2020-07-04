/*******************************************************************************
 * Copyright (c) 2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.blackrook.small.annotation.component.AfterConstruction;
import com.blackrook.small.annotation.component.AfterInitialize;
import com.blackrook.small.annotation.component.BeforeDestruction;
import com.blackrook.small.exception.SmallFrameworkSetupException;
import com.blackrook.small.struct.Utils;

/**
 * A single instance of an instantiated component.
 * @author Matthew Tropiano
 */
public class SmallComponent
{
	protected static final Class<?>[] NO_FILTERS = new Class<?>[0];

	/** Object handler instance. */
	private Object instance;
	/** Method to invoke after environment initialization. */
	private Method afterInitialize;
	/** Method to invoke after component construction. */
	private Method afterConstruction;
	/** Method to invoke before environment destruction. */
	private Method beforeDestruction;

	/**
	 * Creates an entry point descriptor around an object instance.
	 * @param instance the instance to use.
	 */
	protected SmallComponent(Object instance)
	{
		this.instance = instance;
		this.afterInitialize = null;
		this.afterConstruction = null;
		this.beforeDestruction = null;
	}

	/**
	 * @return the instantiated component.
	 */
	public Object getInstance()
	{
		return instance;
	}

	/**
	 * Scans all methods for significance, both declared or inherited.
	 * @param clazz the instance class.
	 */
	void scanMethods()
	{
		for (Method m : instance.getClass().getMethods())
			scanMethod(m);
	}
	
	/**
	 * Invokes the {@link AfterInitialize} annotated methods.
	 */
	void invokeAfterInitializeMethods()
	{
		if (afterInitialize != null)
		{
			try {
				Utils.invoke(afterInitialize, instance);
			} catch (InvocationTargetException e) {
				throw new SmallFrameworkSetupException("Exception thrown from component " + instance.getClass() + " @AfterInitialize method!", e.getCause());
			}
		}
	}

	/**
	 * Invokes the {@link AfterConstruction} annotated methods.
	 */
	void invokeAfterConstructionMethods()
	{
		if (afterConstruction != null)
		{
			try {
				Utils.invoke(afterConstruction, instance);
			} catch (InvocationTargetException e) {
				throw new SmallFrameworkSetupException("Exception thrown from component " + instance.getClass() + " @AfterConstruction method!", e.getCause());
			}
		}
	}

	/**
	 * Invokes the {@link BeforeDestruction} annotated methods.
	 */
	void invokeBeforeDestructionMethods()
	{
		if (beforeDestruction != null)
		{
			try {
				Utils.invoke(beforeDestruction, instance);
			} catch (InvocationTargetException e) {
				throw new SmallFrameworkSetupException("Exception thrown from component " + instance.getClass() + " @BeforeDestruction method!", e.getCause());
			}
		}
	}

	/**
	 * Called to handle a single method scan for this component type.
	 * If this method is overridden, it would be wise to call <code>super.scanMethod(m)</code>.
	 * @param method the method to inspect.
	 */
	protected void scanMethod(Method method)
	{
		if (isValidAfterInitializeMethod(method))
		{
			if (afterInitialize != null)
				throw new SmallFrameworkSetupException("Method " + method.toString() + " is annotated with @AfterInitialize, but method " + afterInitialize.toString() + " is already annotated with it.");
			afterInitialize = method;
		}
		else if (method.isAnnotationPresent(AfterInitialize.class))
		{
			throw new SmallFrameworkSetupException("Method " + method.toString() + " is annotated with @AfterInitialize, but must be public, return void, and have no parameters.");
		}
		
		if (isValidAfterConstructionMethod(method))
		{
			if (afterConstruction != null)
				throw new SmallFrameworkSetupException("Method " + method.toString() + " is annotated with @AfterConstruction, but method " + afterConstruction.toString() + " is already annotated with it.");
			afterConstruction = method;
		}
		else if (method.isAnnotationPresent(AfterConstruction.class))
		{
			throw new SmallFrameworkSetupException("Method " + method.toString() + " is annotated with @AfterConstruction, but must be public, return void, and have no parameters.");
		}

		if (isValidBeforeDestructionMethod(method))
		{
			if (beforeDestruction != null)
				throw new SmallFrameworkSetupException("Method " + method.toString() + " is annotated with @BeforeDestruction, but method " + beforeDestruction.toString() + " is already annotated with it.");
			beforeDestruction = method;
		}
		else if (method.isAnnotationPresent(BeforeDestruction.class))
		{
			throw new SmallFrameworkSetupException("Method " + method.toString() + " is annotated with @BeforeDestruction, but must be public, return void, and have no parameters.");
		}
}
	
	private boolean isValidAfterInitializeMethod(Method method)
	{
		return
			method.isAnnotationPresent(AfterInitialize.class)
			&& (method.getModifiers() & Modifier.PUBLIC) != 0 
			&& (method.getReturnType() == Void.TYPE || method.getReturnType() == Void.class)
			&& (method.getParameterCount() == 0)
			;
	}
	
	private boolean isValidAfterConstructionMethod(Method method)
	{
		return
			method.isAnnotationPresent(AfterConstruction.class)
			&& (method.getModifiers() & Modifier.PUBLIC) != 0 
			&& (method.getReturnType() == Void.TYPE || method.getReturnType() == Void.class)
			&& (method.getParameterCount() == 0)
			;
	}
	
	private boolean isValidBeforeDestructionMethod(Method method)
	{
		return
			method.isAnnotationPresent(BeforeDestruction.class)
			&& (method.getModifiers() & Modifier.PUBLIC) != 0 
			&& (method.getReturnType() == Void.TYPE || method.getReturnType() == Void.class)
			&& (method.getParameterCount() == 0)
			;
	}
	
}
