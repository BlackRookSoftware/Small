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
package com.blackrook.framework.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.blackrook.commons.list.List;
import com.blackrook.framework.BRControlComponent;
import com.blackrook.framework.multipart.Part;
import com.blackrook.lang.json.JSONObject;
import com.blackrook.lang.xml.XMLStruct;

/**
 * Root control servlet for the Black Rook J2EE Framework.
 * This does all of the pool management and other things that should remain
 * transparent to users of the framework. 
 * @author Matthew Tropiano
 */
public abstract class BRController extends BRControlComponent
{
	/**
	 * The entry point for all Black Rook Framework Servlets on a GET request call.
	 * Unless overridden, this returns status 405, Method Not Supported.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 */
	public void onGet(HttpServletRequest request, HttpServletResponse response)
	{
		sendCode(response, 405, "Servlet does not support this method.");
		}

	/**
	 * The entry point for all Black Rook Framework Servlets on a POST request call.
	 * Unless overridden, this returns status 405, Method Not Supported.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 */
	public void onPost(HttpServletRequest request, HttpServletResponse response)
	{
		sendCode(response, 405, "Servlet does not support this method.");
		}

	/**
	 * The entry point for all Black Rook Framework Servlets on a POST request call 
	 * and it contains a multipart request.
	 * Unless overridden, this returns status 405, Method Not Supported.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param partList the list of parts parsed out of the request.
	 */
	public void onMultipart(HttpServletRequest request, HttpServletResponse response, List<Part> partList)
	{
		sendCode(response, 405, "Servlet does not support this method.");
		}


	/**
	 * The entry point for all Black Rook Framework Servlets on a POST request call 
	 * and it contains a JSON content request.
	 * Unless overridden, this returns status 405, Method Not Supported.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param json the parsed JSON request.
	 */
	public void onJSON(HttpServletRequest request, HttpServletResponse response, JSONObject json)
	{
		sendCode(response, 405, "Servlet does not support this method.");
		}


	/**
	 * The entry point for all Black Rook Framework Servlets on a POST request call 
	 * and it contains an XML content request.
	 * Unless overridden, this returns status 405, Method Not Supported.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 * @param xml the parsed XML request.
	 */
	public void onXML(HttpServletRequest request, HttpServletResponse response, XMLStruct xml)
	{
		sendCode(response, 405, "Servlet does not support this method.");
		}


	/**
	 * The entry point for all Black Rook Framework Servlets on a HEAD request call.
	 * Unless overridden, this returns status 405, Method Not Supported.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 */
	public void onHead(HttpServletRequest request, HttpServletResponse response)
	{
		sendCode(response, 405, "Servlet does not support this method.");
		}


	/**
	 * The entry point for all Black Rook Framework Servlets on a PUT request call.
	 * Unless overridden, this returns status 405, Method Not Supported.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 */
	public void onPut(HttpServletRequest request, HttpServletResponse response)
	{
		sendCode(response, 405, "Servlet does not support this method.");
		}


	/**
	 * The entry point for all Black Rook Framework Servlets on a DELETE request call.
	 * Unless overridden, this returns status 405, Method Not Supported.
	 * @param request servlet request object.
	 * @param response servlet response object.
	 */
	public void onDelete(HttpServletRequest request, HttpServletResponse response)
	{
		sendCode(response, 405, "Servlet does not support this method.");
		}

}
