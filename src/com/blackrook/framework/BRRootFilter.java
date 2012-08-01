package com.blackrook.framework;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The root filter.
 * @author Matthew Tropiano
 */
public abstract class BRRootFilter implements Filter
{
	/** Name for all default pools. */
	public static final String DEFAULT_POOL_NAME = "default";

	@Override
	public final void init(FilterConfig config) throws ServletException
	{
		BRToolkit.createToolkit(config.getServletContext());
		onInit(config);
		}

	@Override
	public final void destroy()
	{
		onDestroy();
		}

	@Override
	public final void doFilter(ServletRequest request, 
		ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		if (request instanceof HttpServletRequest && response instanceof HttpServletResponse)
			doHTTPFilter((HttpServletRequest)request, (HttpServletResponse)response, chain);
		else
			doOtherFilter(request, response, chain);
		}
	
	/**
	 * Gets the Black Rook Servlet Toolkit.
	 */
	public final BRToolkit getToolkit()
	{
		return BRToolkit.getInstance();
		}

	/**
	 * Initializes this filter.
	 * Does nothing unless overridden.
	 * @param config the filter configuration context.
	 */
	public void onInit(FilterConfig config)
	{
		// Do nothing.
		}
	
	/**
	 * Does the same thing as {@link #destroy()}, named <code>onDestroy</code> for
	 * consistency's sake.
	 * Does nothing unless overridden.
	 */
	public void onDestroy()
	{
		// Do nothing.
		}
	
	/**
	 * Called to figure out the stuff to do from an HTTP request.
	 * Just calls <code>chain.doFilter(request, response)</code> unless overridden.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param chain the rest of the filter chain.
	 * @throws ServletException if a servlet exception occurs in this filter. 
	 * @throws IOException  if an I/O exception occurs in this filter.
	 */
	public void doHTTPFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		chain.doFilter(request, response);
		}

	/**
	 * Called to figure out the stuff to do from a non-HTTP request.
	 * Just calls <code>chain.doFilter(request, response)</code> unless overridden.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param chain the rest of the filter chain.
	 * @throws ServletException if a servlet exception occurs in this filter. 
	 * @throws IOException  if an I/O exception occurs in this filter.
	 */
	public void doOtherFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		chain.doFilter(request, response);
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
			getToolkit().throwException(e);
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
			getToolkit().throwException(e);
			}
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
			getToolkit().throwException(e);
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
			getToolkit().throwException(e);
			}
		}
}
