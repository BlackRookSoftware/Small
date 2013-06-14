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

import org.apache.commons.fileupload.FileItem;

import com.blackrook.commons.hash.HashMap;
import com.blackrook.framework.BRController;

/**
 * Base servlet for all entry points into Black Rook Framework servlets.
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
	public void onMultiformPost(HttpServletRequest request, HttpServletResponse response)
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
