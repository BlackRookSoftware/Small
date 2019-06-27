package com.blackrook.j2ee.small;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;

import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.blackrook.j2ee.small.annotation.Component;
import com.blackrook.j2ee.small.exception.SmallFrameworkException;
import com.blackrook.j2ee.small.parser.JSONDriver;

/**
 * A base Endpoint that contains useful stuff for instantiated endpoints.
 * Endpoints that extend this class should use the {@link ServerEndpoint} annotation to declare itself.
 * <p>
 * You should use this class for finding components created in Small, as this provides access for retrieving
 * those components once this class is instantiated by the Servlet Container (see {@link #getComponent(Class)}. 
 * @author Matthew Tropiano
 */
public abstract class SmallEndpoint extends Endpoint
{
	/** This endpoint's session. */
	private Session session;
	/** This endpoint's reference to the Small Environment. */
	private SmallEnvironment environment;
	
	/**
	 * Convenience method for getting this endpoint's ID.
	 * @return this endpoint's unique id.
	 */
	public String getId()
	{
		return session.getId();
	}

	/**
	 * When this is called, this gets the {@link SmallEnvironment} from the config
	 * and saves the session on this class.
	 */
	@Override
	public void onOpen(Session session, EndpointConfig config)
	{
		this.session = session;
		this.environment = (SmallEnvironment)config.getUserProperties().get(SmallConstants.SMALL_APPLICATION_ENVIRONMENT_ARTTRIBUTE);
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
	 * @return the Small environment.
	 */
	protected SmallEnvironment getEnvironment()
	{
		return environment;
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
	 * @param object the object to pass back to the connected client.
	 * @throws SmallFrameworkException on a send error.
	 */
	public void sendJSON(Object object)
	{
		try {
			StringWriter sw = new StringWriter(1024);
			getJSONDriver().toJSON(sw, object);
			session.getBasicRemote().sendText(sw.toString());
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
		try {
			sendBinary(ByteBuffer.wrap(buffer));
		} catch (Exception e) {
			throw new SmallFrameworkException(e);
		}
	}

	/**
	 * Sends binary data, synchronously, to the client, hinting that 
	 * it may not be the last one in the complete message.
	 * Execution halts until the client socket acknowledges receipt. 
	 * This method should not be used for repeated sends, as the array is wrapped in a ByteBuffer
	 * before it is sent. 
	 * @param buffer the buffer of data to pass back to the connected client.
	 * @throws SmallFrameworkException on a send error.
	 */
	public void sendBinaryPartial(byte[] buffer, boolean isLast)
	{
		try {
			sendBinaryPartial(ByteBuffer.wrap(buffer), isLast);
		} catch (Exception e) {
			throw new SmallFrameworkException(e);
		}
	}

	/**
	 * Sends a stream of data, synchronously, until the end of the stream.
	 * The stream is not closed after this completes.
	 * Execution halts until the client socket acknowledges receipt. 
	 * @param in the input stream to read from.
	 * @param bufferSize the size of the buffer to use during the send.
	 * @throws SmallFrameworkException on a send or read error.
	 */
	public void sendData(InputStream in, int bufferSize)
	{
		try {
			ByteBuffer bb = ByteBuffer.allocate(bufferSize);
			byte[] buffer = new byte[bufferSize];
			int buf = 0;
			while ((buf = in.read(buffer)) >= 0)
			{
				bb.put(buffer);
				bb.rewind();
				sendBinaryPartial(buffer, buf < bufferSize);
				bb.rewind();
			}
		} catch (Exception e) {
			throw new SmallFrameworkException(e);
		}
	}

	/**
	 * Sends the contents of a file as a stream of data, synchronously, until the end of the file.
	 * Execution halts until the client socket acknowledges receipt. 
	 * @param file the file to read.
	 * @param bufferSize the size of the buffer to use during the send.
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
	 * @throws SmallFrameworkException on a send error.
	 */
	public void sendAsyncJSON(Object object)
	{
		try {
			StringWriter sw = new StringWriter(1024);
			getJSONDriver().toJSON(sw, object);
			sendAsyncText(sw.toString());
		} catch (Exception e) {
			throw new SmallFrameworkException(e);
		}
	}

	/**
	 * Sends binary data, asynchronously, to the client.
	 * @param buffer the buffer of data to pass back to the connected client.
	 * @return the {@link Future} object to monitor the sent request after the call.
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
	 * @throws SmallFrameworkException on a send error.
	 */
	public Future<Void> sendAsyncBinary(byte[] buffer)
	{
		return sendAsyncBinary(ByteBuffer.wrap(buffer));
	}

}
