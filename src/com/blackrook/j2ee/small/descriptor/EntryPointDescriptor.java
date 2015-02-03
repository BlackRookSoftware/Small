package com.blackrook.j2ee.small.descriptor;

/**
 * Interface for descriptors for component objects. 
 * @author Matthew Tropiano
 */
public interface EntryPointDescriptor
{

	/**
	 * Gets a method on the controller that constructs an attribute. 
	 * @param attribName the attribute name.
	 */
	public MethodDescriptor getAttributeConstructor(String attribName);

	/**
	 * Gets a method on the controller that constructs a model object. 
	 * @param modelName the model attribute name.
	 */
	public MethodDescriptor getModelConstructor(String modelName);

	/**
	 * Returns the instantiated component.
	 */
	public Object getInstance();

}
