/*******************************************************************************
 * Copyright (c) 2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.servlet.http.HttpServletResponse;

import com.blackrook.small.exception.SmallFrameworkException;
import com.blackrook.small.struct.Utils;

/**
 * Utility class for {@link HttpServletResponse} manipulation.
 * @author Matthew Tropiano
 */
public final class SmallResponseUtils
{
	private static final Charset UTF8 = Charset.forName("UTF-8");
	
	private SmallResponseUtils() {}

	/**
	 * Sends a file to the client.
	 * The file's name becomes the filename in the content's "disposition".
	 * Via this method, most browsers will be forced to download the file in
	 * question separately, as this adds "Content-Disposition" headers to the response.
	 * <ul>
	 * <li>The "Content-Type" portion of the header is set to <code>mimeType</code>, if not null.</li>
	 * <li>The "Content-Disposition" portion of the header is set to <code>"attachment; filename=\"" + file.getName() + "\""</code>.</li>
	 * <li>The "Content-Length" portion of the header is set to the file's length in bytes.</li>
	 * </ul>
	 * @param response servlet response object.
	 * @param mimeType the MIME Type of the content to send. If null, "application/octet-stream" is sent.
	 * @param file the file content to send.
	 */
	public static void sendFile(HttpServletResponse response, String mimeType, File file)
	{
		sendFileContents(response, mimeType, file.getName(), file);
	}

	/**
	 * Sends contents of a file to the client.
	 * This is not sent as an "attachment".
	 * <ul>
	 * <li>The "Content-Type" portion of the header is set to <code>mimeType</code>, if not null.</li>
	 * <li>The "Content-Length" portion of the header is set to the file's length in bytes.</li>
	 * </ul>
	 * @param response servlet response object.
	 * @param mimeType the MIME-Type of the stream. If null, no type is set.
	 * @param file the file content to send.
	 */
	public static void sendFileContents(HttpServletResponse response, String mimeType, File file)
	{
		sendFileContents(response, mimeType, null, file);
	}

	/**
	 * Sends the contents of a file to the client.
	 * <ul>
	 * <li>The "Content-Type" portion of the header is set to <code>mimeType</code>, if not null.</li>
	 * <li>The "Content-Disposition" portion of the header is set to <code>"attachment; filename=\"" + attachmentFileName + "\""</code>, if not null.</li>
	 * <li>The "Content-Length" portion of the header is set to the file's length in bytes.</li>
	 * </ul>
	 * @param response servlet response object.
	 * @param mimeType the MIME-Type of the stream. If null, no type is set.
	 * @param file the file content to send.
	 * @param attachmentFileName the file name. If null, not sent as an attachment.
	 * care whether the file is downloaded or opened by the browser. 
	 */
	public static void sendFileContents(HttpServletResponse response, String mimeType, String attachmentFileName, File file)
	{
		sendFileContents(response, mimeType, attachmentFileName, null, file);
	}

	/**
	 * Sends the contents of a file to the client.
	 * <ul>
	 * <li>The "Content-Type" portion of the header is set to <code>mimeType</code>, if not null.</li>
	 * <li>The "Content-Encoding" portion of the header is set to <code>encoding</code>, if not null.</li>
	 * <li>The "Content-Disposition" portion of the header is set to <code>"attachment; filename=\"" + attachmentFileName + "\""</code>, if not null.</li>
	 * <li>The "Content-Length" portion of the header is set to the file's length in bytes.</li>
	 * </ul>
	 * @param response servlet response object.
	 * @param mimeType the MIME-Type of the stream. If null, no type is set.
	 * @param file the file content to send.
	 * @param attachmentFileName the file name. If null, not sent as an attachment.
	 * @param encoding if not null, adds a "Content-Encoding" header (not to be confused with charset - that should be set on the MIME-Type).
	 * care whether the file is downloaded or opened by the browser. 
	 */
	public static void sendFileContents(HttpServletResponse response, String mimeType, String attachmentFileName, String encoding, File file)
	{
		try (FileInputStream fis = new FileInputStream(file)) {
			sendData(response, mimeType, attachmentFileName, encoding, fis, file.length());
		} catch (IOException e) {
			throw new SmallFrameworkException(e);
		}
	}

	/**
	 * Writes string data to the response as "text/plain".
	 * <ul>
	 * <li>The "Content-Type" portion of the header is set to "text/plain".</li>
	 * <li>The "Content-Length" portion of the header is set to the byte length.</li>
	 * </ul>
	 * @param response the response object.
	 * @param data the string data to send.
	 */
	public static void sendStringData(HttpServletResponse response, String data)
	{
		sendStringData(response, null, null, data);
	}

	/**
	 * Writes string data to the response.
	 * <ul>
	 * <li>The "Content-Type" portion of the header is set to <code>mimeType</code>, or "text/plain" if null.</li>
	 * <li>The "Content-Length" portion of the header is set to the byte length.</li>
	 * </ul>
	 * @param response the response object.
	 * @param mimeType the response MIME-Type. If null, "text/plain" is used.
	 * @param data the string data to send.
	 */
	public static void sendStringData(HttpServletResponse response, String mimeType, String data)
	{
		sendStringData(response, mimeType, null, data);
	}

	/**
	 * Writes string data to the response.
	 * Charset is always UTF-8.
	 * <ul>
	 * <li>The "Content-Type" portion of the header is set to <code>mimeType</code>, or "text/plain" if null.</li>
	 * <li>The "Content-Disposition" portion of the header is set to <code>"attachment; filename=\"" + attachmentFileName + "\""</code>, if not null.</li>
	 * <li>The "Content-Length" portion of the header is set to the byte length.</li>
	 * </ul>
	 * @param response the response object.
	 * @param mimeType the response MIME-Type. If null, "text/plain" is used.
	 * @param attachmentFileName the file name. If null, not sent as an attachment.
	 * @param data the string data to send.
	 */
	public static void sendStringData(HttpServletResponse response, String mimeType, String attachmentFileName, String data)
	{
		byte[] bytedata = data.getBytes(UTF8);
		if (Utils.isEmpty(mimeType))
			mimeType = "text/plain";
		SmallResponseUtils.sendData(response, mimeType + "; charset=utf-8", attachmentFileName, new ByteArrayInputStream(bytedata), (long)bytedata.length);
	}

	/**
	 * Sends the contents of a stream out through the response.
	 * The input stream is not closed after the data is sent.
	 * No content type is set.
	 * <ul>
	 * <li>The "Content-Length" portion of the header is set to <code>length</code>, if not null and positive.</li>
	 * </ul>
	 * @param response servlet response object.
	 * @param inStream the input stream to read.
	 * @param length the length of data in bytes to send.
	 */
	public static void sendData(HttpServletResponse response, InputStream inStream, Long length)
	{
		sendData(response, null, null, null, inStream, length);
	}

	/**
	 * Sends the contents of a stream out through the response.
	 * The input stream is not closed after the data is sent.
	 * <ul>
	 * <li>The "Content-Type" portion of the header is set to <code>mimeType</code>, if not null.</li>
	 * <li>The "Content-Length" portion of the header is set to <code>length</code>, if not null and positive.</li>
	 * </ul>
	 * @param response servlet response object.
	 * @param mimeType the MIME-Type of the stream. If null, no type is set.
	 * @param inStream the input stream to read.
	 * @param length the length of data in bytes to send.
	 */
	public static void sendData(HttpServletResponse response, String mimeType, InputStream inStream, Long length)
	{
		sendData(response, mimeType, null, null, inStream, length);
	}

	/**
	 * Sends the contents of a stream out through the response.
	 * The input stream is not closed after the data is sent.
	 * <ul>
	 * <li>The "Content-Type" portion of the header is set to <code>mimeType</code>, if not null.</li>
	 * <li>The "Content-Disposition" portion of the header is set to <code>"attachment; filename=\"" + attachmentFileName + "\""</code>, if not null.</li>
	 * <li>The "Content-Length" portion of the header is set to <code>length</code>, if not null and positive.</li>
	 * </ul>
	 * @param response servlet response object.
	 * @param mimeType the MIME-Type of the stream. If null, no type is set.
	 * @param attachmentFileName the name of the data to send (file name). If null, not sent as an attachment.
	 * @param inStream the input stream to read.
	 * @param length the length of data in bytes to send.
	 */
	public static void sendData(HttpServletResponse response, String mimeType, String attachmentFileName, InputStream inStream, Long length)
	{
		sendData(response, mimeType, attachmentFileName, null, inStream, length);
	}

	/**
	 * Sends the contents of a stream out through the response.
	 * The input stream is not closed after the data is sent.
	 * <ul>
	 * <li>The "Content-Type" portion of the header is set to <code>mimeType</code>, if not null.</li>
	 * <li>The "Content-Encoding" portion of the header is set to <code>encoding</code>, if not null.</li>
	 * <li>The "Content-Disposition" portion of the header is set to <code>"attachment; filename=\"" + attachmentFileName + "\""</code>, if not null.</li>
	 * <li>The "Content-Length" portion of the header is set to <code>length</code>, if not null and positive.</li>
	 * </ul>
	 * @param response servlet response object.
	 * @param mimeType the MIME-Type of the stream. If null, no type is set.
	 * @param attachmentFileName the name of the data to send (file name). If null, not sent as an attachment.
	 * @param encoding if not null, adds a "Content-Encoding" header (not to be confused with charset - that should be set on the MIME-Type).
	 * @param inStream the input stream to read.
	 * @param length the length of data in bytes to send, or null to not specify.
	 */
	public static void sendData(HttpServletResponse response, String mimeType, String attachmentFileName, String encoding, InputStream inStream, Long length)
	{
		if (!Utils.isEmpty(mimeType))
			response.setHeader("Content-Type", mimeType);
		if (!Utils.isEmpty(encoding))
			response.setHeader("Content-Encoding", encoding);
		if (!Utils.isEmpty(attachmentFileName))
			response.setHeader("Content-Disposition", "attachment; filename=\"" + attachmentFileName + "\"");
		if (length != null && length >= 0)
			response.setHeader("Content-Length", String.valueOf(length));
		
		try {
			
			if (length != null) while (length > 0)
			{
				length -= Utils.relay(
					inStream, 
					response.getOutputStream(), 
					32768, 
					length > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)(long)length
				);
			}
			else
			{
				Utils.relay(
					inStream, 
					response.getOutputStream(), 
					32768,
					-1
				);
			}
		} catch (IOException e) {
			throw new SmallFrameworkException(e);
		}
	}

	/**
	 * Forwards the client abruptly to another document or servlet (new client request). 
	 * @param response servlet response object.
	 * @param url the target URL.
	 * @see HttpServletResponse#sendRedirect(String)
	 */
	public static void sendRedirect(HttpServletResponse response, String url)
	{
		try{
			response.sendRedirect(url);
		} catch (Exception e) {
			throw new SmallFrameworkException(e);
		}
	}

	/**
	 * Sends request to the error page with a status code.
	 * @param response servlet response object.
	 * @param statusCode the status code to use.
	 * @param message the status message.
	 * @see HttpServletResponse#sendError(int, String)
	 */
	public static void sendError(HttpServletResponse response, int statusCode, String message)
	{
		try{
			response.sendError(statusCode, message);
		} catch (Exception e) {
			throw new SmallFrameworkException(e);
		}
	}
	
}
