/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.small.util;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import com.blackrook.small.exception.SmallFrameworkException;
import com.blackrook.small.struct.Utils;

/**
 * Utility class for {@link HttpServletResponse} manipulation.
 * @author Matthew Tropiano
 */
public final class SmallResponseUtil
{
	private SmallResponseUtil() {}

	/**
	 * Sends a file to the client.
	 * The file's name becomes the filename in the content's "disposition".
	 * Via this method, most browsers will be forced to download the file in
	 * question separately, as this adds "Content-Disposition" headers to the response.
	 * @param response servlet response object.
	 * @param mimeType the MIME Type of the content to send.
	 * @param file the file content to send.
	 */
	public static void sendFile(HttpServletResponse response, String mimeType, File file)
	{
		sendFileContents(response, mimeType, file, file.getName());
	}

	/**
	 * Sends contents of a file to the client.
	 * Via this method, most browsers will attempt to open the file in-browser,
	 * as this has no "Content-Disposition" attached to it.
	 * @param response servlet response object.
	 * @param mimeType the MIME Type of the content to send.
	 * @param file the file content to send.
	 */
	public static void sendFileContents(HttpServletResponse response, String mimeType, File file)
	{
		sendFileContents(response, mimeType, file, null);
	}

	/**
	 * Sends the contents of a file to the client.
	 * @param response servlet response object.
	 * @param mimeType the MIME Type of the content to send.
	 * @param file the file content to send.
	 * @param fileName the new file name of what to send. Can be null - leaving it out
	 * does not send "Content-Disposition" headers. You may want to do this if you don't 
	 * care whether the file is downloaded or opened by the browser. 
	 */
	public static void sendFileContents(HttpServletResponse response, String mimeType, File file, String fileName)
	{
		try (FileInputStream fis = new FileInputStream(file)) {
			sendData(response, mimeType, fileName, fis, file.length());
		} catch (IOException e) {
			throw new SmallFrameworkException(e);
		}
	}

	/**
	 * Sends the contents of a stream out through the response.
	 * The input stream is not closed after the data is sent.
	 * The "Content-Type" portion of the header is changed to <code>mimeType</code>.
	 * The "Content-Length" portion of the header is changed to <code>length</code>, if positive.
	 * @param response servlet response object.
	 * @param mimeType the MIME-Type of the stream.
	 * @param inStream the input stream to read.
	 * @param length the length of data in bytes to send.
	 */
	public static void sendData(HttpServletResponse response, String mimeType, InputStream inStream, long length)
	{
		sendData(response, mimeType, null, null, inStream, length);
	}

	/**
	 * Sends the contents of a stream out through the response.
	 * The input stream is not closed after the data is sent.
	 * The "Content-Type" portion of the header is changed to <code>mimeType</code>.
	 * The "Content-Length" portion of the header is changed to <code>length</code>, if positive.
	 * @param response servlet response object.
	 * @param mimeType the MIME-Type of the stream.
	 * @param contentName the name of the data to send (file name). Can be null - leaving it out
	 * 	does not send "Content-Disposition" headers.
	 * @param inStream the input stream to read.
	 * @param length the length of data in bytes to send.
	 */
	public static void sendData(HttpServletResponse response, String mimeType, String contentName, InputStream inStream, long length)
	{
		sendData(response, mimeType, contentName, null, inStream, length);
	}

	/**
	 * Sends the contents of a stream out through the response.
	 * The input stream is not closed after the data is sent.
	 * The "Content-Type" portion of the header is changed to <code>mimeType</code>.
	 * The "Content-Length" portion of the header is changed to <code>length</code>, if positive.
	 * @param response servlet response object.
	 * @param mimeType the MIME-Type of the stream.
	 * @param contentName the name of the data to send (file name). Can be null - leaving it out does not send "Content-Disposition" headers.
	 * @param encoding if not null, adds a "Content-Encoding" header.
	 * @param inStream the input stream to read.
	 * @param length the length of data in bytes to send.
	 */
	public static void sendData(HttpServletResponse response, String mimeType, String contentName, String encoding, InputStream inStream, long length)
	{
		response.setHeader("Content-Type", mimeType);
		if (!Utils.isEmpty(encoding))
			response.setHeader("Content-Encoding", encoding);
		if (!Utils.isEmpty(contentName))
			response.setHeader("Content-Disposition", "attachment; filename=\"" + contentName + "\"");
		if (length >= 0)
			response.setHeader("Content-Length", String.valueOf(length));
		
		try {
			while (length > 0)
			{
				length -= Utils.relay(
					inStream, 
					response.getOutputStream(), 
					32768, 
					length > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)length
				);
			}
		} catch (IOException e) {
			throw new SmallFrameworkException(e);
		}
	}

	/**
	 * Forwards the client abruptly to another document or servlet (new client request). 
	 * @param response servlet response object.
	 * @param url target URL.
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
