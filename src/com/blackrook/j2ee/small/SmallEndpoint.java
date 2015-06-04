package com.blackrook.j2ee.small;

import java.util.Random;

import javax.websocket.server.ServerEndpoint;

import com.blackrook.j2ee.small.annotation.Component;

/**
 * A base Endpoint that contains useful stuff for instantiated endpoints.
 * Endpoints that extend this class should use the {@link ServerEndpoint} annotation to declare itself.
 * <p>
 * You should use this class for finding components created in Small, as this provides access for retrieving
 * those components once this class is instantiated by the Servlet Container (see {@link #getComponent(Class)}. 
 * @author Matthew Tropiano
 */
public class SmallEndpoint
{
	/** Alphabet for generating unique identifiers. */
	private static final String ID_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	
	/** This endpoint's unique id. */
	private String id;
	
	// Default Endpoint constructor.
	protected SmallEndpoint()
	{
		// generate random id string.
		StringBuilder sb = new StringBuilder();
		Random r = new Random();
		for (int i = 0; i < 32; i++)
			sb.append(ID_ALPHABET.charAt(r.nextInt(ID_ALPHABET.length())));
		id = sb.toString();
	}
	
	/**
	 * Returns this endpoint's unique id.
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * Gets a singleton component annotated with {@link Component} by class.
	 */
	protected <T> T getComponent(Class<T> componentClass)
	{
		return SmallToolkit.INSTANCE.getComponent(componentClass);
	}
}
