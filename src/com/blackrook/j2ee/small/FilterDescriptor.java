package com.blackrook.j2ee.small;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.blackrook.commons.Reflect;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.j2ee.small.annotation.Attribute;
import com.blackrook.j2ee.small.annotation.Filter;
import com.blackrook.j2ee.small.annotation.FilterEntry;
import com.blackrook.j2ee.small.annotation.Model;
import com.blackrook.j2ee.small.exception.SmallFrameworkException;
import com.blackrook.j2ee.small.exception.SmallFrameworkSetupException;

/**
 * Creates a controller profile to assist in re-calling filters.
 * @author Matthew Tropiano
 */
class FilterDescriptor implements ComponentDescriptor
{
	/** Filter instance. */
	private Object instance;
	/** Method descriptor for filter. */
	private MethodDescriptor methodDescriptor;
	/** Model map. */
	private HashMap<String, MethodDescriptor> modelMap;
	/** Attribute map. */
	private HashMap<String, MethodDescriptor> attributeMap;

	/**
	 * Creates the filter profile for a {@link Filter} class.
	 * @param clazz the input class to profile.
	 * @throws SmallFrameworkException if this profile cannot be created due to an initialization problem.
	 */
	FilterDescriptor(Class<?> clazz)
	{
		Filter controllerAnnotation = clazz.getAnnotation(Filter.class);
		if (controllerAnnotation == null)
			throw new SmallFrameworkSetupException("Class "+clazz.getName()+" is not annotated with @Filter.");

		this.methodDescriptor = null; 
		this.modelMap = new HashMap<String, MethodDescriptor>(3);
		this.attributeMap = new HashMap<String, MethodDescriptor>(3);

		for (Method m : clazz.getMethods())
		{
			if (isEntryMethod(m))
			{
				if (m.getReturnType() != Boolean.TYPE && m.getReturnType() != Boolean.class)
					throw new SmallFrameworkSetupException("Methods annotated with @FilterEntry must return a boolean.");
				else if (this.methodDescriptor != null)
					throw new SmallFrameworkSetupException("Filter already contains an entry point.");
				this.methodDescriptor = new MethodDescriptor(m);
			}
			else if (isModelConstructorMethod(m))
			{
				Model anno = m.getAnnotation(Model.class);
				modelMap.put(anno.value(), new ControllerMethodDescriptor(m));
			}
			else if (isAttributeConstructorMethod(m))
			{
				Attribute anno = m.getAnnotation(Attribute.class);
				attributeMap.put(anno.value(), new ControllerMethodDescriptor(m));
			}
		}
		
		this.instance = Reflect.create(clazz);
	}

	/**
	 * Returns the instantiated filter.
	 */
	public Object getInstance()
	{
		return instance;
	}

	/**
	 * Returns the method descriptor 
	 */
	public MethodDescriptor getMethodDescriptor()
	{
		return methodDescriptor;
	}

	@Override
	public MethodDescriptor getAttributeConstructor(String attribName)
	{
		return attributeMap.get(attribName);
	}

	@Override
	public MethodDescriptor getModelConstructor(String modelName)
	{
		return modelMap.get(modelName);
	}

	/** Checks if a method is a valid request entry. */
	private boolean isEntryMethod(Method method)
	{
		return
			(method.getModifiers() & Modifier.PUBLIC) != 0 
			&& method.isAnnotationPresent(FilterEntry.class)
			;
	}
	
	/** Checks if a method is a Model constructor. */
	private boolean isModelConstructorMethod(Method method)
	{
		return
			(method.getModifiers() & Modifier.PUBLIC) != 0 
			&& method.getReturnType() != Void.TYPE 
			&& method.getReturnType() != Void.class
			&& method.isAnnotationPresent(Model.class)
			;
	}
	
	/** Checks if a method is an Attribute constructor. */
	private boolean isAttributeConstructorMethod(Method method)
	{
		return
			(method.getModifiers() & Modifier.PUBLIC) != 0 
			&& method.getReturnType() != Void.TYPE 
			&& method.getReturnType() != Void.class
			&& method.isAnnotationPresent(Attribute.class)
			;
	}
	

}
