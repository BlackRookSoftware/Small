/*******************************************************************************
 * Copyright (c) 2009-2012 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  
 * Contributors:
 *     Matt Tropiano - initial API and implementation
 ******************************************************************************/
package com.blackrook.framework;

import java.util.Random;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.blackrook.commons.Common;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.list.List;

/**
 * Root control servlet for the Black Rook J2EE Framework.
 * This does all of the pool management and other things that should remain
 * transparent to users of the framework. 
 * @author Matthew Tropiano
 */
public abstract class BRRootServlet extends HttpServlet
{
	/** Name for all default pools. */
	public static final String DEFAULT_POOL_NAME = "default";

	private static final long serialVersionUID = 7057939105164581326L;

	private static final Random LAG_SIM_RANDOM = new Random();

	@Override
	public final void doGet(HttpServletRequest request, HttpServletResponse response)
	{
		BRToolkit.createToolkit(getServletContext());
		directService(request,response,false);
		}

	@Override
	public final void doPost(HttpServletRequest request, HttpServletResponse response)
	{
		BRToolkit.createToolkit(getServletContext());
		directService(request,response,true);
		}
	
	/**
	 * Function that redirects a service call to the appropriate handler methods.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param post did this come from a POST request? if not, this is false ("GET", probably).
	 */
	@SuppressWarnings("unchecked")
	public final void directService(HttpServletRequest request, HttpServletResponse response, boolean post)
	{
		if (post)
		{
			if (ServletFileUpload.isMultipartContent(request))
			{
				FileItemFactory factory = new DiskFileItemFactory();
				ServletFileUpload upload = new ServletFileUpload(factory);
				try {
					java.util.List<FileItem> list = upload.parseRequest(request);
					List<FileItem> fitems = new List<FileItem>();
					HashMap<String,String> paramTable = new HashMap<String,String>(4);
					for (FileItem fit : list)
					{
						if (!fit.isFormField())
							fitems.add(fit);
						else
							paramTable.put(fit.getFieldName(),fit.getString());
						}
					list = null;
					FileItem[] fileItems = new FileItem[fitems.size()];
					fitems.toArray(fileItems);
					fitems = null;
					onMultiformPost(request, response, fileItems, paramTable);
				} catch (FileUploadException e) {
					e.printStackTrace(System.err);
					onPost(request,response);
					}
				}
			else
				onPost(request,response);
			}
		else
			onGet(request,response);
		}
	
	/**
	 * The entry point for all Black Rook Framework Servlets on a GET request call.
	 * All servlets that do not implement this method should return status 405, Method Not Supported.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 */
	public abstract void onGet(HttpServletRequest request, HttpServletResponse response);

	/**
	 * The entry point for all Black Rook Framework Servlets on a POST request call.
	 * All servlets that do not implement this method should return status 405, Method Not Supported.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 */
	public abstract void onPost(HttpServletRequest request, HttpServletResponse response);

	/**
	 * The entry point for all Black Rook Framework Servlets on a POST request call 
	 * and it contains a multiform request.
	 * All servlets that do not implement this method should return status 405, Method Not Supported.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param fileItems	the list of file items parsed in the multiform packet.
	 * @param paramMap the table of the parameters passed found in the multiform packet (THEY WILL NOT BE IN THE REQUEST).
	 */
	public abstract void onMultiformPost(HttpServletRequest request, HttpServletResponse response, FileItem[] fileItems, HashMap<String,String> paramMap);

	/**
	 * Sends request to the error page with a status code.
	 * @param response servlet response object.
	 * @param statusCode the status code to use.
	 * @param message the status message.
	 */
	protected final void sendError(HttpServletResponse response, int statusCode, String message)
	{
		try{
			response.sendError(statusCode, message);
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
	 * Simulates latency on a response, for testing.
	 * Just calls {@link Common.sleep(long)} and varies the input value.
	 */
	public final void simulateLag(int millis)
	{
		Common.sleep(LAG_SIM_RANDOM.nextInt(millis));
		}

	/**
	 * Gets the Black Rook Servlet Toolkit.
	 */
	public final BRToolkit getToolkit()
	{
		return BRToolkit.getInstance();
		}

	/**
	 * Includes the output of a view in the response.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param key target view key taken from the mapping XMLs.
	 */
	public final void includeView(HttpServletRequest request, HttpServletResponse response, String key)
	{
		String path = getToolkit().getViewByName(key);
		if (path == null)
			throw new BRFrameworkException("No such view: \""+key+"\". It may not be declared in "+BRToolkit.MAPPING_XML_VIEWS);
		includeViewInline(request, response, path);
		}
	
	/**
	 * Includes the output of a view in the response, not using a view location key.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param path target view path relative to the application context.
	 */
	public final void includeViewInline(HttpServletRequest request, HttpServletResponse response, String path)
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
	public final void sendToView(HttpServletRequest request, HttpServletResponse response, String key)
	{
		String path = getToolkit().getViewByName(key);
		if (path == null)
			throw new BRFrameworkException("No such view: \""+key+"\". It may not be declared in "+BRToolkit.MAPPING_XML_VIEWS);
		sendToViewInline(request, response, path);
		}
	
	/**
	 * Surreptitiously forwards the request to a view, not using a view location key.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param path target view path relative to the application context.
	 */
	public final void sendToViewInline(HttpServletRequest request, HttpServletResponse response, String path)
	{
		try{
			request.getRequestDispatcher(path).forward(request, response);
		} catch (Exception e) {
			throwException(e);
			}
		}
	
}
