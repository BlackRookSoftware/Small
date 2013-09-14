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
import com.blackrook.framework.BRController;
import com.blackrook.framework.multipart.Part;
import com.blackrook.lang.json.JSONObject;
import com.blackrook.lang.xml.XMLStruct;

/**
 * Base servlet for all entry points into Black Rook Framework servlets.
 * All servlets that use the framework should extend this one.
 * The methods {@link #onGet(HttpServletRequest, HttpServletResponse)}, 
 * {@link #onPost(HttpServletRequest, HttpServletResponse)},
 * {@link #onMultipartPost(HttpServletRequest, HttpServletResponse)},
 * {@link #onHead(HttpServletRequest, HttpServletResponse)},
 * {@link #onDelete(HttpServletRequest, HttpServletResponse)}, and
 * {@link #onPut(HttpServletRequest, HttpServletResponse)}
 * all send HTTP 405 status codes by default.
 * @author Matthew Tropiano
 */
public class BRCommonController extends BRController
{
	@Override
	public void onGet(HttpServletRequest request, HttpServletResponse response)
	{
		sendCode(response, 405, "Servlet does not support this method.");
		}

	@Override
	public void onPost(HttpServletRequest request, HttpServletResponse response)
	{
		sendCode(response, 405, "Servlet does not support this method.");
		}

	@Override
	public void onMultipart(HttpServletRequest request, HttpServletResponse response, List<Part> partList)
	{
		sendCode(response, 405, "Servlet does not support this method.");
		}

	@Override
	public void onJSON(HttpServletRequest request, HttpServletResponse response, JSONObject json)
	{
		sendCode(response, 405, "Servlet does not support this method.");
		}

	@Override
	public void onXML(HttpServletRequest request, HttpServletResponse response, XMLStruct xml)
	{
		sendCode(response, 405, "Servlet does not support this method.");
		}

	@Override
	public void onHead(HttpServletRequest request, HttpServletResponse response)
	{
		sendCode(response, 405, "Servlet does not support this method.");
		}

	@Override
	public void onPut(HttpServletRequest request, HttpServletResponse response)
	{
		sendCode(response, 405, "Servlet does not support this method.");
		}

	@Override
	public void onDelete(HttpServletRequest request, HttpServletResponse response)
	{
		sendCode(response, 405, "Servlet does not support this method.");
		}

}
