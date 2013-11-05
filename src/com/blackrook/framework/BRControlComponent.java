package com.blackrook.framework;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.commons.Common;
import com.blackrook.framework.util.BRUtil;
import com.blackrook.lang.json.JSONObject;
import com.blackrook.lang.json.JSONWriter;
import com.blackrook.lang.xml.XMLStruct;
import com.blackrook.lang.xml.XMLWriter;

/**
 * Describes the abilities of the control components of the
 * Black Rook Simple Servlet Framework.
 * @author Matthew Tropiano
 */
public abstract class BRControlComponent
{
	/** MIME type for JSON */
	public static final String CONTENT_MIME_TYPE_JSON = "application/json";
	/** MIME type for XML */
	public static final String CONTENT_MIME_TYPE_XML = "application/xml";
	
	/** Lag simulator seed. */
	private Random randomLagSimulator;
	
	protected BRControlComponent()
	{
		randomLagSimulator = new Random();
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
	public File getApplicationFile(String path)
	{
		return getToolkit().getApplicationFile(path);
		}

	/**
	 * Gets a file path that is on the application path. 
	 * @param relativepath the relative path to the file to get.
	 * @return a file representing the specified resource or null if it couldn't be found.
	 */
	public String getApplicationFilePath(String relativepath)
	{
		return getToolkit().getApplicationFilePath(relativepath);
		}

	/**
	 * Opens an input stream to a resource using a path relative to the
	 * application context path. 
	 * Outside users should not be able to access this!
	 * @param path the path to the resource to open.
	 * @return an open input stream to the specified resource or null if it couldn't be opened.
	 */
	public InputStream getResourceAsStream(String path) throws IOException
	{
		return getToolkit().getResourceAsStream(path);
		}

	/**
	 * Includes the output of a view in the response.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param key target view key taken from the mapping XMLs.
	 */
	protected final void includeView(HttpServletRequest request, HttpServletResponse response, String key)
	{
		String path = getToolkit().getViewByName(key);
		if (path == null)
			throw new BRFrameworkException("No such view: \""+key+"\". No view resolver returned an adequate path.");
		includeViewInline(request, response, path);
		}

	/**
	 * Includes the output of a view in the response, not using a view location key.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param path target view path relative to the application context.
	 */
	protected final void includeViewInline(HttpServletRequest request, HttpServletResponse response, String path)
	{
		try{
			request.getRequestDispatcher(path).include(request, response);
		} catch (Exception e) {
			throwException(e);
			}
		}

	/**
	 * Surreptitiously forwards the request to a view.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param key target view key taken from the mapping XMLs.
	 */
	protected final void sendToView(HttpServletRequest request, HttpServletResponse response, String key)
	{
		String path = getToolkit().getViewByName(key);
		if (path == null)
			throw new BRFrameworkException("No such view: \""+key+"\". No view resolver returned an adequate path.");
		sendToViewInline(request, response, path);
		}

	/**
	 * Surreptitiously forwards the request to a view, not using a view location key.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param path target view path relative to the application context.
	 */
	protected final void sendToViewInline(HttpServletRequest request, HttpServletResponse response, String path)
	{
		try{
			request.getRequestDispatcher(path).forward(request, response);
		} catch (Exception e) {
			throwException(e);
			}
		}

	/**
	 * Sends back JSON to the client.
	 * The "Content-Type" portion of the header is changed to "application/json".
	 * @param response the servlet response to write to.
	 * @param jsonObject the JSON Object to write to the request.
	 */
	protected final void sendJSON(HttpServletResponse response, JSONObject jsonObject)
	{
		response.setHeader("Content-Type", CONTENT_MIME_TYPE_JSON);
		try {
			JSONWriter.writeJSON(jsonObject, response.getWriter());
		} catch (IOException e) {
			throwException(e);
			}
		}

	/**
	 * Sends back a JSON-ified object to the client.
	 * Works best with POJOs and simple beans.
	 * The "Content-Type" portion of the header is changed to "application/json".
	 * @param response the servlet response to write to.
	 * @param object the Object to write to the request, which then is .
	 */
	protected final void sendJSON(HttpServletResponse response, Object object)
	{
		response.setHeader("Content-Type", CONTENT_MIME_TYPE_JSON);
		try {
			JSONWriter.writeJSON(JSONObject.create(object), response.getWriter());
		} catch (IOException e) {
			throwException(e);
			}
		}

	/**
	 * Sends back XML to the client.
	 * The "Content-Type" portion of the header is changed to "application/json".
	 * @param response the servlet response to write to.
	 * @param xml the XML structure to write to the request.
	 */
	protected final void sendXML(HttpServletResponse response, XMLStruct xml)
	{
		response.setHeader("Content-Type", CONTENT_MIME_TYPE_XML);
		try {
			(new XMLWriter()).writeXML(xml, response.getWriter());
		} catch (IOException e) {
			throwException(e);
			}
		}

	/**
	 * Sends a file to the client.
	 * Via this method, most browsers will be forced to download the file in
	 * question, as this adds "Content-Disposition" headers to the response.
	 * The file's MIME type is guessed by its extension.
	 * The file's name becomes the filename in the content's "disposition".
	 * @param response servlet response object.
	 * @param file the file content to send.
	 */
	protected final void sendFile(HttpServletResponse response, File file)
	{
		sendFileContents(response, BRUtil.getMIMEType(file.getName()), file, file.getName());
		}

	/**
	 * Sends a file to the client.
	 * The file's name becomes the filename in the content's "disposition".
	 * Via this method, most browsers will be forced to download the file in
	 * question separately, as this adds "Content-Disposition" headers to the response.
	 * @param response servlet response object.
	 * @param mimeType the MIME Type of the content to send.
	 * @param file the file content to send.
	 */
	protected final void sendFile(HttpServletResponse response, String mimeType, File file)
	{
		sendFileContents(response, mimeType, file, file.getName());
		}

	/**
	 * Sends the contents of a file to the client.
	 * Via this method, most browsers will be attempt to open the file in-browser,
	 * as this has no "Content-Disposition" attached to it.
	 * The file's MIME type is guessed by its extension.
	 * @param response servlet response object.
	 * @param file the file content to send.
	 */
	protected final void sendFileContents(HttpServletResponse response, File file)
	{
		sendFileContents(response, BRUtil.getMIMEType(file.getName()), file, null);
		}

	/**
	 * Sends contents of a file to the client.
	 * Via this method, most browsers will be attempt to open the file in-browser,
	 * as this has no "Content-Disposition" attached to it.
	 * @param response servlet response object.
	 * @param mimeType the MIME Type of the content to send.
	 * @param file the file content to send.
	 */
	protected final void sendFileContents(HttpServletResponse response, String mimeType, File file)
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
	protected final void sendFileContents(HttpServletResponse response, String mimeType, File file, String fileName)
	{
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			sendData(response, mimeType, fileName, fis, file.length());
		} catch (IOException e) {
			throwException(e);
		} finally {
			Common.close(fis);
			}
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
	protected final void sendData(HttpServletResponse response, String mimeType, String contentName, InputStream inStream, long length)
	{
		response.setHeader("Content-Type", mimeType);
		if (!Common.isEmpty(contentName))
			response.setHeader("Content-Disposition", "attachment; filename=\"" + contentName + "\"");
		if (length >= 0)
			response.setHeader("Content-Length", String.valueOf(length));
		
		try {
			while (length > 0)
			{
				length -= Common.relay(
						inStream, 
						response.getOutputStream(), 
						32768, 
						length > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)length
					);
				}
		} catch (IOException e) {
			throwException(e);
			}
		}

	/**
	 * Sends request to the error page with a status code.
	 * @param response servlet response object.
	 * @param statusCode the status code to use.
	 * @param message the status message.
	 */
	protected final void sendCode(HttpServletResponse response, int statusCode, String message)
	{
		try{
			response.sendError(statusCode, message);
		} catch (Exception e) {
			throwException(e);
			}
		}

	/**
	 * Forwards the client abruptly to another document or servlet (new client request). 
	 * @param response	servlet response object.
	 * @param url		target URL.
	 */
	protected final void sendRedirect(HttpServletResponse response, String url)
	{
		try{
			response.sendRedirect(url);
		} catch (Exception e) {
			throwException(e);
			}
		}

	/**
	 * Forces an exception to propagate up to the dispatcher.
	 * Basically encloses the provided throwable in a {@link BRFrameworkException},
	 * which is a {@link RuntimeException}.
	 * @param t the {@link Throwable} to encapsulate and throw.
	 */
	protected final void throwException(Throwable t)
	{
		throw new BRFrameworkException(t);
		}

	/**
	 * Pauses the current thread for up to <code>maxMillis</code>
	 * milliseconds, used for simulating lag.
	 * For debugging and testing only!
	 */
	protected final void simulateLag(long maxMillis)
	{
		Common.sleep(randomLagSimulator.nextLong() % (maxMillis <= 1 ? 1 :maxMillis));
		}
	
	
	
}
