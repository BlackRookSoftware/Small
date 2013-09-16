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

import com.blackrook.framework.filter.BRFilter;

/**
 * Main dispatcher for filter portion of framework.
 * @author Matthew Tropiano
 */
public class BRDispatcherFilter implements Filter
{

	@Override
	public final void init(FilterConfig config) throws ServletException
	{
		BRToolkit.createToolkit(config.getServletContext());
		}

	@Override
	public final void destroy()
	{
		// Do nothing.
		}

	@Override
	public final void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse))
			return;
		
		HttpServletRequest httpRequest = (HttpServletRequest)request;
		HttpServletResponse httpResponse = (HttpServletResponse)response;
		
		String path = getPath(httpRequest);
		BRFilter[] filters = getFiltersUsingPath(path);

		int i = 0;
		boolean go = true;
		while (go && i < filters.length)
		{
			BRFilter filter = filters[i];
			go = filter.onFilter(httpRequest, httpResponse);
			i++;
			}
		if (go)
			chain.doFilter(request, response);

		}

	/**
	 * Get the base path parsed out of the request URI.
	 */
	private final String getPath(HttpServletRequest request)
	{
		String requestURI = request.getRequestURI();
		int contextPathLen = request.getContextPath().length();
		int queryIndex = requestURI.indexOf('?');
		if (queryIndex >= 0)
			return requestURI.substring(contextPathLen, queryIndex);
		else
			return requestURI.substring(contextPathLen); 
		}

	/**
	 * Gets the filter to call using the requested path.
	 * @param uriPath the path to resolve, no query string.
	 * @return a filter, or null if no filter using that path. 
	 * This servlet sends a 404 back if this happens.
	 * @throws BRFrameworkException if a huge error occurred.
	 */
	private final BRFilter[] getFiltersUsingPath(String uriPath)
	{
		return BRToolkit.INSTANCE.getFilters(uriPath);
		}

}
