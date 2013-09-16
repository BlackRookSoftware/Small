package com.blackrook.framework.filter;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.framework.BRControlComponent;

/**
 * The root filter.
 * @author Matthew Tropiano
 */
public abstract class BRFilter extends BRControlComponent
{
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
	public boolean onFilter(HttpServletRequest request, HttpServletResponse response)
	{
		return true;
		}

}
