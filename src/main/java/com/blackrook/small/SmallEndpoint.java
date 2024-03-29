/*******************************************************************************
 * Copyright (c) 2020-2022 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Future;

import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.blackrook.small.annotation.Component;
import com.blackrook.small.exception.SmallFrameworkException;
import com.blackrook.small.roles.JSONDriver;
import com.blackrook.small.roles.XMLDriver;
import com.blackrook.small.util.SmallUtils;

/**
 * A base websocket endpoint that automatically has a connection to the Small application environment.
 * Endpoints that extend this class should use the {@link ServerEndpoint} annotation to declare itself.
 * <p>
 * You should use this class for finding components created in Small, as this provides access for retrieving
 * those components once this class is instantiated by the Servlet Container (see {@link #getComponent(Class)}. 
 * @author Matthew Tropiano
 */
public abstract class SmallEndpoint extends Endpoint
{
	private static final ThreadLocal<DataBuffer> BUFFER = ThreadLocal.withInitial(() -> new DataBuffer());
	
	/** Default buffer size. */
	public static final int DEFAULT_BUFFER_SIZE = 4096;
	
	/** This endpoint's session. */
	private Session session;
	/** This endpoint's reference to the Small Environment. */
	private SmallEnvironment environment;
	
	/**
	 * When this is called, this gets the {@link SmallEnvironment} from the config
	 * and saves the session on this class. This method is already annotated with {@link OnOpen},
	 * and calls {@link #afterOpen(EndpointConfig)} to set up configuration after the {@link Session}
	 * and the {@link SmallEnvironment} is set on the endpoint.
	 */
	@OnOpen
	public final void onOpen(Session session, EndpointConfig config)
	{
		this.session = session;
		this.environment = SmallUtils.getEnvironment(config);
		afterOpen(config);
	}
	
	/**
	 * Called after the session and environment setup in the 
	 * {@link #onOpen(Session, EndpointConfig)} method happens.
	 * Does nothing by default.
	 * See {@link #getSession()} for getting the socket session.
	 * See {@link #getEnvironment()} for setting up references to components.
	 * @param config the endpoint configuration.
	 */
	protected void afterOpen(EndpointConfig config)
	{
		// Do nothing, by default.
	}

	/**
	 * @return gets the session for this websocket.
	 */
	protected Session getSession()
	{
		return session;
	}
	
	/**
	 * @return the Small environment.
	 */
	protected SmallEnvironment getEnvironment()
	{
		return environment;
	}

	/**
	 * Gets this application's JSON converter driver.
	 * @return the instantiated driver.
	 */
	protected JSONDriver getJSONDriver()
	{
		return environment.getJSONDriver();
	}

	/**
	 * Gets this application's XML converter driver.
	 * @return the instantiated driver.
	 */
	protected XMLDriver getXMLDriver()
	{
		return environment.getXMLDriver();
	}

	/**
	 * @return the temporary directory to use for multipart files.
	 */
	protected File getTemporaryDirectory()
	{
		return environment.getTemporaryDirectory();
	}

	/**
	 * Returns a singleton component instantiated by Small.
	 * @param componentClass the component class.
	 * @param <T> the object type.
	 * @return a singleton component annotated with {@link Component} by class.
	 */
	protected <T> T getComponent(Class<T> componentClass)
	{
		return getEnvironment().getComponent(componentClass);
	}

	/**
	 * Convenience method for getting this endpoint's ID.
	 * <p><code>getSession().getId()</code>
	 * @return this endpoint's unique id.
	 */
	public String getId()
	{
		return getSession().getId();
	}

	/**
	 * Convenience method for getting the URI that created this endpoint.
	 * <p><code>getSession().getRequestURI()</code>
	 * @return this endpoint's constructor URI.
	 * @since 1.6.0
	 */
	public URI getURI()
	{
		return getSession().getRequestURI();
	}
	
	/**
	 * Convenience method for getting a path variable from the endpoint URI.
	 * <p><code>getSession().getPathParameters().get(variableName)</code>
	 * @param variableName the variable name.
	 * @return the corresponding value, or null if no value.
	 * @since 1.6.0
	 */
	public String getPathVariable(String variableName)
	{
		return getSession().getPathParameters().get(variableName);
	}
	
	/**
	 * Convenience method for getting a list of query parameter values from the endpoint URI.
	 * <p><code>getSession().getRequestParameterMap().get(parameterName)</code>
	 * @param parameterName the parameter name.
	 * @return the corresponding list of values, or null if no such parameter.
	 * @since 1.6.0
	 */
	public List<String> getParameters(String parameterName)
	{
		return getSession().getRequestParameterMap().get(parameterName);
	}
	
	/**
	 * Convenience method for getting a query parameter from the endpoint URI.
	 * This only returns the last defined one.
	 * <p><code>getSession().getRequestParameterMap().get(parameterName)</code>
	 * @param parameterName the parameter name.
	 * @return the corresponding value, or null if no such parameter.
	 * @since 1.6.0
	 */
	public String getParameter(String parameterName)
	{
		List<String> list = getParameters(parameterName);
		return list != null && !list.isEmpty() ? list.get(list.size() - 1) : null;
	}
	
	/**
	 * Convenience method for getting the query string from the request that created this.
	 * <p><code>getSession().getQueryString()</code>
	 * @return the query string.
	 * @since 1.6.0
	 */
	public String getQueryString()
	{
		return getSession().getQueryString();
	}
	
	/**
	 * Sends a message string, synchronously, to the client.
	 * Execution halts until the client socket acknowledges receipt. 
	 * @param message the message string to pass back to the connected client.
	 * @throws SmallFrameworkException on a send error.
	 */
	public void sendText(String message)
	{
		try {
			session.getBasicRemote().sendText(message);
		} catch (Exception e) {
			throw new SmallFrameworkException(e);
		}
	}

	/**
	 * Sends a message string, synchronously, to the client, hinting that 
	 * it may not be the last one in the complete message.
	 * Execution halts until the client socket acknowledges receipt. 
	 * @param message the (partial) message string to pass back to the connected client.
	 * @param isLast if true, tells the client that this is the final part. if false, it is not the last part.
	 * @throws SmallFrameworkException on a send error.
	 */
	public void sendTextPartial(String message, boolean isLast)
	{
		try {
			session.getBasicRemote().sendText(message, isLast);
		} catch (Exception e) {
			throw new SmallFrameworkException(e);
		}
	}

	/**
	 * Sends a JSON Object, synchronously, to the client.
	 * Execution halts until the client socket acknowledges receipt. 
	 * @param object the object to pass back to the connected client (converted to JSON via the {@link JSONDriver}).
	 * @throws SmallFrameworkException on a send error.
	 */
	public void sendJSON(Object object)
	{
		try {
			session.getBasicRemote().sendText(getJSONDriver().toJSONString(object));
		} catch (Exception e) {
			throw new SmallFrameworkException(e);
		}
	}

	/**
	 * Sends an XML Document, synchronously, to the client.
	 * Execution halts until the client socket acknowledges receipt. 
	 * @param object the object to pass back to the connected client (converted to XML via the {@link XMLDriver}).
	 * @throws SmallFrameworkException on a send error.
	 */
	public void sendXML(Object object)
	{
		try {
			session.getBasicRemote().sendText(getXMLDriver().toXMLString(object));
		} catch (Exception e) {
			throw new SmallFrameworkException(e);
		}
	}

	/**
	 * Sends binary data, synchronously, to the client.
	 * Execution halts until the client socket acknowledges receipt. 
	 * @param buffer the buffer of data to pass back to the connected client.
	 * @throws SmallFrameworkException on a send error.
	 */
	public void sendBinary(ByteBuffer buffer)
	{
		try {
			session.getBasicRemote().sendBinary(buffer);
		} catch (Exception e) {
			throw new SmallFrameworkException(e);
		}
	}

	/**
	 * Sends binary data, synchronously, to the client, hinting that 
	 * it may not be the last one in the complete message.
	 * Execution halts until the client socket acknowledges receipt. 
	 * @param buffer the buffer of data to pass back to the connected client.
	 * @param isLast if true, this is the last part of the message.
	 * @throws SmallFrameworkException on a send error.
	 */
	public void sendBinaryPartial(ByteBuffer buffer, boolean isLast)
	{
		try {
			session.getBasicRemote().sendBinary(buffer, isLast);
		} catch (Exception e) {
			throw new SmallFrameworkException(e);
		}			
	}

	/**
	 * Sends binary data, synchronously, to the client.
	 * Execution halts until the client socket acknowledges receipt.
	 * This method should not be used for repeated sends, as the array is wrapped in a ByteBuffer
	 * before it is sent. 
	 * @param buffer the buffer of data to pass back to the connected client.
	 * @throws SmallFrameworkException on a send error.
	 */
	public void sendBinary(byte[] buffer)
	{
		sendData(new ByteArrayInputStream(buffer));
	}

	/**
	 * Sends binary data, synchronously, to the client, hinting that 
	 * it may not be the last one in the complete message.
	 * Execution halts until the client socket acknowledges receipt. 
	 * This method should not be used for repeated sends, as the array is wrapped in a ByteBuffer
	 * before it is sent. 
	 * @param buffer the buffer of data to pass back to the connected client.
	 * @param isLast if true, this is the last part of the message.
	 * @throws SmallFrameworkException on a send error.
	 */
	public void sendBinaryPartial(byte[] buffer, boolean isLast)
	{
		sendBinaryPartial(ByteBuffer.wrap(buffer), isLast);
	}

	/**
	 * Sends a stream of data, synchronously, until the end of the stream.
	 * The stream is not closed after this completes.
	 * Execution halts until the client socket acknowledges receipt.
	 * <p>
	 * The buffer size used for this socket is {@link #DEFAULT_BUFFER_SIZE}.
	 * @param in the input stream to read from.
	 * @throws SmallFrameworkException on a send or read error.
	 * @since 1.6.0
	 */
	public void sendData(InputStream in)
	{
		sendData(in, DEFAULT_BUFFER_SIZE);
	}

	/**
	 * Sends a stream of data, synchronously, until the end of the stream.
	 * The stream is not closed after this completes.
	 * Execution halts until the client socket acknowledges receipt. 
	 * @param in the input stream to read from.
	 * @param bufferSize the size of the buffer to use during the send.
	 * @throws IllegalArgumentException if bufferSize is 0 or less.
	 * @throws SmallFrameworkException on a send or read error.
	 */
	public void sendData(InputStream in, int bufferSize)
	{
		try {
			DataBuffer db = BUFFER.get().setCapacity(bufferSize);
			ByteBuffer bb = db.buffer;
			byte[] buffer = db.array;
			
			int buf = 0;
			while ((buf = in.read(buffer)) >= 0)
			{
				bb.put(buffer);
				bb.flip();
				sendBinaryPartial(bb, buf < bufferSize);
				bb.clear();
			}
		} catch (Exception e) {
			throw new SmallFrameworkException(e);
		}
	}

	/**
	 * Sends the contents of a file as a stream of data, synchronously, until the end of the file.
	 * Execution halts until the client socket acknowledges receipt. 
	 * @param file the file to read.
	 * @throws SmallFrameworkException on a send or read error.
	 * @since 1.6.0
	 */
	public void sendFileContents(File file)
	{
		sendFileContents(file, DEFAULT_BUFFER_SIZE);
	}

	/**
	 * Sends the contents of a file as a stream of data, synchronously, until the end of the file.
	 * Execution halts until the client socket acknowledges receipt. 
	 * @param file the file to read.
	 * @param bufferSize the size of the buffer to use during the send.
	 * @throws IllegalArgumentException if bufferSize is 0 or less.
	 * @throws SmallFrameworkException on a send or read error.
	 */
	public void sendFileContents(File file, int bufferSize)
	{
		try (BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file))) {
			sendData(fis, bufferSize);
		} catch (IOException e) {
			throw new SmallFrameworkException(e);
		}
	}

	/**
	 * Sends a message string, asynchronously, to the client.
	 * @param message the message string to pass back to the connected client.
	 * @return the {@link Future} object to monitor the sent request after the call.
	 * @throws SmallFrameworkException on a send error.
	 */
	public Future<Void> sendAsyncText(String message)
	{
		return session.getAsyncRemote().sendText(message);
	}

	/**
	 * Sends a JSON Object, asynchronously, to the client.
	 * @param object the object to pass back to the connected client.
	 * @return the {@link Future} object to monitor the sent request after the call.
	 * @throws SmallFrameworkException on a send error.
	 */
	public Future<Void> sendAsyncJSON(Object object)
	{
		try {
			return sendAsyncText(getJSONDriver().toJSONString(object));
		} catch (Exception e) {
			throw new SmallFrameworkException(e);
		}
	}

	/**
	 * Sends a JSON Object, asynchronously, to the client.
	 * @param object the object to pass back to the connected client (converted to XML via the {@link XMLDriver}).
	 * @return the {@link Future} object to monitor the sent request after the call.
	 * @throws SmallFrameworkException on a send error.
	 */
	public Future<Void> sendAsyncXML(Object object)
	{
		try {
			return sendAsyncText(getXMLDriver().toXMLString(object));
		} catch (Exception e) {
			throw new SmallFrameworkException(e);
		}
	}

	/**
	 * Sends binary data, asynchronously, to the client.
	 * @param buffer the buffer of data to pass back to the connected client.
	 * @return the {@link Future} object to monitor the sent request after the call.
	 * @throws IllegalArgumentException if buffer is null.
	 */
	public Future<Void> sendAsyncBinary(ByteBuffer buffer)
	{
		return session.getAsyncRemote().sendBinary(buffer);
	}

	/**
	 * Sends binary data, asynchronously, to the client.
	 * This method should not be used for repeated sends, as the array is wrapped in a ByteBuffer
	 * before it is sent. 
	 * @param buffer the buffer of data to pass back to the connected client.
	 * @return the {@link Future} object to monitor the sent request after the call.
	 * @throws IllegalArgumentException if buffer is null.
	 */
	public Future<Void> sendAsyncBinary(byte[] buffer)
	{
		return sendAsyncBinary(ByteBuffer.wrap(buffer));
	}

	private static class DataBuffer
	{
		private ByteBuffer buffer;
		private byte[] array;
		
		public DataBuffer()
		{
			this.buffer = null;
			this.array = null;
			setCapacity(DEFAULT_BUFFER_SIZE);
		}
		
		private DataBuffer setCapacity(int cap)
		{
			if (cap <= 0)
			{
				throw new IllegalArgumentException("buffer size cannot be 0 or less.");
			}
			
			if (array == null || array.length != cap)
			{
				this.buffer = ByteBuffer.allocate(cap);
				this.array = new byte[cap];
			}
			
			return this;
		}
		
	}
	
}
