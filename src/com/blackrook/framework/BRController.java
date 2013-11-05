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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.framework.multipart.Part;
import com.blackrook.lang.json.JSONObject;
import com.blackrook.lang.xml.XMLStruct;

/**
 * Controller type for all controller entry type.
 * @author Matthew Tropiano
 */
public abstract class BRController extends BRControlComponent
{
	/**
	 * The default entry point for all servlets on a GET request call.
	 * This is called if no matching entry points exist for the desired request method
	 * and file.
	 * Unless overridden, this returns status 405, Method Not Supported.
	 * @param file the requested file (portion of URL - can be blank).
	 * @param request servlet request object.
	 * @param response servlet response object.
	 */
	public void onGet(String file, HttpServletRequest request, HttpServletResponse response)
	{
		sendCode(response, 405, "Servlet does not support this method.");
		}

	/**
	 * The default entry point for all Black Rook Framework Servlets on a POST request call.
	 * This is called if no matching entry points exist for the desired request method
	 * and file.
	 * Unless overridden, this returns status 405, Method Not Supported.
	 * @param file the requested file (portion of URL - can be blank).
	 * @param request servlet request object.
	 * @param response servlet response object.
	 */
	public void onPost(String file, HttpServletRequest request, HttpServletResponse response)
	{
		sendCode(response, 405, "Servlet does not support this method.");
		}

	/**
	 * The default entry point for all Black Rook Framework Servlets on a POST request call 
	 * and it contains a multipart request.
	 * This is called if no matching entry points exist for the desired request method
	 * and file.
	 * Unless overridden, this returns status 405, Method Not Supported.
	 * @param file the requested file (portion of URL - can be blank).
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param parts the list of parts parsed out of the request.
	 */
	public void onMultipart(String file, HttpServletRequest request, HttpServletResponse response, Part[] parts)
	{
		sendCode(response, 405, "Servlet does not support this method.");
		}


	/**
	 * The default entry point for all Black Rook Framework Servlets on a POST request call 
	 * and it contains a JSON content request.
	 * This is called if no matching entry points exist for the desired request method
	 * and file.
	 * Unless overridden, this returns status 405, Method Not Supported.
	 * @param file the requested file (portion of URL - can be blank).
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param json the parsed JSON request.
	 */
	public void onJSON(String file, HttpServletRequest request, HttpServletResponse response, JSONObject json)
	{
		sendCode(response, 405, "Servlet does not support this method.");
		}


	/**
	 * The default entry point for all Black Rook Framework Servlets on a POST request call 
	 * and it contains an XML content request.
	 * This is called if no matching entry points exist for the desired request method
	 * and file.
	 * Unless overridden, this returns status 405, Method Not Supported.
	 * @param file the requested file (portion of URL - can be blank).
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param xml the parsed XML request.
	 */
	public void onXML(String file, HttpServletRequest request, HttpServletResponse response, XMLStruct xml)
	{
		sendCode(response, 405, "Servlet does not support this method.");
		}


	/**
	 * The default entry point for all Black Rook Framework Servlets on a HEAD request call.
	 * This is called if no matching entry points exist for the desired request method
	 * and file.
	 * Unless overridden, this returns status 405, Method Not Supported.
	 * @param file the requested file (portion of URL - can be blank).
	 * @param request servlet request object.
	 * @param response servlet response object.
	 */
	public void onHead(String file, HttpServletRequest request, HttpServletResponse response)
	{
		sendCode(response, 405, "Servlet does not support this method.");
		}


	/**
	 * The default entry point for all Black Rook Framework Servlets on a PUT request call.
	 * This is called if no matching entry points exist for the desired request method
	 * and file.
	 * Unless overridden, this returns status 405, Method Not Supported.
	 * @param file the requested file (portion of URL - can be blank).
	 * @param request servlet request object.
	 * @param response servlet response object.
	 */
	public void onPut(String file, HttpServletRequest request, HttpServletResponse response)
	{
		sendCode(response, 405, "Servlet does not support this method.");
		}


	/**
	 * The default entry point for all Black Rook Framework Servlets on a DELETE request call.
	 * This is called if no matching entry points exist for the desired request method
	 * and file.
	 * Unless overridden, this returns status 405, Method Not Supported.
	 * @param file the requested file (portion of URL - can be blank).
	 * @param request servlet request object.
	 * @param response servlet response object.
	 */
	public void onDelete(String file, HttpServletRequest request, HttpServletResponse response)
	{
		sendCode(response, 405, "Servlet does not support this method.");
		}

}
