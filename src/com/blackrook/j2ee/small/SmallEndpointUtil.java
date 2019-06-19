package com.blackrook.j2ee.small;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;

import javax.websocket.Endpoint;
import javax.websocket.Session;

import com.blackrook.j2ee.small.exception.SmallFrameworkException;
import com.blackrook.json.JSONObject;
import com.blackrook.json.JSONWriter;

/**
 * An {@link Endpoint} utility class that exposes some useful functions for Endpoints.
 * @author Matthew Tropiano
 */
public final class SmallEndpointUtil
{
	private SmallEndpointUtil() {}
	
	/**
	 * Sends a message string, synchronously, to the client.
	 * Execution halts until the client socket acknowledges receipt. 
	 * @param session the connection session.
	 * @param message the message string to pass back to the connected client.
	 * @throws SmallFrameworkException on a send error.
	 */
	public static void sendText(final Session session, String message)
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
	 * @param session the connection session.
	 * @param message the (partial) message string to pass back to the connected client.
	 * @param isLast if true, tells the client that this is the final part. if false, it is not the last part.
	 * @throws SmallFrameworkException on a send error.
	 */
	public static void sendTextPartial(final Session session, String message, boolean isLast)
	{
		try {
			session.getBasicRemote().sendText(message, isLast);
		} catch (Exception e) {
			throw new SmallFrameworkException(e);
		}
	}

	/**
	 * Sends binary data, synchronously, to the client.
	 * Execution halts until the client socket acknowledges receipt. 
	 * @param session the connection session.
	 * @param buffer the buffer of data to pass back to the connected client.
	 * @throws SmallFrameworkException on a send error.
	 */
	public static void sendBinary(final Session session, ByteBuffer buffer)
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
	 * @param session the connection session.
	 * @param buffer the buffer of data to pass back to the connected client.
	 * @throws SmallFrameworkException on a send error.
	 */
	public static void sendBinaryPartial(final Session session, ByteBuffer buffer, boolean isLast)
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
	 * @param session the connection session.
	 * @param buffer the buffer of data to pass back to the connected client.
	 * @throws SmallFrameworkException on a send error.
	 */
	public static void sendBinary(final Session session, byte[] buffer)
	{
		try {
			sendBinary(session, ByteBuffer.wrap(buffer));
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
	 * @param session the connection session.
	 * @param buffer the buffer of data to pass back to the connected client.
	 * @throws SmallFrameworkException on a send error.
	 */
	public static void sendBinaryPartial(final Session session, byte[] buffer, boolean isLast)
	{
		try {
			sendBinaryPartial(session, ByteBuffer.wrap(buffer), isLast);
		} catch (Exception e) {
			throw new SmallFrameworkException(e);
		}
	}
	
	/**
	 * Sends a JSON object to the client, synchronously, as a JSON-ified string
	 * representing the object.
	 * Execution halts until the client socket acknowledges receipt. 
	 * @param session the connection session.
	 * @param object the JSON object to send.
	 * @throws SmallFrameworkException on a send error.
	 */
	public static void sendJSON(final Session session, JSONObject object)
	{
		StringWriter sw = new StringWriter();
		try {
			JSONWriter.writeJSON(object, sw);
		} catch (Exception e) {
			throw new SmallFrameworkException(e);
		}
		sendText(session, sw.toString());
	}
	
	/**
	 * Sends an object to the client, synchronously, as a JSON-ified string
	 * representing the object.
	 * Execution halts until the client socket acknowledges receipt. 
	 * @param session the connection session.
	 * @param object the JSON object to send.
	 * @throws SmallFrameworkException on a send error or if an error occurs during conversion.
	 */
	public static void sendJSON(final Session session, Object object)
	{
		try {
			StringWriter sw = new StringWriter();
			JSONWriter.writeJSON(JSONObject.create(object), sw);
			sendText(session, sw.toString());
		} catch (Exception e) {
			throw new SmallFrameworkException(e);
		}
	}
	
	/**
	 * Sends a stream of data, synchronously, until the end of the stream.
	 * The stream is not closed after this completes.
	 * Execution halts until the client socket acknowledges receipt. 
	 * @param session the connection session.
	 * @param in the input stream to read from.
	 * @param bufferSize the size of the buffer to use during the send.
	 * @throws SmallFrameworkException on a send or read error.
	 */
	public static void sendData(final Session session, InputStream in, int bufferSize)
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
			throw new SmallFrameworkException(e);
		}
	}
	
	/**
	 * Sends the contents of a file as a stream of data, synchronously, until the end of the file.
	 * Execution halts until the client socket acknowledges receipt. 
	 * @param session the connection session.
	 * @param file the file to read.
	 * @param bufferSize the size of the buffer to use during the send.
	 * @throws SmallFrameworkException on a send or read error.
	 */
	public static void sendFileContents(final Session session, File file, int bufferSize)
	{
		try (FileInputStream fis = new FileInputStream(file)) {
			sendData(session, fis, bufferSize);
		} catch (IOException e) {
			throw new SmallFrameworkException(e);
		}
	}

	/**
	 * Sends a message string, asynchronously, to the client.
	 * @param session the connection session.
	 * @param message the message string to pass back to the connected client.
	 * @return the {@link Future} object to monitor the sent request after the call.
	 * @throws SmallFrameworkException on a send error.
	 */
	public static Future<Void> sendAsyncText(final Session session, String message)
	{
		return session.getAsyncRemote().sendText(message);
	}

	/**
	 * Sends binary data, asynchronously, to the client.
	 * @param session the connection session.
	 * @param buffer the buffer of data to pass back to the connected client.
	 * @return the {@link Future} object to monitor the sent request after the call.
	 */
	public static Future<Void> sendAsyncBinary(final Session session, ByteBuffer buffer)
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
	 * @throws SmallFrameworkException on a send error.
	 */
	public static Future<Void> sendAsyncBinary(final Session session, byte[] buffer)
	{
		return sendAsyncBinary(session, ByteBuffer.wrap(buffer));
	}

	/**
	 * Sends a JSON object to the client, asynchronously, as a JSON-ified string
	 * representing the object.
	 * @param session the connection session.
	 * @param object the JSON object to send.
	 * @return the {@link Future} object to monitor the sent request after the call.
	 * @throws SmallFrameworkException on a send error.
	 */
	public static Future<Void> sendAsyncJSON(final Session session, JSONObject object)
	{
		StringWriter sw = new StringWriter();
		try {
			JSONWriter.writeJSON(object, sw);
		} catch (Exception e) {
			throw new SmallFrameworkException(e);
		}
		return sendAsyncText(session, sw.toString());
	}

	/**
	 * Sends an object to the client, asynchronously, as a JSON-ified string
	 * representing the object.
	 * @param session the connection session.
	 * @param object the JSON object to send.
	 * @return the {@link Future} object to monitor the sent request after the call.
	 * @throws SmallFrameworkException on a send error or if an error occurs during conversion.
	 */
	public static Future<Void> sendAsyncJSON(final Session session, Object object)
	{
		StringWriter sw = new StringWriter();
		try {
			JSONWriter.writeJSON(JSONObject.create(object), sw);
		} catch (Exception e) {
			throw new SmallFrameworkException(e);
		}
		return sendAsyncText(session, sw.toString());
	}
	
}
