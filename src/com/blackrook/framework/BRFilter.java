package com.blackrook.framework;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.commons.Common;
import com.blackrook.framework.util.BRUtil;
import com.blackrook.lang.json.JSONObject;
import com.blackrook.lang.json.JSONWriter;

/**
 * The root filter.
 * @author Matthew Tropiano
 */
public abstract class BRFilter
{
	/** Lag simulator seed. */
	private Random randomLagSimulator;
	
	/** Default constructor. */
	protected BRFilter()
	{
		randomLagSimulator = new Random();
		}
	
	/**
	 * Gets the Black Rook Framework Toolkit.
	 */
	public final BRToolkit getToolkit()
	{
		return BRToolkit.INSTANCE;
		}

	/**
	 * Gets the servlet context.
	 */
	public final ServletContext getServletContext()
	{
		return getToolkit().getServletContext();
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
		response.setHeader("Content-Type", "application/json");
		try {
			JSONWriter.writeJSON(jsonObject, response.getWriter());
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
	 * Simulates latency on a response, for testing.
	 * Just calls {@link Common#sleep(long)} and varies the input value.
	 */
	protected final void simulateLag(int millis)
	{
		Common.sleep(randomLagSimulator.nextInt(millis));
		}

	/**
	 * Called to figure out the stuff to do from an HTTP request.
	 * Usually, this filter checks if some conditions are satisfied before continuing on to the next
	 * filter. If conditions are met, this should perform its necessary functions and return true. If not, return false.
	 * Returns true unless overridden.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @return true to continue the filter chain, false otherwise.
	 * @throws ServletException if a servlet exception occurs in this filter. 
	 * @throws IOException  if an I/O exception occurs in this filter.
	 */
	public abstract boolean onFilter(HttpServletRequest request, HttpServletResponse response);

}
