package com.blackrook.framework;

import java.io.IOException;
import java.util.Random;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.commons.Common;
import com.blackrook.framework.util.BRUtil;

/**
 * The root filter.
 * @author Matthew Tropiano
 */
public abstract class BRFilter
{
	/** Lag simulator seed. */
	private Random randomLagSimulator;
	/** Default Servlet Thread Pool. */
	private String defaultThreadPool;
	
	/** Default constructor. */
	protected BRFilter()
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
	 * Simulates latency on a response, for testing.
	 * Just calls {@link Common#sleep(long)} and varies the input value.
	 */
	protected final void simulateLag(int millis)
	{
		Common.sleep(randomLagSimulator.nextInt(millis));
		}

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
			BRUtil.throwException(e);
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
			BRUtil.throwException(e);
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
	 * Includes the output of a view in the response.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param key target view key to be resolved.
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
			BRUtil.throwException(e);
			}
		}

	/**
	 * Surreptitiously forwards the request to a view.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param key target view key to be resolved.
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
			BRUtil.throwException(e);
			}
		}

	/**
	 * Called to figure out the stuff to do from an HTTP request.
	 * Usually, this filter checks if some conditions are satisfied before continuing on to the next
	 * filter. If conditions are met, this should perform its necessary funtions and return true. If not, return false.
	 * Returns true unless overridden.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param chain the rest of the filter chain.
	 * @throws ServletException if a servlet exception occurs in this filter. 
	 * @throws IOException  if an I/O exception occurs in this filter.
	 * @return true to continue the filter chain, false otherwise.
	 */
	public abstract boolean onFilter(HttpServletRequest request, HttpServletResponse response);


}
