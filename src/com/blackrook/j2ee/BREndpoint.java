package com.blackrook.j2ee;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;

import javax.websocket.Session;

import com.blackrook.commons.Common;
import com.blackrook.lang.json.JSONConversionException;
import com.blackrook.lang.json.JSONObject;
import com.blackrook.lang.json.JSONWriter;

/**
 * A WebSocket endpoint helper class that exposes some useful functions to facilitate endpoint creation.
 * @author Matthew Tropiano
 */
public abstract class BREndpoint
{
	// Default Endpoint constructor.
	protected BREndpoint()
	{
		
		}
	
	private BRToolkit getToolkit()
	{
		return BRToolkit.INSTANCE;
		}

	/**
	 * Gets a file that is on the application path. 
	 * @param path the path to the file to get.
	 * @return a file representing the specified resource or null if it couldn't be found.
	 */
	protected final File getApplicationFile(String path)
	{
		return getToolkit().getApplicationFile(path);
		}

	/**
	 * Gets a file path that is on the application path. 
	 * @param relativepath the relative path to the file to get.
	 * @return a file representing the specified resource or null if it couldn't be found.
	 */
	protected final String getApplicationFilePath(String relativepath)
	{
		return getToolkit().getApplicationFilePath(relativepath);
		}

	/**
	 * Opens an input stream to a resource using a path relative to the
	 * application context path. 
	 * Outside users should not be able to access this!
	 * @param path the path to the resource to open.
	 * @return an open input stream to the specified resource or null if it couldn't be opened.
	 * @throws IOException if the stream cannot be opened.
	 */
	protected final InputStream getResourceAsStream(String path) throws IOException
	{
		return getToolkit().getResourceAsStream(path);
		}

	/**
	 * Sends a message string, synchronously, to the client.
	 * Execution halts until the client socket acknowledges receipt. 
	 * @param session the connection session.
	 * @param message the message string to pass back to the connected client.
	 * @throws BRFrameworkException on a send error.
	 */
	protected final void sendText(final Session session, String message)
	{
		try {
			session.getBasicRemote().sendText(message);
		} catch (Exception e) {
			throw new BRFrameworkException(e);
			}
		}
	
	/**
	 * Sends a message string, synchronously, to the client, hinting that 
	 * it may not be the last one in the complete message.
	 * Execution halts until the client socket acknowledges receipt. 
	 * @param session the connection session.
	 * @param message the (partial) message string to pass back to the connected client.
	 * @param isLast if true, tells the client that this is the final part. if false, it is not the last part.
	 * @throws BRFrameworkException on a send error.
	 */
	protected final void sendTextPartial(final Session session, String message, boolean isLast)
	{
		try {
			session.getBasicRemote().sendText(message, isLast);
		} catch (Exception e) {
			throw new BRFrameworkException(e);
			}
		}

	/**
	 * Sends binary data, synchronously, to the client.
	 * Execution halts until the client socket acknowledges receipt. 
	 * @param session the connection session.
	 * @param buffer the buffer of data to pass back to the connected client.
	 * @throws BRFrameworkException on a send error.
	 */
	protected final void sendBinary(final Session session, ByteBuffer buffer)
	{
		try {
			session.getBasicRemote().sendBinary(buffer);
		} catch (Exception e) {
			throw new BRFrameworkException(e);
			}
		}
	
	/**
	 * Sends binary data, synchronously, to the client, hinting that 
	 * it may not be the last one in the complete message.
	 * Execution halts until the client socket acknowledges receipt. 
	 * @param session the connection session.
	 * @param buffer the buffer of data to pass back to the connected client.
	 * @throws BRFrameworkException on a send error.
	 */
	protected final void sendBinaryPartial(final Session session, ByteBuffer buffer, boolean isLast)
	{
		try {
			session.getBasicRemote().sendBinary(buffer, isLast);
		} catch (Exception e) {
			throw new BRFrameworkException(e);
			}			
		}
	
	/**
	 * Sends binary data, synchronously, to the client.
	 * Execution halts until the client socket acknowledges receipt.
	 * This method should not be used for repeated sends, as the array is wrapped in a ByteBuffer
	 * before it is sent. 
	 * @param session the connection session.
	 * @param buffer the buffer of data to pass back to the connected client.
	 * @throws BRFrameworkException on a send error.
	 */
	protected final void sendBinary(final Session session, byte[] buffer)
	{
		try {
			sendBinary(session, ByteBuffer.wrap(buffer));
		} catch (Exception e) {
			throw new BRFrameworkException(e);
			}
		}
	
	/**
	 * Sends binary data, synchronously, to the client, hinting that 
	 * it may not be the last one in the complete message.
	 * Execution halts until the client socket acknowledges receipt. 
	 * This method should not be used for repeated sends, as the array is wrapped in a ByteBuffer
	 * before it is sent. 
	 * @param session the connection session.
	 * @param buffer the buffer of data to pass back to the connected client.
	 * @throws BRFrameworkException on a send error.
	 */
	protected final void sendBinaryPartial(final Session session, byte[] buffer, boolean isLast)
	{
		try {
			sendBinaryPartial(session, ByteBuffer.wrap(buffer), isLast);
		} catch (Exception e) {
			throw new BRFrameworkException(e);
			}
		}
	
	/**
	 * Sends a JSON object to the client, synchronously, as a JSON-ified string
	 * representing the object.
	 * Execution halts until the client socket acknowledges receipt. 
	 * @param session the connection session.
	 * @param object the JSON object to send.
	 * @throws BRFrameworkException on a send error.
	 */
	protected final void sendJSON(final Session session, JSONObject object)
	{
		StringWriter sw = new StringWriter();
		try {
			JSONWriter.writeJSON(object, sw);
		} catch (Exception e) {
			throw new BRFrameworkException(e);
			}
		sendText(session, sw.toString());
		}
	
	/**
	 * Sends an object to the client, synchronously, as a JSON-ified string
	 * representing the object.
	 * Execution halts until the client socket acknowledges receipt. 
	 * @param session the connection session.
	 * @param object the JSON object to send.
	 * @throws BRFrameworkException on a send error or if an error occurs during conversion.
	 */
	protected final void sendJSON(final Session session, Object object)
	{
		try {
			StringWriter sw = new StringWriter();
			JSONWriter.writeJSON(JSONObject.create(object), sw);
			sendText(session, sw.toString());
		} catch (Exception e) {
			throw new BRFrameworkException(e);
			}
		}
	
	/**
	 * Sends a stream of data, synchronously, until the end of the stream.
	 * The stream is not closed after this completes.
	 * Execution halts until the client socket acknowledges receipt. 
	 * @param session the connection session.
	 * @param in the input stream to read from.
	 * @param bufferSize the size of the buffer to use during the send.
	 * @throws BRFrameworkException on a send or read error.
	 */
	protected final void sendData(final Session session, InputStream in, int bufferSize)
	{
		try {
			ByteBuffer bb = ByteBuffer.allocate(bufferSize);
			byte[] buffer = new byte[bufferSize];
			int buf = 0;
			while ((buf = in.read(buffer)) >= 0)
			{
				bb.put(buffer);
				bb.rewind();
				sendBinaryPartial(session, buffer, buf < bufferSize);
				bb.rewind();
				}
		} catch (Exception e) {
			throw new BRFrameworkException(e);
			}
		}
	
	/**
	 * Sends the contents of a file as a stream of data, synchronously, until the end of the file.
	 * Execution halts until the client socket acknowledges receipt. 
	 * @param session the connection session.
	 * @param file the file to read.
	 * @param bufferSize the size of the buffer to use during the send.
	 * @throws BRFrameworkException on a send or read error.
	 */
	protected final void sendFileContents(final Session session, File file, int bufferSize)
	{
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			sendData(session, fis, bufferSize);
		} catch (IOException e) {
			throw new BRFrameworkException(e);
		} finally {
			Common.close(fis);
			}
		}

	/**
	 * Sends a message string, asynchronously, to the client.
	 * @param session the connection session.
	 * @param message the message string to pass back to the connected client.
	 * @return the {@link Future} object to monitor the sent request after the call.
	 * @throws BRFrameworkException on a send error.
	 */
	protected final Future<Void> sendAsyncText(final Session session, String message)
	{
		return session.getAsyncRemote().sendText(message);
		}

	/**
	 * Sends binary data, asynchronously, to the client.
	 * @param session the connection session.
	 * @param buffer the buffer of data to pass back to the connected client.
	 * @return the {@link Future} object to monitor the sent request after the call.
	 * @throws IOException on a send error.
	 */
	protected final Future<Void> sendAsyncBinary(final Session session, ByteBuffer buffer)
	{
		return session.getAsyncRemote().sendBinary(buffer);
		}

	/**
	 * Sends binary data, asynchronously, to the client.
	 * This method should not be used for repeated sends, as the array is wrapped in a ByteBuffer
	 * before it is sent. 
	 * @param session the connection session.
	 * @param buffer the buffer of data to pass back to the connected client.
	 * @return the {@link Future} object to monitor the sent request after the call.
	 * @throws BRFrameworkException on a send error.
	 */
	protected final Future<Void> sendAsyncBinary(final Session session, byte[] buffer)
	{
		return sendAsyncBinary(session, ByteBuffer.wrap(buffer));
		}

	/**
	 * Sends a JSON object to the client, asynchronously, as a JSON-ified string
	 * representing the object.
	 * @param session the connection session.
	 * @param object the JSON object to send.
	 * @return the {@link Future} object to monitor the sent request after the call.
	 * @throws BRFrameworkException on a send error.
	 */
	protected final Future<Void> sendAsyncJSON(final Session session, JSONObject object)
	{
		StringWriter sw = new StringWriter();
		try {
			JSONWriter.writeJSON(object, sw);
		} catch (Exception e) {
			throw new BRFrameworkException(e);
			}
		return sendAsyncText(session, sw.toString());
		}

	/**
	 * Sends an object to the client, asynchronously, as a JSON-ified string
	 * representing the object.
	 * @param session the connection session.
	 * @param object the JSON object to send.
	 * @return the {@link Future} object to monitor the sent request after the call.
	 * @throws BRFrameworkException on a send error or if an error occurs during conversion.
	 */
	protected final Future<Void> sendAsyncJSON(final Session session, Object object)
	{
		StringWriter sw = new StringWriter();
		try {
			JSONWriter.writeJSON(JSONObject.create(object), sw);
		} catch (Exception e) {
			throw new BRFrameworkException(e);
			}
		return sendAsyncText(session, sw.toString());
		}
	
}
