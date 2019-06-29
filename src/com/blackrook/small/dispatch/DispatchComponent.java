package com.blackrook.small.dispatch;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import com.blackrook.small.SmallComponent;
import com.blackrook.small.annotation.dispatch.Attribute;
import com.blackrook.small.annotation.dispatch.Model;
import com.blackrook.small.exception.SmallFrameworkSetupException;
import com.blackrook.small.struct.Part;
import com.blackrook.small.struct.Utils;

/**
 * A component involved in HTTP request dispatching and handling.
 * @author Matthew Tropiano
 */
public abstract class DispatchComponent extends SmallComponent
{
	/** Model map. */
	private Map<String, DispatchEntryPoint<?>> modelMap;
	/** Attribute map. */
	private Map<String, DispatchEntryPoint<?>> attributeMap;

	protected DispatchComponent(Object instance)
	{
		super(instance);
		this.modelMap = new HashMap<>(3);
		this.attributeMap = new HashMap<>(3);
	}

	@Override
	protected void scanMethod(Method method)
	{
		if (isValidModelConstructorMethod(method))
		{
			Model anno = method.getAnnotation(Model.class);
			modelMap.put(anno.value(), new DispatchEntryPoint<>(this, method));
		}
		else if (method.isAnnotationPresent(Model.class))
		{
			throw new SmallFrameworkSetupException("Method " + method.toString() + " is annotated with @Model, but must be public and cannot return void.");
		}
		else if (isValidAttributeConstructorMethod(method))
		{
			Attribute anno = method.getAnnotation(Attribute.class);
			attributeMap.put(anno.value(), new DispatchEntryPoint<>(this, method));
		}
		else if (method.isAnnotationPresent(Attribute.class))
		{
			throw new SmallFrameworkSetupException("Method " + method.toString() + " is annotated with @Attribute, but must be public and cannot return void.");
		}
		super.scanMethod(method);
	}

	/**
	 * Gets a method on this object that constructs an attribute. 
	 * @param attribName the attribute name.
	 */
	public DispatchEntryPoint<?> getAttributeConstructor(String attribName)
	{
		return attributeMap.get(attribName);
	}

	/**
	 * Gets a method on this object that constructs a model object. 
	 * @param modelName the model attribute name.
	 */
	public DispatchEntryPoint<?> getModelConstructor(String modelName)
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
	private boolean isValidModelConstructorMethod(Method method)
	{
		return
			method.isAnnotationPresent(Model.class)
			&& (method.getModifiers() & Modifier.PUBLIC) != 0 
			&& method.getReturnType() != Void.TYPE 
			&& method.getReturnType() != Void.class
			;
	}

	/** Checks if a method is an Attribute constructor. */
	private boolean isValidAttributeConstructorMethod(Method method)
	{
		return
			method.isAnnotationPresent(Attribute.class)
			&& (method.getModifiers() & Modifier.PUBLIC) != 0 
			&& method.getReturnType() != Void.TYPE 
			&& method.getReturnType() != Void.class
			;
	}
	
}
