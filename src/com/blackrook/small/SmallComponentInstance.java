package com.blackrook.small;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import com.blackrook.small.annotation.attribs.Attribute;
import com.blackrook.small.annotation.attribs.Model;
import com.blackrook.small.struct.Part;
import com.blackrook.small.struct.Utils;

/**
 * A single instance of an instantiated component.
 * @author Matthew Tropiano
 */
public abstract class SmallComponentInstance
{
	protected static final Class<?>[] NO_FILTERS = new Class<?>[0];

	/** Object handler instance. */
	private Object instance;
	/** Model map. */
	private Map<String, SmallEntryPoint<?>> modelMap;
	/** Attribute map. */
	private Map<String, SmallEntryPoint<?>> attributeMap;

	/**
	 * Creates an entry point descriptor around an object instance.
	 * @param instance the instance to use.
	 */
	public SmallComponentInstance(Object instance)
	{
		this.instance = instance; 
		this.modelMap = new HashMap<>(3);
		this.attributeMap = new HashMap<>(3);
	}

	/**
	 * Scans all methods for entry points.
	 * @param clazz the instance class.
	 */
	protected void scanMethods(Class<?> clazz)
	{
		for (Method m : clazz.getMethods())
		{
			if (isModelConstructorMethod(m))
			{
				Model anno = m.getAnnotation(Model.class);
				modelMap.put(anno.value(), new SmallEntryPoint<>(this, m));
			}
			else if (isAttributeConstructorMethod(m))
			{
				Attribute anno = m.getAnnotation(Attribute.class);
				attributeMap.put(anno.value(), new SmallEntryPoint<>(this, m));
			}
			else
				scanUnknownMethod(m);
		}
	}
	
	/**
	 * Called by {@link #scanMethods(Class)} to handle non-model, non-attribute creation methods.
	 */
	protected abstract void scanUnknownMethod(Method method);
	
	/**
	 * Returns the instantiated component.
	 */
	public Object getInstance()
	{
		return instance;
	}

	/**
	 * Gets a method on this object that constructs an attribute. 
	 * @param attribName the attribute name.
	 */
	public SmallEntryPoint<?> getAttributeConstructor(String attribName)
	{
		return attributeMap.get(attribName);
	}

	/**
	 * Gets a method on this object that constructs a model object. 
	 * @param modelName the model attribute name.
	 */
	public SmallEntryPoint<?> getModelConstructor(String modelName)
	{
		return modelMap.get(modelName);
	}

	/**
	 * Converts a Multipart Part.
	 */
	public <T> T getPartData(Part part, Class<T> type)
	{
		if (Part.class.isAssignableFrom(type))
			return type.cast(part);
		else if (String.class.isAssignableFrom(type))
			return type.cast(part.isFile() ? part.getFileName() : part.getValue());
		else if (File.class.isAssignableFrom(type))
			return type.cast(part.isFile() ? part.getFile() : null);
		else
			return Utils.createForType(part.isFile() ? null : part.getValue(), type);
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
