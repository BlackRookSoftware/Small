package com.blackrook.j2ee;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.blackrook.commons.Common;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.j2ee.annotation.Attribute;
import com.blackrook.j2ee.annotation.Controller;
import com.blackrook.j2ee.annotation.Model;
import com.blackrook.j2ee.annotation.ControllerEntry;
import com.blackrook.j2ee.component.ViewResolver;
import com.blackrook.j2ee.enums.RequestMethod;
import com.blackrook.j2ee.exception.SimpleFrameworkException;
import com.blackrook.j2ee.exception.SimpleFrameworkSetupException;

/**
 * Creates a controller profile to assist in re-calling controllers by path and methods.
 * @author Matthew Tropiano
 */
class ControllerDescriptor
{
	private static final Class<?>[] NO_FILTERS = new Class<?>[0];
	
	/** Controller instance. */
	private Object instance;
	/** Controller annotation. */
	private Class<? extends ViewResolver> viewResolverClass;
	/** Singular method map. */
	private HashMap<RequestMethod, ControllerMethodDescriptor> defaultMethodMap;
	/** Method map. */
	private HashMap<RequestMethod, HashMap<String, ControllerMethodDescriptor>> methodMap;
	/** Model map. */
	private HashMap<String, ControllerMethodDescriptor> modelMap;
	/** Attribute map. */
	private HashMap<String, ControllerMethodDescriptor> attributeMap;
	/** Filter class list. */
	private Class<?>[] filterChain;
	
	/**
	 * Creates the controller profile for a {@link Controller} class.
	 * @param clazz the input class to profile.
	 * @throws SimpleFrameworkException if this profile cannot be created due to an initialization problem.
	 */
	ControllerDescriptor(Class<?> clazz)
	{
		Controller controllerAnnotation = clazz.getAnnotation(Controller.class);
		if (controllerAnnotation == null)
			throw new SimpleFrameworkSetupException("Class "+clazz.getName()+" is not annotated with @Controller.");
		
		this.filterChain = NO_FILTERS;
		this.defaultMethodMap = new HashMap<RequestMethod, ControllerMethodDescriptor>();
		this.methodMap = new HashMap<RequestMethod, HashMap<String, ControllerMethodDescriptor>>();
		this.modelMap = new HashMap<String, ControllerMethodDescriptor>();
		this.attributeMap = new HashMap<String, ControllerMethodDescriptor>();
		this.viewResolverClass = controllerAnnotation.viewResolver();
		
		String methodPrefix = controllerAnnotation.methodPrefix();
		
		// TODO: Add filter chain handling.
		
		for (Method m : clazz.getMethods())
		{
			if (isEntryMethod(m, methodPrefix))
			{
				ControllerEntry anno = m.getAnnotation(ControllerEntry.class);
				if (Common.isEmpty(anno.value()))
					continue;
				
				String methodName = m.getName();
				String pagename = Character.toLowerCase(methodName.charAt(methodPrefix.length())) + 
						methodName.substring(methodPrefix.length() + 1);
				
				ControllerMethodDescriptor md = new ControllerMethodDescriptor(m);
				
				for (RequestMethod rm : anno.value())
				{
					if (anno.defaultEntry())
					{
						if (defaultMethodMap.containsKey(rm))
							throw new SimpleFrameworkSetupException("Controller already contains a default entry for this request method.");
						else
							defaultMethodMap.put(rm, md);
					}
					else
					{
						HashMap<String, ControllerMethodDescriptor> map = null;
						if ((map = methodMap.get(rm)) == null)
							methodMap.put(rm, map = new HashMap<String, ControllerMethodDescriptor>(4));
						map.put(pagename, md);
					}
				}
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
		
		instance = FrameworkUtil.getBean(clazz);
	}
	
	/** Checks if a method is a valid request entry. */
	private boolean isEntryMethod(Method method, String methodPrefix)
	{
		return
			(method.getModifiers() & Modifier.PUBLIC) != 0 
			&& method.getName().startsWith(methodPrefix)
			&& method.getName().length() > methodPrefix.length()
			&& method.isAnnotationPresent(ControllerEntry.class)
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
	
	/**
	 * Get the base page name parsed out of the page.
	 */
	private final String getPageNoExtension(String page)
	{
		int endIndex = page.indexOf('.');
		if (endIndex >= 0)
			return page.substring(0, endIndex);
		else
			return page; 
	}

	/**
	 * Returns the class of the view resolver that this controller uses.
	 */
	public Class<? extends ViewResolver> getViewResolverClass()
	{
		return viewResolverClass;
	}

	/**
	 * Gets a method on the controller using the specified page string. 
	 * @param rm the request method.
	 * @param pageString the page name (no extension).
	 */
	public ControllerMethodDescriptor getDescriptorUsingPath(RequestMethod rm, String pageString)
	{
		HashMap<String, ControllerMethodDescriptor> map = methodMap.get(rm);
		if (map == null)
			return defaultMethodMap.get(rm);
		
		ControllerMethodDescriptor out = map.get(getPageNoExtension(pageString));
		if (out == null)
			return defaultMethodMap.get(rm);
		
		return out;
	}
	
	/**
	 * Gets a method on the controller that constructs an attribute. 
	 * @param attribName the attribute name.
	 */
	public ControllerMethodDescriptor getAttributeConstructor(String attribName)
	{
		return attributeMap.get(attribName);
	}
	
	/**
	 * Gets a method on the controller that constructs a model object. 
	 * @param modelName the model attribute name.
	 */
	public ControllerMethodDescriptor getModelConstructor(String modelName)
	{
		return modelMap.get(modelName);
	}
	
	/**
	 * Returns the instantiated controller.
	 */
	public Object getInstance()
	{
		return instance;
	}

	/**
	 * Returns the list of filters to call.
	 */
	public Class<?>[] getFilterChain()
	{
		return filterChain;
	}
	
}
