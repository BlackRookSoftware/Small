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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.commons.Common;

/**
 * Root control servlet for the Black Rook J2EE Framework.
 * This does all of the pool management and other things that should remain
 * transparent to users of the framework. 
 * @author Matthew Tropiano
 */
public abstract class BRController
{
	/** Lag simulator seed. */
	private Random randomLagSimulator;
	/** Default Servlet Thread Pool. */
	private String defaultThreadPool;
	
	/** Default constructor. */
	protected BRController()
	{
		randomLagSimulator = new Random();
		defaultThreadPool = BRToolkit.DEFAULT_POOL_NAME;
		}
	
	/**
	 * Sets the name of the default thread pool that this controller uses.
	 */
	final void setDefaultThreadPool(String servletDefaultThreadPool)
	{
		this.defaultThreadPool = servletDefaultThreadPool;
		}

	/**
	 * Gets the name of the default thread pool that this controller uses.
	 */
	public final String getDefaultThreadPool()
	{
		return defaultThreadPool;
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
	public final void includeView(HttpServletRequest request, HttpServletResponse response, String key)
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
			throw new BRFrameworkException("No such view: \""+key+"\". No view resolver returned an adequate path.");
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
	 * Attempts to grab an available thread from the servlet's default 
	 * thread pool and starts a task that can be monitored by the caller.
	 * @param task the task to run.
	 * @return a framework task encapsulation for monitoring the task.
	 */
	protected final BRFrameworkTask spawnTask(BRFrameworkTask task)
	{
		return getToolkit().spawnTaskPooled(defaultThreadPool, task);
		}

	/**
	 * Attempts to grab an available thread from the servlet's default 
	 * thread pool and starts a runnable encapsulated as a 
	 * BRFrameworkTask that can be monitored by the caller.
	 * @param runnable the runnable to run.
	 * @return a framework task encapsulation for monitoring the task.
	 */
	protected final BRFrameworkTask spawnRunnable(Runnable runnable)
	{
		return getToolkit().spawnRunnablePooled(defaultThreadPool, runnable);
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
	 */
	public abstract void onMultipartPost(HttpServletRequest request, HttpServletResponse response);

	/**
	 * The entry point for all Black Rook Framework Servlets on a HEAD request call.
	 * All servlets that do not implement this method should return status 405, Method Not Supported.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 */
	public abstract void onHead(HttpServletRequest request, HttpServletResponse response);

	/**
	 * The entry point for all Black Rook Framework Servlets on a PUT request call.
	 * All servlets that do not implement this method should return status 405, Method Not Supported.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 */
	public abstract void onPut(HttpServletRequest request, HttpServletResponse response);

	/**
	 * The entry point for all Black Rook Framework Servlets on a DELETE request call.
	 * All servlets that do not implement this method should return status 405, Method Not Supported.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 */
	public abstract void onDelete(HttpServletRequest request, HttpServletResponse response);
	
}
