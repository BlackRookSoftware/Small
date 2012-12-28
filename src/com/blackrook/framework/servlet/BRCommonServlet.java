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
package com.blackrook.framework.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;

import com.blackrook.commons.hash.HashMap;
import com.blackrook.framework.BRFrameworkTask;
import com.blackrook.framework.BRRootServlet;
import com.blackrook.framework.BRToolkit;

/**
 * Base servlet for all entry points into Black Rook framework servlets.
 * All servlets that use the framework should extend this one.
 * The methods {@link #onGet(HttpServletRequest, HttpServletResponse)}, 
 * {@link #onPost(HttpServletRequest, HttpServletResponse)},
 * {@link #onMultiformPost(HttpServletRequest, HttpServletResponse, FileItem[], HashMap)},
 * {@link #onHead(HttpServletRequest, HttpServletResponse)},
 * {@link #onDelete(HttpServletRequest, HttpServletResponse)}, and
 * {@link #onPut(HttpServletRequest, HttpServletResponse)}
 * all send HTTP 405 status codes by default.
 * @author Matthew Tropiano
 */
public abstract class BRCommonServlet extends BRRootServlet
{
	private static final long serialVersionUID = -5794345293732460631L;

	/** Default Servlet Thread Pool. */
	private String defaultThreadPool;
	
	/**
	 * Base constructor. Sets default pools to default names.
	 * @see {@link BRToolkit#DEFAULT_POOL_NAME}
	 */
	protected BRCommonServlet()
	{
		this(BRToolkit.DEFAULT_POOL_NAME);
		}

	/**
	 * Other constructor. Sets default pools.
	 */
	protected BRCommonServlet(String defaultThreadPoolName)
	{
		super();
		defaultThreadPool = defaultThreadPoolName;
		}

	/**
	 * Gets the name of the default thread pool that this servlet uses.
	 */
	public String getDefaultThreadPool()
	{
		return defaultThreadPool;
		}

	/**
	 * Sets the name of the default thread pool that this servlet uses.
	 */
	public void setDefaultThreadPool(String servletDefaultThreadPool)
	{
		this.defaultThreadPool = servletDefaultThreadPool;
		}

	@Override
	public void onGet(HttpServletRequest request, HttpServletResponse response)
	{
		sendError(response, 405, "Servlet does not support this method.");
		}

	@Override
	public void onPost(HttpServletRequest request, HttpServletResponse response)
	{
		sendError(response, 405, "Servlet does not support this method.");
		}

	@Override
	public void onMultiformPost(HttpServletRequest request, HttpServletResponse response, FileItem[] fileItems, HashMap<String, String> paramMap)
	{
		sendError(response, 405, "Servlet does not support this method.");
		}

	@Override
	public void onHead(HttpServletRequest request, HttpServletResponse response)
	{
		sendError(response, 405, "Servlet does not support this method.");
		}

	@Override
	public void onPut(HttpServletRequest request, HttpServletResponse response)
	{
		sendError(response, 405, "Servlet does not support this method.");
		}

	@Override
	public void onDelete(HttpServletRequest request, HttpServletResponse response)
	{
		sendError(response, 405, "Servlet does not support this method.");
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

}
