package com.blackrook.framework;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.framework.multipart.Part;
import com.blackrook.lang.json.JSONObject;
import com.blackrook.lang.xml.XMLStruct;

/**
 * All filters.
 * @author Matthew Tropiano
 */
public abstract class BRFilter extends BRControlComponent
{
	/**
	 * Called to figure out the stuff to do from a standard HTTP request: GET, POST, HEAD, PUT, DELETE.
	 * Usually, this filter checks if some conditions are satisfied before continuing on to the next
	 * filter. If conditions are met, this should perform its necessary functions and return true. If not, return false.
	 * Does nothing and returns true unless overridden.
	 * @param file the requested file (portion of URL - can be blank).
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @return true to continue the filter chain, false otherwise.
	 */
	public boolean onFilter(String file, HttpServletRequest request, HttpServletResponse response)
	{
		return true;
		}

	/**
	 * Called to figure out the stuff to do with a POST Multipart HTTP request.
	 * Usually, this filter checks if some conditions are satisfied before continuing on to the next
	 * filter. If conditions are met, this should perform its necessary functions and return true. If not, return false.
	 * Does nothing and returns true unless overridden.
	 * @param file the requested file (portion of URL - can be blank).
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param parts the list of parts parsed out of the request.
	 * @return true to continue the filter chain, false otherwise.
	 */
	public boolean onFilterMultipart(String file, HttpServletRequest request, HttpServletResponse response, Part[] parts)
	{
		return true;
		}
	
	/**
	 * Called to figure out the stuff to do with a POST JSON HTTP request.
	 * Usually, this filter checks if some conditions are satisfied before continuing on to the next
	 * filter. If conditions are met, this should perform its necessary functions and return true. If not, return false.
	 * Does nothing and returns true unless overridden.
	 * @param file the requested file (portion of URL - can be blank).
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param json the parsed JSON request.
	 * @return true to continue the filter chain, false otherwise.
	 */
	public boolean onFilterJSON(String file, HttpServletRequest request, HttpServletResponse response, JSONObject json)
	{
		return true;
		}

	/**
	 * Called to figure out the stuff to do with a POST XML HTTP request.
	 * Usually, this filter checks if some conditions are satisfied before continuing on to the next
	 * filter. If conditions are met, this should perform its necessary functions and return true. If not, return false.
	 * Does nothing and returns true unless overridden.
	 * @param file the requested file (portion of URL - can be blank).
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param xml the parsed XML request.
	 * @return true to continue the filter chain, false otherwise.
	 */
	public boolean onFilterXML(String file, HttpServletRequest request, HttpServletResponse response, XMLStruct struct)
	{
		return true;
		}
}
